package com.example.spotify_song_subject.mapper;

import com.example.spotify_song_subject.dto.SimilarSongDto;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpotifyDataMapper 단위 테스트")
class SpotifyDataMapperTest {

    @Test
    @DisplayName("Map 데이터를 SpotifySongDto로 정상 변환한다")
    void Map데이터를_SpotifySongDto로_변환() {
        // given
        Map<String, Object> songData = createTestSongData();

        // when
        SpotifySongDto dto = SpotifyDataMapper.mapToSpotifySongDto(songData);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getArtists()).isEqualTo("!!!");
        assertThat(dto.getSongTitle()).isEqualTo("Even When the Waters Cold");
        assertThat(dto.getLyrics()).startsWith("Test lyrics");
        assertThat(dto.getLength()).isEqualTo("03:47");
        assertThat(dto.getEmotion()).isEqualTo("sadness");
        assertThat(dto.getGenre()).isEqualTo("hip hop");
        assertThat(dto.getAlbumTitle()).isEqualTo("Thr!!!er");
        assertThat(dto.getReleaseDate()).isEqualTo("2013-04-29");
        assertThat(dto.getMusicKey()).isEqualTo("D min");
        assertThat(dto.getTempo()).isEqualByComparingTo(new BigDecimal("0.4378698225"));
        assertThat(dto.getLoudnessDb()).isEqualByComparingTo(new BigDecimal("0.785065407"));
        assertThat(dto.getTimeSignature()).isEqualTo("4/4");
        assertThat(dto.getExplicit()).isEqualTo("No");
        assertThat(dto.getPopularity()).isEqualTo(40);
        assertThat(dto.getEnergy()).isEqualTo(83);
        assertThat(dto.getDanceability()).isEqualTo(71);
        assertThat(dto.getPositiveness()).isEqualTo(87);
        assertThat(dto.getSpeechiness()).isEqualTo(4);
        assertThat(dto.getLiveness()).isEqualTo(16);
        assertThat(dto.getAcousticness()).isEqualTo(11);
        assertThat(dto.getInstrumentalness()).isEqualTo(0);
    }

    @Test
    @DisplayName("활동 적합도 필드를 정확히 매핑한다")
    void 활동적합도_필드_매핑() {
        // given
        Map<String, Object> songData = createTestSongData();

        // when
        SpotifySongDto dto = SpotifyDataMapper.mapToSpotifySongDto(songData);

        // then
        assertThat(dto.getGoodForParty()).isEqualTo(0);
        assertThat(dto.getGoodForWorkStudy()).isEqualTo(0);
        assertThat(dto.getGoodForRelaxationMeditation()).isEqualTo(0);
        assertThat(dto.getGoodForExercise()).isEqualTo(0);
        assertThat(dto.getGoodForRunning()).isEqualTo(0);
        assertThat(dto.getGoodForYogaStretching()).isEqualTo(0);
        assertThat(dto.getGoodForDriving()).isEqualTo(0);
        assertThat(dto.getGoodForSocialGatherings()).isEqualTo(0);
        assertThat(dto.getGoodForMorningRoutine()).isEqualTo(0);
    }

    @Test
    @DisplayName("Similar Songs 리스트를 정확히 변환한다")
    void SimilarSongs_리스트_변환() {
        // given
        Map<String, Object> songData = createTestSongData();

        // when
        SpotifySongDto dto = SpotifyDataMapper.mapToSpotifySongDto(songData);

        // then
        assertThat(dto.getSimilarSongs()).hasSize(3);

        SimilarSongDto first = dto.getSimilarSongs().get(0);
        assertThat(first.getArtistName()).isEqualTo("Corey Smith");
        assertThat(first.getSongTitle()).isEqualTo("If I Could Do It Again");
        assertThat(first.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.9860607848"));

        SimilarSongDto second = dto.getSimilarSongs().get(1);
        assertThat(second.getArtistName()).isEqualTo("Toby Keith");
        assertThat(second.getSongTitle()).isEqualTo("Drinks After Work");
        assertThat(second.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.9837194774"));

        SimilarSongDto third = dto.getSimilarSongs().get(2);
        assertThat(third.getArtistName()).isEqualTo("Space");
        assertThat(third.getSongTitle()).isEqualTo("Neighbourhood");
        assertThat(third.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.9832363508"));
    }

    @Test
    @DisplayName("null 값이 포함된 Map도 정상 처리한다")
    void null값_포함_Map_처리() {
        // given
        Map<String, Object> songData = new HashMap<>();
        songData.put("Artist(s)", "Test Artist");
        songData.put("song", "Test Song");
        songData.put("Album", null);
        songData.put("Tempo", null);
        songData.put("Popularity", null);

        // when
        SpotifySongDto dto = SpotifyDataMapper.mapToSpotifySongDto(songData);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getArtists()).isEqualTo("Test Artist");
        assertThat(dto.getSongTitle()).isEqualTo("Test Song");
        assertThat(dto.getAlbumTitle()).isNull();
        assertThat(dto.getTempo()).isNull();
        assertThat(dto.getPopularity()).isNull();
    }

    @Test
    @DisplayName("Similar Songs가 없는 경우 빈 리스트를 반환한다")
    void SimilarSongs_없는경우_빈리스트() {
        // given
        Map<String, Object> songData = new HashMap<>();
        songData.put("Artist(s)", "Test Artist");
        songData.put("song", "Test Song");

        // when
        SpotifySongDto dto = SpotifyDataMapper.mapToSpotifySongDto(songData);

        // then
        assertThat(dto.getSimilarSongs()).isEmpty();
    }

    private Map<String, Object> createTestSongData() {
        Map<String, Object> songData = new HashMap<>();
        songData.put("Artist(s)", "!!!");
        songData.put("song", "Even When the Waters Cold");
        songData.put("text", "Test lyrics for the song"); // 저작권 보호를 위해 간단한 텍스트로 대체
        songData.put("Length", "03:47");
        songData.put("emotion", "sadness");
        songData.put("Genre", "hip hop");
        songData.put("Album", "Thr!!!er");
        songData.put("Release Date", "2013-04-29");
        songData.put("Key", "D min");
        songData.put("Tempo", 0.4378698225);
        songData.put("Loudness (db)", 0.785065407);
        songData.put("Time signature", "4/4");
        songData.put("Explicit", "No");
        songData.put("Popularity", 40);
        songData.put("Energy", 83);
        songData.put("Danceability", 71);
        songData.put("Positiveness", 87);
        songData.put("Speechiness", 4);
        songData.put("Liveness", 16);
        songData.put("Acousticness", 11);
        songData.put("Instrumentalness", 0);
        songData.put("Good for Party", 0);
        songData.put("Good for Work/Study", 0);
        songData.put("Good for Relaxation/Meditation", 0);
        songData.put("Good for Exercise", 0);
        songData.put("Good for Running", 0);
        songData.put("Good for Yoga/Stretching", 0);
        songData.put("Good for Driving", 0);
        songData.put("Good for Social Gatherings", 0);
        songData.put("Good for Morning Routine", 0);

        List<Map<String, Object>> similarSongs = getSimilarSongs();
        songData.put("Similar Songs", similarSongs);

        return songData;
    }

    private static List<Map<String, Object>> getSimilarSongs() {
        List<Map<String, Object>> similarSongs = new ArrayList<>();
        Map<String, Object> similar1 = new HashMap<>();
        similar1.put("Similar Artist 1", "Corey Smith");
        similar1.put("Similar Song 1", "If I Could Do It Again");
        similar1.put("Similarity Score", 0.9860607848);
        similarSongs.add(similar1);

        Map<String, Object> similar2 = new HashMap<>();
        similar2.put("Similar Artist 2", "Toby Keith");
        similar2.put("Similar Song 2", "Drinks After Work");
        similar2.put("Similarity Score", 0.9837194774);
        similarSongs.add(similar2);

        Map<String, Object> similar3 = new HashMap<>();
        similar3.put("Similar Artist 3", "Space");
        similar3.put("Similar Song 3", "Neighbourhood");
        similar3.put("Similarity Score", 0.9832363508);
        similarSongs.add(similar3);
        return similarSongs;
    }
}