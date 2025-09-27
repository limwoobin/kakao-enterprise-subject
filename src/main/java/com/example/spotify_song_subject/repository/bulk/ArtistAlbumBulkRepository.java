package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.ArtistAlbum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk insert repository for ArtistAlbums using true bulk SQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ArtistAlbumBulkRepository implements BulkRepository<ArtistAlbum> {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> bulkInsert(Collection<ArtistAlbum> entities) {
        if (entities.isEmpty()) {
            return Mono.just(0L);
        }

        String sql = buildSql(entities);
        DatabaseClient.GenericExecuteSpec spec = bindParameters(databaseClient.sql(sql), entities);

        return executeInsert(spec);
    }

    private String buildSql(Collection<ArtistAlbum> entities) {
        return "INSERT INTO artist_albums (artist_id, album_id) VALUES " +
                IntStream.range(0, entities.size())
                        .mapToObj(idx -> String.format("(:artist_id%d, :album_id%d)", idx, idx))
                        .collect(Collectors.joining(", "));
    }

    private DatabaseClient.GenericExecuteSpec bindParameters(DatabaseClient.GenericExecuteSpec spec,
                                                             Collection<ArtistAlbum> entities) {
        int index = 0;
        for (ArtistAlbum artistAlbum : entities) {
            spec = bindArtistAlbumParameters(spec, artistAlbum, index);
            index++;
        }
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindArtistAlbumParameters(DatabaseClient.GenericExecuteSpec spec,
                                                                        ArtistAlbum artistAlbum, int index) {
        return spec
                .bind("artist_id" + index, artistAlbum.getArtistId())
                .bind("album_id" + index, artistAlbum.getAlbumId());
    }

    private Mono<Long> executeInsert(DatabaseClient.GenericExecuteSpec spec) {
        return spec.fetch()
                .rowsUpdated()
                .doOnSuccess(count -> log.debug("Bulk inserted {} artist-album relationships", count))
                .doOnError(error -> log.error("Failed to bulk insert artist-album relationships", error));
    }
}