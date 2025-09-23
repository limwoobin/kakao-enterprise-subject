package com.example.spotify_song_subject.loader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spotify JSON 데이터셋을 리액티브 스트림으로 읽어 처리하는 Reader
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotifyDataStreamReader {

    @Value("${data.directory:data}")
    private String dataDirectory;

    @Value("${data.batch.size:1000}")
    private int batchSize;

    @Value("${data.buffer.size:65536}")
    private int bufferSize;

    private static final String JSON_FILE_NAME = "900k Definitive Spotify Dataset.json";
    private static final String SIMILAR_SONGS_FIELD = "Similar Songs";

    private static final int EXPECTED_SONG_FIELDS = 30;
    private static final int EXPECTED_SIMILAR_SONGS = 5;
    private static final int EXPECTED_SIMILAR_SONG_FIELDS = 10;

    private static final Scheduler CUSTOM_SCHEDULER = Schedulers.newSingle("spotify-parser");

    /**
     * Spotify 데이터를 배치 단위로 스트리밍
     */
    public Flux<List<Map<String, Object>>> streamSpotifyDataInBatches() {
        Path jsonFilePath = Paths.get(dataDirectory, JSON_FILE_NAME);
        return createStreamingFlux(jsonFilePath)
            .buffer(batchSize);
    }

    /**
     * 실제 JSON 파싱을 수행하는 Flux 생성
     * - 단일 스레드 스케줄러로 순차 처리
     * - 스레드 재사용으로 효율적인 자원 관리
     */
    private Flux<Map<String, Object>> createStreamingFlux(Path jsonFilePath) {
        return Flux.<Map<String, Object>>create(sink -> {
            JsonFactory jsonFactory = new JsonFactory();

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(jsonFilePath.toFile()), bufferSize);
                 JsonParser jsonParser = jsonFactory.createParser(bis)) {
                if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                    sink.error(new IllegalStateException("Expected JSON array"));
                    return;
                }

                while (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                    Map<String, Object> songData = parseSongObject(jsonParser);
                    if (!songData.isEmpty()) {
                        sink.next(songData);
                    }
                }

                sink.complete();
            } catch (Exception e) {
                log.error("Fatal error during JSON streaming", e);
                sink.error(e);
            }
        })
        .subscribeOn(CUSTOM_SCHEDULER);
    }

    /**
     * 개별 곡 JSON 객체를 Map으로 파싱
     */
    private Map<String, Object> parseSongObject(JsonParser jsonParser) throws IOException {
        Map<String, Object> songData = new HashMap<>(EXPECTED_SONG_FIELDS);

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.currentName();
            jsonParser.nextToken();

            if (fieldName != null) {
                if (SIMILAR_SONGS_FIELD.equals(fieldName)) {
                    songData.put(fieldName, parseSimilarSongs(jsonParser));
                } else {
                    Object value = extractValueFromToken(jsonParser);
                    songData.put(fieldName, value);
                }
            }
        }

        return songData;
    }

    /**
     * Similar Songs 배열 파싱
     */
    private List<Map<String, Object>> parseSimilarSongs(JsonParser jsonParser) throws IOException {
        List<Map<String, Object>> similarSongs = new ArrayList<>(EXPECTED_SIMILAR_SONGS);

        if (jsonParser.getCurrentToken() == JsonToken.START_ARRAY) {
            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
                    Map<String, Object> similarSong = new HashMap<>(EXPECTED_SIMILAR_SONG_FIELDS);

                    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                        String field = jsonParser.currentName();
                        jsonParser.nextToken();
                        if (field != null) {
                            similarSong.put(field, extractValueFromToken(jsonParser));
                        }
                    }

                    similarSongs.add(similarSong);
                }
            }
        }

        return similarSongs;
    }

    /**
     * JSON 토큰을 Java 타입으로 변환
     */
    private Object extractValueFromToken(JsonParser parser) throws IOException {
        JsonToken token = parser.getCurrentToken();

        return switch (token) {
            case VALUE_NUMBER_INT -> parser.getLongValue();
            case VALUE_NUMBER_FLOAT -> parser.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            default -> parser.getText();
        };
    }
}