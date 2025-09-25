package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.ArtistAlbum;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ArtistAlbumRepository extends R2dbcRepository<ArtistAlbum, Long> {

    @Query("SELECT * FROM artist_albums WHERE artist_id = :artistId AND album_id = :albumId AND deleted_at IS NULL")
    Mono<ArtistAlbum> findByArtistIdAndAlbumId(Long artistId, Long albumId);

}
