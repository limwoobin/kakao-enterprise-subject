package com.example.spotify_song_subject.loader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SpotifyDataStreamReader 단위 테스트")
class SpotifyDataStreamReaderTest {

    @Test
    @DisplayName("SpotifyDataStreamReader 인스턴스가 정상적으로 생성된다")
    void 인스턴스생성_정상() {
        // given & when
        SpotifyDataStreamReader reader = new SpotifyDataStreamReader();

        // then
        assertThat(reader).isNotNull();
    }

    @Test
    @DisplayName("Component 어노테이션이 적용되어 있다")
    void Component어노테이션_확인() {
        // given
        Class<?> clazz = SpotifyDataStreamReader.class;

        // when
        boolean hasComponentAnnotation = clazz.isAnnotationPresent(org.springframework.stereotype.Component.class);

        // then
        assertThat(hasComponentAnnotation).isTrue();
    }

    @Test
    @DisplayName("필요한 메서드가 정의되어 있다")
    void 메서드정의_확인() {
        // given
        Class<?> clazz = SpotifyDataStreamReader.class;

        // when & then
        assertThat(clazz.getDeclaredMethods())
            .extracting("name")
            .contains("streamSpotifyDataInBatches");
    }

}