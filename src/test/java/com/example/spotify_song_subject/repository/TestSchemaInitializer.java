package com.example.spotify_song_subject.repository;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * R2DBC 테스트용 데이터베이스 초기화 설정
 * schema.sql을 실행하여 테스트 데이터베이스 테이블을 생성합니다.
 */
@TestComponent
public class TestSchemaInitializer {

    private final DatabaseClient databaseClient;
    private boolean initialized = false;

    public TestSchemaInitializer(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent() {
        if (!initialized) {
            initializeSchema();
            initialized = true;
        }
    }

    private void initializeSchema() {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            String schema = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            // Remove comments and split by semicolon
            String[] statements = schema.split(";");
            for (String statement : statements) {
                String trimmedStatement = statement.trim()
                    .replaceAll("--.*$", "")  // Remove single-line comments
                    .replaceAll("/\\*.*?\\*/", "")  // Remove multi-line comments
                    .trim();

                if (!trimmedStatement.isEmpty()) {
                    // Execute each SQL statement
                    try {
                        databaseClient.sql(trimmedStatement)
                            .then()
                            .block();
                    } catch (Exception e) {
                        // Ignore errors for DROP statements or IF EXISTS clauses
                        if (!trimmedStatement.toLowerCase().contains("drop") &&
                            !trimmedStatement.toLowerCase().contains("if exists")) {
                            System.err.println("Failed to execute: " + trimmedStatement.substring(0, Math.min(50, trimmedStatement.length())));
                            throw e;
                        }
                    }
                }
            }

            System.out.println("Test database schema initialized successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test database schema", e);
        }
    }
}