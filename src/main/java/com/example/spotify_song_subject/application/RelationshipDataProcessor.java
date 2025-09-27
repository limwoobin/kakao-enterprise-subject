package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SimilarSongDto;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import com.example.spotify_song_subject.mapper.SpotifyDomainMapper;
import com.example.spotify_song_subject.repository.bulk.ArtistAlbumBulkRepository;
import com.example.spotify_song_subject.repository.bulk.ArtistSongBulkRepository;
import com.example.spotify_song_subject.repository.bulk.SimilarSongBulkRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Song과 관련된 관계 데이터를 처리하는 유틸리티 클래스
 * ArtistSong, ArtistAlbum, SimilarSong 관계 데이터를 생성하고 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationshipDataProcessor {

    private final ArtistSongBulkRepository artistSongBulkRepository;
    private final ArtistAlbumBulkRepository artistAlbumBulkRepository;
    private final SimilarSongBulkRepository similarSongBulkRepository;

    /**
     * Song 관계 데이터를 일괄 생성
     */
    @Builder
    public record RelationshipData(List<ArtistSong> artistSongs,
                                   List<ArtistAlbum> artistAlbums,
                                   List<SimilarSong> similarSongs) {
        public static RelationshipData empty() {
            return RelationshipData.builder()
                .artistSongs(new ArrayList<>())
                .artistAlbums(new ArrayList<>())
                .similarSongs(new ArrayList<>())
                .build();
        }
    }

    /**
     * 저장된 Song들로부터 관계 데이터 생성
     */
    public static RelationshipData buildRelationships(List<Song> savedSongs,
                                                      List<SpotifySongDto> originalDtos,
                                                      Map<String, Artist> artistsMap,
                                                      Map<String, Album> albumsMap,
                                                      Map<Integer, String> songIndexToAlbumKey) {

        if (savedSongs.isEmpty()) {
            return RelationshipData.empty();
        }

        List<ArtistSong> artistSongs = new ArrayList<>();
        List<ArtistAlbum> artistAlbums = new ArrayList<>();
        List<SimilarSong> similarSongs = new ArrayList<>();
        Set<String> artistAlbumKeys = new HashSet<>();

        for (int i = 0; i < savedSongs.size(); i++) {
            Song song = savedSongs.get(i);
            SpotifySongDto dto = findOriginalDto(i, originalDtos, songIndexToAlbumKey);

            if (dto == null) {
                log.warn("Could not find original DTO for saved song at index {}", i);
                continue;
            }

            artistSongs.addAll(buildArtistSongRelationships(song, dto, artistsMap));
            List<ArtistAlbum> artistAlbumsItems = buildArtistAlbumRelationships(
                dto,
                artistsMap,
                albumsMap,
                songIndexToAlbumKey.get(i),
                artistAlbumKeys
            );
            artistAlbums.addAll(artistAlbumsItems);
            similarSongs.addAll(buildSimilarSongRelationships(song, dto));
        }

        return RelationshipData.builder()
            .artistSongs(artistSongs)
            .artistAlbums(artistAlbums)
            .similarSongs(similarSongs)
            .build();
    }

    /**
     * 원본 DTO 찾기
     */
    private static SpotifySongDto findOriginalDto(int index,
                                                  List<SpotifySongDto> originalDtos,
                                                  Map<Integer, String> songIndexToAlbumKey) {

        Integer dtoIndex = songIndexToAlbumKey.keySet().stream()
            .filter(songIndexToAlbumKey::containsKey)
            .skip(index)
            .findFirst()
            .orElse(index);

        if (dtoIndex >= 0 && dtoIndex < originalDtos.size()) {
            return originalDtos.get(dtoIndex);
        }

        return null;
    }

    /**
     * ArtistSong 관계 생성
     */
    private static List<ArtistSong> buildArtistSongRelationships(Song song,
                                                                 SpotifySongDto dto,
                                                                 Map<String, Artist> artistsMap) {

        List<ArtistSong> artistSongs = new ArrayList<>();
        List<Artist> artists = SpotifyDomainMapper.extractArtists(dto);

        for (Artist artistDto : artists) {
            Artist artist = artistsMap.get(artistDto.getName());
            if (artist != null) {
                ArtistSong artistSong = SpotifyDomainMapper.createArtistSong(artist.getId(), song.getId());
                artistSongs.add(artistSong);
            }
        }

        return artistSongs;
    }

    /**
     * ArtistAlbum 관계 생성 (중복 제거 포함)
     */
    private static List<ArtistAlbum> buildArtistAlbumRelationships(SpotifySongDto dto,
                                                                   Map<String, Artist> artistsMap,
                                                                   Map<String, Album> albumsMap,
                                                                   String albumKey,
                                                                   Set<String> artistAlbumKeys) {
        List<ArtistAlbum> artistAlbums = new ArrayList<>();
        if (albumKey == null) {
            return artistAlbums;
        }

        Album album = albumsMap.get(albumKey);
        if (album == null) {
            return artistAlbums;
        }

        List<Artist> artists = SpotifyDomainMapper.extractArtists(dto);

        for (Artist artistDto : artists) {
            Artist artist = artistsMap.get(artistDto.getName());
            if (artist != null) {
                String uniqueKey = artist.getId() + "-" + album.getId();
                if (artistAlbumKeys.add(uniqueKey)) {
                    ArtistAlbum artistAlbum = SpotifyDomainMapper.createArtistAlbum(artist.getId(), album.getId());
                    artistAlbums.add(artistAlbum);
                }
            }
        }

        return artistAlbums;
    }

    /**
     * SimilarSong 관계 생성
     */
    private static List<SimilarSong> buildSimilarSongRelationships(Song song,
                                                                   SpotifySongDto dto) {
        List<SimilarSong> similarSongs = new ArrayList<>();
        if (dto.getSimilarSongs() == null || dto.getSimilarSongs().isEmpty()) {
            return similarSongs;
        }

        for (SimilarSongDto similar : dto.getSimilarSongs()) {
            SimilarSong similarSong = SpotifyDomainMapper.createSimilarSong(
                song.getId(),
                similar.getArtistName(),
                similar.getSongTitle(),
                similar.getSimilarityScore()
            );

            similarSongs.add(similarSong);
        }

        return similarSongs;
    }

    /**
     * ArtistSong 관계 데이터를 일괄 저장
     */
    public Mono<Long> bulkInsertArtistSongs(Collection<ArtistSong> artistSongs) {
        if (artistSongs.isEmpty()) {
            return Mono.just(0L);
        }
        return artistSongBulkRepository.bulkInsert(artistSongs);
    }

    /**
     * ArtistAlbum 관계 데이터를 일괄 저장
     */
    public Mono<Long> bulkInsertArtistAlbums(Collection<ArtistAlbum> artistAlbums) {
        if (artistAlbums.isEmpty()) {
            return Mono.just(0L);
        }
        return artistAlbumBulkRepository.bulkInsert(artistAlbums);
    }

    /**
     * SimilarSong 관계 데이터를 일괄 저장
     */
    public Mono<Long> bulkInsertSimilarSongs(Collection<SimilarSong> similarSongs) {
        if (similarSongs.isEmpty()) {
            return Mono.just(0L);
        }
        return similarSongBulkRepository.bulkInsert(similarSongs);
    }
}