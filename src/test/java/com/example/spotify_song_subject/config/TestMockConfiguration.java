package com.example.spotify_song_subject.config;

import com.example.spotify_song_subject.application.AlbumStatisticsQueryService;
import com.example.spotify_song_subject.repository.AlbumStatisticsCustomRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트용 Mock 빈 설정
 * @MockBean이 deprecated되어 TestConfiguration으로 대체
 */
@TestConfiguration
public class TestMockConfiguration {

    @Bean
    @Primary
    public AlbumStatisticsQueryService mockAlbumStatisticsQueryService() {
        return Mockito.mock(AlbumStatisticsQueryService.class);
    }

    @Bean
    @Primary
    public AlbumStatisticsCustomRepository mockAlbumStatisticsCustomRepository() {
        return Mockito.mock(AlbumStatisticsCustomRepository.class);
    }
}