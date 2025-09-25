package com.example.spotify_song_subject.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtistSongTest {

    @Test
    @DisplayName("Builder를 사용하여 ArtistSong 객체를 생성할 수 있다")
    void testArtistSongBuilder() {
        // given
        Long artistId = 1L;
        Long songId = 100L;

        // when
        ArtistSong artistSong = ArtistSong.builder()
                .artistId(artistId)
                .songId(songId)
                .build();

        // then
        assertThat(artistSong).isNotNull();
        assertThat(artistSong.getArtistId()).isEqualTo(artistId);
        assertThat(artistSong.getSongId()).isEqualTo(songId);
    }
}