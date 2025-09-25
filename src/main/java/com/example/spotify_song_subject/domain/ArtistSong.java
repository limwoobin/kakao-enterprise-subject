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
@Table("artist_songs")
public class ArtistSong extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("artist_id")
    private Long artistId;

    @Column("song_id")
    private Long songId;

    @Builder
    public ArtistSong(Long artistId, Long songId) {
        this.artistId = artistId;
        this.songId = songId;
    }
}
