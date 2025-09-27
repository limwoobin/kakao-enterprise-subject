package com.example.spotify_song_subject.repository.bulk;

import reactor.core.publisher.Mono;
import java.util.Collection;

/**
 * Base interface for bulk insert operations
 * @param <T> Entity type
 */
public interface BulkRepository<T> {

    /**
     * Performs true bulk insert using single SQL statement
     * @param entities Entities to insert
     * @return Number of inserted rows
     */
    Mono<Long> bulkInsert(Collection<T> entities);
}