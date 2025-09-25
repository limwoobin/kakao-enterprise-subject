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
@Table("artists")
public class Artist extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("name")
    private String name;

    @Builder
    public Artist(String name) {
        this.name = name;
    }
}
