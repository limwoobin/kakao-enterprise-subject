package com.example.spotify_song_subject.config;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.example.spotify_song_subject.repository"
)
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        // H2 in-memory database configuration
        return new H2ConnectionFactory(
            H2ConnectionConfiguration.builder()
                .inMemory("testdb")
                .username("sa")
                .build()
        );
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        // R2DBC는 JPA와 달리 Entity 기반 DDL 자동 생성을 지원하지 않음
        // 따라서 schema.sql을 통한 수동 스키마 생성이 필요
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        // schema.sql 파일이 있다면 실행 (옵션)
        try {
            ClassPathResource schemaResource = new ClassPathResource("schema.sql");
            if (schemaResource.exists()) {
                populator.addScript(schemaResource);
            }
        } catch (Exception e) {
            // schema.sql이 없어도 정상 동작 (테이블이 필요한 경우 수동 생성 필요)
        }

        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
