package com.example.spotify_song_subject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 앨범 통계 DTO
 * 앨범별 통계 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumStatisticsDto {

    private Long albumId;
    private String albumName;
    private Long artistId;
    private String artistName;
    private LocalDate releaseDate;
    private Integer releaseYear;
    private Long albumCount;
    private Long songCount;

}