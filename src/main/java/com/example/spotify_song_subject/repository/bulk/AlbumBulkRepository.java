package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.Album;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk insert repository for Albums using true bulk SQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AlbumBulkRepository implements BulkRepository<Album> {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> bulkInsert(Collection<Album> entities) {
        if (entities.isEmpty()) {
            return Mono.just(0L);
        }

        String sql = buildSql(entities);
        DatabaseClient.GenericExecuteSpec spec = bindParameters(databaseClient.sql(sql), entities);

        return executeInsert(spec);
    }

    private String buildSql(Collection<Album> entities) {
        return "INSERT INTO albums (title, release_date, artist_name) VALUES " +
                IntStream.range(0, entities.size())
                        .mapToObj(idx -> "(:title" + idx + ", :release_date" + idx + ", :artist_name" + idx + ")")
                        .collect(Collectors.joining(", "));
    }

    private DatabaseClient.GenericExecuteSpec bindParameters(DatabaseClient.GenericExecuteSpec spec,
                                                             Collection<Album> entities) {
        int index = 0;
        for (Album album : entities) {
            spec = bindAlbumParameters(spec, album, index);
            index++;
        }
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindAlbumParameters(DatabaseClient.GenericExecuteSpec spec,
                                                                  Album album, int index) {
        spec = spec.bind("title" + index, album.getTitle());

        if (album.getReleaseDate() != null) {
            spec = spec.bind("release_date" + index, album.getReleaseDate());
        } else {
            spec = spec.bindNull("release_date" + index, java.time.LocalDate.class);
        }
        
        if (album.getArtistName() != null) {
            return spec.bind("artist_name" + index, album.getArtistName());
        }
        return spec.bindNull("artist_name" + index, String.class);
    }

    private Mono<Long> executeInsert(DatabaseClient.GenericExecuteSpec spec) {
        return spec.fetch()
                .rowsUpdated()
                .doOnSuccess(count -> log.debug("Bulk inserted {} albums", count))
                .doOnError(error -> log.error("Failed to bulk insert albums", error));
    }
}