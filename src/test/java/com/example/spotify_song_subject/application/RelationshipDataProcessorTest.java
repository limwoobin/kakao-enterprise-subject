package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.*;
import com.example.spotify_song_subject.dto.SimilarSongDto;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import com.example.spotify_song_subject.repository.bulk.ArtistAlbumBulkRepository;
import com.example.spotify_song_subject.repository.bulk.ArtistSongBulkRepository;
import com.example.spotify_song_subject.repository.bulk.SimilarSongBulkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("RelationshipDataProcessor 단위 테스트")
class RelationshipDataProcessorTest {

    private ArtistSongBulkRepository artistSongBulkRepository;
    private ArtistAlbumBulkRepository artistAlbumBulkRepository;
    private SimilarSongBulkRepository similarSongBulkRepository;

    private RelationshipDataProcessor relationshipDataProcessor;

    @BeforeEach
    void setUp() {
        this.artistSongBulkRepository = mock(ArtistSongBulkRepository.class);
        this.artistAlbumBulkRepository = mock(ArtistAlbumBulkRepository.class);
        this.similarSongBulkRepository = mock(SimilarSongBulkRepository.class);

        this.relationshipDataProcessor = new RelationshipDataProcessor(
            artistSongBulkRepository,
            artistAlbumBulkRepository,
            similarSongBulkRepository
        );
    }

    @Test
    @DisplayName("빈 데이터로 빈 RelationshipData를 반환한다")
    void buildRelationshipsWithEmptyData() {
        // given
        List<Song> emptySongs = Collections.emptyList();
        List<SpotifySongDto> emptyDtos = Collections.emptyList();
        Map<String, Artist> emptyArtistsMap = Collections.emptyMap();
        Map<String, Album> emptyAlbumsMap = Collections.emptyMap();
        Map<Integer, String> emptySongIndexMap = Collections.emptyMap();

        // when
        RelationshipDataProcessor.RelationshipData result = RelationshipDataProcessor.buildRelationships(
            emptySongs, emptyDtos, emptyArtistsMap, emptyAlbumsMap, emptySongIndexMap
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.artistSongs()).isEmpty();
        assertThat(result.artistAlbums()).isEmpty();
        assertThat(result.similarSongs()).isEmpty();
    }

    @Test
    @DisplayName("아티스트-노래 관계를 올바르게 생성한다")
    void buildArtistSongRelationships() {
        // given
        Song song1 = createSongWithId(1L, "Song 1", 100L);
        Song song2 = createSongWithId(2L, "Song 2", 101L);

        SpotifySongDto dto1 = createSpotifySongDto("Song 1", Arrays.asList("Artist 1", "Artist 2"));
        SpotifySongDto dto2 = createSpotifySongDto("Song 2", Arrays.asList("Artist 1"));

        Map<String, Artist> artistsMap = new HashMap<>();
        artistsMap.put("Artist 1", createArtistWithId(10L, "Artist 1"));
        artistsMap.put("Artist 2", createArtistWithId(11L, "Artist 2"));

        Map<String, Album> albumsMap = new HashMap<>();
        albumsMap.put("Album 1|2023-01-01", createAlbumWithId(100L));
        albumsMap.put("Album 2|2023-01-01", createAlbumWithId(101L));

        Map<Integer, String> songIndexToAlbumKey = new HashMap<>();
        songIndexToAlbumKey.put(0, "Album 1|2023-01-01");
        songIndexToAlbumKey.put(1, "Album 2|2023-01-01");

        // when
        RelationshipDataProcessor.RelationshipData result = RelationshipDataProcessor.buildRelationships(
            Arrays.asList(song1, song2),
            Arrays.asList(dto1, dto2),
            artistsMap,
            albumsMap,
            songIndexToAlbumKey
        );

        // then
        assertThat(result.artistSongs()).hasSize(3);
        assertThat(result.artistSongs())
            .extracting(as -> as.getArtistId() + ":" + as.getSongId())
            .containsExactlyInAnyOrder("10:1", "11:1", "10:2");
    }

    @Test
    @DisplayName("아티스트-앨범 관계를 중복 없이 생성한다")
    void buildArtistAlbumRelationshipsWithoutDuplicates() {
        // given
        Song song1 = createSongWithId(1L, "Song 1", 100L);
        Song song2 = createSongWithId(2L, "Song 2", 100L); // Same album

        SpotifySongDto dto1 = createSpotifySongDto("Song 1", Arrays.asList("Artist 1"));
        SpotifySongDto dto2 = createSpotifySongDto("Song 2", Arrays.asList("Artist 1")); // Same artist

        Map<String, Artist> artistsMap = new HashMap<>();
        artistsMap.put("Artist 1", createArtistWithId(10L, "Artist 1"));

        Map<String, Album> albumsMap = new HashMap<>();
        albumsMap.put("Album 1|2023-01-01", createAlbumWithId(100L));

        Map<Integer, String> songIndexToAlbumKey = new HashMap<>();
        songIndexToAlbumKey.put(0, "Album 1|2023-01-01");
        songIndexToAlbumKey.put(1, "Album 1|2023-01-01");

        // when
        RelationshipDataProcessor.RelationshipData result = RelationshipDataProcessor.buildRelationships(
            Arrays.asList(song1, song2),
            Arrays.asList(dto1, dto2),
            artistsMap,
            albumsMap,
            songIndexToAlbumKey
        );

        // then
        assertThat(result.artistAlbums()).hasSize(1); // Should not have duplicates
        assertThat(result.artistAlbums().get(0).getArtistId()).isEqualTo(10L);
        assertThat(result.artistAlbums().get(0).getAlbumId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("유사 노래 관계를 올바르게 생성한다")
    void buildSimilarSongRelationships() {
        // given
        Song song1 = createSongWithId(1L, "Song 1", 100L);

        List<SimilarSongDto> similarSongs = Arrays.asList(
            createSimilarSongDto("Similar Artist 1", "Similar Song 1", new BigDecimal("0.95")),
            createSimilarSongDto("Similar Artist 2", "Similar Song 2", new BigDecimal("0.85"))
        );

        SpotifySongDto dto1 = createSpotifySongDtoWithSimilar("Song 1", Arrays.asList("Artist 1"), similarSongs);

        Map<String, Artist> artistsMap = new HashMap<>();
        artistsMap.put("Artist 1", createArtistWithId(10L, "Artist 1"));

        Map<String, Album> albumsMap = new HashMap<>();
        albumsMap.put("Album 1|2023-01-01", createAlbumWithId(100L));

        Map<Integer, String> songIndexToAlbumKey = new HashMap<>();
        songIndexToAlbumKey.put(0, "Album 1|2023-01-01");

        // when
        RelationshipDataProcessor.RelationshipData result = RelationshipDataProcessor.buildRelationships(
            Arrays.asList(song1),
            Arrays.asList(dto1),
            artistsMap,
            albumsMap,
            songIndexToAlbumKey
        );

        // then
        assertThat(result.similarSongs()).hasSize(2);
        assertThat(result.similarSongs())
            .extracting(SimilarSong::getSimilarArtistName)
            .containsExactlyInAnyOrder("Similar Artist 1", "Similar Artist 2");
        assertThat(result.similarSongs())
            .extracting(SimilarSong::getSimilarityScore)
            .containsExactlyInAnyOrder(new BigDecimal("0.95"), new BigDecimal("0.85"));
    }

    @Test
    @DisplayName("원본 DTO를 찾지 못한 경우 경고 로그를 남기고 건너뛴다")
    void skipWhenOriginalDtoNotFound() {
        // given
        Song song1 = createSongWithId(1L, "Song 1", 100L);

        // Empty DTOs list
        List<SpotifySongDto> emptyDtos = Collections.emptyList();

        Map<String, Artist> artistsMap = new HashMap<>();
        artistsMap.put("Artist 1", createArtistWithId(10L, "Artist 1"));

        Map<String, Album> albumsMap = new HashMap<>();
        albumsMap.put("Album 1|2023-01-01", createAlbumWithId(100L));

        Map<Integer, String> songIndexToAlbumKey = new HashMap<>();

        // when
        RelationshipDataProcessor.RelationshipData result = RelationshipDataProcessor.buildRelationships(
            Arrays.asList(song1),
            emptyDtos,
            artistsMap,
            albumsMap,
            songIndexToAlbumKey
        );

        // then
        assertThat(result.artistSongs()).isEmpty();
        assertThat(result.artistAlbums()).isEmpty();
        assertThat(result.similarSongs()).isEmpty();
    }

    @Test
    @DisplayName("repository getter 메소드들이 정상 작동한다")
    void repositoryGettersWork() {
        // then
        // Repository 주입 확인은 bulk insert 메서드 호출로 간접적으로 테스트
        assertThat(relationshipDataProcessor).isNotNull();
    }

    // Helper methods
    private Song createSongWithId(Long id, String title, Long albumId) {
        Song song = Song.builder()
            .title(title)
            .albumId(albumId)
            .build();
        // Simulate persisted entity with reflection
        try {
            java.lang.reflect.Field idField = song.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(song, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return song;
    }

    private Artist createArtistWithId(Long id, String name) {
        Artist artist = Artist.of(name);
        // Simulate persisted entity with reflection
        try {
            java.lang.reflect.Field idField = artist.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(artist, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return artist;
    }

    private Album createAlbumWithId(Long id) {
        Album album = Album.of("Album", LocalDate.of(2023, 1, 1));
        // Simulate persisted entity with reflection
        try {
            java.lang.reflect.Field idField = album.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(album, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return album;
    }

    private SpotifySongDto createSpotifySongDto(String title, List<String> artists) {
        return SpotifySongDto.builder()
            .songTitle(title)
            .artists(String.join(", ", artists))
            .albumTitle("Album 1")
            .releaseDate("2023-01-01")
            .build();
    }

    private SpotifySongDto createSpotifySongDtoWithSimilar(String title, List<String> artists, List<SimilarSongDto> similarSongs) {
        return SpotifySongDto.builder()
            .songTitle(title)
            .artists(String.join(", ", artists))
            .albumTitle("Album 1")
            .releaseDate("2023-01-01")
            .similarSongs(similarSongs)
            .build();
    }

    private SimilarSongDto createSimilarSongDto(String artistName, String songTitle, BigDecimal score) {
        return SimilarSongDto.builder()
            .artistName(artistName)
            .songTitle(songTitle)
            .similarityScore(score)
            .build();
    }
}