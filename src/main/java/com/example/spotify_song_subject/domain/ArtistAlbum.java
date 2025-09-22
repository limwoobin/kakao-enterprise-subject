package com.example.spotify_song_subject.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("artist_albums")
public class ArtistAlbum extends BaseDomain {

    @Id
    private Long id;

    @Column("artist_id")
    private Long artistId;

    @Column("album_id")
    private Long albumId;

    @Column("is_main_artist")
    private Boolean isMainArtist = true;

}
