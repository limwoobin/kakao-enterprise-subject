package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.dto.SongLikeScore;
import com.example.spotify_song_subject.dto.TrendingSongDto;
import com.example.spotify_song_subject.repository.SongLikeRedisRepository;
import com.example.spotify_song_subject.repository.SongLikeCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongLikeQueryService {

    private final SongLikeRedisRepository songLikeRedisRepository;
    private final SongLikeCustomRepository songLikeCustomRepository;

    /**
     * 최근 1시간 동안 좋아요 증가 Top 10 조회
     * Redis 버킷에서 집계한 데이터를 기반으로 트렌딩 곡 조회
     */
    public Mono<List<TrendingSongDto>> getTrendingSongs() {
        return getTop10LikeScores()
                .flatMap(this::processTrendingSongs)
                .onErrorResume(error -> {
                    log.error("Failed to get trending songs: {}", error.getMessage());
                    return Mono.just(List.of());
                });
    }

    /**
     * Redis에서 Top 10 좋아요 점수 조회
     */
    private Mono<List<SongLikeScore>> getTop10LikeScores() {
        return songLikeRedisRepository.getTop10TrendingSongs();
    }

    /**
     * 좋아요 점수 리스트를 TrendingSongDto 리스트로 변환
     * @param likeScores Redis에서 조회한 좋아요 점수 목록
     * @return 트렌딩 곡 정보 목록 (좋아요 증가 수 내림차순 정렬)
     */
    private Mono<List<TrendingSongDto>> processTrendingSongs(List<SongLikeScore> likeScores) {
        if (likeScores.isEmpty()) {
            return Mono.just(List.of());
        }

        List<Long> songIds = extractSongIds(likeScores);
        Map<Long, Long> likeIncreaseMap = createLikeIncreaseMap(likeScores);
        return songLikeCustomRepository.findTrendingSongsByIds(songIds, likeIncreaseMap);
    }

    /**
     * 좋아요 점수 리스트에서 Song ID 추출
     */
    private List<Long> extractSongIds(List<SongLikeScore> likeScores) {
        return likeScores.stream()
                .map(SongLikeScore::songId)
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 증가 수 Map 생성
     */
    private Map<Long, Long> createLikeIncreaseMap(List<SongLikeScore> likeScores) {
        return likeScores.stream()
                .collect(Collectors.toMap(
                        SongLikeScore::songId,
                        SongLikeScore::score
                ));
    }


}
