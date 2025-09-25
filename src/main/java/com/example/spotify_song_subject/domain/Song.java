package com.example.spotify_song_subject.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("songs")
public class Song extends BaseDomain {

    @Id
    private Long id;

    @Column("album_id")
    private Long albumId;

    @Column("title")
    private String title;

    @Column("lyrics")
    private String lyrics;

    @Column("length")
    private LocalTime length;

    @Column("music_key")
    private String musicKey;

    @Column("tempo")
    private BigDecimal tempo;

    @Column("loudness_db")
    private BigDecimal loudnessDb;

    @Column("time_signature")
    private String timeSignature;

    @Column("explicit_content")
    private InclusionStatus explicitContent = InclusionStatus.NOT_INCLUDED;

    @Column("emotion")
    private String emotion;

    @Column("genre")
    private String genre;

    @Column("popularity")
    private Integer popularity;

    @Column("energy")
    private Integer energy;

    @Column("danceability")
    private Integer danceability;

    @Column("positiveness")
    private Integer positiveness;

    @Column("speechiness")
    private Integer speechiness;

    @Column("liveness")
    private Integer liveness;

    @Column("acousticness")
    private Integer acousticness;

    @Column("instrumentalness")
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

    @Builder
    public Song(Long albumId, String title, String lyrics, LocalTime length, String musicKey,
                BigDecimal tempo, BigDecimal loudnessDb, String timeSignature,
                InclusionStatus explicitContent, String emotion, String genre,
                Integer popularity, Integer energy, Integer danceability,
                Integer positiveness, Integer speechiness, Integer liveness,
                Integer acousticness, Integer instrumentalness,
                ActivitySuitability activitySuitabilityParty, ActivitySuitability activitySuitabilityWork,
                ActivitySuitability activitySuitabilityRelaxation, ActivitySuitability activitySuitabilityExercise,
                ActivitySuitability activitySuitabilityRunning, ActivitySuitability activitySuitabilityYoga,
                ActivitySuitability activitySuitabilityDriving, ActivitySuitability activitySuitabilitySocial,
                ActivitySuitability activitySuitabilityMorning, Long likeCount) {
        this.albumId = albumId;
        this.title = title;
        this.lyrics = lyrics;
        this.length = length;
        this.musicKey = musicKey;
        this.tempo = tempo;
        this.loudnessDb = loudnessDb;
        this.timeSignature = timeSignature;
        this.explicitContent = explicitContent != null ? explicitContent : InclusionStatus.NOT_INCLUDED;
        this.emotion = emotion;
        this.genre = genre;
        this.popularity = popularity;
        this.energy = energy;
        this.danceability = danceability;
        this.positiveness = positiveness;
        this.speechiness = speechiness;
        this.liveness = liveness;
        this.acousticness = acousticness;
        this.instrumentalness = instrumentalness;
        this.activitySuitabilityParty = activitySuitabilityParty != null ? activitySuitabilityParty : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityWork = activitySuitabilityWork != null ? activitySuitabilityWork : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityRelaxation = activitySuitabilityRelaxation != null ? activitySuitabilityRelaxation : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityExercise = activitySuitabilityExercise != null ? activitySuitabilityExercise : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityRunning = activitySuitabilityRunning != null ? activitySuitabilityRunning : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityYoga = activitySuitabilityYoga != null ? activitySuitabilityYoga : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityDriving = activitySuitabilityDriving != null ? activitySuitabilityDriving : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilitySocial = activitySuitabilitySocial != null ? activitySuitabilitySocial : ActivitySuitability.NOT_SUITABLE;
        this.activitySuitabilityMorning = activitySuitabilityMorning != null ? activitySuitabilityMorning : ActivitySuitability.NOT_SUITABLE;
        this.likeCount = likeCount != null ? likeCount : 0L;
    }

}
