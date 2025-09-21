package com.example.spotify_song_subject.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;

@Slf4j
@Component
public class GoogleDriveDownloader {

    @Value("${google.drive.file.id:1VDXbTqEH15B3oyHCXjgAI2lHa9aSmCcy}")
    private String fileId;

    @Value("${data.directory:data}")
    private String dataDirectory;

    private static final String DOWNLOAD_URL_TEMPLATE = "https://drive.google.com/uc?export=download&id=%s&confirm=t";

    public void downloadAndExtractFile() {
        try {
            Path dataPath = Paths.get(dataDirectory);
            ensureDirectoryExists(dataPath);

            Path zipFile = downloadFile(fileId, dataPath);
            extractZipFile(zipFile, dataPath);

            Files.deleteIfExists(zipFile);
            log.info("✅ Data initialization completed successfully!");
        } catch (Exception e) {
            log.error("❌ Failed to download and extract file", e);
            throw new RuntimeException("Failed to initialize data from Google Drive", e);
        }
    }

    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.info("📁 Created data directory: {}", directory.toAbsolutePath());
        }
    }

    private Path downloadFile(String fileId, Path targetDirectory) throws IOException {
        Path targetFile = targetDirectory.resolve("downloaded_file.zip");

        try {
            // First attempt - basic download URL
            String downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, fileId);
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(false);

            int responseCode = connection.getResponseCode();

            // Handle redirect to Google Drive confirmation page
            if (responseCode == 302 || responseCode == 303) {
                String location = connection.getHeaderField("Location");
                if (location != null) {
                    url = new URL(location);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    responseCode = connection.getResponseCode();
                }
            }

            // Check if we got HTML instead of the actual file
            String contentType = connection.getContentType();
            if (contentType != null && contentType.contains("text/html")) {
                log.info("⚠️ Google Drive virus scan warning detected. Extracting UUID...");

                // Read HTML to extract UUID
                StringBuilder html = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        html.append(line);
                    }
                }

                String uuidPattern = "name=\"uuid\" value=\"([^\"]+)\"";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(uuidPattern);
                java.util.regex.Matcher matcher = pattern.matcher(html.toString());

                if (matcher.find()) {
                    String uuid = matcher.group(1);
                    log.info("📝 Found UUID: {}", uuid);

                    // Build new download URL with UUID
                    downloadUrl = String.format(
                        "https://drive.usercontent.google.com/download?id=%s&export=download&confirm=t&uuid=%s",
                        fileId, uuid
                    );

                    // Retry download with UUID
                    url = new URL(downloadUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(60000);
                } else {
                    throw new IOException("Could not extract UUID from Google Drive warning page");
                }
            }

            // Download the actual file
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                log.info("📊 File size: {} MB", contentLength / 1024 / 1024);
            }

            try (InputStream is = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {

                byte[] buffer = new byte[65536];
                int bytesRead;
                long totalBytes = 0;
                long lastLog = System.currentTimeMillis();

                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    if (System.currentTimeMillis() - lastLog > 5000) {
                        log.info("Downloaded {} MB so far...", totalBytes / 1024 / 1024);
                        lastLog = System.currentTimeMillis();
                    }
                }
            }

            return targetFile;

        } catch (Exception e) {
            Files.deleteIfExists(targetFile);
            throw new IOException("Failed to download file from Google Drive: " + e.getMessage(), e);
        }
    }

    private void extractZipFile(Path zipFilePath, Path targetDirectory) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int fileCount = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path targetPath = targetDirectory.resolve(entry.getName());

                // Path traversal 공격 방지
                if (!targetPath.normalize().startsWith(targetDirectory.normalize())) {
                    throw new IOException("Invalid zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());

                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath);
                        fileCount++;
                    }
                }
            }

            log.info("Extracted {} files", fileCount);
        }
    }
}
