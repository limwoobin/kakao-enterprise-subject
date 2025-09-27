package com.example.spotify_song_subject.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpotifySongDto 테스트")
class SpotifySongDtoTest {

    @Test
    @DisplayName("setAdditionalProperty - 정상적인 속성 추가")
    void shouldAddAdditionalProperty() {
        // given
        SpotifySongDto dto = SpotifySongDto.builder().build();

        // when
        dto.setAdditionalProperty("customField1", "value1");
        dto.setAdditionalProperty("customField2", 123);

        // then
        Map<String, Object> properties = dto.getAdditionalProperties();
        assertThat(properties)
            .hasSize(2)
            .containsEntry("customField1", "value1")
            .containsEntry("customField2", 123);
    }

    @Test
    @DisplayName("setAdditionalProperty - null Map 초기화 처리")
    void shouldInitializeNullMapWhenAddingProperty() {
        // given
        SpotifySongDto dto = new SpotifySongDto();
        // additionalProperties가 null인 상태

        // when
        dto.setAdditionalProperty("key", "value");

        // then
        assertThat(dto.getAdditionalProperties())
            .isNotNull()
            .containsEntry("key", "value");
    }

    @Test
    @DisplayName("Builder 패턴으로 완전한 DTO 생성")
    void shouldCreateCompleteDtoWithBuilder() {
        // given & when
        SpotifySongDto dto = SpotifySongDto.builder()
            .artists("BTS, Halsey")
            .songTitle("Boy With Luv")
            .albumTitle("MAP OF THE SOUL : PERSONA")
            .releaseDate("2019-04-12")
            .tempo(BigDecimal.valueOf(120.5))
            .popularity(95)
            .energy(88)
            .danceability(75)
            .similarSongs(Collections.emptyList())
            .build();

        // then
        assertThat(dto.getArtists()).isEqualTo("BTS, Halsey");
        assertThat(dto.getSongTitle()).isEqualTo("Boy With Luv");
        assertThat(dto.getAlbumTitle()).isEqualTo("MAP OF THE SOUL : PERSONA");
        assertThat(dto.getReleaseDate()).isEqualTo("2019-04-12");
        assertThat(dto.getTempo()).isEqualByComparingTo(BigDecimal.valueOf(120.5));
        assertThat(dto.getPopularity()).isEqualTo(95);
        assertThat(dto.getEnergy()).isEqualTo(88);
        assertThat(dto.getDanceability()).isEqualTo(75);
        assertThat(dto.getSimilarSongs()).isEmpty();
    }

    @Test
    @DisplayName("빈 DTO 생성 및 null 필드 처리")
    void shouldHandleEmptyDto() {
        // given & when
        SpotifySongDto dto = new SpotifySongDto();

        // then
        assertThat(dto.getArtists()).isNull();
        assertThat(dto.getSongTitle()).isNull();
        assertThat(dto.getAlbumTitle()).isNull();
        assertThat(dto.getReleaseDate()).isNull();
        assertThat(dto.getTempo()).isNull();
        assertThat(dto.getPopularity()).isNull();
        // additionalProperties는 기본값으로 빈 HashMap이 설정됨
        assertThat(dto.getAdditionalProperties())
            .isNotNull()
            .isEmpty();
    }

    @Test
    @DisplayName("활동 적합성 필드 설정")
    void shouldSetActivitySuitabilityFields() {
        // given & when
        SpotifySongDto dto = SpotifySongDto.builder()
            .goodForParty(85)
            .goodForWorkStudy(30)
            .goodForRelaxationMeditation(65)
            .goodForExercise(90)
            .goodForRunning(95)
            .goodForDriving(70)
            .build();

        // then
        assertThat(dto.getGoodForParty()).isEqualTo(85);
        assertThat(dto.getGoodForWorkStudy()).isEqualTo(30);
        assertThat(dto.getGoodForRelaxationMeditation()).isEqualTo(65);
        assertThat(dto.getGoodForExercise()).isEqualTo(90);
        assertThat(dto.getGoodForRunning()).isEqualTo(95);
        assertThat(dto.getGoodForDriving()).isEqualTo(70);
    }
}