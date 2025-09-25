package com.example.spotify_song_subject.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("artist_albums")
public class ArtistAlbum extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("artist_id")
    private Long artistId;

    @Column("album_id")
    private Long albumId;

    @Builder
    public ArtistAlbum(Long artistId, Long albumId) {
        this.artistId = artistId;
        this.albumId = albumId;
    }

}
