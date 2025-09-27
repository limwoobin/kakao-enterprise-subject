package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.dto.TrendingSongDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 좋아요 관련 커스텀 쿼리를 처리하는 Repository
 * DatabaseClient를 사용한 복잡한 JOIN 쿼리 처리
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SongLikeCustomRepository {

    private final DatabaseClient databaseClient;

    /**
     * Song ID 목록과 좋아요 증가 맵을 받아서 JOIN 쿼리로 TrendingSongDto 목록 반환
     * 성능 개선: 단일 JOIN 쿼리로 N+1 문제 해결
     *
     * @param songIds 조회할 Song ID 목록
     * @param likeIncreaseMap Song ID별 좋아요 증가 수 맵
     * @return TrendingSongDto List (좋아요 증가 수 내림차순 정렬)
     */
    public Mono<List<TrendingSongDto>> findTrendingSongsByIds(List<Long> songIds, Map<Long, Long> likeIncreaseMap) {
        // 빈 리스트인 경우 빈 결과 반환
        if (songIds == null || songIds.isEmpty()) {
            return Mono.just(List.of());
        }

        String sql = """
            SELECT
                s.id as song_id,
                s.title as song_title,
                a.name as artist_name,
                al.title as album_name
            FROM songs s
            INNER JOIN artist_songs ars ON s.id = ars.song_id AND ars.deleted_at IS NULL
            INNER JOIN artists a ON ars.artist_id = a.id AND a.deleted_at IS NULL
            INNER JOIN albums al ON s.album_id = al.id AND al.deleted_at IS NULL
            WHERE s.id IN (:songIds)
              AND s.deleted_at IS NULL
            """;

        return databaseClient.sql(sql)
                .bind("songIds", songIds)
                .map((row, metadata) -> {
                    Long songId = row.get("song_id", Long.class);
                    Long likeIncrease = likeIncreaseMap.getOrDefault(songId, 0L);

                    return TrendingSongDto.builder()
                            .songId(songId)
                            .songTitle(row.get("song_title", String.class))
                            .artistName(row.get("artist_name", String.class))
                            .albumName(row.get("album_name", String.class))
                            .likeIncrease(likeIncrease)
                            .build();
                })
                .all()
                .sort((a, b) -> Long.compare(b.getLikeIncrease(), a.getLikeIncrease()))
                .collectList();
    }
}