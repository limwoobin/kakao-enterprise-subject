package com.example.spotify_song_subject.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarSongTest {

    @Test
    @DisplayName("Builder를 사용하여 SimilarSong 객체를 생성할 수 있다")
    void testSimilarSongBuilder() {
        // given
        Long songId = 1L;
        String similarArtistName = "Test Artist";
        String similarSongTitle = "Test Song";
        BigDecimal similarityScore = new BigDecimal("0.95");

        // when
        SimilarSong similarSong = SimilarSong.builder()
                .songId(songId)
                .similarArtistName(similarArtistName)
                .similarSongTitle(similarSongTitle)
                .similarityScore(similarityScore)
                .build();

        // then
        assertThat(similarSong).isNotNull();
        assertThat(similarSong.getSongId()).isEqualTo(songId);
        assertThat(similarSong.getSimilarArtistName()).isEqualTo(similarArtistName);
        assertThat(similarSong.getSimilarSongTitle()).isEqualTo(similarSongTitle);
        assertThat(similarSong.getSimilarityScore()).isEqualTo(similarityScore);
    }

    @Test
    @DisplayName("유사곡 정보를 포함한 SimilarSong 객체를 생성할 수 있다")
    void testSimilarSongWithFullInfo() {
        // given
        Long songId = 100L;
        String artistName = "BTS";
        String songTitle = "Dynamite";
        BigDecimal score = new BigDecimal("0.987654321");

        // when
        SimilarSong similarSong = SimilarSong.builder()
                .songId(songId)
                .similarArtistName(artistName)
                .similarSongTitle(songTitle)
                .similarityScore(score)
                .build();

        // then
        assertThat(similarSong).isNotNull();
        assertThat(similarSong.getSongId()).isEqualTo(100L);
        assertThat(similarSong.getSimilarArtistName()).isEqualTo("BTS");
        assertThat(similarSong.getSimilarSongTitle()).isEqualTo("Dynamite");
        assertThat(similarSong.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.987654321"));
    }

    @Test
    @DisplayName("긴 아티스트명과 곡 제목을 저장할 수 있다")
    void testSimilarSongWithLongNames() {
        // given
        Long songId = 1L;
        String longArtistName = "The Beatles featuring Elvis Presley and Michael Jackson";
        String longSongTitle = "A Very Long Song Title That Contains Many Words And Could Be Even Longer Than Expected";
        BigDecimal score = new BigDecimal("0.75");

        // when
        SimilarSong similarSong = SimilarSong.builder()
                .songId(songId)
                .similarArtistName(longArtistName)
                .similarSongTitle(longSongTitle)
                .similarityScore(score)
                .build();

        // then
        assertThat(similarSong.getSimilarArtistName()).isEqualTo(longArtistName);
        assertThat(similarSong.getSimilarSongTitle()).isEqualTo(longSongTitle);
        assertThat(similarSong.getSimilarArtistName().length()).isGreaterThan(50);
        assertThat(similarSong.getSimilarSongTitle().length()).isGreaterThan(80);
    }
}