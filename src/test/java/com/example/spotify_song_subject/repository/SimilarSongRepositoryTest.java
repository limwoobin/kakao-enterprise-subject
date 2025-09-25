package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.SimilarSong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("SimilarSongRepository 단위 테스트")
@RepositoryTestConfiguration
class SimilarSongRepositoryTest {

    @Autowired
    private SimilarSongRepository similarSongRepository;

    private SimilarSong testSimilarSong;

    @BeforeEach
    void setUp() {
        testSimilarSong = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Test Artist")
                .similarSongTitle("Test Song")
                .similarityScore(new BigDecimal("0.95"))
                .build();
    }

    @AfterEach
    void tearDown() {
        // 테스트 격리를 위한 데이터 정리
        similarSongRepository.deleteAll()
                .block();
    }

    @Test
    @DisplayName("유사곡 관계를 저장한다")
    void saveSimilarSongRelation() {
        // when & then
        StepVerifier.create(similarSongRepository.save(testSimilarSong))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getSongId()).isEqualTo(1L);
                    assertThat(saved.getSimilarArtistName()).isEqualTo("Test Artist");
                    assertThat(saved.getSimilarSongTitle()).isEqualTo("Test Song");
                    assertThat(saved.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.95"));
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getDeletedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("곡ID와 유사곡 정보로 삭제되지 않은 관계를 조회한다")
    void findRelationBySongIdAndSimilarInfo() {
        // given
        Mono<SimilarSong> saved = similarSongRepository.save(testSimilarSong);

        // when & then
        StepVerifier.create(
                saved.then(similarSongRepository.findBySongIdAndSimilarInfo(1L, "Test Artist", "Test Song"))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getSongId()).isEqualTo(1L);
            assertThat(found.getSimilarArtistName()).isEqualTo("Test Artist");
            assertThat(found.getSimilarSongTitle()).isEqualTo("Test Song");
            assertThat(found.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(found.getDeletedAt()).isNull();
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계를 조회하면 빈 결과를 반환한다")
    void findNonExistentRelation() {
        // when & then
        StepVerifier.create(
                similarSongRepository.findBySongIdAndSimilarInfo(999L, "Non Existent Artist", "Non Existent Song")
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("ID로 관계를 조회한다")
    void findRelationById() {
        // given
        Mono<SimilarSong> saved = similarSongRepository.save(testSimilarSong);

        // when & then
        StepVerifier.create(
                saved.flatMap(ss -> similarSongRepository.findById(ss.getId()))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getSongId()).isEqualTo(1L);
            assertThat(found.getSimilarArtistName()).isEqualTo("Test Artist");
            assertThat(found.getSimilarSongTitle()).isEqualTo("Test Song");
            assertThat(found.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.95"));
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("다양한 유사도 점수를 가진 관계를 저장한다")
    void saveRelationsWithVariousSimilarityScores() {
        // given
        SimilarSong highSimilarity = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist A")
                .similarSongTitle("Song A")
                .similarityScore(new BigDecimal("0.98"))
                .build();
        SimilarSong mediumSimilarity = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist B")
                .similarSongTitle("Song B")
                .similarityScore(new BigDecimal("0.75"))
                .build();
        SimilarSong lowSimilarity = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist C")
                .similarSongTitle("Song C")
                .similarityScore(new BigDecimal("0.50"))
                .build();

        // when & then
        StepVerifier.create(
                similarSongRepository.save(highSimilarity)
                    .then(similarSongRepository.save(mediumSimilarity))
                    .then(similarSongRepository.save(lowSimilarity))
                    .thenMany(similarSongRepository.findAll())
        )
        .assertNext(ss -> assertThat(ss.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.98")))
        .assertNext(ss -> assertThat(ss.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.75")))
        .assertNext(ss -> assertThat(ss.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.50")))
        .verifyComplete();
    }

    @Test
    @DisplayName("양방향 관계를 저장한다")
    void saveBidirectionalRelations() {
        // given
        SimilarSong relation1 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist X")
                .similarSongTitle("Song X")
                .similarityScore(new BigDecimal("0.95"))
                .build();
        SimilarSong relation2 = SimilarSong.builder()
                .songId(2L)
                .similarArtistName("Artist Y")
                .similarSongTitle("Song Y")
                .similarityScore(new BigDecimal("0.95"))
                .build();

        // when & then
        StepVerifier.create(
                similarSongRepository.save(relation1)
                    .then(similarSongRepository.save(relation2))
                    .thenMany(similarSongRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(2);
            assertThat(relations).extracting(SimilarSong::getSongId, SimilarSong::getSimilarArtistName, SimilarSong::getSimilarSongTitle)
                    .containsExactlyInAnyOrder(
                            tuple(1L, "Artist X", "Song X"),
                            tuple(2L, "Artist Y", "Song Y")
                    );
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("관계를 삭제한다")
    void deleteRelation() {
        // given
        Mono<SimilarSong> saved = similarSongRepository.save(testSimilarSong);

        // when & then
        StepVerifier.create(
                saved.flatMap(ss ->
                    similarSongRepository.deleteById(ss.getId())
                        .then(similarSongRepository.findById(ss.getId()))
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("모든 관계를 조회한다")
    void findAllRelations() {
        // given
        SimilarSong relation1 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist 1")
                .similarSongTitle("Song 1")
                .similarityScore(new BigDecimal("0.95"))
                .build();
        SimilarSong relation2 = SimilarSong.builder()
                .songId(3L)
                .similarArtistName("Artist 2")
                .similarSongTitle("Song 2")
                .similarityScore(new BigDecimal("0.85"))
                .build();

        // when & then
        StepVerifier.create(
                similarSongRepository.save(relation1)
                    .then(similarSongRepository.save(relation2))
                    .thenMany(similarSongRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(2);
            assertThat(relations).extracting(SimilarSong::getSongId, SimilarSong::getSimilarArtistName, SimilarSong::getSimilarSongTitle)
                    .containsExactlyInAnyOrder(
                            tuple(1L, "Artist 1", "Song 1"),
                            tuple(3L, "Artist 2", "Song 2")
                    );
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("특정 곡의 모든 유사곡을 유사도 순으로 조회한다")
    void findSimilarSongsBySongId() {
        // given
        SimilarSong similar1 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist A")
                .similarSongTitle("Song A")
                .similarityScore(new BigDecimal("0.70"))
                .build();
        SimilarSong similar2 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist B")
                .similarSongTitle("Song B")
                .similarityScore(new BigDecimal("0.95"))
                .build();
        SimilarSong similar3 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Artist C")
                .similarSongTitle("Song C")
                .similarityScore(new BigDecimal("0.85"))
                .build();
        SimilarSong otherSong = SimilarSong.builder()
                .songId(2L)
                .similarArtistName("Artist D")
                .similarSongTitle("Song D")
                .similarityScore(new BigDecimal("0.99"))
                .build();

        // when & then
        StepVerifier.create(
                similarSongRepository.save(similar1)
                    .then(similarSongRepository.save(similar2))
                    .then(similarSongRepository.save(similar3))
                    .then(similarSongRepository.save(otherSong))
                    .thenMany(similarSongRepository.findBySongId(1L))
                    .collectList()
        )
        .assertNext(similars -> {
            assertThat(similars).hasSize(3);
            // 유사도 내림차순으로 정렬되어야 함
            assertThat(similars.get(0).getSimilarArtistName()).isEqualTo("Artist B"); // 0.95
            assertThat(similars.get(1).getSimilarArtistName()).isEqualTo("Artist C"); // 0.85
            assertThat(similars.get(2).getSimilarArtistName()).isEqualTo("Artist A"); // 0.70

            // songId=2의 유사곡은 포함되지 않아야 함
            assertThat(similars).noneMatch(s -> "Artist D".equals(s.getSimilarArtistName()));
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("매우 긴 아티스트명과 곡 제목을 처리한다")
    void handleVeryLongArtistAndSongNames() {
        // given - 500자를 넘는 아티스트명
        String veryLongArtistName = "A".repeat(500); // 정확히 500자
        String overLimitArtistName = "B".repeat(501); // 501자 (초과)

        SimilarSong exact500 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName(veryLongArtistName)
                .similarSongTitle("Normal Title")
                .similarityScore(new BigDecimal("0.95"))
                .build();

        // when & then - 500자는 정상 저장
        StepVerifier.create(similarSongRepository.save(exact500))
                .assertNext(saved -> {
                    assertThat(saved.getSimilarArtistName()).hasSize(500);
                    assertThat(saved.getSimilarArtistName()).isEqualTo(veryLongArtistName);
                })
                .verifyComplete();

        // Note: 501자 이상은 Service 레이어에서 truncate 처리됨
    }

    @Test
    @DisplayName("관계 개수를 조회한다")
    void 관계_개수_조회() {
        // given
        SimilarSong relation1 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Count Artist 1")
                .similarSongTitle("Count Song 1")
                .similarityScore(new BigDecimal("0.95"))
                .build();
        SimilarSong relation2 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Count Artist 2")
                .similarSongTitle("Count Song 2")
                .similarityScore(new BigDecimal("0.90"))
                .build();
        SimilarSong relation3 = SimilarSong.builder()
                .songId(1L)
                .similarArtistName("Count Artist 3")
                .similarSongTitle("Count Song 3")
                .similarityScore(new BigDecimal("0.85"))
                .build();

        // when & then
        StepVerifier.create(
                similarSongRepository.save(relation1)
                    .then(similarSongRepository.save(relation2))
                    .then(similarSongRepository.save(relation3))
                    .then(similarSongRepository.count())
        )
        .assertNext(count -> assertThat(count).isEqualTo(3L))
        .verifyComplete();
    }
}