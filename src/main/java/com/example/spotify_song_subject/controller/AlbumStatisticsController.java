package com.example.spotify_song_subject.controller;

import com.example.spotify_song_subject.controller.response.AlbumStatisticsResponse;
import com.example.spotify_song_subject.controller.util.PageableUtils;
import com.example.spotify_song_subject.application.AlbumStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/album-statistics")
public class AlbumStatisticsController {

    private final AlbumStatisticsQueryService albumStatisticsQueryService;

    /**
     * 특정 연도의 아티스트별 앨범 발매 통계 조회
     *
     * @param year 조회할 연도 (필수)
     * @param pageable 페이징 정보
     * @return 해당 연도의 아티스트별 앨범 발매 수
     */
    @GetMapping
    public Mono<ResponseEntity<Page<AlbumStatisticsResponse>>> getAlbumStatistics(@RequestParam(required = true) Integer year,
                                                                                  @RequestParam(required = false) Integer page,
                                                                                  @RequestParam(required = false) Integer size) {
        
        Pageable pageable = PageableUtils.createPageable(page, size);
        
        return albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable)
            .map(dtoPage -> dtoPage.map(AlbumStatisticsResponse::from))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.noContent().build()));
    }
}
