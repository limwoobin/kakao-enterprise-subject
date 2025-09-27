package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.dto.AlbumStatisticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AlbumStatisticsCustomRepository {

    private final DatabaseClient databaseClient;

    /**
     * 특정 연도의 아티스트별 앨범 통계 조회
     * @param year 조회할 연도
     * @param pageable 페이징 정보
     * @return 페이징된 앨범 통계 데이터
     */
    public Mono<Page<AlbumStatisticsDto>> findAlbumStatisticsByYear(Integer year, Pageable pageable) {
        Mono<Tuple2<List<AlbumStatisticsDto>, Long>> zip = Mono.zip(
            findAlbumStatisticsData(year, pageable).collectList(),
            countAlbumStatistics(year)
        );

        return zip.map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    /**
     * 데이터 조회용 - BETWEEN 절을 사용하여 인덱스 효율적 활용
     * @param year 조회할 연도
     * @param pageable 페이징 정보
     * @return 앨범 통계 데이터 Flux
     */
    private Flux<AlbumStatisticsDto> findAlbumStatisticsData(Integer year, Pageable pageable) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        String sql = """
            SELECT
                ar.id as artist_id,
                ar.name as artist_name,
                :year as release_year,
                COUNT(DISTINCT aa.id) as album_count
            FROM artist_albums aa
            INNER JOIN albums al ON aa.album_id = al.id
                AND al.release_date BETWEEN :startDate AND :endDate
                AND al.deleted_at IS NULL
            INNER JOIN artists ar ON aa.artist_id = ar.id
                AND ar.deleted_at IS NULL
            WHERE aa.deleted_at IS NULL
            GROUP BY ar.id, ar.name
            ORDER BY album_count DESC, ar.name
            LIMIT :limit OFFSET :offset
            """;

        log.debug("Executing album statistics query for year: {}", year);

        return databaseClient.sql(sql)
            .bind("year", year)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .bind("limit", pageable.getPageSize())
            .bind("offset", pageable.getOffset())
            .map((row, metadata) -> AlbumStatisticsDto.builder()
                .artistId(row.get("artist_id", Long.class))
                .artistName(row.get("artist_name", String.class))
                .releaseYear(row.get("release_year", Integer.class))
                .albumCount(row.get("album_count", Long.class))
                .build())
            .all();
    }

    /**
     * 전체 개수 조회용 - 해당 연도에 앨범을 발매한 아티스트 수
     * @param year 조회할 연도
     * @return 전체 아티스트 수
     */
    private Mono<Long> countAlbumStatistics(Integer year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 해당 연도에 앨범을 발매한 아티스트 수를 직접 카운트
        String sql = """
            SELECT COUNT(DISTINCT ar.id) as total
            FROM artist_albums aa
            INNER JOIN albums al ON aa.album_id = al.id
                AND al.release_date BETWEEN :startDate AND :endDate
                AND al.deleted_at IS NULL
            INNER JOIN artists ar ON aa.artist_id = ar.id
                AND ar.deleted_at IS NULL
            WHERE aa.deleted_at IS NULL
            """;

        log.debug("Executing count query for year: {}", year);

        return databaseClient.sql(sql)
            .bind("startDate", startDate)
            .bind("endDate", endDate)
            .map(row -> row.get("total", Long.class))
            .one()
            .defaultIfEmpty(0L);
    }
}
