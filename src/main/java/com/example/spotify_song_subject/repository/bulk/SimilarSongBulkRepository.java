package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.SimilarSong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk insert repository for SimilarSongs using true bulk SQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SimilarSongBulkRepository implements BulkRepository<SimilarSong> {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> bulkInsert(Collection<SimilarSong> entities) {
        if (entities.isEmpty()) {
            return Mono.just(0L);
        }

        String sql = buildSql(entities);
        DatabaseClient.GenericExecuteSpec spec = bindParameters(databaseClient.sql(sql), entities);

        return executeInsert(spec);
    }

    private String buildSql(Collection<SimilarSong> entities) {
        return "INSERT INTO similar_songs (song_id, similar_artist_name, similar_song_title, similarity_score) VALUES " +
                IntStream.range(0, entities.size())
                        .mapToObj(idx -> String.format("(:song_id%d, :similar_artist_name%d, :similar_song_title%d, :similarity_score%d)",
                                idx, idx, idx, idx))
                        .collect(Collectors.joining(", "));
    }

    private DatabaseClient.GenericExecuteSpec bindParameters(DatabaseClient.GenericExecuteSpec spec,
                                                             Collection<SimilarSong> entities) {
        int index = 0;
        for (SimilarSong similarSong : entities) {
            spec = bindSimilarSongParameters(spec, similarSong, index);
            index++;
        }
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindSimilarSongParameters(DatabaseClient.GenericExecuteSpec spec,
                                                                        SimilarSong similarSong, int index) {
        spec = spec
                .bind("song_id" + index, similarSong.getSongId())
                .bind("similar_artist_name" + index, similarSong.getSimilarArtistName())
                .bind("similar_song_title" + index, similarSong.getSimilarSongTitle());

        if (similarSong.getSimilarityScore() != null) {
            return spec.bind("similarity_score" + index, similarSong.getSimilarityScore());
        }
        return spec.bindNull("similarity_score" + index, java.math.BigDecimal.class);
    }

    private Mono<Long> executeInsert(DatabaseClient.GenericExecuteSpec spec) {
        return spec.fetch()
                .rowsUpdated()
                .doOnSuccess(count -> log.debug("Bulk inserted {} similar song relationships", count))
                .doOnError(error -> log.error("Failed to bulk insert similar song relationships", error));
    }
}