package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.repository.AlbumRepository;
import com.example.spotify_song_subject.repository.bulk.AlbumBulkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AlbumBatchProcessor 단위 테스트")
class AlbumBatchProcessorTest {

    private AlbumRepository albumRepository;
    private AlbumBulkRepository albumBulkRepository;

    private AlbumBatchProcessor albumBatchProcessor;

    @BeforeEach
    void setUp() {
        this.albumRepository = mock(AlbumRepository.class);
        this.albumBulkRepository = mock(AlbumBulkRepository.class);
        this.albumBatchProcessor = new AlbumBatchProcessor(albumRepository, albumBulkRepository);
    }

    @Test
    @DisplayName("빈 앨범 맵으로 빈 결과를 반환한다")
    void processEmptyAlbumMap() {
        // given
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> emptyMap = Collections.emptyMap();

        // when & then
        StepVerifier.create(albumBatchProcessor.processAlbumsBatch(emptyMap))
            .expectNext(Collections.emptyMap())
            .verifyComplete();

        // verify no repository calls
        verifyNoInteractions(albumRepository, albumBulkRepository);
    }

    @Test
    @DisplayName("새로운 앨범들을 일괄 삽입한다")
    void processNewAlbums() {
        // given
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = new HashMap<>();
        albumsByTitle.put("Album 1", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 1, 1), "Artist1")));
        albumsByTitle.put("Album 2", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 2, 1), "Artist2")));

        Album album1 = createAlbumWithId(1L, "Album 1", LocalDate.of(2023, 1, 1), "Artist1");
        Album album2 = createAlbumWithId(2L, "Album 2", LocalDate.of(2023, 2, 1), "Artist2");

        // Mock: IN 절로 조회 시 빈 결과 반환 (기존 앨범 없음)
        when(albumRepository.findAllByTitleIn(anyCollection()))
            .thenReturn(Flux.empty())  // 첫 번째 조회: 기존 앨범 없음
            .thenReturn(Flux.just(album1, album2));  // 두 번째 조회: 삽입 후

        // Mock bulk insert
        when(albumBulkRepository.bulkInsert(anyCollection()))
            .thenReturn(Mono.just(2L));

        // when
        StepVerifier.create(albumBatchProcessor.processAlbumsBatch(albumsByTitle))
            .assertNext(resultMap -> {
                assertThat(resultMap).hasSize(2);
                assertThat(resultMap).containsKey("Album 1|2023-01-01|Artist1");
                assertThat(resultMap).containsKey("Album 2|2023-02-01|Artist2");
                assertThat(resultMap.get("Album 1|2023-01-01|Artist1").getTitle()).isEqualTo("Album 1");
                assertThat(resultMap.get("Album 2|2023-02-01|Artist2").getTitle()).isEqualTo("Album 2");
            })
            .verifyComplete();

        // verify bulk insert was called
        verify(albumBulkRepository, times(1)).bulkInsert(anyCollection());
    }

    @Test
    @DisplayName("기존 앨범은 건너뛰고 새 앨범만 삽입한다")
    void processWithExistingAlbums() {
        // given
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = new HashMap<>();
        albumsByTitle.put("Existing Album", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 1, 1), "Artist1")));
        albumsByTitle.put("New Album", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 2, 1), "Artist2")));

        Album existingAlbum = createAlbumWithId(1L, "Existing Album", LocalDate.of(2023, 1, 1), "Artist1");
        Album newAlbum = createAlbumWithId(2L, "New Album", LocalDate.of(2023, 2, 1), "Artist2");

        // Mock: IN 절 조회 - 첫 번째는 기존 앨범만, 두 번째는 모든 앨범
        when(albumRepository.findAllByTitleIn(anyCollection()))
            .thenReturn(Flux.just(existingAlbum))  // 첫 번째 조회: 기존 앨범만
            .thenReturn(Flux.just(existingAlbum, newAlbum));  // 두 번째 조회: 모든 앨범

        // Mock bulk insert for new album only
        when(albumBulkRepository.bulkInsert(anyCollection()))
            .thenReturn(Mono.just(1L));

        // when
        StepVerifier.create(albumBatchProcessor.processAlbumsBatch(albumsByTitle))
            .assertNext(resultMap -> {
                assertThat(resultMap).hasSize(2);
                assertThat(resultMap).containsKey("Existing Album|2023-01-01|Artist1");
                assertThat(resultMap).containsKey("New Album|2023-02-01|Artist2");
            })
            .verifyComplete();

        // verify bulk insert was called once for the new album
        verify(albumBulkRepository, times(1)).bulkInsert(argThat(collection -> collection.size() == 1));
    }

    @Test
    @DisplayName("모든 앨범이 이미 존재하면 bulk insert를 호출하지 않는다")
    void processAllExistingAlbums() {
        // given
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = new HashMap<>();
        albumsByTitle.put("Album 1", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 1, 1), "Artist1")));
        albumsByTitle.put("Album 2", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 2, 1), "Artist2")));

        Album album1 = createAlbumWithId(1L, "Album 1", LocalDate.of(2023, 1, 1), "Artist1");
        Album album2 = createAlbumWithId(2L, "Album 2", LocalDate.of(2023, 2, 1), "Artist2");

        // Mock: IN 절 조회 - 모든 앨범이 이미 존재
        when(albumRepository.findAllByTitleIn(anyCollection()))
            .thenReturn(Flux.just(album1, album2));

        // when
        StepVerifier.create(albumBatchProcessor.processAlbumsBatch(albumsByTitle))
            .assertNext(resultMap -> {
                assertThat(resultMap).hasSize(2);
                assertThat(resultMap.get("Album 1|2023-01-01|Artist1")).isEqualTo(album1);
                assertThat(resultMap.get("Album 2|2023-02-01|Artist2")).isEqualTo(album2);
            })
            .verifyComplete();

        // verify bulk insert was never called
        verify(albumBulkRepository, never()).bulkInsert(anyCollection());
    }

    @Test
    @DisplayName("동일한 제목에 다른 발매일을 가진 앨범들을 처리한다")
    void processAlbumsWithSameTitleDifferentDates() {
        // given
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = new HashMap<>();
        Set<AlbumBatchProcessor.AlbumInfo> albumInfos = new HashSet<>();
        albumInfos.add(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 1, 1), "Artist1"));
        albumInfos.add(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 6, 1), "Artist2"));
        albumInfos.add(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 12, 1), "Artist3"));
        albumsByTitle.put("Album", albumInfos);

        Album album1 = createAlbumWithId(1L, "Album", LocalDate.of(2023, 1, 1), "Artist1");
        Album album2 = createAlbumWithId(2L, "Album", LocalDate.of(2023, 6, 1), "Artist2");
        Album album3 = createAlbumWithId(3L, "Album", LocalDate.of(2023, 12, 1), "Artist3");

        // Mock: IN 절 조회 - 모든 앨범이 새로운 것
        when(albumRepository.findAllByTitleIn(anyCollection()))
            .thenReturn(Flux.empty())  // 첫 번째 조회: 비어있음
            .thenReturn(Flux.just(album1, album2, album3));  // 두 번째 조회: 삽입 후

        // Mock bulk insert
        when(albumBulkRepository.bulkInsert(anyCollection()))
            .thenReturn(Mono.just(3L));

        // when
        StepVerifier.create(albumBatchProcessor.processAlbumsBatch(albumsByTitle))
            .assertNext(resultMap -> {
                assertThat(resultMap).hasSize(3);
                assertThat(resultMap).containsKey("Album|2023-01-01|Artist1");
                assertThat(resultMap).containsKey("Album|2023-06-01|Artist2");
                assertThat(resultMap).containsKey("Album|2023-12-01|Artist3");
            })
            .verifyComplete();

        // verify bulk insert was called with 3 albums
        verify(albumBulkRepository, times(1)).bulkInsert(argThat(collection -> collection.size() == 3));
    }

    @Test
    @DisplayName("대량 데이터를 처리한다")
    void processLargeDataSet() {
        // given
        Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albumsByTitle = new HashMap<>();
        List<Album> expectedAlbums = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            String title = "Album " + i;
            LocalDate date = LocalDate.of(2023, 1, 1).plusDays(i);
            String artistName = "Artist " + i;
            albumsByTitle.put(title, Set.of(new AlbumBatchProcessor.AlbumInfo(date, artistName)));
            expectedAlbums.add(createAlbumWithId((long)(i + 1), title, date, artistName));
        }

        // Mock: IN 절 조회 - 모든 앨범이 새로운 것
        when(albumRepository.findAllByTitleIn(anyCollection()))
            .thenReturn(Flux.empty())  // 첫 번째 조회: 비어있음
            .thenReturn(Flux.fromIterable(expectedAlbums));  // 두 번째 조회: 삽입 후

        // Mock bulk insert
        when(albumBulkRepository.bulkInsert(anyCollection()))
            .thenReturn(Mono.just(100L));

        // when
        StepVerifier.create(albumBatchProcessor.processAlbumsBatch(albumsByTitle))
            .assertNext(resultMap -> {
                assertThat(resultMap).hasSize(100);
            })
            .verifyComplete();

        // verify bulk insert was called once
        verify(albumBulkRepository, times(1)).bulkInsert(argThat(collection -> collection.size() == 100));
    }

    private Album createAlbumWithId(Long id, String title, LocalDate releaseDate) {
        return createAlbumWithId(id, title, releaseDate, "Unknown Artist");
    }
    
    private Album createAlbumWithId(Long id, String title, LocalDate releaseDate, String artistName) {
        Album album = Album.of(title, releaseDate, artistName);

        try {
            java.lang.reflect.Field idField = album.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(album, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return album;
    }
}