package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.SimilarSong;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SimilarSongRepository extends R2dbcRepository<SimilarSong, Long> {

    @Query("SELECT * FROM similar_songs WHERE song_id = :songId AND similar_artist_name = :artistName AND similar_song_title = :songTitle AND deleted_at IS NULL")
    Mono<SimilarSong> findBySongIdAndSimilarInfo(Long songId, String artistName, String songTitle);

    @Query("SELECT * FROM similar_songs WHERE song_id = :songId AND deleted_at IS NULL ORDER BY similarity_score DESC")
    Flux<SimilarSong> findBySongId(Long songId);

}
