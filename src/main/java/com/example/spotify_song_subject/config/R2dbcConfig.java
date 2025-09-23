package com.example.spotify_song_subject.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import io.r2dbc.spi.ConnectionFactory;

@Slf4j
@Configuration
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
}