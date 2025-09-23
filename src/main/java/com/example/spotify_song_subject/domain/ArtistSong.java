package com.example.spotify_song_subject.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@Table("artist_songs")
public class ArtistSong extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("artist_id")
    private Long artistId;

    @Column("song_id")
    private Long songId;
}
