package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Artist;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface ArtistRepository extends R2dbcRepository<Artist, Long> {

    @Query("SELECT * FROM artists WHERE name = :name AND deleted_at IS NULL")
    Mono<Artist> findByName(String name);

    /**
     * Bulk 조회 - 여러 아티스트를 이름으로 한번에 조회
     * @param names 아티스트 이름 컬렉션
     * @return 매칭되는 아티스트 Flux
     */
    @Query("SELECT * FROM artists WHERE name IN (:names) AND deleted_at IS NULL")
    Flux<Artist> findAllByNameIn(Collection<String> names);

}
