package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Song;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SongRepository extends R2dbcRepository<Song, Long> {

    @Query("SELECT * FROM songs WHERE title = :title AND album_id = :albumId AND deleted_at IS NULL")
    Mono<Song> findByTitleAndAlbumId(String title, Long albumId);

    @Query("SELECT * FROM songs WHERE title = :title AND deleted_at IS NULL")
    Flux<Song> findByTitle(String title);

}
