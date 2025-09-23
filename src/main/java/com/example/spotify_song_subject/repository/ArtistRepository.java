package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Artist;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ArtistRepository extends R2dbcRepository<Artist, Long> {

    Mono<Artist> findByName(String name);

    Mono<Artist> findByNameAndDeletedAtIsNull(String name);
}