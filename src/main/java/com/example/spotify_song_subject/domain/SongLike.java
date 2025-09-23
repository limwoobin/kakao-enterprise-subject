package com.example.spotify_song_subject.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@Table("song_likes")
public class SongLike extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("song_id")
    private Long songId;

    @Column("user_id")
    private Long userId;
}