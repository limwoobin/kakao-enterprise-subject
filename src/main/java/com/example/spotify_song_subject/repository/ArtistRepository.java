package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Artist;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ArtistRepository extends R2dbcRepository<Artist, Long> {

    @Query("SELECT * FROM artists WHERE name = :name AND deleted_at IS NULL")
    Mono<Artist> findByName(String name);

}
