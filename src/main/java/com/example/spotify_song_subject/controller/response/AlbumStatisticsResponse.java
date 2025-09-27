package com.example.spotify_song_subject.controller.response;

import com.example.spotify_song_subject.dto.AlbumStatisticsDto;

import java.time.LocalDate;

/**
 * 앨범 통계 응답 DTO
 * 앨범별 통계 정보를 담는 응답 객체
 */
public record AlbumStatisticsResponse(Long artistId,
                                      String artistName,
                                      LocalDate releaseDate,
                                      Integer releaseYear,
                                      Long albumCount) {

    /**
     * AlbumStatisticsDto로부터 Response 객체 생성
     * @param dto 앨범 통계 DTO
     * @return AlbumStatisticsResponse 객체
     */
    public static AlbumStatisticsResponse from(AlbumStatisticsDto dto) {
        return new AlbumStatisticsResponse(
                dto.getArtistId(),
                dto.getArtistName(),
                dto.getReleaseDate(),
                dto.getReleaseYear(),
                dto.getAlbumCount()
        );
    }
}