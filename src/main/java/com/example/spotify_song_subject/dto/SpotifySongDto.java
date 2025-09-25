package com.example.spotify_song_subject.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spotify 데이터셋 JSON 매핑용 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotifySongDto {

    @JsonProperty("Artist(s)")
    private String artists;

    @JsonProperty("song")
    private String songTitle;

    @JsonProperty("text")
    private String lyrics;

    @JsonProperty("Length")
    private String length;

    @JsonProperty("emotion")
    private String emotion;

    @JsonProperty("Genre")
    private String genre;

    @JsonProperty("Album")
    private String albumTitle;

    @JsonProperty("Release Date")
    private String releaseDate;

    @JsonProperty("Key")
    private String musicKey;

    @JsonProperty("Tempo")
    private BigDecimal tempo;

    @JsonProperty("Loudness (db)")
    private BigDecimal loudnessDb;

    @JsonProperty("Time signature")
    private String timeSignature;

    @JsonProperty("Explicit")
    private String explicit;

    @JsonProperty("Popularity")
    private Integer popularity;

    @JsonProperty("Energy")
    private Integer energy;

    @JsonProperty("Danceability")
    private Integer danceability;

    @JsonProperty("Positiveness")
    private Integer positiveness;

    @JsonProperty("Speechiness")
    private Integer speechiness;

    @JsonProperty("Liveness")
    private Integer liveness;

    @JsonProperty("Acousticness")
    private Integer acousticness;

    @JsonProperty("Instrumentalness")
    private Integer instrumentalness;

    @JsonProperty("Good for Party")
    private Integer goodForParty;

    @JsonProperty("Good for Work/Study")
    private Integer goodForWorkStudy;

    @JsonProperty("Good for Relaxation/Meditation")
    private Integer goodForRelaxationMeditation;

    @JsonProperty("Good for Exercise")
    private Integer goodForExercise;

    @JsonProperty("Good for Running")
    private Integer goodForRunning;

    @JsonProperty("Good for Yoga/Stretching")
    private Integer goodForYogaStretching;

    @JsonProperty("Good for Driving")
    private Integer goodForDriving;

    @JsonProperty("Good for Social Gatherings")
    private Integer goodForSocialGatherings;

    @JsonProperty("Good for Morning Routine")
    private Integer goodForMorningRoutine;

    @JsonProperty("Similar Songs")
    private List<SimilarSongDto> similarSongs;

    @Builder.Default
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * Jackson이 인식하지 못한 필드를 동적으로 저장
     *
     * @param key 필드명
     * @param value 필드값
     */
    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        if (additionalProperties == null) {
            additionalProperties = new HashMap<>();
        }

        additionalProperties.put(key, value);
    }

}
