package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.dto.SongLikeScore;
import com.example.spotify_song_subject.dto.TrendingSongDto;
import com.example.spotify_song_subject.repository.SongLikeRedisRepository;
import com.example.spotify_song_subject.repository.SongLikeCustomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SongLikeQueryService Unit Tests")
class SongLikeQueryServiceTest {

    private SongLikeRedisRepository songLikeRedisRepository;
    private SongLikeCustomRepository songLikeCustomRepository;

    private SongLikeQueryService songLikeQueryService;

    @BeforeEach
    void setUp() {
        this.songLikeRedisRepository = mock(SongLikeRedisRepository.class);
        this.songLikeCustomRepository =  mock(SongLikeCustomRepository.class);

        songLikeQueryService = new SongLikeQueryService(
            songLikeRedisRepository,
            songLikeCustomRepository
        );
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 정상 케이스")
    void getTrendingSongs_Success() {
        // Given
        List<SongLikeScore> mockScores = List.of(
            new SongLikeScore(1L, 100L),
            new SongLikeScore(2L, 90L),
            new SongLikeScore(3L, 80L)
        );

        List<TrendingSongDto> expectedDtos = List.of(
            TrendingSongDto.builder()
                .songId(1L)
                .songTitle("Song 1")
                .artistName("Artist 1")
                .albumName("Album 1")
                .likeIncrease(100L)
                .build(),
            TrendingSongDto.builder()
                .songId(2L)
                .songTitle("Song 2")
                .artistName("Artist 2")
                .albumName("Album 2")
                .likeIncrease(90L)
                .build(),
            TrendingSongDto.builder()
                .songId(3L)
                .songTitle("Song 3")
                .artistName("Artist 3")
                .albumName("Album 3")
                .likeIncrease(80L)
                .build()
        );

        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(mockScores));

        when(songLikeCustomRepository.findTrendingSongsByIds(
            eq(List.of(1L, 2L, 3L)),
            any(Map.class)
        )).thenReturn(Mono.just(expectedDtos));

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> {
                assertThat(result).hasSize(3);
                assertThat(result.get(0).getSongId()).isEqualTo(1L);
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(100L);
                assertThat(result.get(1).getSongId()).isEqualTo(2L);
                assertThat(result.get(1).getLikeIncrease()).isEqualTo(90L);
                assertThat(result.get(2).getSongId()).isEqualTo(3L);
                assertThat(result.get(2).getLikeIncrease()).isEqualTo(80L);
            })
            .verifyComplete();

        verify(songLikeRedisRepository).getTop10TrendingSongs();
        verify(songLikeCustomRepository).findTrendingSongsByIds(any(List.class), any(Map.class));
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - Redis에서 빈 결과 반환")
    void getTrendingSongs_EmptyScoresFromRedis() {
        // Given
        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(List.of()));

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        verify(songLikeRedisRepository).getTop10TrendingSongs();
        verify(songLikeCustomRepository, never()).findTrendingSongsByIds(any(), any());
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - Redis 에러 발생시 빈 리스트 반환")
    void getTrendingSongs_RedisError_ReturnsEmptyList() {
        // Given
        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.error(new RuntimeException("Redis connection error")));

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        verify(songLikeRedisRepository).getTop10TrendingSongs();
        verify(songLikeCustomRepository, never()).findTrendingSongsByIds(any(), any());
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - Repository 에러 발생시 빈 리스트 반환")
    void getTrendingSongs_RepositoryError_ReturnsEmptyList() {
        // Given
        List<SongLikeScore> mockScores = List.of(
            new SongLikeScore(1L, 100L),
            new SongLikeScore(2L, 90L)
        );

        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(mockScores));

        when(songLikeCustomRepository.findTrendingSongsByIds(any(List.class), any(Map.class)))
            .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        verify(songLikeRedisRepository).getTop10TrendingSongs();
        verify(songLikeCustomRepository).findTrendingSongsByIds(any(List.class), any(Map.class));
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 좋아요 증가 맵 생성 검증")
    void getTrendingSongs_VerifyLikeIncreaseMapCreation() {
        // Given
        List<SongLikeScore> mockScores = List.of(
            new SongLikeScore(10L, 500L),
            new SongLikeScore(20L, 300L),
            new SongLikeScore(30L, 100L)
        );

        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(mockScores));

        when(songLikeCustomRepository.findTrendingSongsByIds(any(List.class), any(Map.class)))
            .thenAnswer(invocation -> {
                List<Long> songIds = invocation.getArgument(0);
                Map<Long, Long> likeIncreaseMap = invocation.getArgument(1);

                // 맵이 올바르게 생성되었는지 검증
                assertThat(songIds).containsExactly(10L, 20L, 30L);
                assertThat(likeIncreaseMap).containsEntry(10L, 500L);
                assertThat(likeIncreaseMap).containsEntry(20L, 300L);
                assertThat(likeIncreaseMap).containsEntry(30L, 100L);

                return Mono.just(List.of());
            });

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        verify(songLikeCustomRepository).findTrendingSongsByIds(any(List.class), any(Map.class));
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 10개 이상의 곡 처리")
    void getTrendingSongs_MoreThan10Songs() {
        // Given
        List<SongLikeScore> mockScores = List.of(
            new SongLikeScore(1L, 100L),
            new SongLikeScore(2L, 90L),
            new SongLikeScore(3L, 80L),
            new SongLikeScore(4L, 70L),
            new SongLikeScore(5L, 60L),
            new SongLikeScore(6L, 50L),
            new SongLikeScore(7L, 40L),
            new SongLikeScore(8L, 30L),
            new SongLikeScore(9L, 20L),
            new SongLikeScore(10L, 10L)
        );

        List<TrendingSongDto> expectedDtos = mockScores.stream()
            .map(score -> TrendingSongDto.builder()
                .songId(score.songId())
                .songTitle("Song " + score.songId())
                .artistName("Artist " + score.songId())
                .albumName("Album " + score.songId())
                .likeIncrease(score.score())
                .build())
            .toList();

        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(mockScores));

        when(songLikeCustomRepository.findTrendingSongsByIds(any(List.class), any(Map.class)))
            .thenReturn(Mono.just(expectedDtos));

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> {
                assertThat(result).hasSize(10);
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(100L);
                assertThat(result.get(9).getLikeIncrease()).isEqualTo(10L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 단일 곡 처리")
    void getTrendingSongs_SingleSong() {
        // Given
        List<SongLikeScore> mockScores = List.of(
            new SongLikeScore(42L, 999L)
        );

        List<TrendingSongDto> expectedDtos = List.of(
            TrendingSongDto.builder()
                .songId(42L)
                .songTitle("Single Hit Song")
                .artistName("Popular Artist")
                .albumName("Best Album")
                .likeIncrease(999L)
                .build()
        );

        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(mockScores));

        when(songLikeCustomRepository.findTrendingSongsByIds(
            eq(List.of(42L)),
            any(Map.class)
        )).thenReturn(Mono.just(expectedDtos));

        // When & Then
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> {
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getSongId()).isEqualTo(42L);
                assertThat(result.get(0).getSongTitle()).isEqualTo("Single Hit Song");
                assertThat(result.get(0).getLikeIncrease()).isEqualTo(999L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("트렌딩 곡 조회 - 중복 Song ID 처리 (에러 케이스)")
    void getTrendingSongs_DuplicateSongIds() {
        // Given (실제로는 발생하지 않아야 하지만 방어적 프로그래밍)
        List<SongLikeScore> mockScores = List.of(
            new SongLikeScore(1L, 100L),
            new SongLikeScore(1L, 200L),
            new SongLikeScore(2L, 150L)
        );

        when(songLikeRedisRepository.getTop10TrendingSongs())
            .thenReturn(Mono.just(mockScores));

        // When & Then - 중복 키로 인한 에러가 발생하고 빈 리스트 반환
        StepVerifier.create(songLikeQueryService.getTrendingSongs())
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();

        verify(songLikeRedisRepository).getTop10TrendingSongs();
        verify(songLikeCustomRepository, never()).findTrendingSongsByIds(any(), any());
    }
}