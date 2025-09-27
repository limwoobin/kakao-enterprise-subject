package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.dto.AlbumStatisticsDto;
import com.example.spotify_song_subject.repository.AlbumStatisticsCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumStatisticsQueryService {

    private final AlbumStatisticsCustomRepository albumStatisticsCustomRepository;

    /**
     * 특정 연도의 아티스트별 앨범 통계 조회
     *
     * @param year 조회할 연도
     * @param pageable 페이지네이션 정보
     * @return 해당 연도의 아티스트별 앨범 발매 수
     */
    public Mono<Page<AlbumStatisticsDto>> getAlbumStatisticsByYear(Integer year, Pageable pageable) {
        log.debug("Fetching album statistics for year: {}", year);
        return albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);
    }
}
