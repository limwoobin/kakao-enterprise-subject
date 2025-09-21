package com.example.spotify_song_subject.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializationRunner implements ApplicationRunner {

    private final GoogleDriveDownloader googleDriveDownloader;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Starting Data Initialization ===");

        try {
          googleDriveDownloader.downloadAndExtractFile();
          log.info("Data initialization completed successfully!");
        } catch (Exception e) {
            log.error("Failed to initialize data during startup", e);
            throw e;
        }

        log.info("=== Data Initialization Process Completed ===");
    }
}
