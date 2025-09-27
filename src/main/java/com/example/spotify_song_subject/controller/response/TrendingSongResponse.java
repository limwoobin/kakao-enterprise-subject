package com.example.spotify_song_subject.controller.response;

import com.example.spotify_song_subject.dto.TrendingSongDto;

/**
 * 트렌딩 곡 응답 DTO
 * 최근 좋아요가 많이 증가한 곡 정보를 담는 응답 객체
 */
public record TrendingSongResponse(Long songId,
                                   String songTitle,
                                   String artistName,
                                   String albumName,
                                   Long likeIncrease) {

    public static TrendingSongResponse from(TrendingSongDto dto) {
        return new TrendingSongResponse(
                dto.getSongId(),
                dto.getSongTitle(),
                dto.getArtistName(),
                dto.getAlbumName(),
                dto.getLikeIncrease()
        );
    }
}