package com.example.spotify_song_subject.mapper;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SimilarSongDto;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO를 도메인 객체로 변환하는 유틸리티
 * 책임: SpotifySongDto → Domain Entity 변환
 */
@Slf4j
public class SpotifyDomainMapper {

    private SpotifyDomainMapper() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * SpotifySongDto에서 Artist 엔티티 리스트 추출
     */
    public static List<Artist> extractArtists(SpotifySongDto dto) {
        if (dto.getArtists() == null || dto.getArtists().isEmpty()) {
            return Collections.emptyList();
        }

        String[] artistNames = dto.getArtists().split(",");
        List<Artist> artists = new ArrayList<>();

        for (String artistName : artistNames) {
            String trimmedName = artistName.trim();
            if (!trimmedName.isEmpty()) {
                artists.add(createArtist(trimmedName));
            }
        }

        return artists;
    }

    /**
     * SpotifySongDto에서 Album 엔티티 추출
     */
    public static Album extractAlbum(SpotifySongDto dto) {
        String albumTitle = dto.getAlbumTitle();
        if (albumTitle == null || albumTitle.isEmpty()) {
            return createDefaultAlbum();
        }

        LocalDate releaseDate = parseReleaseDate(dto.getReleaseDate());
        return createAlbum(albumTitle, releaseDate);
    }

    /**
     * SpotifySongDto를 Song 엔티티로 변환
     */
    public static Song convertToSong(SpotifySongDto dto, Long albumId) {
        Song song = new Song();

        // 기본 정보
        setFieldValue(song, "albumId", albumId);
        setFieldValue(song, "title", dto.getSongTitle() != null ? dto.getSongTitle() : "Unknown Song");
        setFieldValue(song, "lyrics", dto.getLyrics());
        setFieldValue(song, "length", parseTime(dto.getLength()));
        setFieldValue(song, "musicKey", dto.getMusicKey());
        setFieldValue(song, "tempo", normalizeTempo(dto.getTempo()));
        setFieldValue(song, "loudnessDb", normalizeLoudness(dto.getLoudnessDb()));
        setFieldValue(song, "timeSignature", dto.getTimeSignature());

        // Explicit 상태 변환
        setFieldValue(song, "explicitContent", parseInclusionStatus(dto.getExplicit()));

        // 감정 및 장르
        setFieldValue(song, "emotion", dto.getEmotion());
        setFieldValue(song, "genre", dto.getGenre());

        // 음악 특성 (0-100 범위)
        setFieldValue(song, "popularity", parseIntegerWithRange(dto.getPopularity(), 0, 100));
        setFieldValue(song, "energy", parseIntegerWithRange(dto.getEnergy(), 0, 100));
        setFieldValue(song, "danceability", parseIntegerWithRange(dto.getDanceability(), 0, 100));
        setFieldValue(song, "positiveness", parseIntegerWithRange(dto.getPositiveness(), 0, 100));
        setFieldValue(song, "speechiness", parseIntegerWithRange(dto.getSpeechiness(), 0, 100));
        setFieldValue(song, "liveness", parseIntegerWithRange(dto.getLiveness(), 0, 100));
        setFieldValue(song, "acousticness", parseIntegerWithRange(dto.getAcousticness(), 0, 100));
        setFieldValue(song, "instrumentalness", parseIntegerWithRange(dto.getInstrumentalness(), 0, 100));

        // 활동 적합도
        setFieldValue(song, "activitySuitabilityParty", parseActivitySuitability(dto.getGoodForParty()));
        setFieldValue(song, "activitySuitabilityWork", parseActivitySuitability(dto.getGoodForWorkStudy()));
        setFieldValue(song, "activitySuitabilityRelaxation", parseActivitySuitability(dto.getGoodForRelaxationMeditation()));
        setFieldValue(song, "activitySuitabilityExercise", parseActivitySuitability(dto.getGoodForExercise()));
        setFieldValue(song, "activitySuitabilityRunning", parseActivitySuitability(dto.getGoodForRunning()));
        setFieldValue(song, "activitySuitabilityYoga", parseActivitySuitability(dto.getGoodForYogaStretching()));
        setFieldValue(song, "activitySuitabilityDriving", parseActivitySuitability(dto.getGoodForDriving()));
        setFieldValue(song, "activitySuitabilitySocial", parseActivitySuitability(dto.getGoodForSocialGatherings()));
        setFieldValue(song, "activitySuitabilityMorning", parseActivitySuitability(dto.getGoodForMorningRoutine()));

        // 좋아요 수 초기값
        setFieldValue(song, "likeCount", 0L);

        return song;
    }

    /**
     * ArtistSong 관계 엔티티 생성
     */
    public static ArtistSong createArtistSong(Long artistId, Long songId) {
        ArtistSong artistSong = new ArtistSong();
        setFieldValue(artistSong, "artistId", artistId);
        setFieldValue(artistSong, "songId", songId);
        return artistSong;
    }

    /**
     * ArtistAlbum 관계 엔티티 생성
     */
    public static ArtistAlbum createArtistAlbum(Long artistId, Long albumId) {
        ArtistAlbum artistAlbum = new ArtistAlbum();
        setFieldValue(artistAlbum, "artistId", artistId);
        setFieldValue(artistAlbum, "albumId", albumId);
        return artistAlbum;
    }

    /**
     * SimilarSong 관계 엔티티 생성
     */
    public static SimilarSong createSimilarSong(Long songId, Long similarSongId, SimilarSongDto similarDto) {
        return SimilarSong.builder()
            .songId(songId)
            .similarSongId(similarSongId)
            .similarityScore(similarDto.getSimilarityScore())
            .build();
    }

    // ===== Private Helper Methods =====

    private static Artist createArtist(String name) {
        Artist artist = new Artist();
        setFieldValue(artist, "name", name);
        return artist;
    }

    private static Album createAlbum(String title, LocalDate releaseDate) {
        Album album = new Album();
        setFieldValue(album, "title", title);
        setFieldValue(album, "releaseDate", releaseDate);
        return album;
    }

    private static Album createDefaultAlbum() {
        Album album = new Album();
        setFieldValue(album, "title", "Unknown Album");
        return album;
    }

    /**
     * 날짜 파싱
     */
    private static LocalDate parseReleaseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // 여러 날짜 형식 지원
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr);
            } else if (dateStr.matches("\\d{4}/\\d{2}/\\d{2}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } else if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            } else if (dateStr.matches("\\d{4}")) {
                // 연도만 있는 경우
                return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
        }

        return null;
    }

    /**
     * 시간 파싱 (MM:SS 또는 HH:MM:SS 형식)
     */
    private static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }

        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                // MM:SS 형식
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return LocalTime.of(0, minutes, seconds);
            } else if (parts.length == 3) {
                // HH:MM:SS 형식
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return LocalTime.of(hours, minutes, seconds);
            }
        } catch (Exception e) {
            log.warn("Failed to parse time: {}", timeStr);
        }

        return null;
    }

    /**
     * InclusionStatus 파싱
     */
    private static InclusionStatus parseInclusionStatus(String value) {
        if (value == null || value.isEmpty()) {
            return InclusionStatus.UNKNOWN;
        }

        String upperValue = value.toUpperCase();
        if (upperValue.contains("YES") || upperValue.contains("TRUE") || upperValue.equals("1")) {
            return InclusionStatus.INCLUDED;
        } else if (upperValue.contains("NO") || upperValue.contains("FALSE") || upperValue.equals("0")) {
            return InclusionStatus.NOT_INCLUDED;
        }

        return InclusionStatus.UNKNOWN;
    }

    /**
     * ActivitySuitability 파싱
     */
    private static ActivitySuitability parseActivitySuitability(Integer value) {
        if (value == null) {
            return ActivitySuitability.NOT_SUITABLE;
        }

        return value > 0 ? ActivitySuitability.SUITABLE : ActivitySuitability.NOT_SUITABLE;
    }

    /**
     * 문자열을 정수로 파싱 (범위 검증 포함)
     */
    private static Integer parseIntegerWithRange(String value, int min, int max) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            // 퍼센트 기호 제거
            String cleanValue = value.trim().replace("%", "");
            int intValue = Integer.parseInt(cleanValue);

            if (intValue < min) {
                return min;
            } else if (intValue > max) {
                return max;
            }
            return intValue;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer: {}", value);
            return null;
        }
    }

    /**
     * Tempo 값 정규화 (0~1 범위를 실제 BPM으로 변환)
     */
    private static java.math.BigDecimal normalizeTempo(java.math.BigDecimal tempo) {
        if (tempo == null) {
            return null;
        }

        // 0~1 범위인 경우 실제 BPM으로 변환 (60-200 BPM 가정)
        if (tempo.compareTo(java.math.BigDecimal.ONE) <= 0) {
            // 0~1을 60~200 BPM으로 매핑
            java.math.BigDecimal min = new java.math.BigDecimal("60");
            java.math.BigDecimal range = new java.math.BigDecimal("140");
            return min.add(tempo.multiply(range));
        }

        return tempo; // 이미 BPM 값인 경우
    }

    /**
     * Loudness 값 정규화 (0~1 범위를 실제 dB로 변환)
     */
    private static java.math.BigDecimal normalizeLoudness(java.math.BigDecimal loudness) {
        if (loudness == null) {
            return null;
        }

        // 0~1 범위인 경우 실제 dB로 변환 (-60~0 dB 가정)
        if (loudness.compareTo(java.math.BigDecimal.ONE) <= 0) {
            // 0~1을 -60~0 dB로 매핑
            java.math.BigDecimal min = new java.math.BigDecimal("-60");
            java.math.BigDecimal range = new java.math.BigDecimal("60");
            return min.add(loudness.multiply(range));
        }

        return loudness; // 이미 dB 값인 경우
    }

    /**
     * 리플렉션을 사용하여 private 필드에 값 설정
     */
    private static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            // 상위 클래스에서 필드를 찾기
            try {
                Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (Exception ex) {
                log.warn("Failed to set field {} in class {}: {}",
                    fieldName, obj.getClass().getSimpleName(), ex.getMessage());
            }
        } catch (IllegalAccessException e) {
            log.error("Failed to access field {} in class {}",
                fieldName, obj.getClass().getSimpleName());
        }
    }
}