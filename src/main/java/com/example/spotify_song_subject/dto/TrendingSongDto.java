package com.example.spotify_song_subject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 트렌딩 곡 DTO
 * Redis와 DB로부터 조회한 트렌딩 곡 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingSongDto {

    private Long songId;
    private String songTitle;
    private String artistName;
    private String albumName;
    private Long likeIncrease;

}
