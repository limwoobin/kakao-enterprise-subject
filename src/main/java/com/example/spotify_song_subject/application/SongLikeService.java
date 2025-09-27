package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.domain.SongLike;
import com.example.spotify_song_subject.dto.SongLikeContext;
import com.example.spotify_song_subject.dto.SongLikeDto;
import com.example.spotify_song_subject.exception.ResourceNotFoundException;
import com.example.spotify_song_subject.repository.SongLikeRedisRepository;
import com.example.spotify_song_subject.repository.SongLikeRepository;
import com.example.spotify_song_subject.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongLikeService {

    private final SongRepository songRepository;
    private final SongLikeRepository songLikeRepository;
    private final SongLikeRedisRepository songLikeRedisRepository;

    /**
     * 노래 좋아요 토글
     */
    @Transactional
    public Mono<SongLikeDto> like(Long songId, Long userId) {
        Mono<Song> songMono = findSongById(songId);

        Mono<Tuple2<Song, Boolean>> songWithLikeStatus = songMono
            .flatMap(song -> checkIfLikeExists(songId, userId)
                .map(exists -> Tuples.of(song, exists)));

        return songWithLikeStatus
            .flatMap(tuple -> processLikeToggle(songId, userId, tuple.getT1(), tuple.getT2()));
    }

    /**
     * Song 엔티티 조회
     */
    private Mono<Song> findSongById(Long songId) {
        return songRepository.findById(songId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Song", "id", songId)));
    }

    /**
     * 좋아요 존재 여부 확인
     */
    private Mono<Boolean> checkIfLikeExists(Long songId, Long userId) {
        return songLikeRepository.findBySongIdAndUserId(songId, userId)
            .hasElement();
    }

    /**
     * 좋아요 토글 처리
     */
    private Mono<SongLikeDto> processLikeToggle(Long songId, Long userId, Song song, Boolean exists) {
        if (exists) {
            return removeLike(songId, userId, song);
        }

        return addLike(songId, userId, song);
    }

    /**
     * 좋아요 추가 내부 처리
     */
    private Mono<SongLikeDto> addLike(Long songId, Long userId, Song song) {
        return saveLikeToDatabase(songId, userId, song)
            .flatMap(context -> recordLikeToRedis(songId, userId, context))
            .flatMap(context -> updateLikeCountCache(songId, context))
            .map(context -> buildLikeResponse(context, true));
    }

    /**
     * 좋아요 취소 내부 처리
     */
    private Mono<SongLikeDto> removeLike(Long songId, Long userId, Song song) {
        return songLikeRepository.deleteBySongIdAndUserId(songId, userId)
            .then(songRepository.decrementLikeCount(songId))
            .then(songLikeRedisRepository.decrementLikeInBucket(songId))
            .then(songRepository.findById(songId))
            .flatMap(updatedSong -> songLikeRedisRepository.updateLikeCountCache(songId, updatedSong.getLikeCount())
                .thenReturn(updatedSong.getLikeCount()))
            .map(totalLikes -> {
                SongLikeContext context = SongLikeContext.likeRemoved(song, totalLikes);
                return buildLikeResponse(context, false);
            });
    }

    /**
     * 데이터베이스에 좋아요 저장 및 카운트 증가
     */
    private Mono<SongLikeContext> saveLikeToDatabase(Long songId, Long userId, Song song) {
        SongLike songLike = SongLike.create(songId, userId);
        Mono<SongLike> savedLikeMono = songLikeRepository.save(songLike);

        return savedLikeMono
            .flatMap(savedLike -> incrementAndReturnLike(songId, savedLike))
            .map(savedLike -> SongLikeContext.likeAdded(song, savedLike));
    }

    /**
     * 좋아요 카운트 증가 후 좋아요 엔티티 반환
     */
    private Mono<SongLike> incrementAndReturnLike(Long songId, SongLike savedLike) {
        return songRepository.incrementLikeCount(songId)
            .thenReturn(savedLike);
    }

    /**
     * Redis 버킷에 좋아요 기록
     */
    private Mono<SongLikeContext> recordLikeToRedis(Long songId, Long userId, SongLikeContext context) {
        return songLikeRedisRepository.recordLikeToBucket(songId)
            .thenReturn(context);
    }

    /**
     * Redis 캐시에 좋아요 수 업데이트
     */
    private Mono<SongLikeContext> updateLikeCountCache(Long songId, SongLikeContext context) {
        return songRepository.findById(songId)
            .flatMap(song -> updateCacheAndReturnContext(songId, song.getLikeCount(), context));
    }

    /**
     * Redis 캐시 업데이트 후 context 반환
     */
    private Mono<SongLikeContext> updateCacheAndReturnContext(Long songId, Long totalLikes, SongLikeContext context) {
        return songLikeRedisRepository.updateLikeCountCache(songId, totalLikes)
            .map(updatedCount -> {
                context.setTotalLikes(updatedCount);
                return context;
            });
    }

    /**
     * 좋아요 응답 DTO 생성
     */
    private SongLikeDto buildLikeResponse(SongLikeContext context, boolean liked) {
        return SongLikeDto.builder()
            .songId(context.getSong().getId())
            .songTitle(context.getSong().getTitle())
            .totalLikes(context.getTotalLikes())
            .liked(liked)
            .actionAt(LocalDateTime.now())
            .build();
    }
}
