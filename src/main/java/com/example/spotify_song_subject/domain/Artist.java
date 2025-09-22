package com.example.spotify_song_subject.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("artists")
public class Artist extends BaseDomain {

    @Id
    private Long id;

    private String name;
}
