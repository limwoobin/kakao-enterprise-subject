package com.example.spotify_song_subject.loader;

import com.example.spotify_song_subject.application.SpotifyDataPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "data.initialization.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DataInitializationRunner {

    private final GoogleDriveDownloader googleDriveDownloader;
    private final SpotifyDataStreamReader spotifyDataStreamReader;
    private final SpotifyDataPersistenceService spotifyDataPersistenceService;

    @Value("${data.directory:data}")
    private String dataDirectory;

    @Value("${data.skip-download-if-exists:true}")
    private boolean skipDownloadIfExists;

    /**
     * ApplicationReadyEvent를 사용하여 애플리케이션이 완전히 준비된 후 실행
     * 이렇게 하면 모든 빈이 초기화되고 스키마가 생성된 후에 실행됨
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== Starting Data Initialization (After Application Ready) ===");

        try {
            Path dataPath = Paths.get(dataDirectory);
            if (!Files.exists(dataPath)) {
                log.error("Data directory does not exist: {}", dataPath.toAbsolutePath());
                throw new IllegalStateException(String.format("Data directory not found: %s.", dataPath.toAbsolutePath()));
            }

            Path jsonFilePath = Paths.get(dataDirectory, "900k Definitive Spotify Dataset.json");
            boolean fileExists = Files.exists(jsonFilePath);

            if (!fileExists || !skipDownloadIfExists) {
                log.info("Downloading data from Google Drive...");
                googleDriveDownloader.downloadAndExtractFile();

                if (!Files.exists(jsonFilePath)) {
                    log.error("JSON file still missing after download: {}", jsonFilePath.toAbsolutePath());
                    throw new IllegalStateException(String.format("Failed to download Spotify dataset. File not found: %s", jsonFilePath.toAbsolutePath()));
                }
            } else {
                log.info("Data file already exists. Skipping download.");
            }

            processSpotifyData();
            log.info("✅ === Data Initialization Process Completed Successfully ===");
        } catch (Exception e) {
            log.error("Failed to initialize data during startup", e);
            throw new RuntimeException("Data initialization failed", e);
        }
    }

    /**
     * JSON 파일을 스트리밍으로 읽어 처리
     */
    private void processSpotifyData() {
        spotifyDataStreamReader.streamSpotifyDataInBatches()
            .onBackpressureBuffer(10)
            .flatMap(spotifyDataPersistenceService::processSongBatch)
            .doOnComplete(() -> log.info("✅ Successfully processed Spotify dataset"))
            .doOnError(error -> log.error("❌ Error processing Spotify data", error))
            .blockLast();
    }
}
