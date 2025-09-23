package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ArtistSong;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ArtistSongRepository extends R2dbcRepository<ArtistSong, Long> {

    Mono<ArtistSong> findByArtistIdAndSongId(Long artistId, Long songId);

    Mono<ArtistSong> findByArtistIdAndSongIdAndDeletedAtIsNull(Long artistId, Long songId);
}