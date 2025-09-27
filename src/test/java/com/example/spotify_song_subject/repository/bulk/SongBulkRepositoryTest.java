package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.repository.AlbumRepository;
import com.example.spotify_song_subject.repository.RepositoryTestConfiguration;
import com.example.spotify_song_subject.repository.SongRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SongBulkRepository 단위 테스트")

@RepositoryTestConfiguration
class SongBulkRepositoryTest {

    @Autowired
    private SongBulkRepository songBulkRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private Long albumId;
    private List<Song> testSongs;

    @BeforeEach
    void setUp() {
        // Create an album first for foreign key reference
        Album album = Album.of("Test Album", LocalDate.of(2023, 1, 1));
        albumId = albumRepository.save(album).block().getId();

        testSongs = Arrays.asList(
            createSong("Song 1", albumId, 180, "Rock", new BigDecimal("0.8")),
            createSong("Song 2", albumId, 200, "Pop", new BigDecimal("0.7")),
            createSong("Song 3", albumId, 220, "Jazz", new BigDecimal("0.6"))
        );
    }

    @AfterEach
    void tearDown() {
        songRepository.deleteAll().block();
        albumRepository.deleteAll().block();
    }

    @Test
    @DisplayName("여러 노래를 일괄 삽입한다")
    void bulkInsertMultipleSongs() {
        // when & then
        StepVerifier.create(songBulkRepository.bulkInsert(testSongs))
            .expectNext(3L)
            .verifyComplete();

        // verify data was actually inserted
        StepVerifier.create(songRepository.count())
            .expectNext(3L)
            .verifyComplete();

        // verify specific song data
        StepVerifier.create(songRepository.findByTitle("Song 1").next())
            .assertNext(song -> {
                assertThat(song).isNotNull();
                assertThat(song.getTitle()).isEqualTo("Song 1");
                assertThat(song.getAlbumId()).isEqualTo(albumId);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("빈 컬렉션을 삽입하면 0을 반환한다")
    void bulkInsertEmptyCollection() {
        // when & then
        StepVerifier.create(songBulkRepository.bulkInsert(Collections.emptyList()))
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    @DisplayName("nullable 필드가 null인 노래도 삽입한다")
    void bulkInsertSongsWithNullFields() {
        // given
        List<Song> songsWithNulls = Arrays.asList(
            createSong("Song with nulls 1", albumId, null, null, null),
            createSong("Song with nulls 2", albumId, null, null, null)
        );

        // when & then
        StepVerifier.create(songBulkRepository.bulkInsert(songsWithNulls))
            .expectNext(2L)
            .verifyComplete();

        // verify data was inserted
        StepVerifier.create(songRepository.count())
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    @DisplayName("대량 데이터 일괄 삽입 테스트")
    void bulkInsertLargeDataSet() {
        // given
        List<Song> largeDataSet = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeDataSet.add(createSong("Song " + i, albumId, 180 + i, "Genre " + i, new BigDecimal(0.5 + (i * 0.001))));
        }

        // when & then
        StepVerifier.create(songBulkRepository.bulkInsert(largeDataSet))
            .expectNext(100L)
            .verifyComplete();

        // verify all data was inserted
        StepVerifier.create(songRepository.count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    @DisplayName("동일한 제목의 노래도 삽입한다")
    void bulkInsertDuplicateTitles() {
        // given
        List<Song> duplicateSongs = Arrays.asList(
            createSong("Same Song", albumId, 180, "Rock", new BigDecimal("0.8")),
            createSong("Same Song", albumId, 180, "Rock", new BigDecimal("0.8")),
            createSong("Same Song", albumId, 180, "Rock", new BigDecimal("0.8"))
        );

        // when & then
        StepVerifier.create(songBulkRepository.bulkInsert(duplicateSongs))
            .expectNext(3L)
            .verifyComplete();

        // verify all duplicates were inserted
        StepVerifier.create(songRepository.count())
            .expectNext(3L)
            .verifyComplete();
    }

    private Song createSong(String title, Long albumId, Integer duration, String genre, BigDecimal valence) {
        Song song = Song.builder()
            .title(title)
            .albumId(albumId)
            .energy(duration)  // Using energy field as duration proxy
            .genre(genre)
            .tempo(valence)  // Using tempo field as valence proxy
            .build();
        return song;
    }
}