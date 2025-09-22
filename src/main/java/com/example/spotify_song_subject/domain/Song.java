package com.example.spotify_song_subject.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("songs")
public class Song extends BaseDomain {

    @Id
    private Long id;

    @Column("album_id")
    private Long albumId;

    private String title;

    private String lyrics;

    private LocalTime length;

    @Column("music_key")
    private String musicKey;

    private BigDecimal tempo;

    @Column("loudness_db")
    private BigDecimal loudnessDb;

    @Column("time_signature")
    private String timeSignature;

    @Column("is_explicit")
    private Boolean isExplicit = false;

    private String emotion;

    private String genre;

    private Integer popularity;

    private Integer energy;

    private Integer danceability;

    private Integer positiveness;

    private Integer speechiness;

    private Integer liveness;

    private Integer acousticness;

    private Integer instrumentalness;

    @Column("activity_suitability_party")
    private ActivitySuitability activitySuitabilityParty = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_work")
    private ActivitySuitability activitySuitabilityWork = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_relaxation")
    private ActivitySuitability activitySuitabilityRelaxation = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_exercise")
    private ActivitySuitability activitySuitabilityExercise = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_running")
    private ActivitySuitability activitySuitabilityRunning = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_yoga")
    private ActivitySuitability activitySuitabilityYoga = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_driving")
    private ActivitySuitability activitySuitabilityDriving = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_social")
    private ActivitySuitability activitySuitabilitySocial = ActivitySuitability.NOT_SUITABLE;

    @Column("activity_suitability_morning")
    private ActivitySuitability activitySuitabilityMorning = ActivitySuitability.NOT_SUITABLE;

    @Column("like_count")
    private Long likeCount = 0L;
}
