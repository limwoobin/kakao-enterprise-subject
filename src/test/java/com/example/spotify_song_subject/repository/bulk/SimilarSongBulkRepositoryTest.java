package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.repository.*;
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

@DisplayName("SimilarSongBulkRepository 단위 테스트")

@RepositoryTestConfiguration
class SimilarSongBulkRepositoryTest {

    @Autowired
    private SimilarSongBulkRepository similarSongBulkRepository;

    @Autowired
    private SimilarSongRepository similarSongRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private Long songId1;
    private Long songId2;
    private List<SimilarSong> testSimilarSongs;

    @BeforeEach
    void setUp() {
        // Create test data
        Album album = Album.of("Test Album", LocalDate.of(2023, 1, 1));
        Long albumId = albumRepository.save(album).block().getId();

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

        testSimilarSongs = Arrays.asList(
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Similar Artist 1")
                .similarSongTitle("Similar Song 1")
                .similarityScore(new BigDecimal("0.95"))
                .build(),
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Similar Artist 2")
                .similarSongTitle("Similar Song 2")
                .similarityScore(new BigDecimal("0.85"))
                .build(),
            SimilarSong.builder()
                .songId(songId2)
                .similarArtistName("Similar Artist 3")
                .similarSongTitle("Similar Song 3")
                .similarityScore(new BigDecimal("0.75"))
                .build()
        );
    }

    @AfterEach
    void tearDown() {
        similarSongRepository.deleteAll().block();
        songRepository.deleteAll().block();
        albumRepository.deleteAll().block();
    }

    @Test
    @DisplayName("여러 유사 노래 관계를 일괄 삽입한다")
    void bulkInsertMultipleSimilarSongs() {
        // when & then
        StepVerifier.create(similarSongBulkRepository.bulkInsert(testSimilarSongs))
            .expectNext(3L)
            .verifyComplete();

        // verify data was actually inserted
        StepVerifier.create(similarSongRepository.count())
            .expectNext(3L)
            .verifyComplete();

        // verify specific relationships
        StepVerifier.create(similarSongRepository.findBySongId(songId1).collectList())
            .assertNext(similarSongs -> {
                assertThat(similarSongs).hasSize(2);
                assertThat(similarSongs)
                    .extracting(song -> song.getSimilarityScore().stripTrailingZeros())
                    .containsExactlyInAnyOrder(new BigDecimal("0.95"), new BigDecimal("0.85"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("빈 컬렉션을 삽입하면 0을 반환한다")
    void bulkInsertEmptyCollection() {
        // when & then
        StepVerifier.create(similarSongBulkRepository.bulkInsert(Collections.emptyList()))
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    @DisplayName("0 점수를 가진 데이터도 삽입한다")
    void bulkInsertWithZeroScore() {
        // given
        List<SimilarSong> songsWithZeroScore = Arrays.asList(
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Artist")
                .similarSongTitle("Song")
                .similarityScore(BigDecimal.ZERO)  // 0 score
                .build(),
            SimilarSong.builder()
                .songId(songId2)
                .similarArtistName("Another Artist")
                .similarSongTitle("Another Song")
                .similarityScore(new BigDecimal("0.0"))
                .build()
        );

        // when & then
        StepVerifier.create(similarSongBulkRepository.bulkInsert(songsWithZeroScore))
            .expectNext(2L)
            .verifyComplete();

        // verify data was inserted
        StepVerifier.create(similarSongRepository.count())
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    @DisplayName("대량 데이터 일괄 삽입 테스트")
    void bulkInsertLargeDataSet() {
        // given
        List<SimilarSong> largeDataSet = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeDataSet.add(
                SimilarSong.builder()
                    .songId(i % 2 == 0 ? songId1 : songId2)
                    .similarArtistName("Artist " + i)
                    .similarSongTitle("Song " + i)
                    .similarityScore(new BigDecimal(0.5 + (i * 0.005)))
                    .build()
            );
        }

        // when & then
        StepVerifier.create(similarSongBulkRepository.bulkInsert(largeDataSet))
            .expectNext(100L)
            .verifyComplete();

        // verify all data was inserted
        StepVerifier.create(similarSongRepository.count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    @DisplayName("동일한 유사 노래 정보도 중복 삽입한다")
    void bulkInsertDuplicateSimilarSongs() {
        // given - same similar song multiple times
        List<SimilarSong> duplicates = Arrays.asList(
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Same Artist")
                .similarSongTitle("Same Song")
                .similarityScore(new BigDecimal("0.9"))
                .build(),
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Same Artist")
                .similarSongTitle("Same Song")
                .similarityScore(new BigDecimal("0.9"))
                .build(),
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Same Artist")
                .similarSongTitle("Same Song")
                .similarityScore(new BigDecimal("0.9"))
                .build()
        );

        // when & then
        StepVerifier.create(similarSongBulkRepository.bulkInsert(duplicates))
            .expectNext(3L)
            .verifyComplete();

        // verify all duplicates were inserted
        StepVerifier.create(similarSongRepository.count())
            .expectNext(3L)
            .verifyComplete();
    }

    @Test
    @DisplayName("특수 문자가 포함된 아티스트명과 노래 제목도 삽입한다")
    void bulkInsertWithSpecialCharacters() {
        // given
        List<SimilarSong> specialCharSongs = Arrays.asList(
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Artist's Name")
                .similarSongTitle("Song's Title")
                .similarityScore(new BigDecimal("0.8"))
                .build(),
            SimilarSong.builder()
                .songId(songId2)
                .similarArtistName("아티스트")
                .similarSongTitle("한글 제목")
                .similarityScore(new BigDecimal("0.7"))
                .build(),
            SimilarSong.builder()
                .songId(songId1)
                .similarArtistName("Artist & Co.")
                .similarSongTitle("Rock & Roll")
                .similarityScore(new BigDecimal("0.9"))
                .build()
        );

        // when & then
        StepVerifier.create(similarSongBulkRepository.bulkInsert(specialCharSongs))
            .expectNext(3L)
            .verifyComplete();

        // verify data was inserted correctly
        StepVerifier.create(similarSongRepository.findBySongId(songId1).collectList())
            .assertNext(similarSongs -> {
                assertThat(similarSongs).hasSize(2);
                assertThat(similarSongs)
                    .extracting(SimilarSong::getSimilarArtistName)
                    .containsExactlyInAnyOrder("Artist's Name", "Artist & Co.");
            })
            .verifyComplete();
    }
}