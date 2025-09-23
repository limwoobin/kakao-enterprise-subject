package com.example.spotify_song_subject.loader;

import com.example.spotify_song_subject.application.SpotifyDataPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DataInitializationRunner 단위 테스트")
class DataInitializationRunnerTest {

    private GoogleDriveDownloader googleDriveDownloader;
    private SpotifyDataStreamReader spotifyDataStreamReader;
    private SpotifyDataPersistenceService spotifyDataPersistenceService;

    @BeforeEach
    void setUp() {
        googleDriveDownloader = mock(GoogleDriveDownloader.class);
        spotifyDataStreamReader = mock(SpotifyDataStreamReader.class);
        spotifyDataPersistenceService = mock(SpotifyDataPersistenceService.class);
    }

    @Test
    @DisplayName("DataInitializationRunner 인스턴스가 정상적으로 생성된다")
    void 인스턴스생성_정상() {
        // given
        DataInitializationRunner runner = new DataInitializationRunner(
            googleDriveDownloader,
            spotifyDataStreamReader,
            spotifyDataPersistenceService
        );

        // when & then
        assertThat(runner).isNotNull();
    }

    @Test
    @DisplayName("ApplicationRunner 인터페이스를 구현한다")
    void ApplicationRunner구현_확인() {
        // given
        DataInitializationRunner runner = new DataInitializationRunner(
            googleDriveDownloader,
            spotifyDataStreamReader,
            spotifyDataPersistenceService
        );

        // when & then
        assertThat(runner).isInstanceOf(org.springframework.boot.ApplicationRunner.class);
    }

    @Test
    @DisplayName("필수 의존성이 주입된다")
    void 의존성주입_확인() {
        // given & when
        DataInitializationRunner runner = new DataInitializationRunner(
            googleDriveDownloader,
            spotifyDataStreamReader,
            spotifyDataPersistenceService
        );

        // then
        assertThat(runner).hasFieldOrProperty("googleDriveDownloader");
        assertThat(runner).hasFieldOrProperty("spotifyDataStreamReader");
    }

}