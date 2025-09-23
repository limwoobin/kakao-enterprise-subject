package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Album;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface AlbumRepository extends R2dbcRepository<Album, Long> {

    Mono<Album> findByTitleAndReleaseDate(String title, LocalDate releaseDate);

    Mono<Album> findByTitleAndReleaseDateAndDeletedAtIsNull(String title, LocalDate releaseDate);
}