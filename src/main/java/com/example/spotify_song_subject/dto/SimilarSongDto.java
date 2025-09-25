package com.example.spotify_song_subject.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Similar Songs 정보를 담는 DTO
 *
 * <p>JSON 구조 특징:
 * <ul>
 *   <li>동적 필드명 (Similar Artist 1, Similar Song 1 등)</li>
 *   <li>유사도 점수 포함</li>
 * </ul>
 *
 * <p>JSON 예시:
 * <pre>
 * {
 *   "Similar Artist 1": "Corey Smith",
 *   "Similar Song 1": "If I Could Do It Again",
 *   "Similarity Score": 0.9860607848
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimilarSongDto {

    private String artistName;
    private String songTitle;

    @JsonProperty("Similarity Score")
    private BigDecimal similarityScore;

    // 동적 필드를 저장하기 위한 Map
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Jackson이 인식하지 못한 필드를 동적으로 처리
     * "Similar Artist N", "Similar Song N" 형태의 필드를 파싱
     *
     * @param key 필드명
     * @param value 필드값
     */
    @JsonAnySetter
    public void setDynamicProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        // 동적 필드명에서 아티스트명과 곡명 추출
        if (key.startsWith("Similar Artist")) {
            this.artistName = (String) value;
        } else if (key.startsWith("Similar Song")) {
            this.songTitle = (String) value;
        } else {
            properties.put(key, value);
        }
    }

    /**
     * 추가 속성 조회
     *
     * @param key 속성 키
     * @return 속성 값
     */
    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    /**
     * 유효성 검증
     *
     * @return 필수 필드가 모두 있으면 true
     */
    public boolean isValid() {
        return artistName != null && songTitle != null && similarityScore != null;
    }
}
