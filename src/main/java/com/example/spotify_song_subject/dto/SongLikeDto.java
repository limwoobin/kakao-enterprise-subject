package com.example.spotify_song_subject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 좋아요 DTO
 * 좋아요 추가/취소 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongLikeDto {

    private Long songId;
    private String songTitle;
    private Long totalLikes;
    private boolean liked;  // true: 좋아요 추가됨, false: 좋아요 취소됨
    private LocalDateTime actionAt;  // 좋아요/취소 시간

}
