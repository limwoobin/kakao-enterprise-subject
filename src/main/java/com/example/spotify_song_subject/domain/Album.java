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

    @Column("artist_name")
    private String artistName;

    @Builder
    public Album(String title, LocalDate releaseDate, String artistName) {
        this.title = title;
        this.releaseDate = releaseDate;
        this.artistName = artistName;
    }

    /**
     * 정적 팩토리 메소드 - 타이틀과 발매일로 Album 생성
     */
    public static Album of(String title, LocalDate releaseDate) {
        return Album.builder()
            .title(title)
            .releaseDate(releaseDate)
            .build();
    }
    
    /**
     * 정적 팩토리 메소드 - 타이틀, 발매일, 아티스트명으로 Album 생성
     */
    public static Album of(String title, LocalDate releaseDate, String artistName) {
        return Album.builder()
            .title(title)
            .releaseDate(releaseDate)
            .artistName(artistName)
            .build();
    }

}
