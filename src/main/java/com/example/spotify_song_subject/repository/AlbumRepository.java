package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Album;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface AlbumRepository extends R2dbcRepository<Album, Long> {

    @Query("SELECT * FROM albums WHERE title = :title AND release_date = :releaseDate AND deleted_at IS NULL")
    Mono<Album> findByTitleAndReleaseDate(String title, LocalDate releaseDate);

}
