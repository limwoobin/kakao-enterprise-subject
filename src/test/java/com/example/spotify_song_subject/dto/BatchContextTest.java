package com.example.spotify_song_subject.dto;

import com.example.spotify_song_subject.application.AlbumBatchProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BatchContext 테스트")
class BatchContextTest {

    @Nested
    @DisplayName("from 메서드 테스트")
    class FromMethodTest {

        @Test
        @DisplayName("정상적인 DTO 리스트로 BatchContext 생성")
        void shouldCreateBatchContextFromValidDtos() {
            // given
            List<SpotifySongDto> dtos = Arrays.asList(
                createSongDto("Artist1, Artist2", "Album1", "2023-01-15"),
                createSongDto("Artist2, Artist3", "Album2", "2023-02-20"),
                createSongDto("Artist1", "Album1", "2023-01-15")
            );

            // when
            BatchContext context = BatchContext.from(dtos);

            // then
            assertThat(context.artistNames())
                .containsExactlyInAnyOrder("Artist1", "Artist2", "Artist3");
            assertThat(context.albumsByTitle())
                .hasSize(2)
                .containsKeys("Album1", "Album2");
            
            assertThat(context.albumsByTitle().get("Album1"))
                .hasSize(2)  // "Artist1, Artist2"와 "Artist1"로 두 개의 서로 다른 앨범 정보
                .anySatisfy(info -> {
                    assertThat(info.releaseDate()).isEqualTo(LocalDate.of(2023, 1, 15));
                    assertThat(info.artistName()).isEqualTo("Artist1, Artist2");
                })
                .anySatisfy(info -> {
                    assertThat(info.releaseDate()).isEqualTo(LocalDate.of(2023, 1, 15));
                    assertThat(info.artistName()).isEqualTo("Artist1");
                });
                
            assertThat(context.songs()).hasSize(3);
        }

        @Test
        @DisplayName("null과 빈 리스트 처리")
        void shouldReturnEmptyContextForNullOrEmptyList() {
            // null list
            BatchContext nullContext = BatchContext.from(null);
            assertThat(nullContext.artistNames()).isEmpty();
            assertThat(nullContext.albumsByTitle()).isEmpty();
            assertThat(nullContext.songs()).isEmpty();

            // empty list
            BatchContext emptyContext = BatchContext.from(new ArrayList<>());
            assertThat(emptyContext.artistNames()).isEmpty();
            assertThat(emptyContext.albumsByTitle()).isEmpty();
            assertThat(emptyContext.songs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("아티스트 처리 테스트")
    class ArtistProcessingTest {

        @Test
        @DisplayName("아티스트 이름 파싱 및 정규화")
        void shouldParseAndNormalizeArtistNames() {
            // given - 다양한 형태의 아티스트 입력
            List<SpotifySongDto> dtos = Arrays.asList(
                createSongDto("  Artist1  ,   Artist2   ,Artist3  ", null, null), // 공백 포함
                createSongDto("Artist1, Artist2", null, null), // 중복
                createSongDto("Artist4", null, null) // 단일 아티스트
            );

            // when
            BatchContext context = BatchContext.from(dtos);

            // then
            assertThat(context.artistNames())
                .hasSize(4) // 중복 제거됨
                .containsExactlyInAnyOrder("Artist1", "Artist2", "Artist3", "Artist4");
        }

        @Test
        @DisplayName("빈 아티스트 항목 처리")
        void shouldHandleEmptyArtistEntries() {
            // given - null, 빈 문자열, 공백만 있는 경우
            List<SpotifySongDto> dtos = Arrays.asList(
                createSongDto(null, null, null),
                createSongDto("", null, null),
                createSongDto("   ", null, null)
            );

            // when
            BatchContext context = BatchContext.from(dtos);

            // then
            assertThat(context.artistNames()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Artist1,,Artist2", "Artist1, ,Artist2", ",Artist1,Artist2,"})
        @DisplayName("빈 아티스트 항목 무시")
        void shouldIgnoreEmptyArtistEntries(String artists) {
            // given
            List<SpotifySongDto> dtos = Collections.singletonList(
                createSongDto(artists, null, null)
            );

            // when
            BatchContext context = BatchContext.from(dtos);

            // then - 빈 항목은 무시하고 실제 아티스트만 추출
            assertThat(context.artistNames())
                .containsExactlyInAnyOrder("Artist1", "Artist2");
        }
    }

    @Nested
    @DisplayName("앨범 처리 테스트")
    class AlbumProcessingTest {

        @Test
        @DisplayName("앨범 정보 유효성 검증")
        void shouldValidateAlbumInfo() {
            // given - 다양한 앨범 정보 상태
            List<SpotifySongDto> dtos = Arrays.asList(
                createSongDto("Artist1", "ValidAlbum", "2023-03-15"), // 정상
                createSongDto("Artist2", null, "2023-03-15"), // 타이틀 없음 - 무시됨
                createSongDto("Artist3", "AlbumNoDate", null), // 날짜 없음 - 포함됨 (null 날짜 허용)
                createSongDto("Artist4", "", "2023-03-15"), // 빈 타이틀 - 무시됨
                createSongDto("Artist5", "   ", "2023-03-15") // 공백만 있는 타이틀 - 무시됨
            );

            // when
            BatchContext context = BatchContext.from(dtos);

            // then - 유효한 앨범만 포함됨
            assertThat(context.albumsByTitle()).hasSize(2); // ValidAlbum과 AlbumNoDate
            assertThat(context.albumsByTitle()).containsKeys("ValidAlbum", "AlbumNoDate");
            
            // ValidAlbum 확인
            assertThat(context.albumsByTitle().get("ValidAlbum"))
                .hasSize(1)
                .anySatisfy(info -> {
                    assertThat(info.releaseDate()).isEqualTo(LocalDate.of(2023, 3, 15));
                    assertThat(info.artistName()).isEqualTo("Artist1");
                });
            
            // AlbumNoDate 확인 (null 날짜 허용)
            assertThat(context.albumsByTitle().get("AlbumNoDate"))
                .hasSize(1)
                .anySatisfy(info -> {
                    assertThat(info.releaseDate()).isNull();
                    assertThat(info.artistName()).isEqualTo("Artist3");
                });
        }

        @Test
        @DisplayName("잘못된 날짜 형식 처리")
        void shouldHandleInvalidDateFormat() {
            // given
            SpotifySongDto dto = SpotifySongDto.builder()
                .albumTitle("Album Title")
                .releaseDate("invalid-date")
                .build();

            // when & then - 예외 없이 처리 (SpotifyDomainMapper가 null 반환 가정)
            assertThatCode(() -> BatchContext.from(Collections.singletonList(dto)))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Builder 및 empty 메서드 테스트")
    class BuilderTest {

        @Test
        @DisplayName("empty 메서드로 빈 컨텍스트 생성")
        void shouldCreateEmptyBatchContext() {
            // when
            BatchContext context = BatchContext.empty();

            // then
            assertThat(context.artistNames()).isNotNull().isEmpty();
            assertThat(context.albumsByTitle()).isNotNull().isEmpty();
            assertThat(context.songs()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Builder로 커스텀 컨텍스트 생성")
        void shouldCreateCustomBatchContext() {
            // given
            Set<String> artists = Set.of("Artist1", "Artist2");
            Map<String, Set<AlbumBatchProcessor.AlbumInfo>> albums = Map.of(
                "Album1", Set.of(new AlbumBatchProcessor.AlbumInfo(LocalDate.of(2023, 1, 1), "Artist1"))
            );
            List<SpotifySongDto> songs = new ArrayList<>();

            // when
            BatchContext context = BatchContext.builder()
                .artistNames(artists)
                .albumsByTitle(albums)
                .songs(songs)
                .build();

            // then
            assertThat(context.artistNames()).isEqualTo(artists);
            assertThat(context.albumsByTitle()).isEqualTo(albums);
            assertThat(context.songs()).isEqualTo(songs);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("실제 복잡한 데이터 처리")
        void shouldProcessComplexRealWorldData() {
            // given - K-POP 실제 데이터 시뮬레이션
            List<SpotifySongDto> dtos = Arrays.asList(
                createSongDto("BTS, Halsey", "MAP OF THE SOUL : PERSONA", "2019-04-12"),
                createSongDto("BTS", "MAP OF THE SOUL : PERSONA", "2019-04-12"),
                createSongDto("IU, SUGA", "Eight", "2020-05-06"),
                createSongDto("IU", "Love poem", "2019-11-18"),
                createSongDto(null, null, null), // 빈 데이터
                createSongDto("", "", ""), // 빈 문자열
                createSongDto("BLACKPINK, Dua Lipa", "THE ALBUM", "2020-10-02")
            );

            // when
            BatchContext context = BatchContext.from(dtos);

            // then
            assertThat(context.artistNames())
                .containsExactlyInAnyOrder("BTS", "Halsey", "IU", "SUGA", "BLACKPINK", "Dua Lipa");
            assertThat(context.albumsByTitle())
                .hasSize(4)
                .containsKeys("MAP OF THE SOUL : PERSONA", "Eight", "Love poem", "THE ALBUM");
            assertThat(context.songs()).hasSize(7);
        }
    }

    // Helper method
    private SpotifySongDto createSongDto(String artists, String albumTitle, String releaseDate) {
        return SpotifySongDto.builder()
            .artists(artists)
            .albumTitle(albumTitle)
            .releaseDate(releaseDate)
            .build();
    }
}