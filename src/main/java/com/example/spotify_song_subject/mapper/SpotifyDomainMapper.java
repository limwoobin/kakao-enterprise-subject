package com.example.spotify_song_subject.mapper;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
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
     * SpotifySongDto에서 Album 엔티티 추출 (첫 번째 아티스트를 대표 아티스트로 사용)
     */
    public static Album extractAlbum(SpotifySongDto dto) {
        String albumTitle = dto.getAlbumTitle();
        if (albumTitle == null || albumTitle.isEmpty()) {
            return createDefaultAlbum();
        }

        LocalDate releaseDate = parseReleaseDate(dto.getReleaseDate());
        String artistName = extractAllArtistNames(dto);
        return createAlbum(albumTitle, releaseDate, artistName);
    }
    
    /**
     * DTO에서 전체 아티스트명 추출 (앨범용)
     * 앨범에는 모든 아티스트가 포함되어야 함 (예: "A, B, C")
     */
    private static String extractAllArtistNames(SpotifySongDto dto) {
        if (dto.getArtists() == null || dto.getArtists().isEmpty()) {
            return "Unknown Artist";
        }
        
        // 앨범에는 전체 아티스트 이름을 저장
        return dto.getArtists().trim();
    }

    /**
     * SpotifySongDto를 Song 엔티티로 변환
     */
    public static Song convertToSong(SpotifySongDto dto, Long albumId) {
        return Song.builder()
                .albumId(albumId)
                .title(dto.getSongTitle() != null ? dto.getSongTitle() : "Unknown Song")
                .lyrics(dto.getLyrics())
                .length(parseTime(dto.getLength()))
                .musicKey(dto.getMusicKey())
                .tempo(dto.getTempo())
                .loudnessDb(dto.getLoudnessDb())
                .timeSignature(dto.getTimeSignature())
                .explicitContent(parseInclusionStatus(dto.getExplicit()))
                .emotion(dto.getEmotion())
                .genre(dto.getGenre())
                .popularity(dto.getPopularity())
                .energy(dto.getEnergy())
                .danceability(dto.getDanceability())
                .positiveness(dto.getPositiveness())
                .speechiness(dto.getSpeechiness())
                .liveness(dto.getLiveness())
                .acousticness(dto.getAcousticness())
                .instrumentalness(dto.getInstrumentalness())
                .activitySuitabilityParty(parseActivitySuitability(dto.getGoodForParty()))
                .activitySuitabilityWork(parseActivitySuitability(dto.getGoodForWorkStudy()))
                .activitySuitabilityRelaxation(parseActivitySuitability(dto.getGoodForRelaxationMeditation()))
                .activitySuitabilityExercise(parseActivitySuitability(dto.getGoodForExercise()))
                .activitySuitabilityRunning(parseActivitySuitability(dto.getGoodForRunning()))
                .activitySuitabilityYoga(parseActivitySuitability(dto.getGoodForYogaStretching()))
                .activitySuitabilityDriving(parseActivitySuitability(dto.getGoodForDriving()))
                .activitySuitabilitySocial(parseActivitySuitability(dto.getGoodForSocialGatherings()))
                .activitySuitabilityMorning(parseActivitySuitability(dto.getGoodForMorningRoutine()))
                .likeCount(0L)
                .build();
    }

    /**
     * ArtistSong 관계 엔티티 생성
     */
    public static ArtistSong createArtistSong(Long artistId, Long songId) {
        return ArtistSong.builder()
                .artistId(artistId)
                .songId(songId)
                .build();
    }

    /**
     * ArtistAlbum 관계 엔티티 생성
     */
    public static ArtistAlbum createArtistAlbum(Long artistId, Long albumId) {
        return ArtistAlbum.builder()
                .artistId(artistId)
                .albumId(albumId)
                .build();
    }

    /**
     * SimilarSong 관계 엔티티 생성 (artist name과 song title 직접 저장)
     */
    public static SimilarSong createSimilarSong(Long songId, String artistName, String songTitle, BigDecimal similarityScore) {
        return SimilarSong.builder()
                .songId(songId)
                .similarArtistName(artistName)
                .similarSongTitle(songTitle)
                .similarityScore(similarityScore)
                .build();
    }

  private static Artist createArtist(String name) {
        return Artist.builder()
                .name(name)
                .build();
    }
    
    private static Album createAlbum(String title, LocalDate releaseDate, String artistName) {
        return Album.builder()
                .title(title)
                .releaseDate(releaseDate)
                .artistName(artistName)
                .build();
    }

    private static Album createDefaultAlbum() {
        return Album.builder()
                .title("Unknown Album")
                .artistName("Unknown Artist")
                .build();
    }

    /**
     * 날짜 파싱
     */
    public static LocalDate parseReleaseDate(String dateStr) {
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
            return InclusionStatus.NOT_INCLUDED;
        }

        String upperValue = value.toUpperCase();
        if (upperValue.contains("YES")) {
            return InclusionStatus.INCLUDED;
        }

        return InclusionStatus.NOT_INCLUDED;
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

}
