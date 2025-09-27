package com.example.spotify_song_subject.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 특정 속성의 포함/불포함 상태를 나타내는 범용 열거형
 *
 * <p>다양한 도메인에서 재사용 가능:
 * <ul>
 *   <li>명시적 콘텐츠 포함 여부</li>
 *   <li>특정 기능 포함 여부</li>
 *   <li>옵션 포함 여부</li>
 * </ul>
 */
@Slf4j
@Getter
@AllArgsConstructor
public enum InclusionStatus {

    INCLUDED("포함"),
    NOT_INCLUDED("미포함"),
    UNKNOWN("알수 없음");

    private final String description;

    public static final Map<String, InclusionStatus> VALUE_CACHE = Stream.of(values())
        .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    public static InclusionStatus from(String name) {
        try {
            return VALUE_CACHE.get(name);
        } catch (IllegalArgumentException e) {
            log.error("Failed to parse InclusionStatus from value: {}", name);
            throw e;
        }
    }
}
