package com.example.spotify_song_subject.application;

import com.example.spotify_song_subject.dto.AlbumStatisticsDto;
import com.example.spotify_song_subject.repository.AlbumStatisticsCustomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("AlbumStatisticsQueryService 단위 테스트")
class AlbumStatisticsQueryServiceTest {

    private AlbumStatisticsCustomRepository albumStatisticsCustomRepository;
    private AlbumStatisticsQueryService albumStatisticsQueryService;

    @BeforeEach
    void setUp() {
        albumStatisticsCustomRepository = mock(AlbumStatisticsCustomRepository.class);
        albumStatisticsQueryService = new AlbumStatisticsQueryService(albumStatisticsCustomRepository);
    }

    @Test
    @DisplayName("특정 연도의 아티스트별 앨범 통계를 조회한다")
    void getAlbumStatisticsByYear_Success() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 10);

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .artistId(1L)
                .artistName("BTS")
                .releaseYear(2023)
                .albumCount(3L)
                .build(),
            AlbumStatisticsDto.builder()
                .artistId(2L)
                .artistName("IU")
                .releaseYear(2023)
                .albumCount(2L)
                .build()
        );

        Page<AlbumStatisticsDto> expectedPage = new PageImpl<>(dtoList, pageable, dtoList.size());

        given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable))
            .willReturn(Mono.just(expectedPage));

        // when
        Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(2);
                assertThat(page.getContent().get(0).getArtistName()).isEqualTo("BTS");
                assertThat(page.getContent().get(0).getAlbumCount()).isEqualTo(3L);
                assertThat(page.getContent().get(1).getArtistName()).isEqualTo("IU");
                assertThat(page.getContent().get(1).getAlbumCount()).isEqualTo(2L);
                assertThat(page.getTotalElements()).isEqualTo(2);
                assertThat(page.getNumber()).isEqualTo(0);
                assertThat(page.getSize()).isEqualTo(10);
            })
            .verifyComplete();

        verify(albumStatisticsCustomRepository, times(1))
            .findAlbumStatisticsByYear(year, pageable);
    }

    @Test
    @DisplayName("페이징 정보와 함께 앨범 통계를 조회한다")
    void getAlbumStatisticsByYear_WithPagination() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "albumCount"));

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .artistId(6L)
                .artistName("Seventeen")
                .releaseYear(2023)
                .albumCount(1L)
                .build()
        );

        Page<AlbumStatisticsDto> expectedPage = new PageImpl<>(dtoList, pageable, 10);

        given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable))
            .willReturn(Mono.just(expectedPage));

        // when
        Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(1);
                assertThat(page.getTotalElements()).isEqualTo(10);
                assertThat(page.getTotalPages()).isEqualTo(2);
                assertThat(page.getNumber()).isEqualTo(1);
                assertThat(page.getSize()).isEqualTo(5);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("데이터가 없을 때 빈 페이지를 반환한다")
    void getAlbumStatisticsByYear_EmptyResult() {
        // given
        Integer year = 2025;
        Pageable pageable = PageRequest.of(0, 10);

        Page<AlbumStatisticsDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable))
            .willReturn(Mono.just(emptyPage));

        // when
        Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).isEmpty();
                assertThat(page.getTotalElements()).isEqualTo(0);
                assertThat(page.getTotalPages()).isEqualTo(0);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("상세 정보가 포함된 앨범 통계를 조회한다")
    void getAlbumStatisticsByYear_WithFullDetails() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate releaseDate = LocalDate.of(2023, 6, 15);

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .albumId(100L)
                .albumName("Test Album")
                .artistId(1L)
                .artistName("Test Artist")
                .releaseDate(releaseDate)
                .releaseYear(2023)
                .albumCount(1L)
                .songCount(12L)
                .build()
        );

        Page<AlbumStatisticsDto> expectedPage = new PageImpl<>(dtoList, pageable, 1);

        given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable))
            .willReturn(Mono.just(expectedPage));

        // when
        Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                AlbumStatisticsDto dto = page.getContent().get(0);
                assertThat(dto.getAlbumId()).isEqualTo(100L);
                assertThat(dto.getAlbumName()).isEqualTo("Test Album");
                assertThat(dto.getArtistId()).isEqualTo(1L);
                assertThat(dto.getArtistName()).isEqualTo("Test Artist");
                assertThat(dto.getReleaseDate()).isEqualTo(releaseDate);
                assertThat(dto.getReleaseYear()).isEqualTo(2023);
                assertThat(dto.getAlbumCount()).isEqualTo(1L);
                assertThat(dto.getSongCount()).isEqualTo(12L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("repository에서 에러가 발생하면 에러를 전파한다")
    void getAlbumStatisticsByYear_RepositoryError() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 10);

        RuntimeException expectedException = new RuntimeException("Database error");

        given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable))
            .willReturn(Mono.error(expectedException));

        // when
        Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    @DisplayName("repository가 empty Mono를 반환하면 그대로 전달한다")
    void getAlbumStatisticsByYear_EmptyMono() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 10);

        given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable))
            .willReturn(Mono.empty());

        // when
        Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    @DisplayName("다양한 연도에 대해 조회할 수 있다")
    void getAlbumStatisticsByYear_DifferentYears() {
        // given
        Integer[] years = {2020, 2021, 2022, 2023, 2024};
        Pageable pageable = PageRequest.of(0, 10);

        for (Integer year : years) {
            List<AlbumStatisticsDto> dtoList = Arrays.asList(
                AlbumStatisticsDto.builder()
                    .artistId(1L)
                    .artistName("Artist " + year)
                    .releaseYear(year)
                    .albumCount(1L)
                    .build()
            );

            Page<AlbumStatisticsDto> expectedPage = new PageImpl<>(dtoList, pageable, 1);

            given(albumStatisticsCustomRepository.findAlbumStatisticsByYear(eq(year), eq(pageable)))
                .willReturn(Mono.just(expectedPage));

            // when
            Mono<Page<AlbumStatisticsDto>> result = albumStatisticsQueryService.getAlbumStatisticsByYear(year, pageable);

            // then
            StepVerifier.create(result)
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().get(0).getArtistName()).isEqualTo("Artist " + year);
                    assertThat(page.getContent().get(0).getReleaseYear()).isEqualTo(year);
                })
                .verifyComplete();
        }

        verify(albumStatisticsCustomRepository, times(years.length))
            .findAlbumStatisticsByYear(any(Integer.class), eq(pageable));
    }
}