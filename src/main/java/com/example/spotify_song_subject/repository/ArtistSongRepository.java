package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ArtistSong;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ArtistSongRepository extends R2dbcRepository<ArtistSong, Long> {

    @Query("SELECT * FROM artist_songs WHERE artist_id = :artistId AND song_id = :songId AND deleted_at IS NULL")
    Mono<ArtistSong> findByArtistIdAndSongId(Long artistId, Long songId);

    @Query("SELECT * FROM artist_songs WHERE song_id = :songId AND deleted_at IS NULL LIMIT 1")
    Mono<ArtistSong> findBySongId(Long songId);

}
