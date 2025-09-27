package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.repository.AlbumRepository;
import com.example.spotify_song_subject.repository.bulk.AlbumBulkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Album 배치 처리를 담당하는 전용 프로세서
 * 개별 save 대신 bulk insert를 사용하도록 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumBatchProcessor {

    private final AlbumRepository albumRepository;
    private final AlbumBulkRepository albumBulkRepository;

    /**
     * Albums 배치 처리 - Bulk Insert 최적화 버전
     * 1. 기존 앨범 조회
     * 2. 새 앨범 필터링
     * 3. Bulk Insert
     */
    public Mono<Map<String, Album>> processAlbumsBatch(Map<String, Set<AlbumInfo>> albumsByTitle) {
        if (albumsByTitle.isEmpty()) {
            return Mono.just(Collections.emptyMap());
        }

        List<AlbumKey> albumKeys = buildAlbumKeys(albumsByTitle);
        return findExistingAlbums(albumKeys)
            .flatMap(existingAlbums -> insertNewAlbums(albumKeys, existingAlbums))
            .map(this::createAlbumMap);
    }

    /**
     * AlbumKey 생성 - title, releaseDate, artistName의 조합
     */
    private List<AlbumKey> buildAlbumKeys(Map<String, Set<AlbumInfo>> albumsByTitle) {
        List<AlbumKey> albumKeys = new ArrayList<>();

        for (Map.Entry<String, Set<AlbumInfo>> entry : albumsByTitle.entrySet()) {
            String title = entry.getKey();
            for (AlbumInfo info : entry.getValue()) {
                albumKeys.add(new AlbumKey(title, info.releaseDate(), info.artistName()));
            }
        }

        return albumKeys;
    }

    /**
     * 기존 앨범 조회 - IN 절을 사용한 최적화
     */
    private Mono<Map<AlbumKey, Album>> findExistingAlbums(List<AlbumKey> albumKeys) {
        if (albumKeys.isEmpty()) {
            return Mono.just(Collections.emptyMap());
        }

        Set<String> titles = albumKeys.stream()
            .map(AlbumKey::title)
            .collect(Collectors.toSet());

        return albumRepository.findAllByTitleIn(titles)
            .collectList()
            .map(albums -> {
                Map<String, Album> albumsByKey = new HashMap<>();
                for (Album album : albums) {
                    String dbKey = album.getTitle() + "|" + album.getReleaseDate() + "|" + album.getArtistName();
                    albumsByKey.put(dbKey, album);
                }

                Map<AlbumKey, Album> resultMap = new HashMap<>();
                for (AlbumKey key : albumKeys) {
                    String lookupKey = key.title() + "|" + key.releaseDate() + "|" + key.artistName();
                    Album matchedAlbum = albumsByKey.get(lookupKey);
                    if (matchedAlbum != null) {
                        resultMap.put(key, matchedAlbum);
                    }
                }

                return resultMap;
            });
    }

    /**
     * 새로운 앨범 일괄 삽입
     */
    private Mono<List<Album>> insertNewAlbums(List<AlbumKey> allKeys,
                                              Map<AlbumKey, Album> existingAlbums) {

        List<Album> newAlbums = allKeys.stream()
            .filter(key -> !existingAlbums.containsKey(key))
            .map(key -> Album.of(key.title(), key.releaseDate(), key.artistName()))
            .collect(Collectors.toList());

        if (newAlbums.isEmpty()) {
            return Mono.just(new ArrayList<>(existingAlbums.values()));
        }

        return albumBulkRepository.bulkInsert(newAlbums)
            .flatMap(count -> reloadAlbums(allKeys));
    }

    /**
     * 모든 앨범 재조회 (삽입 후 ID 포함된 데이터 가져오기)
     * IN 절을 사용하여 한 번에 조회
     */
    private Mono<List<Album>> reloadAlbums(List<AlbumKey> albumKeys) {
        if (albumKeys.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        Set<String> titles = albumKeys.stream()
            .map(AlbumKey::title)
            .collect(Collectors.toSet());

        Set<String> validKeys = albumKeys.stream()
            .map(key -> key.title() + "|" + key.releaseDate() + "|" + key.artistName())
            .collect(Collectors.toSet());

        return albumRepository.findAllByTitleIn(titles)
            .filter(album -> {
                String albumKey = album.getTitle() + "|" + album.getReleaseDate() + "|" + album.getArtistName();
                return validKeys.contains(albumKey);
            }).collectList();
    }

    /**
     * Album 리스트를 Map으로 변환
     */
    private Map<String, Album> createAlbumMap(List<Album> albums) {
        Map<String, Album> albumMap = new HashMap<>();

        for (Album album : albums) {
            String key = album.getTitle() + "|" + album.getReleaseDate() + "|" + album.getArtistName();
            albumMap.put(key, album);
        }

        return albumMap;
    }

    /**
     * Album 키를 표현하는 내부 클래스
     */
    private record AlbumKey(String title, LocalDate releaseDate, String artistName) {}
    
    /**
     * Album 정보를 표현하는 클래스
     */
    public record AlbumInfo(LocalDate releaseDate, String artistName) {}

}
