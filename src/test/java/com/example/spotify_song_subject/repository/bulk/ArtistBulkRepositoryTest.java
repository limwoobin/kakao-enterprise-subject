package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.Artist;
import com.example.spotify_song_subject.repository.ArtistRepository;
import com.example.spotify_song_subject.repository.RepositoryTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({RepositoryTestConfiguration.class, ArtistBulkRepository.class})
class ArtistBulkRepositoryTest {

    @Autowired
    private ArtistBulkRepository artistBulkRepository;
    
    @Autowired
    private ArtistRepository artistRepository;
    
    @Autowired
    private DatabaseClient databaseClient;
    
    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 정리
        databaseClient.sql("DELETE FROM artists")
            .fetch()
            .rowsUpdated()
            .block();
    }
    
    @Test
    @DisplayName("INSERT IGNORE로 중복된 아티스트는 무시하고 새로운 아티스트만 삽입")
    void testBulkInsertWithDuplicates() {
        // Given
        Artist existingArtist = Artist.of("Artist1");
        artistRepository.save(existingArtist).block();
        
        List<Artist> artistsToInsert = Arrays.asList(
            Artist.of("Artist1"), // 이미 존재 - 무시됨
            Artist.of("Artist2"), // 새로운 아티스트
            Artist.of("Artist3"), // 새로운 아티스트
            Artist.of("Artist2")  // 같은 배치 내 중복 - 무시됨
        );
        
        // When
        Mono<Long> result = artistBulkRepository.bulkInsert(artistsToInsert);
        
        // Then
        StepVerifier.create(result)
            .assertNext(count -> {
                // H2의 INSERT IGNORE는 실제로 삽입된 행 수를 반환
                assertThat(count).isEqualTo(2L); // Artist2, Artist3만 삽입
            })
            .verifyComplete();
        
        // 전체 아티스트 수 확인
        StepVerifier.create(artistRepository.count())
            .assertNext(total -> assertThat(total).isEqualTo(3L)) // Artist1, Artist2, Artist3
            .verifyComplete();
    }
    
    @Test
    @DisplayName("모든 아티스트가 새로운 경우 전부 삽입")
    void testBulkInsertAllNew() {
        // Given
        List<Artist> artistsToInsert = Arrays.asList(
            Artist.of("NewArtist1"),
            Artist.of("NewArtist2"),
            Artist.of("NewArtist3")
        );
        
        // When
        Mono<Long> result = artistBulkRepository.bulkInsert(artistsToInsert);
        
        // Then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(3L))
            .verifyComplete();
    }
    
    @Test
    @DisplayName("빈 리스트 처리")
    void testBulkInsertEmptyList() {
        // When
        Mono<Long> result = artistBulkRepository.bulkInsert(List.of());
        
        // Then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(0L))
            .verifyComplete();
    }
}