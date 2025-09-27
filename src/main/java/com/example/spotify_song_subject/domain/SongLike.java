package com.example.spotify_song_subject.domain;

import lombok.Builder;
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

    @Builder
    public SongLike(Long songId, Long userId) {
        this.songId = songId;
        this.userId = userId;
    }

    public static SongLike create(Long songId, Long userId) {
        return SongLike.builder()
                .songId(songId)
                .userId(userId)
                .build();
    }
}