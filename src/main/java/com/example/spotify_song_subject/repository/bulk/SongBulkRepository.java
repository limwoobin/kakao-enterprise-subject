package com.example.spotify_song_subject.repository.bulk;

import com.example.spotify_song_subject.domain.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bulk insert repository for Songs using true bulk SQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SongBulkRepository implements BulkRepository<Song> {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> bulkInsert(Collection<Song> entities) {
        if (entities.isEmpty()) {
            return Mono.just(0L);
        }

        String sql = buildSql(entities);
        DatabaseClient.GenericExecuteSpec spec = bindParameters(databaseClient.sql(sql), entities);
        return spec
            .fetch()
            .rowsUpdated();
    }

    private String buildSql(Collection<Song> entities) {
        String columns = "INSERT INTO songs (album_id, title, lyrics, length, music_key, tempo, " +
                "loudness_db, time_signature, explicit_content, emotion, genre, popularity, " +
                "energy, danceability, positiveness, speechiness, liveness, acousticness, " +
                "instrumentalness, activity_suitability_party, activity_suitability_work, " +
                "activity_suitability_relaxation, activity_suitability_exercise, " +
                "activity_suitability_running, activity_suitability_yoga, " +
                "activity_suitability_driving, activity_suitability_social, " +
                "activity_suitability_morning, like_count) VALUES ";

        String values = IntStream.range(0, entities.size())
                .mapToObj(this::buildValuePlaceholders)
                .collect(Collectors.joining(", "));

        return columns + values;
    }

    private String buildValuePlaceholders(int idx) {
        return String.format("(:album_id%d, :title%d, :lyrics%d, :length%d, :music_key%d, " +
                ":tempo%d, :loudness_db%d, :time_signature%d, :explicit_content%d, " +
                ":emotion%d, :genre%d, :popularity%d, :energy%d, :danceability%d, " +
                ":positiveness%d, :speechiness%d, :liveness%d, :acousticness%d, " +
                ":instrumentalness%d, :activity_suitability_party%d, " +
                ":activity_suitability_work%d, :activity_suitability_relaxation%d, " +
                ":activity_suitability_exercise%d, :activity_suitability_running%d, " +
                ":activity_suitability_yoga%d, :activity_suitability_driving%d, " +
                ":activity_suitability_social%d, :activity_suitability_morning%d, " +
                ":like_count%d)",
                idx, idx, idx, idx, idx, idx, idx, idx, idx, idx, idx, idx, idx,
                idx, idx, idx, idx, idx, idx, idx, idx, idx, idx, idx, idx, idx,
                idx, idx, idx);
    }

    private DatabaseClient.GenericExecuteSpec bindParameters(DatabaseClient.GenericExecuteSpec spec,
                                                             Collection<Song> entities) {
        int index = 0;
        for (Song song : entities) {
            spec = bindSongParameters(spec, song, index);
            index++;
        }
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindSongParameters(DatabaseClient.GenericExecuteSpec spec,
                                                                 Song song,
                                                                 int index) {
        // album_id에 대한 null 처리
        if (song.getAlbumId() != null) {
            spec = spec.bind("album_id" + index, song.getAlbumId());
        } else {
            spec = spec.bindNull("album_id" + index, Long.class);
        }
        
        spec = spec.bind("title" + index, song.getTitle());
        spec = bindBasicFields(spec, song, index);
        spec = bindAudioFeatures(spec, song, index);
        spec = bindActivitySuitabilities(spec, song, index);
        spec = spec.bind("like_count" + index, song.getLikeCount() != null ? song.getLikeCount() : 0L);
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindBasicFields(DatabaseClient.GenericExecuteSpec spec,
                                                              Song song,
                                                              int index) {
        spec = bindNullableField(spec, "lyrics" + index, song.getLyrics());
        spec = bindNullableField(spec, "length" + index, song.getLength());
        spec = bindNullableField(spec, "music_key" + index, song.getMusicKey());
        spec = bindNullableField(spec, "tempo" + index, song.getTempo());
        spec = bindNullableField(spec, "loudness_db" + index, song.getLoudnessDb());
        spec = bindNullableField(spec, "time_signature" + index, song.getTimeSignature());
        spec = bindNullableField(spec, "explicit_content" + index,
                song.getExplicitContent() != null ? song.getExplicitContent().name() : null);
        spec = bindNullableField(spec, "emotion" + index, song.getEmotion());
        spec = bindNullableField(spec, "genre" + index, song.getGenre());
        spec = bindNullableField(spec, "popularity" + index, song.getPopularity());
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindAudioFeatures(DatabaseClient.GenericExecuteSpec spec,
                                                                Song song,
                                                                int index) {
        spec = bindNullableField(spec, "energy" + index, song.getEnergy());
        spec = bindNullableField(spec, "danceability" + index, song.getDanceability());
        spec = bindNullableField(spec, "positiveness" + index, song.getPositiveness());
        spec = bindNullableField(spec, "speechiness" + index, song.getSpeechiness());
        spec = bindNullableField(spec, "liveness" + index, song.getLiveness());
        spec = bindNullableField(spec, "acousticness" + index, song.getAcousticness());
        spec = bindNullableField(spec, "instrumentalness" + index, song.getInstrumentalness());
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindActivitySuitabilities(DatabaseClient.GenericExecuteSpec spec,
                                                                        Song song,
                                                                        int index) {
        spec = bindNullableField(spec, "activity_suitability_party" + index,
                song.getActivitySuitabilityParty() != null ? song.getActivitySuitabilityParty().name() : null);
        spec = bindNullableField(spec, "activity_suitability_work" + index,
                song.getActivitySuitabilityWork() != null ? song.getActivitySuitabilityWork().name() : null);
        spec = bindNullableField(spec, "activity_suitability_relaxation" + index,
                song.getActivitySuitabilityRelaxation() != null ? song.getActivitySuitabilityRelaxation().name() : null);
        spec = bindNullableField(spec, "activity_suitability_exercise" + index,
                song.getActivitySuitabilityExercise() != null ? song.getActivitySuitabilityExercise().name() : null);
        spec = bindNullableField(spec, "activity_suitability_running" + index,
                song.getActivitySuitabilityRunning() != null ? song.getActivitySuitabilityRunning().name() : null);
        spec = bindNullableField(spec, "activity_suitability_yoga" + index,
                song.getActivitySuitabilityYoga() != null ? song.getActivitySuitabilityYoga().name() : null);
        spec = bindNullableField(spec, "activity_suitability_driving" + index,
                song.getActivitySuitabilityDriving() != null ? song.getActivitySuitabilityDriving().name() : null);
        spec = bindNullableField(spec, "activity_suitability_social" + index,
                song.getActivitySuitabilitySocial() != null ? song.getActivitySuitabilitySocial().name() : null);
        spec = bindNullableField(spec, "activity_suitability_morning" + index,
                song.getActivitySuitabilityMorning() != null ? song.getActivitySuitabilityMorning().name() : null);
        return spec;
    }

    private DatabaseClient.GenericExecuteSpec bindNullableField(DatabaseClient.GenericExecuteSpec spec,
                                                                String paramName, Object value) {
        if (value != null) {
            return spec.bind(paramName, value);
        }

        return spec.bindNull(paramName, String.class);
    }
}
