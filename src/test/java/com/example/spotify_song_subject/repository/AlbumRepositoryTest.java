package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Album;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlbumRepository 단위 테스트")
@RepositoryTestConfiguration
class AlbumRepositoryTest {

    @Autowired
    private AlbumRepository albumRepository;

    private Album testAlbum;

    @BeforeEach
    void setUp() {
        testAlbum = Album.builder()
                .title("Test Album")
                .releaseDate(LocalDate.of(2023, 12, 25))
                .build();
    }

    @AfterEach
    void tearDown() {
        // 테스트 격리를 위한 데이터 정리
        albumRepository.deleteAll()
                .block();
    }

    @Test
    @DisplayName("앨범을 저장한다")
    void saveAlbum() {
        // when & then
        StepVerifier.create(albumRepository.save(testAlbum))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getTitle()).isEqualTo("Test Album");
                    assertThat(saved.getReleaseDate()).isEqualTo(LocalDate.of(2023, 12, 25));
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getDeletedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("제목과 발매일로 삭제되지 않은 앨범을 조회한다")
    void findAlbumByTitleAndReleaseDate() {
        // given
        Mono<Album> savedAlbum = albumRepository.save(testAlbum);

        // when & then
        StepVerifier.create(
                savedAlbum.then(albumRepository.findByTitleAndReleaseDate(
                        "Test Album",
                        LocalDate.of(2023, 12, 25)
                ))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getTitle()).isEqualTo("Test Album");
            assertThat(found.getReleaseDate()).isEqualTo(LocalDate.of(2023, 12, 25));
            assertThat(found.getDeletedAt()).isNull();
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 앨범을 조회하면 빈 결과를 반환한다")
    void findNonExistentAlbum() {
        // when & then
        StepVerifier.create(
                albumRepository.findByTitleAndReleaseDate(
                        "Non Existent Album",
                        LocalDate.of(2023, 1, 1)
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("ID로 앨범을 조회한다")
    void findAlbumById() {
        // given
        Mono<Album> savedAlbum = albumRepository.save(testAlbum);

        // when & then
        StepVerifier.create(
                savedAlbum.flatMap(album -> albumRepository.findById(album.getId()))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getTitle()).isEqualTo("Test Album");
            assertThat(found.getReleaseDate()).isEqualTo(LocalDate.of(2023, 12, 25));
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("발매일이 없는 앨범을 저장한다")
    void saveAlbumWithoutReleaseDate() {
        // given
        Album albumWithoutDate = Album.builder()
                .title("Album Without Date")
                .releaseDate(null)
                .build();

        // when & then
        StepVerifier.create(albumRepository.save(albumWithoutDate))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getTitle()).isEqualTo("Album Without Date");
                    assertThat(saved.getReleaseDate()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("모든 앨범을 조회한다")
    void findAllAlbums() {
        // given
        Album album1 = Album.builder()
                .title("Album 1")
                .releaseDate(LocalDate.of(2023, 1, 1))
                .build();
        Album album2 = Album.builder()
                .title("Album 2")
                .releaseDate(LocalDate.of(2023, 6, 1))
                .build();

        // when & then
        StepVerifier.create(
                albumRepository.save(album1)
                    .then(albumRepository.save(album2))
                    .thenMany(albumRepository.findAll())
                    .collectList()
        )
        .assertNext(albums -> {
            assertThat(albums).hasSize(2);
            assertThat(albums).extracting(Album::getTitle)
                    .containsExactlyInAnyOrder("Album 1", "Album 2");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("앨범을 삭제한다")
    void deleteAlbum() {
        // given
        Mono<Album> savedAlbum = albumRepository.save(testAlbum);

        // when & then
        StepVerifier.create(
                savedAlbum.flatMap(album ->
                    albumRepository.deleteById(album.getId())
                        .then(albumRepository.findById(album.getId()))
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("동일한 제목과 발매일을 가진 앨범을 중복 저장할 수 있다")
    void saveDuplicateAlbums() {
        // given
        Album duplicate = Album.builder()
                .title("Test Album")
                .releaseDate(LocalDate.of(2023, 12, 25))
                .build();

        // when & then
        StepVerifier.create(
                albumRepository.save(testAlbum)
                    .then(albumRepository.save(duplicate))
                    .thenMany(albumRepository.findAll())
                    .collectList()
        )
        .assertNext(albums -> {
            assertThat(albums).hasSize(2);
            assertThat(albums).allMatch(a -> a.getTitle().equals("Test Album"));
            assertThat(albums).allMatch(a -> a.getReleaseDate().equals(LocalDate.of(2023, 12, 25)));
            assertThat(albums).extracting(Album::getId).doesNotHaveDuplicates();
        })
        .verifyComplete();
    }
}
