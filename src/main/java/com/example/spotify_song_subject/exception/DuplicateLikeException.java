package com.example.spotify_song_subject.exception;

/**
 * 중복 좋아요 시도 시 발생하는 예외
 */
public class DuplicateLikeException extends RuntimeException {

    private final Long songId;
    private final Long userId;

    public DuplicateLikeException(Long songId, Long userId) {
        super(String.format("User %d has already liked song %d", userId, songId));
        this.songId = songId;
        this.userId = userId;
    }

    public DuplicateLikeException(String message, Long songId, Long userId) {
        super(message);
        this.songId = songId;
        this.userId = userId;
    }

    public Long getSongId() {
        return songId;
    }

    public Long getUserId() {
        return userId;
    }
}