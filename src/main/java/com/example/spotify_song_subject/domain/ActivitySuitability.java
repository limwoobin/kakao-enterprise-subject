package com.example.spotify_song_subject.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ActivitySuitability {
    NOT_SUITABLE("NOT_SUITABLE"),
    SUITABLE("SUITABLE");

    private final String value;

    private static final Map<String, ActivitySuitability> VALUE_CACHE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(
                ActivitySuitability::getValue,
                e -> e
            ));

    public static ActivitySuitability fromValue(String value) {
        ActivitySuitability suitability = VALUE_CACHE.get(value);
        if (suitability == null) {
            throw new IllegalArgumentException("Unknown value: " + value);
        }

        return suitability;
    }
}
