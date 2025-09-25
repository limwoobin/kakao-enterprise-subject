package com.example.spotify_song_subject.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumTest {

    @Test
    @DisplayName("Builder를 사용하여 Album 객체를 생성할 수 있다")
    void testAlbumBuilder() {
        // given
        String title = "Dark Side of the Moon";
        LocalDate releaseDate = LocalDate.of(1973, 3, 1);

        // when
        Album album = Album.builder()
                .title(title)
                .releaseDate(releaseDate)
                .build();

        // then
        assertThat(album).isNotNull();
        assertThat(album.getTitle()).isEqualTo(title);
        assertThat(album.getReleaseDate()).isEqualTo(releaseDate);
    }
}