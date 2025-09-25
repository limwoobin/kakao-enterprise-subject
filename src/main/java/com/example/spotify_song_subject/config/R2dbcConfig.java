package com.example.spotify_song_subject.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import reactor.core.publisher.Mono;
import io.r2dbc.spi.ConnectionFactory;

@Slf4j
@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories(basePackages = "com.example.spotify_song_subject.repository")
public class R2dbcConfig {

    /**
     * Spring Boot가 자동으로 ConnectionFactory를 생성하도록 위임
     * application.yml의 spring.r2dbc 설정을 사용
     */

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.setSeparator(";");
        populator.setContinueOnError(false);

        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    /**
     * Reactive Auditor 설정
     * @CreatedBy, @LastModifiedBy 어노테이션을 위한 사용자 정보 제공
     * 실제 운영 환경에서는 SecurityContext에서 사용자 정보를 가져와야 함
     */
    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("system");  // 기본값으로 "system" 사용
        // 실제 구현 예시:
        // return () -> ReactiveSecurityContextHolder.getContext()
        //     .map(SecurityContext::getAuthentication)
        //     .filter(Authentication::isAuthenticated)
        //     .map(Authentication::getName)
        //     .switchIfEmpty(Mono.just("system"));
    }
}