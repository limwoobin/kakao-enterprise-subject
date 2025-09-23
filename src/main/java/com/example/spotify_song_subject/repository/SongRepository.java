package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Song;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SongRepository extends R2dbcRepository<Song, Long> {

    Mono<Song> findByTitleAndAlbumId(String title, Long albumId);

    Mono<Song> findByTitleAndAlbumIdAndDeletedAtIsNull(String title, Long albumId);

    Flux<Song> findByTitle(String title);

    Flux<Song> findByTitleAndDeletedAtIsNull(String title);
}