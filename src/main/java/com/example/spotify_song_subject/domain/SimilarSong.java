package com.example.spotify_song_subject.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Table("similar_songs")
public class SimilarSong extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("song_id")
    private Long songId;

    @Column("similar_song_id")
    private Long similarSongId;

    @Column("similarity_score")
    private BigDecimal similarityScore;
}