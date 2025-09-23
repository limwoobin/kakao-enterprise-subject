package com.example.spotify_song_subject.domain;

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
public enum InclusionStatus {

    /**
     * 포함됨
     */
    INCLUDED,

    /**
     * 포함되지 않음
     */
    NOT_INCLUDED,

    /**
     * 알 수 없음 / 미확인
     */
    UNKNOWN;

  public static final Map<String, InclusionStatus> VALUE_CACHE = Stream.of(values())
    .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));
}