package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import com.example.spotify_song_subject.mapper.SpotifyDataMapper;
import com.example.spotify_song_subject.mapper.SpotifyDomainMapper;
import com.example.spotify_song_subject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spotify 데이터를 도메인 엔티티로 변환하고 저장하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyDataPersistenceService {

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final ArtistSongRepository artistSongRepository;
    private final ArtistAlbumRepository artistAlbumRepository;
    private final SimilarSongRepository similarSongRepository;
    private final TransactionalOperator transactionalOperator;

    private final Map<String, Artist> artistCache = new ConcurrentHashMap<>();
    private final Map<String, Album> albumCache = new ConcurrentHashMap<>();
    private final Map<String, Song> songCache = new ConcurrentHashMap<>();

    /**
     * Map 데이터 리스트를 처리하고 저장
     * Map → DTO → Domain 변환 후 저장
     */
    public Mono<Void> processSongBatch(List<Map<String, Object>> songMaps) {
        return Flux.fromIterable(songMaps)
            .map(SpotifyDataMapper::mapToSpotifySongDto)
            .flatMap(this::processSingleSong)
            .then()
            .as(transactionalOperator::transactional)
            .doOnError(e -> log.error("Error processing song batch", e));
    }

    /**
     * 단일 곡 데이터 처리 및 저장
     */
    private Mono<Void> processSingleSong(SpotifySongDto dto) {
        List<Artist> artistsToProcess = SpotifyDomainMapper.extractArtists(dto);
        Album albumToProcess = SpotifyDomainMapper.extractAlbum(dto);

        Mono<List<Artist>> artistsMono = processArtists(artistsToProcess);
        Mono<Album> albumMono = processAlbum(albumToProcess);

        return Mono.zip(artistsMono, albumMono)
            .flatMap(tuple -> {
                List<Artist> artists = tuple.getT1();
                Album album = tuple.getT2();

                return processSong(dto, album.getId())
                    .flatMap(song -> {
                        Mono<Void> artistSongRelations = saveArtistSongRelations(artists, song);
                        Mono<Void> artistAlbumRelations = saveArtistAlbumRelations(artists, album);
                        Mono<Void> similarSongRelations = processSimilarSongs(dto, song);
                        return Mono.when(artistSongRelations, artistAlbumRelations, similarSongRelations);
                    });
            }).then();
    }

    /**
     * 아티스트 처리 (복수 아티스트 지원)
     */
    private Mono<List<Artist>> processArtists(List<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        List<Mono<Artist>> artistMonos = new ArrayList<>();

        for (Artist artist : artists) {
            String artistName = artist.getName();

            if (artistCache.containsKey(artistName)) {
                artistMonos.add(Mono.just(artistCache.get(artistName)));
            } else {
                Mono<Artist> artistMono = artistRepository.findByNameAndDeletedAtIsNull(artistName)
                    .switchIfEmpty(
                        Mono.defer(() -> artistRepository.save(artist))
                    )
                    .doOnNext(saved -> artistCache.put(artistName, saved));

                artistMonos.add(artistMono);
            }
        }

        return Flux.merge(artistMonos).collectList();
    }

    /**
     * 앨범 처리
     */
    private Mono<Album> processAlbum(Album album) {
        String albumTitle = album.getTitle();
        String cacheKey = albumTitle + "_" + album.getReleaseDate();

        if (albumCache.containsKey(cacheKey)) {
            return Mono.just(albumCache.get(cacheKey));
        }

        return albumRepository.findByTitleAndReleaseDateAndDeletedAtIsNull(albumTitle, album.getReleaseDate())
            .switchIfEmpty(Mono.defer(() -> albumRepository.save(album)))
            .doOnNext(saved -> albumCache.put(cacheKey, saved));
    }

    /**
     * 곡 처리
     */
    private Mono<Song> processSong(SpotifySongDto dto, Long albumId) {
        Song songToSave = SpotifyDomainMapper.convertToSong(dto, albumId);
        String title = songToSave.getTitle();

        String cacheKey = title + "_" + albumId;

        // 캐시에서 먼저 확인
        if (songCache.containsKey(cacheKey)) {
            return Mono.just(songCache.get(cacheKey));
        }

        return songRepository.findByTitleAndAlbumIdAndDeletedAtIsNull(title, albumId)
            .switchIfEmpty(
                Mono.defer(() -> songRepository.save(songToSave))
            )
            .doOnNext(song -> songCache.put(cacheKey, song));
    }

    /**
     * 아티스트-곡 관계 저장
     */
    private Mono<Void> saveArtistSongRelations(List<Artist> artists, Song song) {
        return Flux.fromIterable(artists)
            .flatMap(artist ->
                artistSongRepository.findByArtistIdAndSongIdAndDeletedAtIsNull(artist.getId(), song.getId())
                    .switchIfEmpty(
                        Mono.defer(() -> {
                            ArtistSong artistSong = SpotifyDomainMapper.createArtistSong(artist.getId(), song.getId());
                            return artistSongRepository.save(artistSong);
                        })
                    )
            )
            .then();
    }

    /**
     * 아티스트-앨범 관계 저장
     */
    private Mono<Void> saveArtistAlbumRelations(List<Artist> artists, Album album) {
        return Flux.fromIterable(artists)
            .flatMap(artist ->
                artistAlbumRepository.findByArtistIdAndAlbumIdAndDeletedAtIsNull(artist.getId(), album.getId())
                    .switchIfEmpty(
                        Mono.defer(() -> {
                            ArtistAlbum artistAlbum = SpotifyDomainMapper.createArtistAlbum(artist.getId(), album.getId());
                            return artistAlbumRepository.save(artistAlbum);
                        })
                    )
            )
            .then();
    }

    /**
     * Similar Songs 처리
     */
    private Mono<Void> processSimilarSongs(SpotifySongDto dto, Song song) {
        if (dto.getSimilarSongs() == null || dto.getSimilarSongs().isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(dto.getSimilarSongs())
            .flatMap(similarDto -> findSimilarSong(similarDto)
            .flatMap(similarSong -> saveSimilarSongRelation(song.getId(), similarSong.getId(), similarDto.getSimilarityScore())))
            .then();
    }

    /**
     * Similar Song 찾기
     */
    private Mono<Song> findSimilarSong(com.example.spotify_song_subject.dto.SimilarSongDto similarDto) {
        // Similar Song을 title로만 찾기 (간단한 매칭)
        return songRepository.findByTitleAndDeletedAtIsNull(similarDto.getSongTitle())
            .collectList()
            .flatMap(songs -> {
                if (songs.isEmpty()) {
                    // Similar Song이 없으면 스킵
                    return Mono.empty();
                }
                // 첫 번째 매칭된 곡 반환
                return Mono.just(songs.get(0));
            });
    }

    /**
     * Similar Song 관계 저장
     */
    private Mono<Void> saveSimilarSongRelation(Long songId, Long similarSongId, BigDecimal score) {
        return similarSongRepository.findBySongIdAndSimilarSongIdAndDeletedAtIsNull(songId, similarSongId)
            .switchIfEmpty(
                Mono.defer(() -> {
                    SimilarSong relation = SimilarSong.builder()
                        .songId(songId)
                        .similarSongId(similarSongId)
                        .similarityScore(normalizeScore(score))
                        .build();
                    return similarSongRepository.save(relation);
                })
            )
            .then();
    }

    /**
     * 유사도 점수 정규화 (0~1 범위)
     */
    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }

        if (score.compareTo(BigDecimal.ONE) <= 0) {
            return score;
        }

        return BigDecimal.ONE;
    }

}
