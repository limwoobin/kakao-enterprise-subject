package com.example.spotify_song_subject.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("albums")
public class Album extends BaseDomain {

    @Id
    @Column("id")
    private Long id;

    @Column("title")
    private String title;

    @Column("release_date")
    private LocalDate releaseDate;

    @Builder
    public Album(String title, LocalDate releaseDate) {
        this.title = title;
        this.releaseDate = releaseDate;
    }

}
