package com.example.spotify_song_subject.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GoogleDriveDownloader 단위 테스트")
class GoogleDriveDownloaderTest {

    private GoogleDriveDownloader googleDriveDownloader;

    @BeforeEach
    void setUp() {
        googleDriveDownloader = new GoogleDriveDownloader();
    }

    @Test
    @DisplayName("GoogleDriveDownloader 인스턴스가 정상적으로 생성된다")
    void 인스턴스생성_정상() {
        // when & then
        assertThat(googleDriveDownloader).isNotNull();
    }
}