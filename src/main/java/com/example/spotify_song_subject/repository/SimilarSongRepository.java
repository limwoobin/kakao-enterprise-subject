package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.SimilarSong;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SimilarSongRepository extends R2dbcRepository<SimilarSong, Long> {

    Mono<SimilarSong> findBySongIdAndSimilarSongId(Long songId, Long similarSongId);

    Mono<SimilarSong> findBySongIdAndSimilarSongIdAndDeletedAtIsNull(Long songId, Long similarSongId);
}