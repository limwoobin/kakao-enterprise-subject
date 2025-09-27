package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.repository.AlbumRepository;
import com.example.spotify_song_subject.repository.RepositoryTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlbumBulkRepository 단위 테스트")
@RepositoryTestConfiguration
class AlbumBulkRepositoryTest {

    @Autowired
    private AlbumBulkRepository albumBulkRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private List<Album> testAlbums;

    @BeforeEach
    void setUp() {
        testAlbums = Arrays.asList(
            Album.of("Album 1", LocalDate.of(2023, 1, 1)),
            Album.of("Album 2", LocalDate.of(2023, 2, 1)),
            Album.of("Album 3", LocalDate.of(2023, 3, 1))
        );
    }

    @AfterEach
    void tearDown() {
        albumRepository.deleteAll().block();
    }

    @Test
    @DisplayName("여러 앨범을 일괄 삽입한다")
    void bulkInsertMultipleAlbums() {
        // when & then
        StepVerifier.create(albumBulkRepository.bulkInsert(testAlbums))
            .expectNext(3L)
            .verifyComplete();

        // verify data was actually inserted
        StepVerifier.create(albumRepository.count())
            .expectNext(3L)
            .verifyComplete();
    }

    @Test
    @DisplayName("빈 컬렉션을 삽입하면 0을 반환한다")
    void bulkInsertEmptyCollection() {
        // when & then
        StepVerifier.create(albumBulkRepository.bulkInsert(Collections.emptyList()))
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    @DisplayName("release_date가 null인 앨범도 삽입한다")
    void bulkInsertAlbumsWithNullReleaseDate() {
        // given
        List<Album> albumsWithNullDate = Arrays.asList(
            Album.of("Album Without Date 1", null),
            Album.of("Album Without Date 2", null)
        );

        // when & then
        StepVerifier.create(albumBulkRepository.bulkInsert(albumsWithNullDate))
            .expectNext(2L)
            .verifyComplete();

        // verify data was inserted
        StepVerifier.create(albumRepository.count())
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    @DisplayName("대량 데이터 일괄 삽입 테스트")
    void bulkInsertLargeDataSet() {
        // given
        List<Album> largeDataSet = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeDataSet.add(Album.of("Album " + i, LocalDate.of(2023, 1, 1).plusDays(i)));
        }

        // when & then
        StepVerifier.create(albumBulkRepository.bulkInsert(largeDataSet))
            .expectNext(100L)
            .verifyComplete();

        // verify all data was inserted
        StepVerifier.create(albumRepository.count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    @DisplayName("중복된 앨범 제목과 날짜도 삽입한다")
    void bulkInsertDuplicateAlbums() {
        // given
        List<Album> duplicateAlbums = Arrays.asList(
            Album.of("Same Album", LocalDate.of(2023, 1, 1)),
            Album.of("Same Album", LocalDate.of(2023, 1, 1)),
            Album.of("Same Album", LocalDate.of(2023, 1, 1))
        );

        // when & then
        StepVerifier.create(albumBulkRepository.bulkInsert(duplicateAlbums))
            .expectNext(3L)
            .verifyComplete();

        // verify all duplicates were inserted
        StepVerifier.create(albumRepository.count())
            .expectNext(3L)
            .verifyComplete();
    }
    
    @Test
    @DisplayName("아티스트명을 포함한 앨범을 일괄 삽입한다")
    void bulkInsertAlbumsWithArtistName() {
        // given
        List<Album> albumsWithArtist = Arrays.asList(
            Album.of("Album 1", LocalDate.of(2023, 1, 1), "Artist A"),
            Album.of("Album 2", LocalDate.of(2023, 2, 1), "Artist B"),
            Album.of("Album 3", LocalDate.of(2023, 3, 1), "Artist C")
        );

        // when & then
        StepVerifier.create(albumBulkRepository.bulkInsert(albumsWithArtist))
            .expectNext(3L)
            .verifyComplete();

        // verify data was inserted with artist names
        StepVerifier.create(albumRepository.findAll())
            .assertNext(album -> {
                assertThat(album.getTitle()).isIn("Album 1", "Album 2", "Album 3");
                assertThat(album.getArtistName()).isNotNull();
                assertThat(album.getArtistName()).isIn("Artist A", "Artist B", "Artist C");
            })
            .assertNext(album -> {
                assertThat(album.getTitle()).isIn("Album 1", "Album 2", "Album 3");
                assertThat(album.getArtistName()).isNotNull();
            })
            .assertNext(album -> {
                assertThat(album.getTitle()).isIn("Album 1", "Album 2", "Album 3");
                assertThat(album.getArtistName()).isNotNull();
            })
            .verifyComplete();
    }
}