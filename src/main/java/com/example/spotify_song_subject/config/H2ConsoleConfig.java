package com.example.spotify_song_subject.config;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * H2 웹 콘솔 설정
 * WebFlux 환경에서 H2 콘솔을 사용하기 위한 대체 설정
 */
@Slf4j
@Component
@Profile("!prod") // 프로덕션 환경에서는 비활성화
public class H2ConsoleConfig {

    private Server webServer;

    @Value("${h2.console.port:8082}")
    private String port;

    @EventListener(ContextRefreshedEvent.class)
    public void start() throws SQLException {
        log.info("Starting H2 Console at port {}", port);
        this.webServer = Server.createWebServer("-webPort", port, "-tcpAllowOthers")
            .start();
        log.info("H2 Console started at http://localhost:{}", port);
    }

    @EventListener(ContextClosedEvent.class)
    public void stop() {
        if (this.webServer != null) {
            log.info("Stopping H2 Console");
            this.webServer.stop();
        }
    }
}