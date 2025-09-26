package com.example.spotify_song_subject.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(String message, String errorCode, String path) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
