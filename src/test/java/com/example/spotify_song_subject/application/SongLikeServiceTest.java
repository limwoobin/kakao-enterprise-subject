package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.domain.SongLike;
import com.example.spotify_song_subject.dto.SongLikeDto;
import com.example.spotify_song_subject.exception.ResourceNotFoundException;
import com.example.spotify_song_subject.repository.SongLikeRedisRepository;
import com.example.spotify_song_subject.repository.SongLikeRepository;
import com.example.spotify_song_subject.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class SongLikeServiceTest {

    private SongRepository songRepository;
    private SongLikeRepository songLikeRepository;
    private SongLikeRedisRepository songLikeRedisRepository;

    private SongLikeService songLikeService;

    private Long songId;
    private Long userId;
    private Song testSong;
    private SongLike testSongLike;

    @BeforeEach
    void setUp() {
        this.songRepository = mock(SongRepository.class);
        this.songLikeRepository = mock(SongLikeRepository.class);
        this.songLikeRedisRepository = mock(SongLikeRedisRepository.class);
        this.songLikeService = new SongLikeService(songRepository, songLikeRepository, songLikeRedisRepository);

        songId = 1L;
        userId = 100L;

        testSong = Song.builder()
            .title("Test Song")
            .genre("Pop")
            .build();
        setId(testSong, songId);

        testSongLike = SongLike.create(songId, userId);
    }

    private void setId(Song song, Long id) {
        try {
            Field idField = Song.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(song, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id", e);
        }
    }

    @Nested
    @DisplayName("좋아요 토글 테스트")
    class LikeToggleTest {

        @Test
        @DisplayName("새로운 좋아요 추가 - 정상 케이스")
        void addLike_Success() {
            // given
            Song updatedSong = Song.builder()
                .title("Test Song")
                .genre("Pop")
                .likeCount(1L)
                .build();
            setId(updatedSong, songId);

            when(songRepository.findById(songId))
                .thenReturn(Mono.just(testSong))
                .thenReturn(Mono.just(updatedSong));

            when(songLikeRepository.findBySongIdAndUserId(songId, userId))
                .thenReturn(Mono.empty());

            when(songLikeRepository.save(any(SongLike.class)))
                .thenReturn(Mono.just(testSongLike));

            when(songRepository.incrementLikeCount(songId))
                .thenReturn(Mono.just(Integer.valueOf(1)));

            when(songLikeRedisRepository.recordLikeToBucket(songId))
                .thenReturn(Mono.just(true));

            when(songLikeRedisRepository.updateLikeCountCache(songId, 1L))
                .thenReturn(Mono.just(1L));

            // when
            Mono<SongLikeDto> result = songLikeService.like(songId, userId);

            // then
            StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.getSongId()).isEqualTo(songId);
                    assertThat(dto.getSongTitle()).isEqualTo("Test Song");
                    assertThat(dto.getTotalLikes()).isEqualTo(1L);
                    assertThat(dto.isLiked()).isTrue();
                    assertThat(dto.getActionAt()).isNotNull();
                })
                .verifyComplete();

            verify(songRepository, times(2)).findById(songId);
            verify(songLikeRepository).findBySongIdAndUserId(songId, userId);
            verify(songLikeRepository).save(any(SongLike.class));
            verify(songRepository).incrementLikeCount(songId);
            verify(songLikeRedisRepository).recordLikeToBucket(songId);
            verify(songLikeRedisRepository).updateLikeCountCache(songId, 1L);
        }

        @Test
        @DisplayName("기존 좋아요 삭제 - 정상 케이스")
        void removeLike_Success() {
            // given
            Song updatedSong = Song.builder()
                .title("Test Song")
                .genre("Pop")
                .likeCount(0L)
                .build();
            setId(updatedSong, songId);

            when(songRepository.findById(songId))
                .thenReturn(Mono.just(testSong))
                .thenReturn(Mono.just(updatedSong));

            when(songLikeRepository.findBySongIdAndUserId(songId, userId))
                .thenReturn(Mono.just(testSongLike));

            when(songLikeRepository.deleteBySongIdAndUserId(songId, userId))
                .thenReturn(Mono.empty());

            when(songRepository.decrementLikeCount(songId))
                .thenReturn(Mono.just(Integer.valueOf(1)));

            when(songLikeRedisRepository.decrementLikeInBucket(songId))
                .thenReturn(Mono.just(true));

            when(songLikeRedisRepository.updateLikeCountCache(songId, 0L))
                .thenReturn(Mono.just(0L));

            // when
            Mono<SongLikeDto> result = songLikeService.like(songId, userId);

            // then
            StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.getSongId()).isEqualTo(songId);
                    assertThat(dto.getSongTitle()).isEqualTo("Test Song");
                    assertThat(dto.getTotalLikes()).isEqualTo(0L);
                    assertThat(dto.isLiked()).isFalse();
                    assertThat(dto.getActionAt()).isNotNull();
                })
                .verifyComplete();

            verify(songRepository, times(2)).findById(songId);
            verify(songLikeRepository).findBySongIdAndUserId(songId, userId);
            verify(songLikeRepository).deleteBySongIdAndUserId(songId, userId);
            verify(songRepository).decrementLikeCount(songId);
            verify(songLikeRedisRepository).decrementLikeInBucket(songId);
            verify(songLikeRedisRepository).updateLikeCountCache(songId, 0L);
        }
    }

    @Nested
    @DisplayName("예외 케이스 테스트")
    class ExceptionTest {

        @Test
        @DisplayName("존재하지 않는 노래 ID로 좋아요 시도")
        void like_SongNotFound() {
            // given
            when(songRepository.findById(songId))
                .thenReturn(Mono.empty());

            // when
            Mono<SongLikeDto> result = songLikeService.like(songId, userId);

            // then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(error.getMessage()).contains("Song");
                    assertThat(error.getMessage()).contains(songId.toString());
                })
                .verify();

            verify(songRepository).findById(songId);
            verifyNoInteractions(songLikeRepository);
            verifyNoInteractions(songLikeRedisRepository);
        }

        @Test
        @DisplayName("좋아요 저장 실패")
        void addLike_SaveFailure() {
            // given
            when(songRepository.findById(songId))
                .thenReturn(Mono.just(testSong));

            when(songLikeRepository.findBySongIdAndUserId(songId, userId))
                .thenReturn(Mono.empty());

            when(songLikeRepository.save(any(SongLike.class)))
                .thenReturn(Mono.error(new RuntimeException("DB save failed")));

            // when
            Mono<SongLikeDto> result = songLikeService.like(songId, userId);

            // then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RuntimeException.class);
                    assertThat(error.getMessage()).isEqualTo("DB save failed");
                })
                .verify();

            verify(songRepository).findById(songId);
            verify(songLikeRepository).findBySongIdAndUserId(songId, userId);
            verify(songLikeRepository).save(any(SongLike.class));
            verifyNoMoreInteractions(songRepository);
            verifyNoInteractions(songLikeRedisRepository);
        }

        @Test
        @DisplayName("좋아요 카운트 증가 실패")
        void addLike_IncrementCountFailure() {
            // given
            when(songRepository.findById(songId))
                .thenReturn(Mono.just(testSong));

            when(songLikeRepository.findBySongIdAndUserId(songId, userId))
                .thenReturn(Mono.empty());

            when(songLikeRepository.save(any(SongLike.class)))
                .thenReturn(Mono.just(testSongLike));

            when(songRepository.incrementLikeCount(songId))
                .thenReturn(Mono.error(new RuntimeException("Increment failed")));

            // when
            Mono<SongLikeDto> result = songLikeService.like(songId, userId);

            // then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RuntimeException.class);
                    assertThat(error.getMessage()).isEqualTo("Increment failed");
                })
                .verify();

            verify(songRepository).findById(songId);
            verify(songLikeRepository).findBySongIdAndUserId(songId, userId);
            verify(songLikeRepository).save(any(SongLike.class));
            verify(songRepository).incrementLikeCount(songId);
            verifyNoInteractions(songLikeRedisRepository);
        }

        @Test
        @DisplayName("Redis 버킷 기록 실패")
        void addLike_RedisRecordFailure() {
            // given
            when(songRepository.findById(songId))
                .thenReturn(Mono.just(testSong));

            when(songLikeRepository.findBySongIdAndUserId(songId, userId))
                .thenReturn(Mono.empty());

            when(songLikeRepository.save(any(SongLike.class)))
                .thenReturn(Mono.just(testSongLike));

            when(songRepository.incrementLikeCount(songId))
                .thenReturn(Mono.just(Integer.valueOf(1)));

            when(songLikeRedisRepository.recordLikeToBucket(songId))
                .thenReturn(Mono.error(new RuntimeException("Redis record failed")));

            // when
            Mono<SongLikeDto> result = songLikeService.like(songId, userId);

            // then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RuntimeException.class);
                    assertThat(error.getMessage()).isEqualTo("Redis record failed");
                })
                .verify();

            verify(songRepository).findById(songId);
            verify(songLikeRepository).findBySongIdAndUserId(songId, userId);
            verify(songLikeRepository).save(any(SongLike.class));
            verify(songRepository).incrementLikeCount(songId);
            verify(songLikeRedisRepository).recordLikeToBucket(songId);
            verify(songLikeRedisRepository, never()).updateLikeCountCache(anyLong(), anyLong());
        }
    }
}
