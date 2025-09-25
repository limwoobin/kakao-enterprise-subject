package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ActivitySuitability;
import com.example.spotify_song_subject.domain.InclusionStatus;
import com.example.spotify_song_subject.domain.Song;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SongRepository 단위 테스트")
@RepositoryTestConfiguration
class SongRepositoryTest {

    @Autowired
    private SongRepository songRepository;

    private Song testSong;

    @BeforeEach
    void setUp() {
        testSong = Song.builder()
                .albumId(1L)
                .title("Test Song")
                .lyrics("Test lyrics")
                .length(LocalTime.of(0, 3, 45))
                .musicKey("C Major")
                .tempo(new BigDecimal("120.0"))
                .loudnessDb(new BigDecimal("-5.0"))
                .timeSignature("4/4")
                .explicitContent(InclusionStatus.NOT_INCLUDED)
                .emotion("happy")
                .genre("pop")
                .popularity(75)
                .energy(80)
                .danceability(70)
                .positiveness(85)
                .speechiness(5)
                .liveness(10)
                .acousticness(20)
                .instrumentalness(0)
                .activitySuitabilityParty(ActivitySuitability.SUITABLE)
                .activitySuitabilityWork(ActivitySuitability.NOT_SUITABLE)
                .likeCount(0L)
                .build();
    }

    @AfterEach
    void tearDown() {
        // 테스트 격리를 위한 데이터 정리
        songRepository.deleteAll()
                .block();
    }

    @Test
    @DisplayName("곡을 저장한다")
    void saveSong() {
        // when & then
        StepVerifier.create(songRepository.save(testSong))
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getTitle()).isEqualTo("Test Song");
                    assertThat(saved.getAlbumId()).isEqualTo(1L);
                    assertThat(saved.getLyrics()).isEqualTo("Test lyrics");
                    assertThat(saved.getGenre()).isEqualTo("pop");
                    assertThat(saved.getPopularity()).isEqualTo(75);
                    assertThat(saved.getCreatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("제목과 앨범ID로 삭제되지 않은 곡을 조회한다")
    void findSongByTitleAndAlbumId() {
        // given
        Mono<Song> savedSong = songRepository.save(testSong);

        // when & then
        StepVerifier.create(
                savedSong.then(songRepository.findByTitleAndAlbumId("Test Song", 1L))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getTitle()).isEqualTo("Test Song");
            assertThat(found.getAlbumId()).isEqualTo(1L);
            assertThat(found.getDeletedAt()).isNull();
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("제목으로 삭제되지 않은 모든 곡을 조회한다")
    void findAllSongsByTitle() {
        // given
        Song song1 = Song.builder()
                .albumId(1L)
                .title("Same Title")
                .genre("pop")
                .build();
        Song song2 = Song.builder()
                .albumId(2L)
                .title("Same Title")
                .genre("rock")
                .build();
        Song song3 = Song.builder()
                .albumId(3L)
                .title("Different Title")
                .genre("jazz")
                .build();

        // when & then
        StepVerifier.create(
                songRepository.save(song1)
                    .then(songRepository.save(song2))
                    .then(songRepository.save(song3))
                    .thenMany(songRepository.findByTitle("Same Title"))
                    .collectList()
        )
        .assertNext(songs -> {
            assertThat(songs).hasSize(2);
            assertThat(songs).allMatch(song -> song.getTitle().equals("Same Title"));
            assertThat(songs).extracting(Song::getGenre)
                    .containsExactlyInAnyOrder("pop", "rock");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 곡을 조회하면 빈 결과를 반환한다")
    void findNonExistentSong() {
        // when & then
        StepVerifier.create(
                songRepository.findByTitleAndAlbumId("Non Existent", 999L)
        )
        .expectNextCount(0)
        .verifyComplete();

        StepVerifier.create(
                songRepository.findByTitle("Non Existent")
                    .collectList()
        )
        .assertNext(songs -> assertThat(songs).isEmpty())
        .verifyComplete();
    }

    @Test
    @DisplayName("ID로 곡을 조회한다")
    void findSongById() {
        // given
        Mono<Song> savedSong = songRepository.save(testSong);

        // when & then
        StepVerifier.create(
                savedSong.flatMap(song -> songRepository.findById(song.getId()))
        )
        .assertNext(found -> {
            assertThat(found).isNotNull();
            assertThat(found.getTitle()).isEqualTo("Test Song");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("곡의 음악 특성을 저장하고 조회한다")
    void saveSongWithMusicCharacteristics() {
        // when & then
        StepVerifier.create(songRepository.save(testSong))
                .assertNext(saved -> {
                    assertThat(saved.getTempo()).isEqualByComparingTo(new BigDecimal("120.0"));
                    assertThat(saved.getLoudnessDb()).isEqualByComparingTo(new BigDecimal("-5.0"));
                    assertThat(saved.getEnergy()).isEqualTo(80);
                    assertThat(saved.getDanceability()).isEqualTo(70);
                    assertThat(saved.getPositiveness()).isEqualTo(85);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("곡의 활동 적합도를 저장하고 조회한다")
    void saveSongWithActivitySuitability() {
        // when & then
        StepVerifier.create(songRepository.save(testSong))
                .assertNext(saved -> {
                    assertThat(saved.getActivitySuitabilityParty()).isEqualTo(ActivitySuitability.SUITABLE);
                    assertThat(saved.getActivitySuitabilityWork()).isEqualTo(ActivitySuitability.NOT_SUITABLE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("곡을 삭제한다")
    void deleteSong() {
        // given
        Mono<Song> savedSong = songRepository.save(testSong);

        // when & then
        StepVerifier.create(
                savedSong.flatMap(song ->
                    songRepository.deleteById(song.getId())
                        .then(songRepository.findById(song.getId()))
                )
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    @DisplayName("모든 곡을 조회한다")
    void findAllSongs() {
        // given
        Song song1 = Song.builder()
                .albumId(1L)
                .title("Song 1")
                .genre("pop")
                .build();
        Song song2 = Song.builder()
                .albumId(2L)
                .title("Song 2")
                .genre("rock")
                .build();

        // when & then
        StepVerifier.create(
                songRepository.save(song1)
                    .then(songRepository.save(song2))
                    .thenMany(songRepository.findAll())
                    .collectList()
        )
        .assertNext(songs -> {
            assertThat(songs).hasSize(2);
            assertThat(songs).extracting(Song::getTitle)
                    .containsExactlyInAnyOrder("Song 1", "Song 2");
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("곡 개수를 조회한다")
    void countSongs() {
        // given
        Song song1 = Song.builder()
                .albumId(1L)
                .title("Song 1")
                .genre("pop")
                .build();
        Song song2 = Song.builder()
                .albumId(2L)
                .title("Song 2")
                .genre("rock")
                .build();
        Song song3 = Song.builder()
                .albumId(3L)
                .title("Song 3")
                .genre("jazz")
                .build();

        // when & then
        StepVerifier.create(
                songRepository.save(song1)
                    .then(songRepository.save(song2))
                    .then(songRepository.save(song3))
                    .then(songRepository.count())
        )
        .expectNext(3L)
        .verifyComplete();
    }
}