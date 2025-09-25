package com.example.spotify_song_subject.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class SongTest {

    @Test
    @DisplayName("Builder를 사용하여 Song 객체를 생성할 수 있다")
    void testSongBuilder() {
        // given
        Long albumId = 1L;
        String title = "Bohemian Rhapsody";
        String lyrics = "Is this the real life? Is this just fantasy?";
        LocalTime length = LocalTime.of(0, 5, 55);
        String musicKey = "Bb major";
        BigDecimal tempo = new BigDecimal("72.0");
        BigDecimal loudnessDb = new BigDecimal("-10.5");
        String timeSignature = "4/4";
        InclusionStatus explicitContent = InclusionStatus.NOT_INCLUDED;
        String emotion = "dramatic";
        String genre = "rock";
        Integer popularity = 95;
        Integer energy = 75;
        Integer danceability = 40;
        Integer positiveness = 30;
        Integer speechiness = 10;
        Integer liveness = 20;
        Integer acousticness = 10;
        Integer instrumentalness = 0;
        ActivitySuitability partyActivity = ActivitySuitability.SUITABLE;
        Long likeCount = 1000000L;

        // when
        Song song = Song.builder()
                .albumId(albumId)
                .title(title)
                .lyrics(lyrics)
                .length(length)
                .musicKey(musicKey)
                .tempo(tempo)
                .loudnessDb(loudnessDb)
                .timeSignature(timeSignature)
                .explicitContent(explicitContent)
                .emotion(emotion)
                .genre(genre)
                .popularity(popularity)
                .energy(energy)
                .danceability(danceability)
                .positiveness(positiveness)
                .speechiness(speechiness)
                .liveness(liveness)
                .acousticness(acousticness)
                .instrumentalness(instrumentalness)
                .activitySuitabilityParty(partyActivity)
                .likeCount(likeCount)
                .build();

        // then
        assertThat(song).isNotNull();
        assertThat(song.getAlbumId()).isEqualTo(albumId);
        assertThat(song.getTitle()).isEqualTo(title);
        assertThat(song.getLyrics()).isEqualTo(lyrics);
        assertThat(song.getLength()).isEqualTo(length);
        assertThat(song.getMusicKey()).isEqualTo(musicKey);
        assertThat(song.getTempo()).isEqualTo(tempo);
        assertThat(song.getLoudnessDb()).isEqualTo(loudnessDb);
        assertThat(song.getTimeSignature()).isEqualTo(timeSignature);
        assertThat(song.getExplicitContent()).isEqualTo(explicitContent);
        assertThat(song.getEmotion()).isEqualTo(emotion);
        assertThat(song.getGenre()).isEqualTo(genre);
        assertThat(song.getPopularity()).isEqualTo(popularity);
        assertThat(song.getEnergy()).isEqualTo(energy);
        assertThat(song.getDanceability()).isEqualTo(danceability);
        assertThat(song.getPositiveness()).isEqualTo(positiveness);
        assertThat(song.getSpeechiness()).isEqualTo(speechiness);
        assertThat(song.getLiveness()).isEqualTo(liveness);
        assertThat(song.getAcousticness()).isEqualTo(acousticness);
        assertThat(song.getInstrumentalness()).isEqualTo(instrumentalness);
        assertThat(song.getActivitySuitabilityParty()).isEqualTo(partyActivity);
        assertThat(song.getLikeCount()).isEqualTo(likeCount);
    }
}