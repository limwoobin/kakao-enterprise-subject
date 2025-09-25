package com.example.spotify_song_subject.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtistTest {

    @Test
    @DisplayName("Builder를 사용하여 Artist 객체를 생성할 수 있다")
    void testArtistBuilder() {
        // given
        String name = "Pink Floyd";

        // when
        Artist artist = Artist.builder()
                .name(name)
                .build();

        // then
        assertThat(artist).isNotNull();
        assertThat(artist.getName()).isEqualTo(name);
    }
}