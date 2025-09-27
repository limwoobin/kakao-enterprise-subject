package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import com.example.spotify_song_subject.mapper.SpotifyDomainMapper;
import com.example.spotify_song_subject.repository.SongRepository;
import com.example.spotify_song_subject.repository.bulk.SongBulkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Song 배치 처리를 담당하는 전용 프로세서
 * Bulk Insert를 통한 최적화된 대량 처리 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SongBatchProcessor {

    private final SongRepository songRepository;
    private final SongBulkRepository songBulkRepository;

    /**
     * Songs 배치 처리 및 저장
     */
    public Mono<SongProcessResult> processSongsBatch(List<SpotifySongDto> songDtos, 
                                                     Map<String, Album> albumsMap) {
        if (songDtos.isEmpty()) {
            return Mono.just(new SongProcessResult(Collections.emptyList(), Collections.emptyMap()));
        }

        PreparedSongs preparedSongs = prepareSongs(songDtos, albumsMap);
        if (preparedSongs.songs().isEmpty()) {
            log.warn("No valid songs to process after album mapping");
            return Mono.just(new SongProcessResult(Collections.emptyList(), Collections.emptyMap()));
        }

        return songBulkRepository.bulkInsert(preparedSongs.songs())
            .doOnNext(count -> log.info("Bulk inserted {} songs", count))
            .then(Mono.defer(() -> reloadSavedSongsByAlbumIds(preparedSongs.songs())))
            .map(savedSongs -> new SongProcessResult(savedSongs, preparedSongs.songIndexToAlbumKey()));
    }

    /**
     * Song 엔티티 준비
     */
    private PreparedSongs prepareSongs(List<SpotifySongDto> songDtos, 
                                       Map<String, Album> albumsMap) {
        List<Song> songsToSave = new ArrayList<>();
        Map<Integer, String> songIndexToAlbumKey = new HashMap<>();

        for (int i = 0; i < songDtos.size(); i++) {
            SpotifySongDto dto = songDtos.get(i);
            String albumKey = createAlbumKey(dto);
            Album album = albumsMap.get(albumKey);

            if (album != null) {
                Song song = SpotifyDomainMapper.convertToSong(dto, album.getId());
                songsToSave.add(song);
                songIndexToAlbumKey.put(i, albumKey);
            } else {
                Song song = SpotifyDomainMapper.convertToSong(dto, null);
                songsToSave.add(song);
                songIndexToAlbumKey.put(i, null);
            }
        }

        return new PreparedSongs(songsToSave, songIndexToAlbumKey);
    }


    /**
     * 저장된 Song들을 다시 조회하여 ID 포함된 객체 반환
     * title로 모든 song 조회 (album_id가 null인 것도 포함)
     */
    private Mono<List<Song>> reloadSavedSongsByAlbumIds(List<Song> songsToSave) {
        if (songsToSave.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        
        // 모든 title을 수집
        Set<String> titles = songsToSave.stream()
            .map(Song::getTitle)
            .collect(Collectors.toSet());
        
        // title로 모든 song 조회 (album_id가 null인 것도 포함)
        return songRepository.findAllByTitleIn(titles)
            .collectList()
            .map(dbSongs -> {
                // DB에서 조회한 Song들을 키로 매핑
                Map<String, Song> dbSongMap = new HashMap<>();
                for (Song dbSong : dbSongs) {
                    String key;
                    if (dbSong.getAlbumId() != null) {
                        key = createSongKey(dbSong.getAlbumId(), dbSong.getTitle());
                    } else {
                        key = "null|" + dbSong.getTitle();
                    }
                    dbSongMap.put(key, dbSong);
                }
                
                // 저장하려던 Song들과 매칭
                List<Song> resultSongs = new ArrayList<>();
                for (Song savedSong : songsToSave) {
                    String key;
                    if (savedSong.getAlbumId() != null) {
                        key = createSongKey(savedSong.getAlbumId(), savedSong.getTitle());
                    } else {
                        key = "null|" + savedSong.getTitle();
                    }
                    
                    Song dbSong = dbSongMap.get(key);
                    if (dbSong != null) {
                        resultSongs.add(dbSong);
                    } else {
                        log.warn("Could not find saved song in DB: {}", savedSong.getTitle());
                    }
                }
                
                return resultSongs;
            });
    }
    
    /**
     * Song의 고유 키 생성 (albumId + title)
     */
    private String createSongKey(Long albumId, String title) {
        return albumId + "|" + title;
    }
    
    /**
     * 앨범 키 생성 (title|releaseDate|artistName)
     */
    private String createAlbumKey(SpotifySongDto dto) {
        LocalDate releaseDate = SpotifyDomainMapper.parseReleaseDate(dto.getReleaseDate());
        String artistName = extractAllArtists(dto);
        return dto.getAlbumTitle() + "|" + releaseDate + "|" + artistName;
    }
    
    /**
     * DTO에서 전체 아티스트 이름 추출 (앨범용)
     * 앨범에는 모든 아티스트가 포함되어야 함 (예: "A, B, C")
     */
    private String extractAllArtists(SpotifySongDto dto) {
        if (dto.getArtists() == null || dto.getArtists().isEmpty()) {
            return "Unknown Artist";
        }

        return dto.getArtists().trim();
    }

    /**
     * Song 처리 결과
     */
    public record SongProcessResult(List<Song> savedSongs, Map<Integer, String> songIndexToAlbumKey) {}

    /**
     * 준비된 Songs 정보
     */
    private record PreparedSongs(List<Song> songs, Map<Integer, String> songIndexToAlbumKey) {}

}
