package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ArtistAlbum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("ArtistAlbumRepository 단위 테스트")
@RepositoryTestConfiguration
class ArtistAlbumRepositoryTest {

    @Autowired
    private ArtistAlbumRepository artistAlbumRepository;

    private ArtistAlbum testArtistAlbum;

    @BeforeEach
    void setUp() {
        testArtistAlbum = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(50L)
                .build();
    }

    @AfterEach
    void tearDown() {
        // 테스트 격리를 위한 데이터 정리
        artistAlbumRepository.deleteAll()
                .block();
    }

    @Test
    @DisplayName("아티스트-앨범 관계를 저장한다")
    void saveArtistAlbumRelation() {
        // when & then
        StepVerifier.create(artistAlbumRepository.save(testArtistAlbum))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getArtistId()).isEqualTo(1L);
                    assertThat(saved.getAlbumId()).isEqualTo(50L);
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getDeletedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("아티스트ID와 앨범ID로 삭제되지 않은 관계를 조회한다")
    void findRelationByArtistIdAndAlbumId() {
        // given
        Mono<ArtistAlbum> saved = artistAlbumRepository.save(testArtistAlbum);

        // when & then
        StepVerifier.create(
                saved.then(artistAlbumRepository.findByArtistIdAndAlbumId(1L, 50L))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getArtistId()).isEqualTo(1L);
            assertThat(found.getAlbumId()).isEqualTo(50L);
            assertThat(found.getDeletedAt()).isNull();
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 관계를 조회하면 빈 결과를 반환한다")
    void findNonExistentRelation() {
        // when & then
        StepVerifier.create(
                artistAlbumRepository.findByArtistIdAndAlbumId(999L, 999L)
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("ID로 관계를 조회한다")
    void findRelationById() {
        // given
        Mono<ArtistAlbum> saved = artistAlbumRepository.save(testArtistAlbum);

        // when & then
        StepVerifier.create(
                saved.flatMap(aa -> artistAlbumRepository.findById(aa.getId()))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getArtistId()).isEqualTo(1L);
            assertThat(found.getAlbumId()).isEqualTo(50L);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("여러 아티스트-앨범 관계를 저장한다")
    void saveMultipleRelations() {
        // given
        ArtistAlbum relation1 = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(50L)
                .build();
        ArtistAlbum relation2 = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(60L)
                .build();
        ArtistAlbum relation3 = ArtistAlbum.builder()
                .artistId(2L)
                .albumId(50L)
                .build();

        // when & then
        StepVerifier.create(
                artistAlbumRepository.save(relation1)
                    .then(artistAlbumRepository.save(relation2))
                    .then(artistAlbumRepository.save(relation3))
                    .thenMany(artistAlbumRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(3);
            assertThat(relations).extracting(ArtistAlbum::getArtistId)
                    .containsExactlyInAnyOrder(1L, 1L, 2L);
            assertThat(relations).extracting(ArtistAlbum::getAlbumId)
                    .containsExactlyInAnyOrder(50L, 60L, 50L);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("관계를 삭제한다")
    void deleteRelation() {
        // given
        Mono<ArtistAlbum> saved = artistAlbumRepository.save(testArtistAlbum);

        // when & then
        StepVerifier.create(
                saved.flatMap(aa ->
                    artistAlbumRepository.deleteById(aa.getId())
                        .then(artistAlbumRepository.findById(aa.getId()))
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("동일한 아티스트-앨범 관계를 중복 저장할 수 있다")
    void saveDuplicateRelations() {
        // given
        ArtistAlbum duplicate = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(50L)
                .build();

        // when & then
        StepVerifier.create(
                artistAlbumRepository.save(testArtistAlbum)
                    .then(artistAlbumRepository.save(duplicate))
                    .thenMany(artistAlbumRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(2);
            assertThat(relations).allMatch(r -> r.getArtistId().equals(1L));
            assertThat(relations).allMatch(r -> r.getAlbumId().equals(50L));
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("모든 관계를 조회한다")
    void findAllRelations() {
        // given
        ArtistAlbum relation1 = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(50L)
                .build();
        ArtistAlbum relation2 = ArtistAlbum.builder()
                .artistId(2L)
                .albumId(60L)
                .build();

        // when & then
        StepVerifier.create(
                artistAlbumRepository.save(relation1)
                    .then(artistAlbumRepository.save(relation2))
                    .thenMany(artistAlbumRepository.findAll())
                    .collectList()
        )
        .assertNext(relations -> {
            assertThat(relations).hasSize(2);
            assertThat(relations).extracting(ArtistAlbum::getArtistId, ArtistAlbum::getAlbumId)
                    .containsExactlyInAnyOrder(
                            tuple(1L, 50L),
                            tuple(2L, 60L)
                    );
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("관계 개수를 조회한다")
    void countRelations() {
        // given
        ArtistAlbum relation1 = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(50L)
                .build();
        ArtistAlbum relation2 = ArtistAlbum.builder()
                .artistId(1L)
                .albumId(60L)
                .build();
        ArtistAlbum relation3 = ArtistAlbum.builder()
                .artistId(2L)
                .albumId(70L)
                .build();

        // when & then
        StepVerifier.create(
                artistAlbumRepository.save(relation1)
                    .then(artistAlbumRepository.save(relation2))
                    .then(artistAlbumRepository.save(relation3))
                    .then(artistAlbumRepository.count())
        )
        .assertNext(count -> assertThat(count).isEqualTo(3L))
        .verifyComplete();
    }
}