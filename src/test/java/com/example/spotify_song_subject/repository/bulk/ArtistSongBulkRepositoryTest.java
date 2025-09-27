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

@DisplayName("ArtistSongBulkRepository 단위 테스트")

@RepositoryTestConfiguration
class ArtistSongBulkRepositoryTest {

    @Autowired
    private ArtistSongBulkRepository artistSongBulkRepository;

    @Autowired
    private ArtistSongRepository artistSongRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private Long artistId1;
    private Long artistId2;
    private Long songId1;
    private Long songId2;
    private List<ArtistSong> testArtistSongs;

    @BeforeEach
    void setUp() {
        // Create test data
        Album album = Album.of("Test Album", LocalDate.of(2023, 1, 1));
        Long albumId = albumRepository.save(album).block().getId();

        Artist artist1 = Artist.of("Artist 1");
        Artist artist2 = Artist.of("Artist 2");
        artistId1 = artistRepository.save(artist1).block().getId();
        artistId2 = artistRepository.save(artist2).block().getId();

        Song song1 = Song.builder()
            .title("Song 1")
            .albumId(albumId)
            .build();
        Song song2 = Song.builder()
            .title("Song 2")
            .albumId(albumId)
            .build();
        songId1 = songRepository.save(song1).block().getId();
        songId2 = songRepository.save(song2).block().getId();

        testArtistSongs = Arrays.asList(
            ArtistSong.builder()
                .artistId(artistId1)
                .songId(songId1)
                .build(),
            ArtistSong.builder()
                .artistId(artistId1)
                .songId(songId2)
                .build(),
            ArtistSong.builder()
                .artistId(artistId2)
                .songId(songId1)
                .build()
        );
    }

    @AfterEach
    void tearDown() {
        artistSongRepository.deleteAll().block();
        songRepository.deleteAll().block();
        artistRepository.deleteAll().block();
        albumRepository.deleteAll().block();
    }

    @Test
    @DisplayName("여러 아티스트-노래 관계를 일괄 삽입한다")
    void bulkInsertMultipleArtistSongs() {
        // when & then
        StepVerifier.create(artistSongBulkRepository.bulkInsert(testArtistSongs))
            .expectNext(3L)
            .verifyComplete();

        // verify data was actually inserted
        StepVerifier.create(artistSongRepository.count())
            .expectNext(3L)
            .verifyComplete();

        // verify specific relationships
        StepVerifier.create(artistSongRepository.findAll().filter(as -> as.getArtistId().equals(artistId1)).collectList())
            .assertNext(artistSongs -> {
                assertThat(artistSongs).hasSize(2);
                assertThat(artistSongs)
                    .extracting(ArtistSong::getSongId)
                    .containsExactlyInAnyOrder(songId1, songId2);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("빈 컬렉션을 삽입하면 0을 반환한다")
    void bulkInsertEmptyCollection() {
        // when & then
        StepVerifier.create(artistSongBulkRepository.bulkInsert(Collections.emptyList()))
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    @DisplayName("대량 데이터 일괄 삽입 테스트")
    void bulkInsertLargeDataSet() {
        // given
        List<ArtistSong> largeDataSet = new java.util.ArrayList<>();

        // Create more songs for testing
        for (int i = 0; i < 50; i++) {
            Song song = Song.builder()
                .title("Song " + i)
                .albumId(songRepository.findByTitle("Song 1").next().block().getAlbumId())
                .build();
            Long songId = songRepository.save(song).block().getId();

            // Create relationships
            largeDataSet.add(ArtistSong.builder()
                .artistId(artistId1)
                .songId(songId)
                .build());
            largeDataSet.add(ArtistSong.builder()
                .artistId(artistId2)
                .songId(songId)
                .build());
        }

        // when & then
        StepVerifier.create(artistSongBulkRepository.bulkInsert(largeDataSet))
            .expectNext(100L)
            .verifyComplete();

        // verify all data was inserted (plus initial test data)
        StepVerifier.create(artistSongRepository.count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    @DisplayName("중복된 아티스트-노래 관계도 삽입한다")
    void bulkInsertDuplicateRelationships() {
        // given - same artist and song combination multiple times
        List<ArtistSong> duplicates = Arrays.asList(
            ArtistSong.builder()
                .artistId(artistId1)
                .songId(songId1)
                .build(),
            ArtistSong.builder()
                .artistId(artistId1)
                .songId(songId1)
                .build(),
            ArtistSong.builder()
                .artistId(artistId1)
                .songId(songId1)
                .build()
        );

        // when & then
        StepVerifier.create(artistSongBulkRepository.bulkInsert(duplicates))
            .expectNext(3L)
            .verifyComplete();

        // verify all duplicates were inserted
        StepVerifier.create(artistSongRepository.count())
            .expectNext(3L)
            .verifyComplete();
    }
}