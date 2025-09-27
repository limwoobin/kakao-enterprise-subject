package com.example.spotify_song_subject.repository;

import com.example.spotify_song_subject.domain.Album;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface AlbumRepository extends R2dbcRepository<Album, Long> {

    @Query("SELECT * FROM albums WHERE title = :title AND release_date = :releaseDate AND deleted_at IS NULL")
    Mono<Album> findByTitleAndReleaseDate(String title, LocalDate releaseDate);
    
    /**
     * 여러 앨범을 title로 한번에 조회
     * title IN 절을 사용하여 성능 최적화
     */
    @Query("SELECT * FROM albums WHERE title IN (:titles) AND deleted_at IS NULL")
    Flux<Album> findAllByTitleIn(Collection<String> titles);

}
