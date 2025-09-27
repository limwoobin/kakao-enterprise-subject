package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.dto.SpotifySongDto;
import com.example.spotify_song_subject.repository.SongRepository;
import com.example.spotify_song_subject.repository.bulk.SongBulkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SongBatchProcessor 단위 테스트")
class SongBatchProcessorTest {

    private SongRepository songRepository;
    private SongBulkRepository songBulkRepository;

    private SongBatchProcessor songBatchProcessor;

    private List<SpotifySongDto> songDtos;
    private Map<String, Album> albumsMap;
    private List<Song> expectedSongs;

    @BeforeEach
    void setUp() {
        this.songRepository = mock(SongRepository.class);
        this.songBulkRepository = mock(SongBulkRepository.class);
        this.songBatchProcessor = new SongBatchProcessor(songRepository, songBulkRepository);

        songDtos = Arrays.asList(
            createSongDto("Song1", "Album1", "2023-01-01", "Artist1"),
            createSongDto("Song2", "Album1", "2023-01-01", "Artist1"),
            createSongDto("Song3", "Album2", "2023-02-01", "Artist2")
        );

        albumsMap = new HashMap<>();
        albumsMap.put("Album1|2023-01-01|Artist1", createAlbum(1L, "Album1", LocalDate.of(2023, 1, 1), "Artist1"));
        albumsMap.put("Album2|2023-02-01|Artist2", createAlbum(2L, "Album2", LocalDate.of(2023, 2, 1), "Artist2"));

        expectedSongs = Arrays.asList(
            createSong(1L, "Song1", 1L),
            createSong(2L, "Song2", 1L),
            createSong(3L, "Song3", 2L)
        );
    }

    @Test
    @DisplayName("빈 songDtos 리스트를 처리하면 빈 결과를 반환한다")
    void processEmptySongDtos() {
        // given
        List<SpotifySongDto> emptyList = Collections.emptyList();

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(emptyList, albumsMap))
            .assertNext(result -> {
                assertThat(result.savedSongs()).isEmpty();
                assertThat(result.songIndexToAlbumKey()).isEmpty();
            })
            .verifyComplete();

        verify(songBulkRepository, never()).bulkInsert(any());
        verify(songRepository, never()).findAllByTitleIn(any());
    }

    @Test
    @DisplayName("빈 albumsMap을 처리해도 album_id 없이 songs를 저장한다")
    void processEmptyAlbumsMap() {
        // given
        Map<String, Album> emptyMap = Collections.emptyMap();

        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(3L));
        
        // title로 조회하지만 결과가 없음
        when(songRepository.findAllByTitleIn(anyCollection()))
            .thenReturn(Flux.empty());

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(songDtos, emptyMap))
            .assertNext(result -> {
                assertThat(result.savedSongs()).isEmpty(); // title로 조회했지만 결과가 없음
                assertThat(result.songIndexToAlbumKey()).hasSize(3);
                assertThat(result.songIndexToAlbumKey().values()).allMatch(Objects::isNull);
            })
            .verifyComplete();

        verify(songBulkRepository).bulkInsert(argThat(songs -> 
            songs.size() == 3 && 
            songs.stream().allMatch(song -> song.getAlbumId() == null)
        ));
    }

    @Test
    @DisplayName("정상적으로 songs를 bulk insert하고 재조회한다")
    void processNormalSongs() {
        // given
        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(3L));

        when(songRepository.findAllByTitleIn(Set.of("Song1", "Song2", "Song3")))
            .thenReturn(Flux.fromIterable(expectedSongs));

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(songDtos, albumsMap))
            .assertNext(result -> {
                assertThat(result.savedSongs()).hasSize(3);
                assertThat(result.savedSongs()).containsExactlyInAnyOrderElementsOf(expectedSongs);
                
                assertThat(result.songIndexToAlbumKey()).hasSize(3);
                assertThat(result.songIndexToAlbumKey()).containsEntry(0, "Album1|2023-01-01|Artist1");
                assertThat(result.songIndexToAlbumKey()).containsEntry(1, "Album1|2023-01-01|Artist1");
                assertThat(result.songIndexToAlbumKey()).containsEntry(2, "Album2|2023-02-01|Artist2");
            })
            .verifyComplete();

        verify(songBulkRepository).bulkInsert(argThat(songs -> songs.size() == 3));
    }

    @Test
    @DisplayName("일부 songs의 album이 없으면 album_id를 null로 저장한다")
    void processWithMissingAlbums() {
        // given
        // Album2만 있고 Album1은 없는 상황
        Map<String, Album> partialAlbumsMap = new HashMap<>();
        partialAlbumsMap.put("Album2|2023-02-01|Artist2", createAlbum(2L, "Album2", LocalDate.of(2023, 2, 1), "Artist2"));

        // Song1, Song2는 album이 없어서 null album_id로 저장, Song3만 album_id 2로 저장
        List<Song> savedSongs = Arrays.asList(
            createSong(1L, "Song1", null),  // album 없음
            createSong(2L, "Song2", null),  // album 없음
            createSong(3L, "Song3", 2L)     // album 있음
        );

        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(3L));  // 3개 모두 저장

        when(songRepository.findAllByTitleIn(Set.of("Song1", "Song2", "Song3")))
            .thenReturn(Flux.fromIterable(savedSongs));  // 모든 song 반환

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(songDtos, partialAlbumsMap))
            .assertNext(result -> {
                assertThat(result.savedSongs()).hasSize(3);  // 모든 song이 저장됨
                
                // album이 없는 songs
                assertThat(result.savedSongs().stream()
                    .filter(s -> s.getAlbumId() == null)
                    .count()).isEqualTo(2);
                
                // album이 있는 song
                assertThat(result.savedSongs().stream()
                    .filter(s -> s.getAlbumId() != null)
                    .count()).isEqualTo(1);
                
                assertThat(result.songIndexToAlbumKey()).hasSize(3);
                assertThat(result.songIndexToAlbumKey()).containsEntry(0, null);  // Song1 - album 없음
                assertThat(result.songIndexToAlbumKey()).containsEntry(1, null);  // Song2 - album 없음
                assertThat(result.songIndexToAlbumKey()).containsEntry(2, "Album2|2023-02-01|Artist2");
            })
            .verifyComplete();

        verify(songBulkRepository).bulkInsert(argThat(songs -> songs.size() == 3));
    }

    @Test
    @DisplayName("대량의 songs를 효율적으로 처리한다")
    void processLargeDataSet() {
        // given
        List<SpotifySongDto> largeSongDtos = new ArrayList<>();
        Map<String, Album> largeAlbumsMap = new HashMap<>();
        List<Song> largeSavedSongs = new ArrayList<>();

        // 1000개의 song 생성
        for (int i = 0; i < 1000; i++) {
            String albumTitle = "Album" + (i / 100); // 10개의 앨범
            String artistName = "Artist" + (i / 100);
            LocalDate releaseDate = LocalDate.of(2023, 1, 1).plusDays(i / 100);
            
            largeSongDtos.add(createSongDto("Song" + i, albumTitle, releaseDate.toString(), artistName));
            
            String albumKey = albumTitle + "|" + releaseDate + "|" + artistName;
            if (!largeAlbumsMap.containsKey(albumKey)) {
                largeAlbumsMap.put(albumKey, createAlbum((long) (i / 100), albumTitle, releaseDate, artistName));
            }
            
            largeSavedSongs.add(createSong((long) i, "Song" + i, (long) (i / 100)));
        }

        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(1000L));

        Set<String> titles = largeSongDtos.stream()
            .map(SpotifySongDto::getSongTitle)
            .collect(Collectors.toSet());
        when(songRepository.findAllByTitleIn(titles))
            .thenReturn(Flux.fromIterable(largeSavedSongs));

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(largeSongDtos, largeAlbumsMap))
            .assertNext(result -> {
                assertThat(result.savedSongs()).hasSize(1000);
                assertThat(result.songIndexToAlbumKey()).hasSize(1000);
            })
            .verifyComplete();

        verify(songBulkRepository).bulkInsert(argThat(songs -> songs.size() == 1000));
    }

    @Test
    @DisplayName("bulk insert 실패 시 에러를 전파한다")
    void handleBulkInsertError() {
        // given
        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.error(new RuntimeException("Bulk insert failed")));

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(songDtos, albumsMap))
            .expectErrorMessage("Bulk insert failed")
            .verify();
    }

    @Test
    @DisplayName("재조회 실패 시 에러를 전파한다")
    void handleReloadError() {
        // given
        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(3L));

        when(songRepository.findAllByTitleIn(anySet()))
            .thenReturn(Flux.error(new RuntimeException("Reload failed")));

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(songDtos, albumsMap))
            .expectErrorMessage("Reload failed")
            .verify();
    }

    @Test
    @DisplayName("다양한 날짜 포맷을 올바르게 처리한다")
    void handleVariousDateFormats() {
        // given
        List<SpotifySongDto> mixedDateSongs = Arrays.asList(
            createSongDto("Song1", "Album1", "2023-01-01", "Artist1"),
            createSongDto("Song2", "Album2", "02/01/2023", "Artist2"),
            createSongDto("Song3", "Album3", "2023/03/01", "Artist3"),
            createSongDto("Song4", "Album4", "2023", "Artist4")
        );

        Map<String, Album> dateAlbumsMap = new HashMap<>();
        dateAlbumsMap.put("Album1|2023-01-01|Artist1", createAlbum(1L, "Album1", LocalDate.of(2023, 1, 1), "Artist1"));
        dateAlbumsMap.put("Album2|2023-02-01|Artist2", createAlbum(2L, "Album2", LocalDate.of(2023, 2, 1), "Artist2"));
        dateAlbumsMap.put("Album3|2023-03-01|Artist3", createAlbum(3L, "Album3", LocalDate.of(2023, 3, 1), "Artist3"));
        dateAlbumsMap.put("Album4|2023-01-01|Artist4", createAlbum(4L, "Album4", LocalDate.of(2023, 1, 1), "Artist4"));

        List<Song> savedSongs = Arrays.asList(
            createSong(1L, "Song1", 1L),
            createSong(2L, "Song2", 2L),
            createSong(3L, "Song3", 3L),
            createSong(4L, "Song4", 4L)
        );

        when(songBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(4L));

        when(songRepository.findAllByTitleIn(anySet()))
            .thenReturn(Flux.fromIterable(savedSongs));

        // when & then
        StepVerifier.create(songBatchProcessor.processSongsBatch(mixedDateSongs, dateAlbumsMap))
            .assertNext(result -> {
                assertThat(result.savedSongs()).hasSize(4);
                assertThat(result.songIndexToAlbumKey()).hasSize(4);
            })
            .verifyComplete();
    }

    private SpotifySongDto createSongDto(String title, String albumTitle, String releaseDate) {
        return createSongDto(title, albumTitle, releaseDate, "Unknown Artist");
    }
    
    private SpotifySongDto createSongDto(String title, String albumTitle, String releaseDate, String artists) {
        SpotifySongDto dto = SpotifySongDto.builder()
            .songTitle(title)
            .albumTitle(albumTitle)
            .releaseDate(releaseDate)
            .artists(artists)
            .popularity(50)
            .explicit("false")
            .danceability(50)
            .energy(50)
            .musicKey("C")
            .loudnessDb(new BigDecimal("-5.0"))
            .speechiness(5)
            .acousticness(30)
            .instrumentalness(0)
            .liveness(10)
            .positiveness(50)
            .tempo(new BigDecimal("120.0"))
            .timeSignature("4/4")
            .build();
        
        // songId를 additionalProperties에 추가
        dto.setAdditionalProperty("uri", "spotify:track:" + title);
        
        return dto;
    }

    private Album createAlbum(Long id, String title, LocalDate releaseDate) {
        return createAlbum(id, title, releaseDate, "Unknown Artist");
    }
    
    private Album createAlbum(Long id, String title, LocalDate releaseDate, String artistName) {
        Album album = Album.of(title, releaseDate, artistName);
        ReflectionTestUtils.setField(album, "id", id);
        return album;
    }

    private Song createSong(Long id, String title, Long albumId) {
        Song song = Song.builder()
            .title(title)
            .albumId(albumId)
            .popularity(50)
            .energy(50)
            .tempo(new BigDecimal("120.0"))
            .loudnessDb(new BigDecimal("-5.0"))
            .musicKey("C")
            .timeSignature("4/4")
            .build();
        ReflectionTestUtils.setField(song, "id", id);
        return song;
    }
}
