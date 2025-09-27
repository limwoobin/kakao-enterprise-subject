package com.example.spotify_song_subject.controller.response;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 * API 에러 발생 시 반환되는 응답 객체
 */
public record ErrorResponse(String message,
                            String errorCode,
                            LocalDateTime timestamp,
                            String path) {

    public static ErrorResponse of(String message, String errorCode, String path) {
        return new ErrorResponse(
                message,
                errorCode,
                LocalDateTime.now(),
                path
        );
    }
}
