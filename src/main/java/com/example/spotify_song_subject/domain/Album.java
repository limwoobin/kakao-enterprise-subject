package com.example.spotify_song_subject.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("albums")
public class Album extends BaseDomain {

    @Id
    private Long id;

    private String title;

    @Column("release_date")
    private LocalDate releaseDate;

}
