package com.example.spotify_song_subject.dto;

import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.domain.SongLike;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("SongLikeContext 테스트")
class SongLikeContextTest {

    @Test
    @DisplayName("likeAdded - 좋아요 추가 컨텍스트 생성")
    void shouldCreateLikeAddedContext() {
        // given
        Song song = mock(Song.class);
        SongLike songLike = mock(SongLike.class);

        // when
        SongLikeContext context = SongLikeContext.likeAdded(song, songLike);

        // then
        assertThat(context.getSong()).isEqualTo(song);
        assertThat(context.getSongLike()).isEqualTo(songLike);
        assertThat(context.getTotalLikes()).isNull();
    }

    @Test
    @DisplayName("likeAdded - 총 좋아요 수 포함 컨텍스트 생성")
    void shouldCreateLikeAddedContextWithTotalLikes() {
        // given
        Song song = mock(Song.class);
        SongLike songLike = mock(SongLike.class);
        Long totalLikes = 100L;

        // when
        SongLikeContext context = SongLikeContext.likeAdded(song, songLike, totalLikes);

        // then
        assertThat(context.getSong()).isEqualTo(song);
        assertThat(context.getSongLike()).isEqualTo(songLike);
        assertThat(context.getTotalLikes()).isEqualTo(100L);
    }

    @Test
    @DisplayName("likeRemoved - 좋아요 취소 컨텍스트 생성")
    void shouldCreateLikeRemovedContext() {
        // given
        Song song = mock(Song.class);
        Long totalLikes = 99L;

        // when
        SongLikeContext context = SongLikeContext.likeRemoved(song, totalLikes);

        // then
        assertThat(context.getSong()).isEqualTo(song);
        assertThat(context.getSongLike()).isNull();
        assertThat(context.getTotalLikes()).isEqualTo(99L);
    }

    @Test
    @DisplayName("setTotalLikes - 총 좋아요 수 업데이트")
    void shouldUpdateTotalLikes() {
        // given
        Song song = mock(Song.class);
        SongLikeContext context = SongLikeContext.likeRemoved(song, 50L);

        // when
        context.setTotalLikes(75L);

        // then
        assertThat(context.getTotalLikes()).isEqualTo(75L);
    }
}