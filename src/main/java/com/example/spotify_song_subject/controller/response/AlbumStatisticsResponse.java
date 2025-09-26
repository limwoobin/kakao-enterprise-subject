package com.example.spotify_song_subject.controller.response;

import com.example.spotify_song_subject.dto.AlbumStatisticsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumStatisticsResponse {
    private Long albumId;
    private String albumName;
    private Long artistId;
    private String artistName;
    private LocalDate releaseDate;
    private Integer releaseYear;
    private Long albumCount;
    private Long songCount;

    public static AlbumStatisticsResponse from(AlbumStatisticsDto dto) {
        return AlbumStatisticsResponse.builder()
                .albumId(dto.getAlbumId())
                .albumName(dto.getAlbumName())
                .artistId(dto.getArtistId())
                .artistName(dto.getArtistName())
                .releaseDate(dto.getReleaseDate())
                .releaseYear(dto.getReleaseYear())
                .albumCount(dto.getAlbumCount())
                .songCount(dto.getSongCount())
                .build();
    }
}