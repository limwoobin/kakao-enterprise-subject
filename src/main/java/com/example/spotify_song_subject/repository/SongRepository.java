package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Song;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface SongRepository extends R2dbcRepository<Song, Long> {

    @Query("SELECT * FROM songs WHERE title = :title AND album_id = :albumId AND deleted_at IS NULL")
    Mono<Song> findByTitleAndAlbumId(String title, Long albumId);

    @Query("SELECT * FROM songs WHERE title = :title AND deleted_at IS NULL")
    Flux<Song> findByTitle(String title);

    @Query("UPDATE songs SET like_count = like_count + 1 WHERE id = :songId")
    Mono<Integer> incrementLikeCount(Long songId);

    @Query("UPDATE songs SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END WHERE id = :songId")
    Mono<Integer> decrementLikeCount(Long songId);

    @Query("SELECT * FROM songs WHERE id IN (:ids) AND deleted_at IS NULL")
    Flux<Song> findByIds(@Param("ids") List<Long> ids);

}
