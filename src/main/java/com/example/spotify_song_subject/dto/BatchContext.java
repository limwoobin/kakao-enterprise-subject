package com.example.spotify_song_subject.dto;

import com.example.spotify_song_subject.application.AlbumBatchProcessor;
import com.example.spotify_song_subject.mapper.SpotifyDomainMapper;
import lombok.Builder;

import java.time.LocalDate;
import java.util.*;

/**
 * 배치 처리를 위한 데이터 수집 컨텍스트
 * 배치 내의 모든 아티스트, 앨범 정보를 집계하여 중복 제거 및 Bulk 처리 지원
 */
@Builder
public record BatchContext(Set<String> artistNames,
                           Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle,
                           List<SpotifySongDto> songs) {

    /**
     * SpotifySongDto 리스트로부터 BatchContext 생성
     */
    public static BatchContext from(List<SpotifySongDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return empty();
        }

        Set<String> artistNames = extractArtistNames(dtos);
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = extractAlbumsByTitle(dtos);

        return BatchContext.builder()
            .artistNames(artistNames)
            .albumsByTitle(albumsByTitle)
            .songs(dtos)
            .build();
    }

    /**
     * 빈 BatchContext 생성
     */
    public static BatchContext empty() {
        return BatchContext.builder()
            .artistNames(new HashSet<>())
            .albumsByTitle(new HashMap<>())
            .songs(new ArrayList<>())
            .build();
    }

    /**
     * DTO 리스트에서 아티스트 이름 추출
     */
    private static Set<String> extractArtistNames(List<SpotifySongDto> dtos) {
        Set<String> artistNames = new HashSet<>();

        for (SpotifySongDto dto : dtos) {
            addArtistsFromDto(dto, artistNames);
        }

        return artistNames;
    }

    /**
     * 단일 DTO에서 아티스트 추가
     */
    private static void addArtistsFromDto(SpotifySongDto dto, Set<String> artistNames) {
        if (!hasArtists(dto)) {
            return;
        }

        String[] artists = dto.getArtists().split(",");
        for (String artist : artists) {
            String trimmedArtist = artist.trim();
            if (!trimmedArtist.isEmpty()) {
                artistNames.add(trimmedArtist);
            }
        }
    }

    /**
     * DTO가 아티스트 정보를 가지고 있는지 확인
     */
    private static boolean hasArtists(SpotifySongDto dto) {
        return dto != null && dto.getArtists() != null && !dto.getArtists().isEmpty();
    }

    /**
     * DTO 리스트에서 앨범 정보 추출
     */
    private static Map<String, Set<AlbumBatchProcessor.AlbumInfo>> extractAlbumsByTitle(List<SpotifySongDto> dtos) {
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = new HashMap<>();

        for (SpotifySongDto dto : dtos) {
            addAlbumFromDto(dto, albumsByTitle);
        }

        return albumsByTitle;
    }

    /**
     * 단일 DTO에서 앨범 정보 추가
     */
    private static void addAlbumFromDto(SpotifySongDto dto, Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle) {
        if (!hasAlbumTitle(dto)) {
            return;
        }

        LocalDate releaseDate = SpotifyDomainMapper.parseReleaseDate(dto.getReleaseDate());
        String artistName = extractAllArtists(dto);
        
        AlbumBatchProcessor.AlbumInfo albumInfo = new AlbumBatchProcessor.AlbumInfo(releaseDate, artistName);
        albumsByTitle.computeIfAbsent(dto.getAlbumTitle(), k -> new HashSet<>())
                    .add(albumInfo);
    }
    
    /**
     * DTO에서 전체 아티스트 이름 추출 (앨범용)
     * 앨범에는 모든 아티스트가 포함되어야 함 (예: "A, B, C")
     */
    private static String extractAllArtists(SpotifySongDto dto) {
        if (!hasArtists(dto)) {
            return "Unknown Artist";
        }
        
        return dto.getArtists().trim();
    }

    /**
     * DTO가 앨범 타이틀을 가지고 있는지 확인
     */
    private static boolean hasAlbumTitle(SpotifySongDto dto) {
        return dto != null &&
               dto.getAlbumTitle() != null &&
               !dto.getAlbumTitle().trim().isEmpty();
    }

}