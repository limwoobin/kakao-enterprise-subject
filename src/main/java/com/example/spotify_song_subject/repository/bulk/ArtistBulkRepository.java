package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.Artist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk insert repository for Artists using true bulk SQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ArtistBulkRepository implements BulkRepository<Artist> {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> bulkInsert(Collection<Artist> entities) {
        if (entities.isEmpty()) {
            return Mono.just(0L);
        }

        String sql = buildSql(entities);
        DatabaseClient.GenericExecuteSpec spec = bindParameters(databaseClient.sql(sql), entities);

        return executeInsert(spec);
    }

    private String buildSql(Collection<Artist> entities) {
        return "INSERT IGNORE INTO artists (name) VALUES " +
                IntStream.range(0, entities.size())
                        .mapToObj(idx -> "(:name" + idx + ")")
                        .collect(Collectors.joining(", "));
    }

    private DatabaseClient.GenericExecuteSpec bindParameters(DatabaseClient.GenericExecuteSpec spec,
                                                             Collection<Artist> entities) {
        int index = 0;
        for (Artist artist : entities) {
            spec = spec.bind("name" + index, artist.getName());
            index++;
        }
        return spec;
    }

    private Mono<Long> executeInsert(DatabaseClient.GenericExecuteSpec spec) {
        return spec.fetch()
                .rowsUpdated()
                .doOnSuccess(count -> log.debug("Bulk inserted {} artists", count))
                .doOnError(error -> log.error("Failed to bulk insert artists", error));
    }
}