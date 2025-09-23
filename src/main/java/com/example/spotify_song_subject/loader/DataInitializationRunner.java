package com.example.spotify_song_subject.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializationRunner implements ApplicationRunner {

    private final GoogleDriveDownloader googleDriveDownloader;
    private final SpotifyDataStreamReader spotifyDataStreamReader;

    @Value("${data.directory:data}")
    private String dataDirectory;

    @Value("${data.skip-download-if-exists:true}")
    private boolean skipDownloadIfExists;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Starting Data Initialization ===");

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

            log.info("Starting to process Spotify dataset...");
            processSpotifyData();
            log.info("=== Data Initialization Process Completed ===");
        } catch (Exception e) {
            log.error("Failed to initialize data during startup", e);
            throw new RuntimeException("Data initialization failed", e);
        }
    }

    /**
     * JSON 파일을 스트리밍으로 읽어 처리
     */
    private void processSpotifyData() {
        AtomicInteger recordCount = new AtomicInteger(0);

        spotifyDataStreamReader.streamSpotifyDataInBatches()
            .onBackpressureBuffer(10)
            .doOnNext(batch -> {
                // TODO: 추후 데이터베이스 저장 로직 추가
                int total = recordCount.addAndGet(batch.size());
                if (total % 10000 == 0) {
                    log.info("Processed {} records...", total);
                }
            })
            .doOnComplete(() ->
                log.info("✅ Successfully processed {} records from Spotify dataset", recordCount.get()))
            .doOnError(error ->
                log.error("❌ Error processing Spotify data", error))
            .blockLast();
    }
}
