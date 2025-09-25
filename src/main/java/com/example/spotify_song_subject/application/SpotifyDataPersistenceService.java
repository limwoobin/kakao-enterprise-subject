package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SimilarSongDto;
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
import java.util.*;

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

    /**
     * Map 데이터 리스트를 처리하고 저장
     * Map → DTO → Domain 변환 후 저장
     */
    public Mono<Void> processSongBatch(List<Map<String, Object>> songMaps) {
        return Flux.fromIterable(songMaps)
            .mapNotNull(SpotifyDataMapper::mapToSpotifySongDto)
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

            Mono<Artist> artistMono = artistRepository.findByName(artistName)
                .switchIfEmpty(
                    Mono.defer(() -> artistRepository.save(artist))
                );

            artistMonos.add(artistMono);
        }

        return Flux.merge(artistMonos).collectList();
    }

    /**
     * 앨범 처리
     * title + release_date로 중복 체크
     * (하나의 앨범은 여러 아티스트가 공유할 수 있으므로 artist_id를 포함하지 않음)
     */
    private Mono<Album> processAlbum(Album album) {
        String albumTitle = album.getTitle();

        return albumRepository.findByTitleAndReleaseDate(albumTitle, album.getReleaseDate())
            .switchIfEmpty(Mono.defer(() -> albumRepository.save(album)));
    }

    /**
     * 곡 처리
     */
    private Mono<Song> processSong(SpotifySongDto dto, Long albumId) {
        Song songToSave = SpotifyDomainMapper.convertToSong(dto, albumId);
        String title = songToSave.getTitle();

        return songRepository.findByTitleAndAlbumId(title, albumId)
            .switchIfEmpty(
                Mono.defer(() -> songRepository.save(songToSave))
            );
    }

    /**
     * 아티스트-곡 관계 저장
     * artist_id와 song_id의 조합으로 중복 체크
     * (song 자체에 album_id가 포함되어 있으므로 song_id로 충분함)
     */
    private Mono<Void> saveArtistSongRelations(List<Artist> artists, Song song) {
        return Flux.fromIterable(artists)
            .flatMap(artist ->
                artistSongRepository.findByArtistIdAndSongId(artist.getId(), song.getId())
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
                artistAlbumRepository.findByArtistIdAndAlbumId(artist.getId(), album.getId())
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
     * Similar Songs 처리 - artist name과 song title을 직접 저장
     */
    private Mono<Void> processSimilarSongs(SpotifySongDto dto, Song song) {
        if (dto.getSimilarSongs() == null || dto.getSimilarSongs().isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(dto.getSimilarSongs())
            .flatMap(similarDto -> saveSimilarSongRelation(song.getId(), similarDto))
            .then();
    }

    /**
     * Similar Song 관계 저장 - 중복 체크 후 저장
     */
    private Mono<Void> saveSimilarSongRelation(Long songId, SimilarSongDto similarDto) {
        String artistName = similarDto.getArtistName();
        String songTitle = similarDto.getSongTitle();
        BigDecimal score = similarDto.getSimilarityScore();

        return similarSongRepository.findBySongIdAndSimilarInfo(songId, artistName, songTitle)
            .switchIfEmpty(
                Mono.defer(() -> {
                    SimilarSong relation = SpotifyDomainMapper.createSimilarSong(
                        songId, artistName, songTitle, score
                    );
                    return similarSongRepository.save(relation);
                })
            )
            .then()
            .doOnSuccess(v -> log.debug("Saved similar song relation: {} - {} (score: {})",
                artistName, songTitle, score));
    }

}
