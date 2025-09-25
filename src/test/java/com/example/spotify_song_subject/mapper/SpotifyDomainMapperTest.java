package com.example.spotify_song_subject.mapper;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SimilarSongDto;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpotifyDomainMapper 단위 테스트")
class SpotifyDomainMapperTest {

    @Test
    @DisplayName("SpotifySongDto를 Song 엔티티로 정상 변환한다")
    void SpotifySongDto를_Song엔티티로_변환() {
        // given
        SpotifySongDto dto = createTestSpotifySongDto();
        Long albumId = 1L;

        // when
        Song song = SpotifyDomainMapper.convertToSong(dto, albumId);

        // then
        assertThat(song).isNotNull();
        assertThat(song.getAlbumId()).isEqualTo(albumId);
        assertThat(song.getTitle()).isEqualTo("Even When the Waters Cold");
        assertThat(song.getLyrics()).isEqualTo("Test lyrics for the song");
        assertThat(song.getLength()).isEqualTo(LocalTime.of(0, 3, 47));
        assertThat(song.getMusicKey()).isEqualTo("D min");
        assertThat(song.getTempo()).isNotNull();
        assertThat(song.getTempo()).isEqualTo(new BigDecimal("0.4378698225"));
        assertThat(song.getLoudnessDb()).isNotNull();
        assertThat(song.getLoudnessDb()).isEqualTo(new BigDecimal("0.785065407"));
        assertThat(song.getTimeSignature()).isEqualTo("4/4");
        assertThat(song.getExplicitContent()).isEqualTo(InclusionStatus.NOT_INCLUDED);
        assertThat(song.getEmotion()).isEqualTo("sadness");
        assertThat(song.getGenre()).isEqualTo("hip hop");
        assertThat(song.getPopularity()).isEqualTo(40);
        assertThat(song.getEnergy()).isEqualTo(83);
        assertThat(song.getDanceability()).isEqualTo(71);
        assertThat(song.getPositiveness()).isEqualTo(87);
        assertThat(song.getSpeechiness()).isEqualTo(4);
        assertThat(song.getLiveness()).isEqualTo(16);
        assertThat(song.getAcousticness()).isEqualTo(11);
        assertThat(song.getInstrumentalness()).isEqualTo(0);
        assertThat(song.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("활동 적합도를 올바르게 변환한다")
    void 활동적합도_변환() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .goodForParty(1)
            .goodForWorkStudy(0)
            .goodForExercise(1)
            .build();

        // when
        Song song = SpotifyDomainMapper.convertToSong(dto, 1L);

        // then
        assertThat(song.getActivitySuitabilityParty()).isEqualTo(ActivitySuitability.SUITABLE);
        assertThat(song.getActivitySuitabilityWork()).isEqualTo(ActivitySuitability.NOT_SUITABLE);
        assertThat(song.getActivitySuitabilityExercise()).isEqualTo(ActivitySuitability.SUITABLE);
    }

    @Test
    @DisplayName("아티스트 리스트를 정확히 추출한다")
    void 아티스트_리스트_추출() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .artists("Artist1, Artist2, Artist3")
            .songTitle("Test Song")
            .build();

        // when
        List<Artist> artists = SpotifyDomainMapper.extractArtists(dto);

        // then
        assertThat(artists).hasSize(3);
        assertThat(artists.get(0).getName()).isEqualTo("Artist1");
        assertThat(artists.get(1).getName()).isEqualTo("Artist2");
        assertThat(artists.get(2).getName()).isEqualTo("Artist3");
    }

    @Test
    @DisplayName("단일 아티스트를 정확히 추출한다")
    void 단일_아티스트_추출() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .artists("!!!")
            .songTitle("Test Song")
            .build();

        // when
        List<Artist> artists = SpotifyDomainMapper.extractArtists(dto);

        // then
        assertThat(artists).hasSize(1);
        assertThat(artists.get(0).getName()).isEqualTo("!!!");
    }

    @Test
    @DisplayName("아티스트가 없는 경우 빈 리스트를 반환한다")
    void 아티스트_없는경우_빈리스트() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .songTitle("Test Song")
            .build();

        // when
        List<Artist> artists = SpotifyDomainMapper.extractArtists(dto);

        // then
        assertThat(artists).isEmpty();
    }

    @Test
    @DisplayName("앨범 정보를 정확히 추출한다")
    void 앨범_정보_추출() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .albumTitle("Thr!!!er")
            .releaseDate("2013-04-29")
            .songTitle("Test Song")
            .build();

        // when
        Album album = SpotifyDomainMapper.extractAlbum(dto);

        // then
        assertThat(album).isNotNull();
        assertThat(album.getTitle()).isEqualTo("Thr!!!er");
        assertThat(album.getReleaseDate()).isEqualTo(LocalDate.of(2013, 4, 29));
    }

    @Test
    @DisplayName("앨범 제목이 없는 경우 기본 앨범을 생성한다")
    void 앨범제목_없는경우_기본앨범() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .songTitle("Test Song")
            .build();

        // when
        Album album = SpotifyDomainMapper.extractAlbum(dto);

        // then
        assertThat(album).isNotNull();
        assertThat(album.getTitle()).isEqualTo("Unknown Album");
        assertThat(album.getReleaseDate()).isNull();
    }

    @Test
    @DisplayName("다양한 날짜 형식을 처리한다")
    void 다양한_날짜형식_처리() {
        // given - YYYY-MM-DD 형식
        SpotifySongDto dto1 = SpotifySongDto.builder()
            .albumTitle("Test Album")
            .releaseDate("2023-12-25")
            .build();

        // given - YYYY/MM/DD 형식
        SpotifySongDto dto2 = SpotifySongDto.builder()
            .albumTitle("Test Album")
            .releaseDate("2023/12/25")
            .build();

        // given - MM/DD/YYYY 형식
        SpotifySongDto dto3 = SpotifySongDto.builder()
            .albumTitle("Test Album")
            .releaseDate("12/25/2023")
            .build();

        // given - YYYY 형식
        SpotifySongDto dto4 = SpotifySongDto.builder()
            .albumTitle("Test Album")
            .releaseDate("2023")
            .build();

        // when
        Album album1 = SpotifyDomainMapper.extractAlbum(dto1);
        Album album2 = SpotifyDomainMapper.extractAlbum(dto2);
        Album album3 = SpotifyDomainMapper.extractAlbum(dto3);
        Album album4 = SpotifyDomainMapper.extractAlbum(dto4);

        // then
        assertThat(album1.getReleaseDate()).isEqualTo(LocalDate.of(2023, 12, 25));
        assertThat(album2.getReleaseDate()).isEqualTo(LocalDate.of(2023, 12, 25));
        assertThat(album3.getReleaseDate()).isEqualTo(LocalDate.of(2023, 12, 25));
        assertThat(album4.getReleaseDate()).isEqualTo(LocalDate.of(2023, 1, 1));
    }

    @Test
    @DisplayName("ArtistSong 관계 엔티티를 생성한다")
    void ArtistSong_관계엔티티_생성() {
        // given
        Long artistId = 1L;
        Long songId = 2L;

        // when
        ArtistSong artistSong = SpotifyDomainMapper.createArtistSong(artistId, songId);

        // then
        assertThat(artistSong).isNotNull();
        assertThat(artistSong.getArtistId()).isEqualTo(artistId);
        assertThat(artistSong.getSongId()).isEqualTo(songId);
    }

    @Test
    @DisplayName("ArtistAlbum 관계 엔티티를 생성한다")
    void ArtistAlbum_관계엔티티_생성() {
        // given
        Long artistId = 1L;
        Long albumId = 3L;

        // when
        ArtistAlbum artistAlbum = SpotifyDomainMapper.createArtistAlbum(artistId, albumId);

        // then
        assertThat(artistAlbum).isNotNull();
        assertThat(artistAlbum.getArtistId()).isEqualTo(artistId);
        assertThat(artistAlbum.getAlbumId()).isEqualTo(albumId);
    }

    @Test
    @DisplayName("SimilarSong 관계 엔티티를 생성한다")
    void SimilarSong_관계엔티티_생성() {
        // given
        Long songId = 1L;
        String artistName = "Test Artist";
        String songTitle = "Test Song";
        BigDecimal similarityScore = new BigDecimal("0.95");

        // when
        SimilarSong similarSong = SpotifyDomainMapper.createSimilarSong(songId, artistName, songTitle, similarityScore);

        // then
        assertThat(similarSong).isNotNull();
        assertThat(similarSong.getSongId()).isEqualTo(songId);
        assertThat(similarSong.getSimilarArtistName()).isEqualTo(artistName);
        assertThat(similarSong.getSimilarSongTitle()).isEqualTo(songTitle);
        assertThat(similarSong.getSimilarityScore()).isEqualTo(similarityScore);
    }

    @Test
    @DisplayName("실제 아티스트 정보로 SimilarSong 관계 엔티티를 생성한다")
    void SimilarSong_실제데이터로_생성() {
        // given
        Long songId = 100L;
        String artistName = "Corey Smith";
        String songTitle = "If I Could Do It Again";
        BigDecimal similarityScore = new BigDecimal("0.9860607848");

        // when
        SimilarSong similarSong = SpotifyDomainMapper.createSimilarSong(songId, artistName, songTitle, similarityScore);

        // then
        assertThat(similarSong).isNotNull();
        assertThat(similarSong.getSongId()).isEqualTo(100L);
        assertThat(similarSong.getSimilarArtistName()).isEqualTo("Corey Smith");
        assertThat(similarSong.getSimilarSongTitle()).isEqualTo("If I Could Do It Again");
        assertThat(similarSong.getSimilarityScore()).isEqualByComparingTo(new BigDecimal("0.9860607848"));
    }

    @Test
    @DisplayName("긴 아티스트명과 곡 제목으로 SimilarSong을 생성한다")
    void SimilarSong_긴제목_생성() {
        // given
        Long songId = 1L;
        String longArtistName = "The Beatles featuring Elvis Presley and Michael Jackson";
        String longSongTitle = "A Very Long Song Title That Contains Many Words And Could Be Even Longer Than Expected";
        BigDecimal score = new BigDecimal("0.75");

        // when
        SimilarSong similarSong = SpotifyDomainMapper.createSimilarSong(songId, longArtistName, longSongTitle, score);

        // then
        assertThat(similarSong.getSimilarArtistName()).isEqualTo(longArtistName);
        assertThat(similarSong.getSimilarSongTitle()).isEqualTo(longSongTitle);
        assertThat(similarSong.getSimilarityScore()).isEqualByComparingTo(score);
    }

    @Test
    @DisplayName("Explicit 상태를 올바르게 변환한다")
    void Explicit_상태_변환() {
        // given - YES 케이스
        SpotifySongDto dto1 = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .explicit("YES")
            .build();

        // given - NO 케이스
        SpotifySongDto dto2 = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .explicit("NO")
            .build();

        // given - null 케이스
        SpotifySongDto dto3 = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .build();

        // when
        Song song1 = SpotifyDomainMapper.convertToSong(dto1, 1L);
        Song song2 = SpotifyDomainMapper.convertToSong(dto2, 1L);
        Song song3 = SpotifyDomainMapper.convertToSong(dto3, 1L);

        // then
        assertThat(song1.getExplicitContent()).isEqualTo(InclusionStatus.INCLUDED);
        assertThat(song2.getExplicitContent()).isEqualTo(InclusionStatus.NOT_INCLUDED);
        assertThat(song3.getExplicitContent()).isEqualTo(InclusionStatus.NOT_INCLUDED);
    }

    @Test
    @DisplayName("시간 형식을 올바르게 파싱한다")
    void 시간형식_파싱() {
        // given - MM:SS 형식
        SpotifySongDto dto1 = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .length("03:47")
            .build();

        // given - HH:MM:SS 형식
        SpotifySongDto dto2 = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .length("01:03:47")
            .build();

        // given - null 케이스
        SpotifySongDto dto3 = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .build();

        // when
        Song song1 = SpotifyDomainMapper.convertToSong(dto1, 1L);
        Song song2 = SpotifyDomainMapper.convertToSong(dto2, 1L);
        Song song3 = SpotifyDomainMapper.convertToSong(dto3, 1L);

        // then
        assertThat(song1.getLength()).isEqualTo(LocalTime.of(0, 3, 47));
        assertThat(song2.getLength()).isEqualTo(LocalTime.of(1, 3, 47));
        assertThat(song3.getLength()).isNull();
    }

    @Test
    @DisplayName("퍼센트 기호가 있는 값도 올바르게 파싱한다")
    void 퍼센트기호_파싱() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder()
            .artists("Test Artist")
            .songTitle("Test Song")
            .popularity(40)
            .energy(83)
            .build();

        // when
        Song song = SpotifyDomainMapper.convertToSong(dto, 1L);

        // then
        assertThat(song.getPopularity()).isEqualTo(40);
        assertThat(song.getEnergy()).isEqualTo(83);
    }

    private SpotifySongDto createTestSpotifySongDto() {
        List<SimilarSongDto> similarSongs = new ArrayList<>();
        similarSongs.add(SimilarSongDto.builder()
            .artistName("Corey Smith")
            .songTitle("If I Could Do It Again")
            .similarityScore(new BigDecimal("0.9860607848"))
            .build());
        similarSongs.add(SimilarSongDto.builder()
            .artistName("Toby Keith")
            .songTitle("Drinks After Work")
            .similarityScore(new BigDecimal("0.9837194774"))
            .build());
        similarSongs.add(SimilarSongDto.builder()
            .artistName("Space")
            .songTitle("Neighbourhood")
            .similarityScore(new BigDecimal("0.9832363508"))
            .build());

        return SpotifySongDto.builder()
            .artists("!!!")
            .songTitle("Even When the Waters Cold")
            .lyrics("Test lyrics for the song")
            .length("03:47")
            .emotion("sadness")
            .genre("hip hop")
            .albumTitle("Thr!!!er")
            .releaseDate("2013-04-29")
            .musicKey("D min")
            .tempo(new BigDecimal("0.4378698225"))
            .loudnessDb(new BigDecimal("0.785065407"))
            .timeSignature("4/4")
            .explicit("No")
            .popularity(40)
            .energy(83)
            .danceability(71)
            .positiveness(87)
            .speechiness(4)
            .liveness(16)
            .acousticness(11)
            .instrumentalness(0)
            .goodForParty(0)
            .goodForWorkStudy(0)
            .goodForRelaxationMeditation(0)
            .goodForExercise(0)
            .goodForRunning(0)
            .goodForYogaStretching(0)
            .goodForDriving(0)
            .goodForSocialGatherings(0)
            .goodForMorningRoutine(0)
            .similarSongs(similarSongs)
            .build();
    }
}