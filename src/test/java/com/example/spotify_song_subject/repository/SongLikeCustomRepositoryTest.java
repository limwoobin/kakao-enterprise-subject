package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.dto.TrendingSongDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTestConfiguration
@DisplayName("SongLikeCustomRepository Integration Tests")
class SongLikeCustomRepositoryTest {

    @Autowired
    private DatabaseClient databaseClient;

    private SongLikeCustomRepository songLikeCustomRepository;

    @BeforeEach
    void setUp() {
        songLikeCustomRepository = new SongLikeCustomRepository(databaseClient);

        // 테스트 데이터 초기화
        cleanupTestData().block();
        initializeTestData().block();
    }

    private Mono<Void> cleanupTestData() {
        return databaseClient.sql("DELETE FROM artist_songs").fetch().rowsUpdated()
            .then(databaseClient.sql("DELETE FROM artists").fetch().rowsUpdated())
            .then(databaseClient.sql("DELETE FROM songs").fetch().rowsUpdated())
            .then(databaseClient.sql("DELETE FROM albums").fetch().rowsUpdated())
            .then();
    }

    private Mono<Void> initializeTestData() {
        String insertAlbumsSql = """
            INSERT INTO albums (id, title, release_date, created_at, updated_at)
            VALUES
            (1, 'Album 1', '2024-01-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 'Album 2', '2024-02-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (3, 'Album 3', '2024-03-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        String insertArtistsSql = """
            INSERT INTO artists (id, name, created_at, updated_at)
            VALUES
            (1, 'Artist 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 'Artist 2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (3, 'Artist 3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        String insertSongsSql = """
            INSERT INTO songs (id, title, album_id, popularity, created_at, updated_at)
            VALUES
            (1, 'Song 1', 1, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 'Song 2', 2, 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (3, 'Song 3', 3, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (4, 'Song 4', 1, 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (5, 'Song 5', 2, 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        String insertArtistSongsSql = """
            INSERT INTO artist_songs (artist_id, song_id, created_at, updated_at)
            VALUES
            (1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (3, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        return databaseClient.sql(insertAlbumsSql).fetch().rowsUpdated()
            .then(databaseClient.sql(insertArtistsSql).fetch().rowsUpdated())
            .then(databaseClient.sql(insertSongsSql).fetch().rowsUpdated())
            .then(databaseClient.sql(insertArtistSongsSql).fetch().rowsUpdated())
            .then();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 정상 케이스")
    void findTrendingSongsByIds_Success() {
        // Given
        List<Long> songIds = List.of(1L, 2L, 3L);
        Map<Long, Long> likeIncreaseMap = Map.of(
            1L, 100L,
            2L, 90L,
            3L, 80L
        );

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> {
                assertThat(result).hasSize(3);

                // 좋아요 증가 수로 정렬되어 있는지 확인
                assertThat(result.get(0).getSongId()).isEqualTo(1L);
                assertThat(result.get(0).getSongTitle()).isEqualTo("Song 1");
                assertThat(result.get(0).getArtistName()).isEqualTo("Artist 1");
                assertThat(result.get(0).getAlbumName()).isEqualTo("Album 1");
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(100L);

                assertThat(result.get(1).getSongId()).isEqualTo(2L);
                assertThat(result.get(1).getLikeIncrease()).isEqualTo(90L);

                assertThat(result.get(2).getSongId()).isEqualTo(3L);
                assertThat(result.get(2).getLikeIncrease()).isEqualTo(80L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 앨범이 없는 곡 처리 (INNER JOIN으로 인해 결과 없음)")
    void findTrendingSongsByIds_SongWithoutAlbum() {
        // Given - Song 100은 존재하지 않는 앨범을 참조하도록 설정
        databaseClient.sql("INSERT INTO songs (id, title, album_id, created_at, updated_at) VALUES (100, 'Song Without Album', 999, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
            .fetch()
            .rowsUpdated()
            .block();

        List<Long> songIds = List.of(100L);
        Map<Long, Long> likeIncreaseMap = Map.of(100L, 50L);

        // When & Then - INNER JOIN이므로 존재하지 않는 앨범을 참조하는 곡은 조회되지 않음
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 존재하지 않는 곡 ID")
    void findTrendingSongsByIds_NonExistentSongIds() {
        // Given
        List<Long> songIds = List.of(999L, 1000L);
        Map<Long, Long> likeIncreaseMap = Map.of(
            999L, 100L,
            1000L, 90L
        );

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 빈 리스트 처리")
    void findTrendingSongsByIds_EmptyList() {
        // Given
        List<Long> songIds = List.of();
        Map<Long, Long> likeIncreaseMap = Map.of();

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 좋아요 증가 수 내림차순 정렬 검증")
    void findTrendingSongsByIds_VerifyDescendingOrder() {
        // Given
        List<Long> songIds = List.of(4L, 1L, 3L, 2L);
        Map<Long, Long> likeIncreaseMap = Map.of(
            1L, 50L,
            2L, 200L,
            3L, 150L,
            4L, 10L
        );

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> {
                assertThat(result).hasSize(4);
                // 200L, 150L, 50L, 10L 순서로 정렬되어야 함
                assertThat(result.get(0).getSongId()).isEqualTo(2L);
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(200L);

                assertThat(result.get(1).getSongId()).isEqualTo(3L);
                assertThat(result.get(1).getLikeIncrease()).isEqualTo(150L);

                assertThat(result.get(2).getSongId()).isEqualTo(1L);
                assertThat(result.get(2).getLikeIncrease()).isEqualTo(50L);

                assertThat(result.get(3).getSongId()).isEqualTo(4L);
                assertThat(result.get(3).getLikeIncrease()).isEqualTo(10L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 아티스트 정보가 삭제된 곡 처리 (INNER JOIN으로 인해 결과 없음)")
    void findTrendingSongsByIds_DeletedArtist() {
        // Given
        // 아티스트를 soft delete 처리
        databaseClient.sql("UPDATE artists SET deleted_at = CURRENT_TIMESTAMP WHERE id = 1")
            .fetch()
            .rowsUpdated()
            .block();

        List<Long> songIds = List.of(1L);
        Map<Long, Long> likeIncreaseMap = Map.of(1L, 100L);

        // When & Then - INNER JOIN이므로 삭제된 아티스트의 곡은 조회되지 않음
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        // 원상복구
        databaseClient.sql("UPDATE artists SET deleted_at = NULL WHERE id = 1")
            .fetch()
            .rowsUpdated()
            .block();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 다수의 곡 처리 (10개)")
    void findTrendingSongsByIds_MultipleSongs() {
        // Given - 추가 곡 데이터 삽입
        String insertMoreSongsSql = """
            INSERT INTO songs (id, title, album_id, popularity, created_at, updated_at)
            VALUES
            (6, 'Song 6', 1, 65, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (7, 'Song 7', 2, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (8, 'Song 8', 3, 55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (9, 'Song 9', 1, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (10, 'Song 10', 2, 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        databaseClient.sql(insertMoreSongsSql).fetch().rowsUpdated().block();

        // 추가 곡에 대한 artist_songs 관계 추가
        String insertMoreArtistSongsSql = """
            INSERT INTO artist_songs (artist_id, song_id, created_at, updated_at)
            VALUES
            (1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (3, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (1, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
            (2, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        databaseClient.sql(insertMoreArtistSongsSql).fetch().rowsUpdated().block();

        List<Long> songIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        Map<Long, Long> likeIncreaseMap = Map.of(
            1L, 100L, 2L, 90L, 3L, 80L, 4L, 70L, 5L, 60L,
            6L, 50L, 7L, 40L, 8L, 30L, 9L, 20L, 10L, 10L
        );

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> {
                assertThat(result).hasSize(10);
                // 첫 번째와 마지막 항목만 확인
                assertThat(result.get(0).getSongId()).isEqualTo(1L);
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(100L);
                assertThat(result.get(9).getSongId()).isEqualTo(10L);
                assertThat(result.get(9).getLikeIncrease()).isEqualTo(10L);

                // 정렬 순서 확인
                for (int i = 0; i < result.size() - 1; i++) {
                    assertThat(result.get(i).getLikeIncrease())
                        .isGreaterThanOrEqualTo(result.get(i + 1).getLikeIncrease());
                }
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - likeIncreaseMap에 없는 곡 처리")
    void findTrendingSongsByIds_MissingLikeIncreaseEntry() {
        // Given
        List<Long> songIds = List.of(1L, 2L, 3L);
        Map<Long, Long> likeIncreaseMap = Map.of(
            1L, 100L,
            3L, 80L
            // 2L은 맵에 없음 - 0으로 처리됨
        );

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> {
                assertThat(result).hasSize(3);

                // 맵에 없는 곡은 0으로 처리
                TrendingSongDto song2 = result.stream()
                    .filter(dto -> dto.getSongId().equals(2L))
                    .findFirst()
                    .orElseThrow();

                assertThat(song2.getLikeIncrease()).isEqualTo(0L);

                // 정렬 확인 (100L, 80L, 0L)
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(100L);
                assertThat(result.get(1).getLikeIncrease()).isEqualTo(80L);
                assertThat(result.get(2).getLikeIncrease()).isEqualTo(0L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 동일한 좋아요 증가 수 처리")
    void findTrendingSongsByIds_SameLikeIncreaseValues() {
        // Given
        List<Long> songIds = List.of(1L, 2L, 3L, 4L);
        Map<Long, Long> likeIncreaseMap = Map.of(
            1L, 100L,
            2L, 100L,  // 동일한 값
            3L, 50L,
            4L, 50L    // 동일한 값
        );

        // When & Then
        StepVerifier.create(songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap))
            .assertNext(result -> {
                assertThat(result).hasSize(4);

                // 상위 2개는 100L
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(100L);
                assertThat(result.get(1).getLikeIncrease()).isEqualTo(100L);

                // 하위 2개는 50L
                assertThat(result.get(2).getLikeIncrease()).isEqualTo(50L);
                assertThat(result.get(3).getLikeIncrease()).isEqualTo(50L);
            })
            .verifyComplete();
    }
}
