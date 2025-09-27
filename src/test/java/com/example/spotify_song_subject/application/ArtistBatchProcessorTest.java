package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Artist;
import com.example.spotify_song_subject.repository.ArtistRepository;
import com.example.spotify_song_subject.repository.bulk.ArtistBulkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ArtistBatchProcessor 단위 테스트")
class ArtistBatchProcessorTest {

    private ArtistRepository artistRepository;
    private ArtistBulkRepository artistBulkRepository;

    private ArtistBatchProcessor artistBatchProcessor;

    private Set<String> artistNames;
    private Map<String, Artist> existingArtists;
    private List<Artist> newArtists;

    @BeforeEach
    void setUp() {
        this.artistRepository = mock(ArtistRepository.class);
        this.artistBulkRepository = mock(ArtistBulkRepository.class);
        this.artistBatchProcessor = new ArtistBatchProcessor(artistRepository, artistBulkRepository);

        artistNames = new HashSet<>(Arrays.asList("Artist1", "Artist2", "Artist3"));
        existingArtists = new HashMap<>();
        existingArtists.put("Artist1", createArtist(1L, "Artist1"));
        
        newArtists = Arrays.asList(
            createArtist(2L, "Artist2"),
            createArtist(3L, "Artist3")
        );
    }

    @Test
    @DisplayName("빈 아티스트 이름 세트를 처리하면 빈 맵을 반환한다")
    void processEmptyArtistNames() {
        // given
        Set<String> emptySet = Collections.emptySet();

        // when & then
        StepVerifier.create(artistBatchProcessor.processArtistsBatch(emptySet))
            .assertNext(result -> {
                assertThat(result).isEmpty();
            })
            .verifyComplete();

        verify(artistRepository, never()).findAllByNameIn(any());
        verify(artistBulkRepository, never()).bulkInsert(any());
    }

    @Test
    @DisplayName("모든 아티스트가 이미 존재하면 기존 아티스트 맵을 반환한다")
    void processExistingArtists() {
        // given
        Map<String, Artist> allExisting = new HashMap<>();
        allExisting.put("Artist1", createArtist(1L, "Artist1"));
        allExisting.put("Artist2", createArtist(2L, "Artist2"));
        allExisting.put("Artist3", createArtist(3L, "Artist3"));

        // processArtistsBatchFast 방식에서는 항상 bulkInsert를 시도함
        when(artistBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(0L)); // 이미 존재하므로 0개 삽입

        when(artistRepository.findAllByNameIn(artistNames))
            .thenReturn(Flux.fromIterable(allExisting.values()));

        // when & then
        StepVerifier.create(artistBatchProcessor.processArtistsBatch(artistNames))
            .assertNext(result -> {
                assertThat(result).hasSize(3);
                assertThat(result).containsAllEntriesOf(allExisting);
            })
            .verifyComplete();

        verify(artistBulkRepository).bulkInsert(any());
    }

    @Test
    @DisplayName("새로운 아티스트만 bulk insert하고 전체 맵을 반환한다")
    void processNewArtists() {
        // given
        // processArtistsBatchFast에서는 모든 아티스트를 INSERT IGNORE로 시도
        when(artistBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(2L)); // 2개가 새로 삽입됨

        // 전체 아티스트 조회 시 모든 아티스트 반환
        Map<String, Artist> allArtists = new HashMap<>();
        allArtists.put("Artist1", createArtist(1L, "Artist1"));
        allArtists.put("Artist2", createArtist(2L, "Artist2"));
        allArtists.put("Artist3", createArtist(3L, "Artist3"));

        when(artistRepository.findAllByNameIn(artistNames))
            .thenReturn(Flux.fromIterable(allArtists.values()));

        // when & then
        StepVerifier.create(artistBatchProcessor.processArtistsBatch(artistNames))
            .assertNext(result -> {
                assertThat(result).hasSize(3);
                assertThat(result).containsKeys("Artist1", "Artist2", "Artist3");
                assertThat(result.get("Artist1").getId()).isEqualTo(1L);
                assertThat(result.get("Artist2").getId()).isEqualTo(2L);
                assertThat(result.get("Artist3").getId()).isEqualTo(3L);
            })
            .verifyComplete();

        // processArtistsBatchFast는 모든 아티스트를 bulk insert 시도
        verify(artistBulkRepository).bulkInsert(argThat(artists -> 
            artists.size() == 3 && 
            artists.stream().map(Artist::getName).collect(Collectors.toSet()).equals(artistNames)
        ));
    }

    @Test
    @DisplayName("bulk insert 실패 시 에러를 전파한다")
    void handleBulkInsertError() {
        // given
        // processArtistsBatchFast에서는 모든 아티스트를 bulk insert 시도
        when(artistBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.error(new RuntimeException("Bulk insert failed")));

        // bulk insert가 실패하면 findAllByNameIn이 호출되지 않지만, 
        // 에러 처리 과정에서 호출될 수 있으므로 설정
        when(artistRepository.findAllByNameIn(anySet()))
            .thenReturn(Flux.empty());

        // when & then
        StepVerifier.create(artistBatchProcessor.processArtistsBatch(artistNames))
            .expectErrorMessage("Bulk insert failed")
            .verify();
    }

    @Test
    @DisplayName("재조회 실패 시 에러를 전파한다")
    void handleReloadError() {
        // given
        // processArtistsBatchFast에서는 bulk insert 후 전체를 다시 조회
        when(artistBulkRepository.bulkInsert(anyList()))
            .thenReturn(Mono.just(2L));

        // 재조회 시 에러 발생
        when(artistRepository.findAllByNameIn(artistNames))
            .thenReturn(Flux.error(new RuntimeException("Reload failed")));

        // when & then
        StepVerifier.create(artistBatchProcessor.processArtistsBatch(artistNames))
            .expectErrorMessage("Reload failed")
            .verify();
    }

    private Artist createArtist(Long id, String name) {
        Artist artist = Artist.of(name);
        ReflectionTestUtils.setField(artist, "id", id);
        return artist;
    }
}
