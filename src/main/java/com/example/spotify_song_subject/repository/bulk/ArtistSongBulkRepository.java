package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.ArtistSong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk insert repository for ArtistSongs using true bulk SQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ArtistSongBulkRepository implements BulkRepository<ArtistSong> {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> bulkInsert(Collection<ArtistSong> entities) {
        if (entities.isEmpty()) {
            return Mono.just(0L);
        }

        String sql = buildSql(entities);
        DatabaseClient.GenericExecuteSpec spec = bindParameters(databaseClient.sql(sql), entities);

        return executeInsert(spec);
    }

    private String buildSql(Collection<ArtistSong> entities) {
        return "INSERT INTO artist_songs (artist_id, song_id) VALUES " +
                IntStream.range(0, entities.size())
                        .mapToObj(idx -> String.format("(:artist_id%d, :song_id%d)", idx, idx))
                        .collect(Collectors.joining(", "));
    }

    private DatabaseClient.GenericExecuteSpec bindParameters(DatabaseClient.GenericExecuteSpec spec,
                                                             Collection<ArtistSong> entities) {
        int index = 0;
        for (ArtistSong artistSong : entities) {
            spec = bindArtistSongParameters(spec, artistSong, index);
            index++;
        }
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindArtistSongParameters(DatabaseClient.GenericExecuteSpec spec,
                                                                       ArtistSong artistSong, int index) {
        spec = spec.bind("artist_id" + index, artistSong.getArtistId());
        
        if (artistSong.getSongId() != null) {
            return spec.bind("song_id" + index, artistSong.getSongId());
        } else {
            log.error("ArtistSong has null songId for artistId: {}", artistSong.getArtistId());
            throw new IllegalArgumentException("ArtistSong cannot have null songId");
        }
    }

    private Mono<Long> executeInsert(DatabaseClient.GenericExecuteSpec spec) {
        return spec.fetch()
                .rowsUpdated()
                .doOnSuccess(count -> log.debug("Bulk inserted {} artist-song relationships", count))
                .doOnError(error -> log.error("Failed to bulk insert artist-song relationships", error));
    }
}