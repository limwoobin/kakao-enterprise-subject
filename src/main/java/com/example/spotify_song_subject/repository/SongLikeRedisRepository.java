package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.dto.SongLikeScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ZSetOperations;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 좋아요 관련 Redis 작업을 담당하는 Repository
 * 버킷 기반 좋아요 기록, 캐시 관리 등 Redis 관련 모든 작업 처리
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SongLikeRedisRepository {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String LIKE_COUNT_CACHE_KEY_PREFIX = "song:like:cache:";
    private static final String LIKE_BUCKET_KEY_PREFIX = "likes:bucket:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 버킷 유지 시간
     */
    private static final Duration BUCKET_TTL = Duration.ofMinutes(70);

    /**
     * 5분 단위 버킷
     */
    private static final int BUCKET_INTERVAL_MINUTES = 5;

    /**
     * Redis 버킷에 좋아요 기록
     * 5분 단위 버킷에 ZSET으로 좋아요 증가량 기록
     *
     * @param songId 노래 ID
     * @return 성공 여부 (Redis 실패 시에도 true 반환하여 메인 플로우 계속 진행)
     */
    public Mono<Boolean> recordLikeToBucket(Long songId) {
        String bucketKey = getCurrentBucketKey();

        return redisTemplate.opsForZSet()
                .incrementScore(bucketKey, songId.toString(), 1.0)
                .then(redisTemplate.expire(bucketKey, BUCKET_TTL)) // TTL 설정
                .thenReturn(true)
                .onErrorResume(error -> {
                    log.error("Failed to record like to bucket for songId: {}, error: {}", songId, error.getMessage());
                    return Mono.just(true);
                });
    }

    /**
     * Redis 버킷에서 좋아요 카운트 감소
     * 현재 버킷에서만 감소 (음수 허용)
     *
     * @param songId 노래 ID
     * @return 성공 여부 (Redis 실패 시에도 true 반환하여 메인 플로우 계속 진행)
     */
    public Mono<Boolean> decrementLikeInBucket(Long songId) {
        String bucketKey = getCurrentBucketKey();

        return redisTemplate.opsForZSet()
                .incrementScore(bucketKey, songId.toString(), -1.0)
                .thenReturn(true)
                .onErrorResume(error -> {
                    log.error("Failed to decrement like in bucket for songId: {}, error: {}", songId, error.getMessage());
                    return Mono.just(true);
                });
    }

    /**
     * Redis 캐시에 좋아요 수 업데이트
     *
     * @param songId 노래 ID
     * @param likeCount 좋아요 수
     * @return 저장된 좋아요 수
     */
    public Mono<Long> updateLikeCountCache(Long songId, Long likeCount) {
        String cacheKey = generateCacheKey(songId);
        return redisTemplate.opsForValue()
                .set(cacheKey, likeCount.toString(), CACHE_TTL)
                .thenReturn(likeCount)
                .onErrorResume(error -> Mono.just(likeCount)); // 캐시 실패 시 무시
    }

    /**
     * 캐시 키 생성
     *
     * @param songId 노래 ID
     * @return 캐시 키
     */
    private String generateCacheKey(Long songId) {
        return LIKE_COUNT_CACHE_KEY_PREFIX + songId;
    }

    /**
     * 현재 시간 기준 버킷 키 생성
     * 5분 단위로 버킷을 구분 (예: likes:bucket:202401151030)
     *
     * @return 버킷 키
     */
    private String getCurrentBucketKey() {
        LocalDateTime now = LocalDateTime.now();
        int minute = (now.getMinute() / BUCKET_INTERVAL_MINUTES) * BUCKET_INTERVAL_MINUTES;

        String timestamp = String.format("%d%02d%02d%02d%02d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                minute);

        return LIKE_BUCKET_KEY_PREFIX + timestamp;
    }

    /**
     * 최근 1시간 동안 좋아요가 많이 증가한 상위 10개 노래 조회
     * Redis ZUNIONSTORE를 사용하여 여러 버킷을 합산한 후 상위 10개 추출
     *
     * @return 상위 10개 노래 ID와 좋아요 증가 수 리스트
     */
    public Mono<List<SongLikeScore>> getTop10TrendingSongs() {
        List<String> bucketKeys = getRecentBucketKeys();
        String tempKey = "temp:trending:" + System.currentTimeMillis();

        return findExistingBucketKeys(bucketKeys)
                .flatMap(existingKeys -> performUnionStore(existingKeys, tempKey))
                .flatMap(count -> retrieveTopSongs(count, tempKey))
                .doFinally(signal -> cleanupTempKey(tempKey))
                .onErrorResume((error) -> {
                    log.error("Failed to get trending songs: {}", error.getMessage());
                    return Mono.just(List.of());
                });
    }

    /**
     * 최근 1시간 동안의 버킷 키 목록 조회
     * 현재 시간부터 과거 12개 버킷 (5분 * 12 = 60분)
     *
     * @return 버킷 키 리스트
     */
    public List<String> getRecentBucketKeys() {
        List<String> bucketKeys = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 현재 버킷부터 과거 11개 버킷까지 (총 12개)
        for (int i = 0; i < 12; i++) {
            LocalDateTime bucketTime = now.minusMinutes(i * BUCKET_INTERVAL_MINUTES);
            int minute = (bucketTime.getMinute() / BUCKET_INTERVAL_MINUTES) * BUCKET_INTERVAL_MINUTES;

            String timestamp = String.format("%d%02d%02d%02d%02d",
                bucketTime.getYear(),
                bucketTime.getMonthValue(),
                bucketTime.getDayOfMonth(),
                bucketTime.getHour(),
                minute);

            bucketKeys.add(LIKE_BUCKET_KEY_PREFIX + timestamp);
        }

        return bucketKeys;
    }

    /**
     * 존재하는 버킷 키만 필터링
     *
     * @param bucketKeys 확인할 버킷 키 목록
     * @return 존재하는 키 목록
     */
    private Mono<List<String>> findExistingBucketKeys(List<String> bucketKeys) {
        return Flux.fromIterable(bucketKeys)
                .flatMap(redisTemplate::hasKey)
                .collectList()
                .map(existsList -> filterExistingKeys(bucketKeys, existsList));
    }

    /**
     * 존재 여부 리스트를 기반으로 실제 존재하는 키만 필터링
     *
     * @param bucketKeys 원본 버킷 키 목록
     * @param existsList 존재 여부 리스트
     * @return 존재하는 키 목록
     */
    private List<String> filterExistingKeys(List<String> bucketKeys, List<Boolean> existsList) {
        List<String> existingKeys = new ArrayList<>();
        for (int i = 0; i < bucketKeys.size() && i < existsList.size(); i++) {
            if (Boolean.TRUE.equals(existsList.get(i))) {
                existingKeys.add(bucketKeys.get(i));
            }
        }
        return existingKeys;
    }

    /**
     * Redis ZUNIONSTORE 수행하여 버킷들을 합산
     *
     * @param existingKeys 존재하는 버킷 키 목록
     * @param tempKey 임시 키
     * @return 합산된 아이템 개수
     */
    private Mono<Long> performUnionStore(List<String> existingKeys, String tempKey) {
        if (existingKeys.isEmpty()) {
            return Mono.just(0L);
        }

        return redisTemplate.opsForZSet()
                .unionAndStore(
                        existingKeys.get(0),
                        existingKeys.subList(1, existingKeys.size()),
                        tempKey
                );
    }

    /**
     * 상위 10개 노래 조회
     *
     * @param count 합산된 아이템 개수
     * @param tempKey 임시 키
     * @return 상위 10개 노래 스코어 리스트
     */
    private Mono<List<SongLikeScore>> retrieveTopSongs(Long count, String tempKey) {
        if (count == 0) {
            return Mono.just(List.of());
        }

        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(tempKey, Range.closed(0L, 9L))
                .map(this::convertToSongLikeScore)
                .collectList();
    }

    /**
     * Redis Tuple을 SongLikeScore로 변환
     *
     * @param tuple Redis ZSet Tuple
     * @return SongLikeScore 객체
     */
    private SongLikeScore convertToSongLikeScore(ZSetOperations.TypedTuple<String> tuple) {
        return new SongLikeScore(
                Long.parseLong(tuple.getValue()),
                tuple.getScore().longValue()
        );
    }

    /**
     * 임시 키 삭제
     *
     * @param tempKey 삭제할 임시 키
     */
    private void cleanupTempKey(String tempKey) {
        redisTemplate.delete(tempKey).subscribe();
    }

}