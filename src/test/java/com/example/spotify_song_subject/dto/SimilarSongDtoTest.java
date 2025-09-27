package com.example.spotify_song_subject.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimilarSongDto 테스트")
class SimilarSongDtoTest {

    @Test
    @DisplayName("setDynamicProperty - Similar Artist 필드 파싱")
    void shouldParseSimilarArtistField() {
        // given
        SimilarSongDto dto = new SimilarSongDto();

        // when
        dto.setDynamicProperty("Similar Artist 1", "IU");
        dto.setDynamicProperty("Similar Artist 2", "BTS");

        // then
        assertThat(dto.getArtistName()).isEqualTo("BTS"); // 마지막 값으로 덮어씀
    }

    @Test
    @DisplayName("setDynamicProperty - Similar Song 필드 파싱")
    void shouldParseSimilarSongField() {
        // given
        SimilarSongDto dto = new SimilarSongDto();

        // when
        dto.setDynamicProperty("Similar Song 1", "Eight");
        dto.setDynamicProperty("Similar Song 5", "Lilac");

        // then
        assertThat(dto.getSongTitle()).isEqualTo("Lilac"); // 마지막 값으로 덮어씀
    }

    @Test
    @DisplayName("setDynamicProperty - 기타 필드는 properties에 저장")
    void shouldStoreOtherFieldsInProperties() {
        // given
        SimilarSongDto dto = new SimilarSongDto();

        // when
        dto.setDynamicProperty("Custom Field", "custom value");
        dto.setDynamicProperty("Extra Data", 123);

        // then
        assertThat(dto.getProperty("Custom Field")).isEqualTo("custom value");
        assertThat(dto.getProperty("Extra Data")).isEqualTo(123);
        assertThat(dto.getArtistName()).isNull(); // Similar Artist가 아니므로 null
        assertThat(dto.getSongTitle()).isNull(); // Similar Song이 아니므로 null
    }

    @Test
    @DisplayName("setDynamicProperty - null properties Map 초기화")
    void shouldInitializeNullPropertiesMap() {
        // given
        SimilarSongDto dto = new SimilarSongDto();

        // when
        dto.setDynamicProperty("key", "value");

        // then
        assertThat(dto.getProperties()).isNotNull();
        assertThat(dto.getProperty("key")).isEqualTo("value");
    }

    @Test
    @DisplayName("getProperty - 존재하지 않는 키 조회")
    void shouldReturnNullForNonExistentKey() {
        // given
        SimilarSongDto dto = SimilarSongDto.builder()
            .properties(null)
            .build();

        // then
        assertThat(dto.getProperty("non-existent")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "Artist1, Song1, 0.95, true",    // 모든 필수 필드 있음
        ", Song1, 0.95, false",          // artistName 없음
        "Artist1, , 0.95, false",        // songTitle 없음
        "Artist1, Song1, , false",       // similarityScore 없음
        ", , , false"                    // 모든 필드 없음
    })
    @DisplayName("isValid - 유효성 검증")
    void shouldValidateRequiredFields(String artist, String song, BigDecimal score, boolean expected) {
        // given
        SimilarSongDto dto = SimilarSongDto.builder()
            .artistName(artist)
            .songTitle(song)
            .similarityScore(score)
            .build();

        // when
        boolean result = dto.isValid();

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Builder로 완전한 DTO 생성")
    void shouldCreateCompleteDtoWithBuilder() {
        // given & when
        SimilarSongDto dto = SimilarSongDto.builder()
            .artistName("IU")
            .songTitle("Blueming")
            .similarityScore(BigDecimal.valueOf(0.987))
            .build();

        // then
        assertThat(dto.getArtistName()).isEqualTo("IU");
        assertThat(dto.getSongTitle()).isEqualTo("Blueming");
        assertThat(dto.getSimilarityScore()).isEqualByComparingTo(BigDecimal.valueOf(0.987));
        assertThat(dto.isValid()).isTrue();
    }

    @Test
    @DisplayName("JSON 파싱 시뮬레이션 - 실제 사용 예제")
    void shouldSimulateJsonParsing() {
        // given
        SimilarSongDto dto = SimilarSongDto.builder()
            .similarityScore(BigDecimal.valueOf(0.9860607848))
            .build();

        // when - JSON 파싱을 시뮬레이션
        dto.setDynamicProperty("Similar Artist 1", "Corey Smith");
        dto.setDynamicProperty("Similar Song 1", "If I Could Do It Again");

        // then
        assertThat(dto.getArtistName()).isEqualTo("Corey Smith");
        assertThat(dto.getSongTitle()).isEqualTo("If I Could Do It Again");
        assertThat(dto.getSimilarityScore()).isEqualByComparingTo(BigDecimal.valueOf(0.9860607848));
        assertThat(dto.isValid()).isTrue();
    }
}