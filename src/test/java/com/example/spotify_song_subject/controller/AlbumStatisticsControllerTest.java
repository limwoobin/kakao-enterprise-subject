package com.example.spotify_song_subject.controller;

import com.example.spotify_song_subject.application.AlbumStatisticsQueryService;
import com.example.spotify_song_subject.dto.AlbumStatisticsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("AlbumStatisticsController 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AlbumStatisticsControllerTest {

    private AlbumStatisticsQueryService albumStatisticsQueryService;
    private AlbumStatisticsController albumStatisticsController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.albumStatisticsQueryService = mock(AlbumStatisticsQueryService.class);
        this.albumStatisticsController = new AlbumStatisticsController(albumStatisticsQueryService);

        webTestClient = WebTestClient
            .bindToController(albumStatisticsController)
            .controllerAdvice(new GlobalExceptionHandler())
            .argumentResolvers(configurers -> {
                configurers.addCustomResolver(new ReactivePageableHandlerMethodArgumentResolver());
            })
            .build();
    }

    @Test
    @DisplayName("특정 연도의 아티스트별 앨범 통계를 조회한다")
    void getAlbumStatistics_Success() {
        // given
        Integer year = 2023;

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .artistId(1L)
                .artistName("Artist 1")
                .releaseYear(2023)
                .albumCount(5L)
                .build(),
            AlbumStatisticsDto.builder()
                .artistId(2L)
                .artistName("Artist 2")
                .releaseYear(2023)
                .albumCount(3L)
                .build()
        );

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<AlbumStatisticsDto> dtoPage = new PageImpl<>(dtoList, pageRequest, dtoList.size());

        given(albumStatisticsQueryService.getAlbumStatisticsByYear(eq(year), any(Pageable.class)))
            .willReturn(Mono.just(dtoPage));

        // when & then
        webTestClient.get()
            .uri("/api/v1/album-statistics?year={year}", year)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content").isArray()
            .jsonPath("$.content.length()").isEqualTo(2)
            .jsonPath("$.content[0].artistId").isEqualTo(1)
            .jsonPath("$.content[0].artistName").isEqualTo("Artist 1")
            .jsonPath("$.content[0].releaseYear").isEqualTo(2023)
            .jsonPath("$.content[0].albumCount").isEqualTo(5)
            .jsonPath("$.content[1].artistId").isEqualTo(2)
            .jsonPath("$.content[1].artistName").isEqualTo("Artist 2")
            .jsonPath("$.content[1].releaseYear").isEqualTo(2023)
            .jsonPath("$.content[1].albumCount").isEqualTo(3)
            .jsonPath("$.totalElements").isEqualTo(2)
            .jsonPath("$.totalPages").isEqualTo(1)
            .jsonPath("$.number").isEqualTo(0)
            .jsonPath("$.size").isEqualTo(10);
    }

    @Test
    @DisplayName("페이징 파라미터를 지정하여 앨범 통계를 조회한다")
    void getAlbumStatistics_WithPagination() {
        // given
        Integer year = 2023;
        int page = 1;
        int size = 5;

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .artistId(6L)
                .artistName("Artist 6")
                .releaseYear(2023)
                .albumCount(2L)
                .build()
        );

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AlbumStatisticsDto> dtoPage = new PageImpl<>(dtoList, pageRequest, 10);

        given(albumStatisticsQueryService.getAlbumStatisticsByYear(eq(year), any(Pageable.class)))
            .willReturn(Mono.just(dtoPage));

        // when & then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/album-statistics")
                .queryParam("year", year)
                .queryParam("page", page)
                .queryParam("size", size)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(1)
            .jsonPath("$.totalElements").isEqualTo(10)
            .jsonPath("$.totalPages").isEqualTo(2)
            .jsonPath("$.number").isEqualTo(1)
            .jsonPath("$.size").isEqualTo(5);
    }

    @Test
    @DisplayName("year 파라미터 없이 요청하면 400 Bad Request를 반환한다")
    void getAlbumStatistics_MissingYear() {
        // when & then
        webTestClient.get()
            .uri("/api/v1/album-statistics")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("데이터가 없을 때 204 No Content를 반환한다")
    void getAlbumStatistics_NoContent() {
        // given
        Integer year = 2023;

        given(albumStatisticsQueryService.getAlbumStatisticsByYear(eq(year), any(Pageable.class)))
            .willReturn(Mono.empty());

        // when & then
        webTestClient.get()
            .uri("/api/v1/album-statistics?year={year}", year)
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("빈 페이지일 때도 200 OK를 반환한다")
    void getAlbumStatistics_EmptyPage() {
        // given
        Integer year = 2023;

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<AlbumStatisticsDto> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(albumStatisticsQueryService.getAlbumStatisticsByYear(eq(year), any(Pageable.class)))
            .willReturn(Mono.just(emptyPage));

        // when & then
        webTestClient.get()
            .uri("/api/v1/album-statistics?year={year}", year)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content").isArray()
            .jsonPath("$.content.length()").isEqualTo(0)
            .jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    @DisplayName("정렬 파라미터를 지정하여 앨범 통계를 조회한다")
    void getAlbumStatistics_WithSort() {
        // given
        Integer year = 2023;

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .artistId(1L)
                .artistName("Artist A")
                .releaseYear(2023)
                .albumCount(3L)
                .build(),
            AlbumStatisticsDto.builder()
                .artistId(2L)
                .artistName("Artist B")
                .releaseYear(2023)
                .albumCount(5L)
                .build()
        );

        Sort sort = Sort.by(Sort.Direction.DESC, "albumCount");
        PageRequest pageRequest = PageRequest.of(0, 10, sort);
        Page<AlbumStatisticsDto> dtoPage = new PageImpl<>(dtoList, pageRequest, dtoList.size());

        given(albumStatisticsQueryService.getAlbumStatisticsByYear(eq(year), any(Pageable.class)))
            .willReturn(Mono.just(dtoPage));

        // when & then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/album-statistics")
                .queryParam("year", year)
                .queryParam("sort", "albumCount,desc")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content[0].albumCount").isEqualTo(3)
            .jsonPath("$.content[1].albumCount").isEqualTo(5);
    }

    @Test
    @DisplayName("날짜 정보가 포함된 앨범 통계를 조회한다")
    void getAlbumStatistics_WithFullDetails() {
        // given
        Integer year = 2023;
        LocalDate releaseDate = LocalDate.of(2023, 3, 15);

        List<AlbumStatisticsDto> dtoList = Arrays.asList(
            AlbumStatisticsDto.builder()
                .artistId(1L)
                .artistName("Test Artist")
                .releaseDate(releaseDate)
                .releaseYear(2023)
                .albumCount(1L)
                .build()
        );

        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<AlbumStatisticsDto> dtoPage = new PageImpl<>(dtoList, pageRequest, 1);

        given(albumStatisticsQueryService.getAlbumStatisticsByYear(eq(year), any(Pageable.class)))
            .willReturn(Mono.just(dtoPage));

        // when & then
        webTestClient.get()
            .uri("/api/v1/album-statistics?year={year}", year)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content[0].artistId").isEqualTo(1)
            .jsonPath("$.content[0].artistName").isEqualTo("Test Artist")
            .jsonPath("$.content[0].releaseDate[0]").isEqualTo(2023)
            .jsonPath("$.content[0].releaseDate[1]").isEqualTo(3)
            .jsonPath("$.content[0].releaseDate[2]").isEqualTo(15)
            .jsonPath("$.content[0].releaseYear").isEqualTo(2023)
            .jsonPath("$.content[0].albumCount").isEqualTo(1);
    }
}
