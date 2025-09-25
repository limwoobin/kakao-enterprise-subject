package com.example.spotify_song_subject.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("similar_songs")
public class SimilarSong extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("song_id")
    private Long songId;

    @Column("similar_artist_name")
    private String similarArtistName;

    @Column("similar_song_title")
    private String similarSongTitle;

    @Column("similarity_score")
    private BigDecimal similarityScore;

    @Builder
    public SimilarSong(Long songId, String similarArtistName, String similarSongTitle, BigDecimal similarityScore) {
        this.songId = songId;
        this.similarArtistName = similarArtistName;
        this.similarSongTitle = similarSongTitle;
        this.similarityScore = similarityScore;
    }
}