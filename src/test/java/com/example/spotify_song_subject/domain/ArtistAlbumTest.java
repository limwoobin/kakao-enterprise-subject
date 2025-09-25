package com.example.spotify_song_subject.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtistAlbumTest {

    @Test
    @DisplayName("Builder를 사용하여 ArtistAlbum 객체를 생성할 수 있다")
    void testArtistAlbumBuilder() {
        // given
        Long artistId = 1L;
        Long albumId = 100L;

        // when
        ArtistAlbum artistAlbum = ArtistAlbum.builder()
                .artistId(artistId)
                .albumId(albumId)
                .build();

        // then
        assertThat(artistAlbum).isNotNull();
        assertThat(artistAlbum.getArtistId()).isEqualTo(artistId);
        assertThat(artistAlbum.getAlbumId()).isEqualTo(albumId);
    }
}