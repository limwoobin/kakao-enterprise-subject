package com.example.spotify_song_subject.controller;

import com.example.spotify_song_subject.application.SongLikeQueryService;
import com.example.spotify_song_subject.controller.response.SongLikeResponse;
import com.example.spotify_song_subject.controller.response.TrendingSongResponse;
import com.example.spotify_song_subject.application.SongLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/songs")
public class SongLikeController {

    private final SongLikeQueryService songLikeQueryService;
    private final SongLikeService songLikeService;

    /**
     * 노래 좋아요 토글 (추가/취소)
     */
    @PostMapping("/{songId}/likes")
    public Mono<ResponseEntity<SongLikeResponse>> toggleLike(@PathVariable Long songId, @RequestParam Long userId) {
        return songLikeService.like(songId, userId)
            .map(SongLikeResponse::from)
            .map(ResponseEntity::ok);
    }

    /**
     * 최근 1시간 동안 좋아요 증가 Top 10 조회
     * Service layer에서 정렬된 List<TrendingSongDto>를 받아서 Response로 변환
     */
    @GetMapping("/trending/likes")
    public Mono<ResponseEntity<List<TrendingSongResponse>>> getTrendingSongs() {
        return songLikeQueryService.getTrendingSongs()
            .map(dtos -> dtos.stream()
                    .map(TrendingSongResponse::from)
                    .toList())
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.ok(List.of()));
    }

}