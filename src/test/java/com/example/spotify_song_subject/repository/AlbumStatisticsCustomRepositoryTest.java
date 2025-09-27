package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.config.DatabaseClientConfig;
import com.example.spotify_song_subject.domain.Album;
import com.example.spotify_song_subject.domain.Artist;
import com.example.spotify_song_subject.domain.ArtistAlbum;
import com.example.spotify_song_subject.dto.AlbumStatisticsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlbumStatisticsCustomRepository 통합 테스트")
@RepositoryTestConfiguration
@Import({AlbumStatisticsCustomRepository.class, DatabaseClientConfig.class})
class AlbumStatisticsCustomRepositoryTest {

    @Autowired
    private AlbumStatisticsCustomRepository albumStatisticsCustomRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistAlbumRepository artistAlbumRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private List<Artist> testArtists = new ArrayList<>();
    private List<Album> testAlbums = new ArrayList<>();

    @BeforeEach
    void setUp() {
        initializeTestData().block();
    }

    @AfterEach
    void tearDown() {
        // 데이터 정리 (역순으로)
        artistAlbumRepository.deleteAll()
            .then(albumRepository.deleteAll())
            .then(artistRepository.deleteAll())
            .block();
    }

    private Mono<Void> initializeTestData() {
        // 2023년 데이터 - Artist 1: 3개 앨범
        Artist artist1 = Artist.builder()
            .name("BTS")
            .build();

        Artist artist2 = Artist.builder()
            .name("IU")
            .build();

        Artist artist3 = Artist.builder()
            .name("Seventeen")
            .build();

        // 2023년 앨범들
        Album album1_2023_1 = Album.builder()
            .title("BTS Album 1")
            .releaseDate(LocalDate.of(2023, 1, 15))
            .build();

        Album album1_2023_2 = Album.builder()
            .title("BTS Album 2")
            .releaseDate(LocalDate.of(2023, 6, 20))
            .build();

        Album album1_2023_3 = Album.builder()
            .title("BTS Album 3")
            .releaseDate(LocalDate.of(2023, 11, 30))
            .build();

        Album album2_2023_1 = Album.builder()
            .title("IU Album 1")
            .releaseDate(LocalDate.of(2023, 3, 10))
            .build();

        Album album2_2023_2 = Album.builder()
            .title("IU Album 2")
            .releaseDate(LocalDate.of(2023, 9, 5))
            .build();

        Album album3_2023_1 = Album.builder()
            .title("Seventeen Album 1")
            .releaseDate(LocalDate.of(2023, 5, 1))
            .build();

        // 2022년 앨범
        Album album1_2022_1 = Album.builder()
            .title("BTS Album 2022")
            .releaseDate(LocalDate.of(2022, 7, 15))
            .build();

        // 2024년 앨범
        Album album2_2024_1 = Album.builder()
            .title("IU Album 2024")
            .releaseDate(LocalDate.of(2024, 2, 28))
            .build();

        return Flux.concat(
            // Artists 저장
            artistRepository.save(artist1),
            artistRepository.save(artist2),
            artistRepository.save(artist3)
        ).collectList()
        .flatMapMany(artists -> {
            testArtists.addAll(artists);
            // Albums 저장
            return Flux.concat(
                albumRepository.save(album1_2023_1),
                albumRepository.save(album1_2023_2),
                albumRepository.save(album1_2023_3),
                albumRepository.save(album2_2023_1),
                albumRepository.save(album2_2023_2),
                albumRepository.save(album3_2023_1),
                albumRepository.save(album1_2022_1),
                albumRepository.save(album2_2024_1)
            );
        })
        .collectList()
        .flatMapMany(albums -> {
            testAlbums.addAll(albums);

            // ArtistAlbums 연결
            List<Mono<ArtistAlbum>> artistAlbumMonos = new ArrayList<>();

            // BTS - 2023년 앨범 3개
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(0).getId())
                .albumId(testAlbums.get(0).getId())
                .build()));
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(0).getId())
                .albumId(testAlbums.get(1).getId())
                .build()));
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(0).getId())
                .albumId(testAlbums.get(2).getId())
                .build()));

            // IU - 2023년 앨범 2개
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(1).getId())
                .albumId(testAlbums.get(3).getId())
                .build()));
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(1).getId())
                .albumId(testAlbums.get(4).getId())
                .build()));

            // Seventeen - 2023년 앨범 1개
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(2).getId())
                .albumId(testAlbums.get(5).getId())
                .build()));

            // BTS - 2022년 앨범 1개
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(0).getId())
                .albumId(testAlbums.get(6).getId())
                .build()));

            // IU - 2024년 앨범 1개
            artistAlbumMonos.add(artistAlbumRepository.save(ArtistAlbum.builder()
                .artistId(testArtists.get(1).getId())
                .albumId(testAlbums.get(7).getId())
                .build()));

            return Flux.concat(artistAlbumMonos);
        })
        .collectList()
        .then();
    }

    @Test
    @DisplayName("특정 연도의 아티스트별 앨범 통계를 조회한다")
    void findAlbumStatisticsByYear_Success() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Mono<Page<AlbumStatisticsDto>> result =
            albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(3);
                assertThat(page.getTotalElements()).isEqualTo(3);

                // 앨범 수 기준 내림차순 정렬 확인
                List<AlbumStatisticsDto> content = page.getContent();
                assertThat(content.get(0).getArtistName()).isEqualTo("BTS");
                assertThat(content.get(0).getAlbumCount()).isEqualTo(3L);
                assertThat(content.get(1).getArtistName()).isEqualTo("IU");
                assertThat(content.get(1).getAlbumCount()).isEqualTo(2L);
                assertThat(content.get(2).getArtistName()).isEqualTo("Seventeen");
                assertThat(content.get(2).getAlbumCount()).isEqualTo(1L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("페이징 처리가 올바르게 동작한다")
    void findAlbumStatisticsByYear_WithPagination() {
        // given
        Integer year = 2023;
        Pageable pageableFirst = PageRequest.of(0, 2);
        Pageable pageableSecond = PageRequest.of(1, 2);

        // when & then - 첫 페이지
        StepVerifier.create(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageableFirst))
            .assertNext(page -> {
                assertThat(page.getContent()).hasSize(2);
                assertThat(page.getTotalElements()).isEqualTo(3);
                assertThat(page.getTotalPages()).isEqualTo(2);
                assertThat(page.getNumber()).isEqualTo(0);
                assertThat(page.getContent().get(0).getArtistName()).isEqualTo("BTS");
                assertThat(page.getContent().get(1).getArtistName()).isEqualTo("IU");
            })
            .verifyComplete();

        // when & then - 두 번째 페이지
        StepVerifier.create(albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageableSecond))
            .assertNext(page -> {
                assertThat(page.getContent()).hasSize(1);
                assertThat(page.getTotalElements()).isEqualTo(3);
                assertThat(page.getNumber()).isEqualTo(1);
                assertThat(page.getContent().get(0).getArtistName()).isEqualTo("Seventeen");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("데이터가 없는 연도를 조회하면 빈 페이지를 반환한다")
    void findAlbumStatisticsByYear_EmptyResult() {
        // given
        Integer year = 2025;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Mono<Page<AlbumStatisticsDto>> result =
            albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).isEmpty();
                assertThat(page.getTotalElements()).isEqualTo(0);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("2022년 데이터를 조회한다")
    void findAlbumStatisticsByYear_Year2022() {
        // given
        Integer year = 2022;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Mono<Page<AlbumStatisticsDto>> result =
            albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(1);
                assertThat(page.getContent().get(0).getArtistName()).isEqualTo("BTS");
                assertThat(page.getContent().get(0).getAlbumCount()).isEqualTo(1L);
                assertThat(page.getContent().get(0).getReleaseYear()).isEqualTo(2022);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("2024년 데이터를 조회한다")
    void findAlbumStatisticsByYear_Year2024() {
        // given
        Integer year = 2024;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Mono<Page<AlbumStatisticsDto>> result =
            albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(1);
                assertThat(page.getContent().get(0).getArtistName()).isEqualTo("IU");
                assertThat(page.getContent().get(0).getAlbumCount()).isEqualTo(1L);
                assertThat(page.getContent().get(0).getReleaseYear()).isEqualTo(2024);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("삭제된 데이터는 통계에서 제외된다")
    void findAlbumStatisticsByYear_ExcludesDeletedData() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 10);

        // 한 앨범을 삭제 처리 (deleted_at 필드는 setter가 없으므로 DB에서 직접 삭제)
        Album albumToDelete = testAlbums.stream()
            .filter(a -> a.getTitle().equals("BTS Album 1"))
            .findFirst()
            .orElseThrow();

        // 직접 DELETE SQL 실행 (실제 삭제)
        databaseClient.sql("UPDATE albums SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
            .bind("id", albumToDelete.getId())
            .then()
            .block();

        // when
        Mono<Page<AlbumStatisticsDto>> result =
            albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(3);

                // BTS의 앨범 수가 3개에서 2개로 감소
                AlbumStatisticsDto btsStats = page.getContent().stream()
                    .filter(dto -> dto.getArtistName().equals("BTS"))
                    .findFirst()
                    .orElse(null);

                assertThat(btsStats).isNotNull();
                assertThat(btsStats.getAlbumCount()).isEqualTo(2L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("큰 페이지 사이즈로 조회해도 정상 동작한다")
    void findAlbumStatisticsByYear_LargePageSize() {
        // given
        Integer year = 2023;
        Pageable pageable = PageRequest.of(0, 1000);

        // when
        Mono<Page<AlbumStatisticsDto>> result =
            albumStatisticsCustomRepository.findAlbumStatisticsByYear(year, pageable);

        // then
        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page).isNotNull();
                assertThat(page.getContent()).hasSize(3);
                assertThat(page.getTotalElements()).isEqualTo(3);
                assertThat(page.getSize()).isEqualTo(1000);
            })
            .verifyComplete();
    }
}