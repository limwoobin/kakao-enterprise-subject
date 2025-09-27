package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.repository.*;
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

@DisplayName("ArtistAlbumBulkRepository 단위 테스트")

@RepositoryTestConfiguration
class ArtistAlbumBulkRepositoryTest {

    @Autowired
    private ArtistAlbumBulkRepository artistAlbumBulkRepository;

    @Autowired
    private ArtistAlbumRepository artistAlbumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private Long artistId1;
    private Long artistId2;
    private Long albumId1;
    private Long albumId2;
    private List<ArtistAlbum> testArtistAlbums;

    @BeforeEach
    void setUp() {
        // Create test data
        Artist artist1 = Artist.of("Artist 1");
        Artist artist2 = Artist.of("Artist 2");
        artistId1 = artistRepository.save(artist1).block().getId();
        artistId2 = artistRepository.save(artist2).block().getId();

        Album album1 = Album.of("Album 1", LocalDate.of(2023, 1, 1));
        Album album2 = Album.of("Album 2", LocalDate.of(2023, 2, 1));
        albumId1 = albumRepository.save(album1).block().getId();
        albumId2 = albumRepository.save(album2).block().getId();

        testArtistAlbums = Arrays.asList(
            ArtistAlbum.builder()
                .artistId(artistId1)
                .albumId(albumId1)
                .build(),
            ArtistAlbum.builder()
                .artistId(artistId1)
                .albumId(albumId2)
                .build(),
            ArtistAlbum.builder()
                .artistId(artistId2)
                .albumId(albumId1)
                .build()
        );
    }

    @AfterEach
    void tearDown() {
        artistAlbumRepository.deleteAll().block();
        albumRepository.deleteAll().block();
        artistRepository.deleteAll().block();
    }

    @Test
    @DisplayName("여러 아티스트-앨범 관계를 일괄 삽입한다")
    void bulkInsertMultipleArtistAlbums() {
        // when & then
        StepVerifier.create(artistAlbumBulkRepository.bulkInsert(testArtistAlbums))
            .expectNext(3L)
            .verifyComplete();

        // verify data was actually inserted
        StepVerifier.create(artistAlbumRepository.count())
            .expectNext(3L)
            .verifyComplete();

        // verify specific relationships
        StepVerifier.create(artistAlbumRepository.findAll().filter(aa -> aa.getArtistId().equals(artistId1)).collectList())
            .assertNext(artistAlbums -> {
                assertThat(artistAlbums).hasSize(2);
                assertThat(artistAlbums)
                    .extracting(ArtistAlbum::getAlbumId)
                    .containsExactlyInAnyOrder(albumId1, albumId2);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("빈 컬렉션을 삽입하면 0을 반환한다")
    void bulkInsertEmptyCollection() {
        // when & then
        StepVerifier.create(artistAlbumBulkRepository.bulkInsert(Collections.emptyList()))
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    @DisplayName("대량 데이터 일괄 삽입 테스트")
    void bulkInsertLargeDataSet() {
        // given
        List<ArtistAlbum> largeDataSet = new java.util.ArrayList<>();

        // Create more albums for testing
        for (int i = 0; i < 50; i++) {
            Album album = Album.of("Album " + i, LocalDate.of(2023, 1, 1).plusDays(i));
            Long albumId = albumRepository.save(album).block().getId();

            // Create relationships
            largeDataSet.add(ArtistAlbum.builder()
                .artistId(artistId1)
                .albumId(albumId)
                .build());
            largeDataSet.add(ArtistAlbum.builder()
                .artistId(artistId2)
                .albumId(albumId)
                .build());
        }

        // when & then
        StepVerifier.create(artistAlbumBulkRepository.bulkInsert(largeDataSet))
            .expectNext(100L)
            .verifyComplete();

        // verify all data was inserted
        StepVerifier.create(artistAlbumRepository.count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    @DisplayName("중복된 아티스트-앨범 관계도 삽입한다")
    void bulkInsertDuplicateRelationships() {
        // given - same artist and album combination multiple times
        List<ArtistAlbum> duplicates = Arrays.asList(
            ArtistAlbum.builder()
                .artistId(artistId1)
                .albumId(albumId1)
                .build(),
            ArtistAlbum.builder()
                .artistId(artistId1)
                .albumId(albumId1)
                .build(),
            ArtistAlbum.builder()
                .artistId(artistId1)
                .albumId(albumId1)
                .build()
        );

        // when & then
        StepVerifier.create(artistAlbumBulkRepository.bulkInsert(duplicates))
            .expectNext(3L)
            .verifyComplete();

        // verify all duplicates were inserted
        StepVerifier.create(artistAlbumRepository.count())
            .expectNext(3L)
            .verifyComplete();
    }
}