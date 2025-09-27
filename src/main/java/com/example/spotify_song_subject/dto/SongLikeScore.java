package com.example.spotify_song_subject.dto;

/**
 * Redis에서 조회한 좋아요 점수 데이터
 * 노래 ID와 좋아요 증가 점수를 담는 DTO
 */
public record SongLikeScore(Long songId, Long score) {
}