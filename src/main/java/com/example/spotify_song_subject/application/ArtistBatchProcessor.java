package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Artist;
import com.example.spotify_song_subject.repository.ArtistRepository;
import com.example.spotify_song_subject.repository.bulk.ArtistBulkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Artist 배치 처리를 담당하는 전용 프로세서
 * 개별 save 대신 bulk insert를 사용하도록 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistBatchProcessor {

    private final ArtistRepository artistRepository;
    private final ArtistBulkRepository artistBulkRepository;

    /**
     * Artists 배치 처리 - Bulk Insert 최적화 버전
     * INSERT IGNORE를 사용하여 중복 체크를 생략하고 더 빠르게 처리 가능
     */
    public Mono<Map<String, Artist>> processArtistsBatch(Set<String> artistNames) {
        if (artistNames.isEmpty()) {
            return Mono.just(Collections.emptyMap());
        }

        return processArtistsBatchFast(artistNames);
    }
    
    /**
     * INSERT IGNORE를 활용한 빠른 배치 처리
     * 1. 모든 아티스트를 INSERT IGNORE로 시도
     * 2. 이후 전체 조회하여 반환
     */
    private Mono<Map<String, Artist>> processArtistsBatchFast(Set<String> artistNames) {
        List<Artist> allArtists = artistNames.stream()
            .map(Artist::of)
            .collect(Collectors.toList());
        
        return artistBulkRepository.bulkInsert(allArtists)
            .doOnNext(count -> log.debug("Attempted to insert {} artists, {} were new", 
                allArtists.size(), count))
            .then(artistRepository.findAllByNameIn(artistNames)
                .collectMap(Artist::getName));
    }

}
