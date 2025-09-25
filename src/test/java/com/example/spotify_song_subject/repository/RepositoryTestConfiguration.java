package com.example.spotify_song_subject.repository;

import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository 테스트를 위한 공통 설정 어노테이션
 * R2DBC 테스트 환경을 구성하고 H2 인메모리 DB를 사용
 * 데이터베이스 설정은 src/test/resources/application.yml에서 관리됨
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataR2dbcTest
@ActiveProfiles("test")
@Import({TestSchemaInitializer.class, TestR2dbcAuditingConfig.class})
public @interface RepositoryTestConfiguration {
}