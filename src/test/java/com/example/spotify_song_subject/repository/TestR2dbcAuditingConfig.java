package com.example.spotify_song_subject.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Mono;

/**
 * 테스트 환경용 R2DBC Auditing 설정
 * @DataR2dbcTest는 @Configuration 클래스를 자동으로 로드하지 않으므로
 * 테스트용 Auditing 설정을 별도로 제공
 */
@TestConfiguration
@EnableR2dbcAuditing
public class TestR2dbcAuditingConfig {

    /**
     * 테스트용 Auditor 제공
     * 테스트 환경에서는 "test-user"로 설정
     */
    @Bean
    public ReactiveAuditorAware<String> testAuditorAware() {
        return () -> Mono.just("test-user");
    }
}