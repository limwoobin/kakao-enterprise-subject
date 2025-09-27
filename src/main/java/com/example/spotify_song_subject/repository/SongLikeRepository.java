package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.SongLike;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SongLikeRepository extends R2dbcRepository<SongLike, Long> {

    /**
     * 특정 사용자가 특정 노래에 좋아요를 눌렀는지 확인
     */
    @Query("SELECT * FROM song_likes WHERE song_id = :songId AND user_id = :userId AND deleted_at IS NULL")
    Mono<SongLike> findBySongIdAndUserId(Long songId, Long userId);

    /**
     * 특정 노래의 전체 좋아요 수 조회
     */
    @Query("SELECT COUNT(*) FROM song_likes WHERE song_id = :songId AND deleted_at IS NULL")
    Mono<Long> countBySongId(Long songId);

    /**
     * 좋아요 삭제 (soft delete)
     */
    @Query("UPDATE song_likes SET deleted_at = CURRENT_TIMESTAMP WHERE song_id = :songId AND user_id = :userId AND deleted_at IS NULL")
    Mono<Void> deleteBySongIdAndUserId(Long songId, Long userId);
}