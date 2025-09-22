package com.example.spotify_song_subject.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("song_likes")
public class SongLike extends BaseDomain {

    @Id
    private Long id;

    @Column("song_id")
    private Long songId;

    @Column("user_id")
    private Long userId;
}