package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ArtistSong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("ArtistSongRepository 단위 테스트")
@RepositoryTestConfiguration
class ArtistSongRepositoryTest {

    @Autowired
    private ArtistSongRepository artistSongRepository;

    private ArtistSong testArtistSong;

    @BeforeEach
    void setUp() {
        testArtistSong = ArtistSong.builder()
                .artistId(1L)
                .songId(100L)
                .build();
    }

    @AfterEach
    void tearDown() {
        // 테스트 격리를 위한 데이터 정리
        artistSongRepository.deleteAll()
                .block();
    }

    @Test
    @DisplayName("아티스트-곡 관계를 저장한다")
    void saveArtistSongRelation() {
        // when & then
        StepVerifier.create(artistSongRepository.save(testArtistSong))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getArtistId()).isEqualTo(1L);
                    assertThat(saved.getSongId()).isEqualTo(100L);
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getDeletedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("아티스트ID와 곡ID로 삭제되지 않은 관계를 조회한다")
    void findRelationByArtistIdAndSongId() {
        // given
        Mono<ArtistSong> saved = artistSongRepository.save(testArtistSong);

        // when & then
        StepVerifier.create(
                saved.then(artistSongRepository.findByArtistIdAndSongId(1L, 100L))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getArtistId()).isEqualTo(1L);
            assertThat(found.getSongId()).isEqualTo(100L);
            assertThat(found.getDeletedAt()).isNull();
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계를 조회하면 빈 결과를 반환한다")
    void findNonExistentRelation() {
        // when & then
        StepVerifier.create(
                artistSongRepository.findByArtistIdAndSongId(999L, 999L)
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("ID로 관계를 조회한다")
    void findRelationById() {
        // given
        Mono<ArtistSong> saved = artistSongRepository.save(testArtistSong);

        // when & then
        StepVerifier.create(
                saved.flatMap(as -> artistSongRepository.findById(as.getId()))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getArtistId()).isEqualTo(1L);
            assertThat(found.getSongId()).isEqualTo(100L);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("여러 아티스트-곡 관계를 저장한다")
    void saveMultipleRelations() {
        // given
        ArtistSong relation1 = ArtistSong.builder()
                .artistId(1L)
                .songId(100L)
                .build();
        ArtistSong relation2 = ArtistSong.builder()
                .artistId(1L)
                .songId(200L)
                .build();
        ArtistSong relation3 = ArtistSong.builder()
                .artistId(2L)
                .songId(100L)
                .build();

        // when & then
        StepVerifier.create(
                artistSongRepository.save(relation1)
                    .then(artistSongRepository.save(relation2))
                    .then(artistSongRepository.save(relation3))
                    .thenMany(artistSongRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(3);
            assertThat(relations).extracting(ArtistSong::getArtistId)
                    .containsExactlyInAnyOrder(1L, 1L, 2L);
            assertThat(relations).extracting(ArtistSong::getSongId)
                    .containsExactlyInAnyOrder(100L, 200L, 100L);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("관계를 삭제한다")
    void deleteRelation() {
        // given
        Mono<ArtistSong> saved = artistSongRepository.save(testArtistSong);

        // when & then
        StepVerifier.create(
                saved.flatMap(as ->
                    artistSongRepository.deleteById(as.getId())
                        .then(artistSongRepository.findById(as.getId()))
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("동일한 아티스트-곡 관계를 중복 저장할 수 있다")
    void saveDuplicateRelations() {
        // given
        ArtistSong duplicate = ArtistSong.builder()
                .artistId(1L)
                .songId(100L)
                .build();

        // when & then
        StepVerifier.create(
                artistSongRepository.save(testArtistSong)
                    .then(artistSongRepository.save(duplicate))
                    .thenMany(artistSongRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(2);
            assertThat(relations).allMatch(r -> r.getArtistId().equals(1L));
            assertThat(relations).allMatch(r -> r.getSongId().equals(100L));
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("모든 관계를 조회한다")
    void findAllRelations() {
        // given
        ArtistSong relation1 = ArtistSong.builder()
                .artistId(1L)
                .songId(100L)
                .build();
        ArtistSong relation2 = ArtistSong.builder()
                .artistId(2L)
                .songId(200L)
                .build();

        // when & then
        StepVerifier.create(
                artistSongRepository.save(relation1)
                    .then(artistSongRepository.save(relation2))
                    .thenMany(artistSongRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(2);
            assertThat(relations).extracting(ArtistSong::getArtistId, ArtistSong::getSongId)
                    .containsExactlyInAnyOrder(
                            tuple(1L, 100L),
                            tuple(2L, 200L)
                    );
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("관계 개수를 조회한다")
    void countRelations() {
        // given
        ArtistSong relation1 = ArtistSong.builder()
                .artistId(1L)
                .songId(100L)
                .build();
        ArtistSong relation2 = ArtistSong.builder()
                .artistId(1L)
                .songId(200L)
                .build();
        ArtistSong relation3 = ArtistSong.builder()
                .artistId(2L)
                .songId(300L)
                .build();

        // when & then
        StepVerifier.create(
                artistSongRepository.save(relation1)
                    .then(artistSongRepository.save(relation2))
                    .then(artistSongRepository.save(relation3))
                    .then(artistSongRepository.count())
        )
        .assertNext(count -> assertThat(count).isEqualTo(3L))
        .verifyComplete();
    }
}