package com.example.spotify_song_subject.mapper;

import com.example.spotify_song_subject.dto.SimilarSongDto;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON Map 데이터를 도메인 DTO로 변환하는 유틸리티
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpotifyDataMapper {

    /**
     * Map 데이터를 SpotifySongDto로 변환
     */
    public static SpotifySongDto mapToSpotifySongDto(Map<String, Object> songData) {
        try {
            SpotifySongDto.SpotifySongDtoBuilder builder = SpotifySongDto.builder();

            builder.artists(getString(songData, "Artist(s)"));
            builder.songTitle(getString(songData, "song"));
            builder.lyrics(getString(songData, "text"));
            builder.length(getString(songData, "Length"));
            builder.emotion(getString(songData, "emotion"));
            builder.genre(getString(songData, "Genre"));
            builder.albumTitle(getString(songData, "Album"));
            builder.releaseDate(getString(songData, "Release Date"));
            builder.musicKey(getString(songData, "Key"));

            // 숫자 필드 매핑
            builder.tempo(getBigDecimal(songData, "Tempo"));
            builder.loudnessDb(getBigDecimal(songData, "Loudness (db)"));
            builder.timeSignature(getString(songData, "Time signature"));
            builder.explicit(getString(songData, "Explicit"));

            // 음악 특성 필드
            builder.popularity(getInteger(songData, "Popularity"));
            builder.energy(getInteger(songData, "Energy"));
            builder.danceability(getInteger(songData, "Danceability"));
            builder.positiveness(getInteger(songData, "Positiveness"));
            builder.speechiness(getInteger(songData, "Speechiness"));
            builder.liveness(getInteger(songData, "Liveness"));
            builder.acousticness(getInteger(songData, "Acousticness"));
            builder.instrumentalness(getInteger(songData, "Instrumentalness"));

            // 활동 적합도 필드
            builder.goodForParty(getInteger(songData, "Good for Party"));
            builder.goodForWorkStudy(getInteger(songData, "Good for Work/Study"));
            builder.goodForRelaxationMeditation(getInteger(songData, "Good for Relaxation/Meditation"));
            builder.goodForExercise(getInteger(songData, "Good for Exercise"));
            builder.goodForRunning(getInteger(songData, "Good for Running"));
            builder.goodForYogaStretching(getInteger(songData, "Good for Yoga/Stretching"));
            builder.goodForDriving(getInteger(songData, "Good for Driving"));
            builder.goodForSocialGatherings(getInteger(songData, "Good for Social Gatherings"));
            builder.goodForMorningRoutine(getInteger(songData, "Good for Morning Routine"));

            // Similar Songs 처리
            List<SimilarSongDto> similarSongs = mapSimilarSongs(songData.get("Similar Songs"));
            builder.similarSongs(similarSongs);
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to map song data to DTO", e);
            return null;
        }
    }

    /**
     * Similar Songs 리스트 변환
     */
    @SuppressWarnings("unchecked")
    private static List<SimilarSongDto> mapSimilarSongs(Object similarSongsData) {
        List<SimilarSongDto> similarSongs = new ArrayList<>();

        if (similarSongsData instanceof List) {
            List<Map<String, Object>> similarSongsList = (List<Map<String, Object>>) similarSongsData;

            for (Map<String, Object> similarSongData : similarSongsList) {
                // Similar Song 데이터는 동적 필드명을 가짐 (Similar Artist 1, Similar Song 1 등)
                String artistName = null;
                String songTitle = null;
                BigDecimal similarityScore = null;

                for (Map.Entry<String, Object> entry : similarSongData.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("Similar Artist")) {
                        artistName = getString(similarSongData, key);
                    } else if (key.startsWith("Similar Song")) {
                        songTitle = getString(similarSongData, key);
                    } else if ("Similarity Score".equals(key)) {
                        similarityScore = getBigDecimal(similarSongData, key);
                    }
                }

                if (artistName != null && songTitle != null && similarityScore != null) {
                    SimilarSongDto similarSong = SimilarSongDto.builder()
                        .artistName(artistName)
                        .songTitle(songTitle)
                        .similarityScore(similarityScore)
                        .build();

                    similarSongs.add(similarSong);
                }
            }
        }

        return similarSongs;
    }

    private static String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private static BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.error("Failed to parse BigDecimal for key: {}, value: {}", key, value);
                return null;
            }
        }

        return null;
    }

    private static Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse Integer for key: {}, value: {}", key, value);
                return null;
            }
        }

        return null;
    }
}
