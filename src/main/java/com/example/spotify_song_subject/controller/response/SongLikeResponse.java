package com.example.spotify_song_subject.controller.response;

import com.example.spotify_song_subject.dto.SongLikeDto;

import java.time.LocalDateTime;

/**
 * 좋아요 응답 DTO
 * 좋아요 추가/취소 결과를 담는 응답 객체
 */
public record SongLikeResponse(Long songId,
                               String songTitle,
                               Long totalLikes,
                               boolean liked,
                               LocalDateTime actionAt) {

    public static SongLikeResponse from(SongLikeDto dto) {
        return new SongLikeResponse(
                dto.getSongId(),
                dto.getSongTitle(),
                dto.getTotalLikes(),
                dto.isLiked(),
                dto.getActionAt()
        );
    }
}