package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.BatchContext;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import com.example.spotify_song_subject.mapper.SpotifyDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spotify 데이터를 도메인 엔티티로 변환하고 저장하는 서비스
 * Bulk Insert 방식으로 최적화된 배치 처리 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyDataPersistenceService {

    private final TransactionalOperator transactionalOperator;
    
    private final ArtistBatchProcessor artistBatchProcessor;
    private final AlbumBatchProcessor albumBatchProcessor;
    private final SongBatchProcessor songBatchProcessor;
    private final RelationshipDataProcessor relationshipProcessor;

    /**
     * Map 데이터 리스트를 처리하고 저장
     * Map → DTO → Domain 변환 후 Bulk 저장
     * 배치 크기: 1000개 단위로 처리
     */
    public Mono<Void> processSongBatch(List<Map<String, Object>> songMaps) {
        List<SpotifySongDto> songDtos = convertToDtos(songMaps);

        if (songDtos.isEmpty()) {
            log.debug("No valid songs in batch");
            return Mono.empty();
        }

        return processBatchInternal(songDtos)
            .as(transactionalOperator::transactional)
            .subscribeOn(Schedulers.boundedElastic()); // I/O 작업에 최적화된 스케줄러
    }

    /**
     * Map 데이터를 DTO로 변환
     */
    private List<SpotifySongDto> convertToDtos(List<Map<String, Object>> songMaps) {
        return songMaps.stream()
            .map(SpotifyDataMapper::mapToSpotifySongDto)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 배치 데이터 내부 처리
     * 1. Artists & Albums 병렬 처리 (중복 체크 필요)
     * 2. Songs & Relations 순차 처리 (Bulk Insert)
     */
    private Mono<Void> processBatchInternal(List<SpotifySongDto> songDtos) {
        BatchContext context = BatchContext.from(songDtos);

        return processArtistsAndAlbums(context)
            .flatMap(tuple -> processSongsWithRelations(songDtos, tuple.getT1(), tuple.getT2()));
    }

    /**
     * Artists와 Albums 병렬 처리
     */
    private Mono<Tuple2<Map<String, Artist>, Map<String, Album>>> processArtistsAndAlbums(BatchContext context) {
        Mono<Map<String, Artist>> artistsMapMono = artistBatchProcessor.processArtistsBatch(context.artistNames());
        Mono<Map<String, Album>> albumsMapMono = albumBatchProcessor.processAlbumsBatch(context.albumsByTitle());

        return Mono.zip(artistsMapMono, albumsMapMono);
    }


    /**
     * Songs와 관계 데이터 처리
     */
    private Mono<Void> processSongsWithRelations(List<SpotifySongDto> songDtos,
                                                 Map<String, Artist> artistsMap,
                                                 Map<String, Album> albumsMap) {
        return songBatchProcessor.processSongsBatch(songDtos, albumsMap)
            .flatMap(songResult -> {
                if (songResult.savedSongs().isEmpty()) {
                    return Mono.empty();
                }
                
                return processRelationships(
                    songResult.savedSongs(), 
                    songDtos, 
                    artistsMap, 
                    albumsMap, 
                    songResult.songIndexToAlbumKey()
                );
            });
    }

    /**
     * 관계 데이터 처리
     */
    private Mono<Void> processRelationships(List<Song> savedSongs,
                                            List<SpotifySongDto> songDtos,
                                            Map<String, Artist> artistsMap,
                                            Map<String, Album> albumsMap,
                                            Map<Integer, String> songIndexToAlbumKey) {
        RelationshipDataProcessor.RelationshipData relationships =
            RelationshipDataProcessor.buildRelationships(
                savedSongs, songDtos, artistsMap, albumsMap, songIndexToAlbumKey
            );

        return insertRelationships(relationships);
    }

    /**
     * 관계 데이터 Bulk Insert
     */
    private Mono<Void> insertRelationships(RelationshipDataProcessor.RelationshipData relationships) {
        List<Mono<Long>> insertOps = new ArrayList<>();
        
        if (!relationships.artistSongs().isEmpty()) {
            insertOps.add(relationshipProcessor.bulkInsertArtistSongs(relationships.artistSongs()));
        }

        if (!relationships.artistAlbums().isEmpty()) {
            insertOps.add(relationshipProcessor.bulkInsertArtistAlbums(relationships.artistAlbums()));
        }

        if (!relationships.similarSongs().isEmpty()) {
            insertOps.add(relationshipProcessor.bulkInsertSimilarSongs(relationships.similarSongs()));
        }

        return Mono.when(insertOps);
    }

}
