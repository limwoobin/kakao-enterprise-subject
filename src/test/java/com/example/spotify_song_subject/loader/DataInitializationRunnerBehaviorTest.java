package com.example.spotify_song_subject.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DataInitializationRunner 동작 검증 테스트")
class DataInitializationRunnerBehaviorTest {

    private DataInitializationRunner dataInitializationRunner;
    private GoogleDriveDownloader googleDriveDownloader;
    private SpotifyDataStreamReader spotifyDataStreamReader;
    private ApplicationArguments applicationArguments;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        googleDriveDownloader = mock(GoogleDriveDownloader.class);
        spotifyDataStreamReader = mock(SpotifyDataStreamReader.class);
        applicationArguments = mock(ApplicationArguments.class);

        dataInitializationRunner = new DataInitializationRunner(
            googleDriveDownloader,
            spotifyDataStreamReader
        );
    }

    @Test
    @DisplayName("애플리케이션 시작 시 run 메서드가 실행되어야 한다")
    void 애플리케이션시작시_run실행() {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", false);

        Path jsonFile = tempDir.resolve("900k Definitive Spotify Dataset.json");

        // 다운로드 후 파일이 생성된다고 가정
        doAnswer(invocation -> {
            Files.createFile(jsonFile);
            return null;
        }).when(googleDriveDownloader).downloadAndExtractFile();

        when(spotifyDataStreamReader.streamSpotifyDataInBatches())
            .thenReturn(Flux.empty());

        // when
        assertThatCode(() -> dataInitializationRunner.run(applicationArguments))
            .doesNotThrowAnyException();

        // then - 데이터 초기화 동작이 실행되었는지 검증
        verify(googleDriveDownloader, times(1)).downloadAndExtractFile();
        verify(spotifyDataStreamReader, times(1)).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("데이터 파일이 이미 존재하고 skip 설정이 true면 다운로드를 건너뛴다")
    void 파일존재_skip설정_다운로드건너뜀() throws IOException {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", true);

        // 이미 파일이 존재하는 상황
        Path jsonFile = tempDir.resolve("900k Definitive Spotify Dataset.json");
        Files.createFile(jsonFile);

        when(spotifyDataStreamReader.streamSpotifyDataInBatches())
            .thenReturn(Flux.empty());

        // when
        dataInitializationRunner.run(applicationArguments);

        // then - 다운로드는 건너뛰고 스트리밍만 실행
        verify(googleDriveDownloader, never()).downloadAndExtractFile();
        verify(spotifyDataStreamReader, times(1)).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("데이터 파일이 없으면 다운로드를 실행한다")
    void 파일없음_다운로드실행() {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", true);

        Path jsonFile = tempDir.resolve("900k Definitive Spotify Dataset.json");

        // 다운로드 후 파일 생성
        doAnswer(invocation -> {
            Files.createFile(jsonFile);
            return null;
        }).when(googleDriveDownloader).downloadAndExtractFile();

        when(spotifyDataStreamReader.streamSpotifyDataInBatches())
            .thenReturn(Flux.empty());

        // when
        dataInitializationRunner.run(applicationArguments);

        // then - 다운로드와 스트리밍 모두 실행
        verify(googleDriveDownloader, times(1)).downloadAndExtractFile();
        verify(spotifyDataStreamReader, times(1)).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("skip 설정이 false면 파일이 있어도 다운로드를 실행한다")
    void skip설정false_항상다운로드() throws IOException {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", false);

        // 파일이 이미 존재
        Path jsonFile = tempDir.resolve("900k Definitive Spotify Dataset.json");
        Files.createFile(jsonFile);

        when(spotifyDataStreamReader.streamSpotifyDataInBatches())
            .thenReturn(Flux.empty());

        // when
        dataInitializationRunner.run(applicationArguments);

        // then - skip이 false이므로 다운로드 실행
        verify(googleDriveDownloader, times(1)).downloadAndExtractFile();
        verify(spotifyDataStreamReader, times(1)).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("데이터 스트리밍 처리가 실행되어야 한다")
    void 데이터스트리밍_처리실행() throws IOException {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", true);

        Path jsonFile = tempDir.resolve("900k Definitive Spotify Dataset.json");
        Files.createFile(jsonFile);

        // 스트리밍 데이터는 Mock으로만 처리 (실제 데이터 파싱하지 않음)
        List<Map<String, Object>> mockBatch = List.of(
            Map.of("mock", "data1"),
            Map.of("mock", "data2")
        );
        when(spotifyDataStreamReader.streamSpotifyDataInBatches())
            .thenReturn(Flux.just(mockBatch));

        // when
        dataInitializationRunner.run(applicationArguments);

        // then - 스트리밍 처리가 실행되었는지만 검증
        verify(spotifyDataStreamReader, times(1)).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("데이터 디렉토리가 없으면 예외가 발생한다")
    void 디렉토리없음_예외발생() {
        // given
        Path nonExistentDir = tempDir.resolve("non-existent");
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", nonExistentDir.toString());

        // when & then
        assertThatThrownBy(() -> dataInitializationRunner.run(applicationArguments))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Data initialization failed")
            .hasCauseInstanceOf(IllegalStateException.class);

        // 초기화가 시작도 되지 않음
        verify(googleDriveDownloader, never()).downloadAndExtractFile();
        verify(spotifyDataStreamReader, never()).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("다운로드 실패 시 예외가 발생한다")
    void 다운로드실패_예외발생() {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", false);

        // 다운로드 실패 시뮬레이션
        doThrow(new RuntimeException("Download failed"))
            .when(googleDriveDownloader).downloadAndExtractFile();

        // when & then
        assertThatThrownBy(() -> dataInitializationRunner.run(applicationArguments))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Data initialization failed");

        verify(googleDriveDownloader, times(1)).downloadAndExtractFile();
        verify(spotifyDataStreamReader, never()).streamSpotifyDataInBatches();
    }

    @Test
    @DisplayName("스트리밍 처리 실패 시 예외가 발생한다")
    void 스트리밍실패_예외발생() throws IOException {
        // given
        ReflectionTestUtils.setField(dataInitializationRunner, "dataDirectory", tempDir.toString());
        ReflectionTestUtils.setField(dataInitializationRunner, "skipDownloadIfExists", true);

        Path jsonFile = tempDir.resolve("900k Definitive Spotify Dataset.json");
        Files.createFile(jsonFile);

        // 스트리밍 처리 실패
        when(spotifyDataStreamReader.streamSpotifyDataInBatches())
            .thenReturn(Flux.error(new RuntimeException("Streaming failed")));

        // when & then
        assertThatThrownBy(() -> dataInitializationRunner.run(applicationArguments))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Data initialization failed");

        verify(spotifyDataStreamReader, times(1)).streamSpotifyDataInBatches();
    }
}