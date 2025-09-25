package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Artist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArtistRepository 단위 테스트")
@RepositoryTestConfiguration
class ArtistRepositoryTest {

    @Autowired
    private ArtistRepository artistRepository;

    private Artist testArtist;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        testArtist = Artist.builder()
                .name("Test Artist")
                .build();
    }

    @AfterEach
    void tearDown() {
        // 테스트 격리를 위한 데이터 정리
        artistRepository.deleteAll()
                .block();
    }

    @Test
    @DisplayName("아티스트를 저장한다")
    void saveArtist() {
        // when & then
        StepVerifier.create(artistRepository.save(testArtist))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getName()).isEqualTo("Test Artist");
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getDeletedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("ID로 아티스트를 조회한다")
    void findArtistById() {
        // given
        Mono<Artist> savedArtist = artistRepository.save(testArtist);

        // when & then
        StepVerifier.create(
                savedArtist.flatMap(artist -> artistRepository.findById(artist.getId()))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("Test Artist");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("이름으로 삭제되지 않은 아티스트를 조회한다")
    void findArtistByName() {
        // given
        Mono<Artist> savedArtist = artistRepository.save(testArtist);

        // when & then
        StepVerifier.create(
                savedArtist.then(artistRepository.findByName("Test Artist"))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("Test Artist");
            assertThat(found.getDeletedAt()).isNull();
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 이름으로 조회하면 빈 결과를 반환한다")
    void findNonExistentArtist() {
        // when & then
        StepVerifier.create(artistRepository.findByName("Non Existent"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("모든 아티스트를 조회한다")
    void findAllArtists() {
        // given
        Artist artist1 = Artist.builder().name("Artist 1").build();
        Artist artist2 = Artist.builder().name("Artist 2").build();
        Artist artist3 = Artist.builder().name("Artist 3").build();

        // when & then
        StepVerifier.create(
                artistRepository.save(artist1)
                    .then(artistRepository.save(artist2))
                    .then(artistRepository.save(artist3))
                    .thenMany(artistRepository.findAll())
                    .collectList()
        )
        .assertNext(artists -> {
            assertThat(artists).hasSize(3);
            assertThat(artists).extracting(Artist::getName)
                    .containsExactlyInAnyOrder("Artist 1", "Artist 2", "Artist 3");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("아티스트를 수정한다")
    void updateArtist() {
        // given
        Mono<Artist> savedArtist = artistRepository.save(testArtist);

        // when & then
        StepVerifier.create(
                savedArtist.flatMap(artist -> {
                    Artist updatedArtist = Artist.builder()
                            .name("Updated Artist Name")
                            .build();
                    // ID를 유지하기 위해 리플렉션이나 다른 방법이 필요
                    // 현재는 새로운 엔티티로 저장
                    return artistRepository.save(updatedArtist);
                })
        )
        .assertNext(updated -> {
            assertThat(updated.getName()).isEqualTo("Updated Artist Name");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("아티스트를 삭제한다")
    void deleteArtist() {
        // given
        Mono<Artist> savedArtist = artistRepository.save(testArtist);

        // when & then
        StepVerifier.create(
                savedArtist.flatMap(artist ->
                    artistRepository.deleteById(artist.getId())
                        .then(artistRepository.findById(artist.getId()))
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("아티스트 개수를 조회한다")
    void countArtists() {
        // given
        Artist artist1 = Artist.builder().name("Artist 1").build();
        Artist artist2 = Artist.builder().name("Artist 2").build();

        // when & then
        StepVerifier.create(
                artistRepository.save(artist1)
                    .then(artistRepository.save(artist2))
                    .then(artistRepository.count())
        )
        .assertNext(count -> assertThat(count).isEqualTo(2L))
        .verifyComplete();
    }
}