package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ArtistAlbum;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ArtistAlbumRepository extends R2dbcRepository<ArtistAlbum, Long> {

    Mono<ArtistAlbum> findByArtistIdAndAlbumId(Long artistId, Long albumId);

    Mono<ArtistAlbum> findByArtistIdAndAlbumIdAndDeletedAtIsNull(Long artistId, Long albumId);
}