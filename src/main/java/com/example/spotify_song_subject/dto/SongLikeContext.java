package com.example.spotify_song_subject.dto;

import com.example.spotify_song_subject.domain.Song;
import com.example.spotify_song_subject.domain.SongLike;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 좋아요 처리 컨텍스트
 * 좋아요 처리 과정에서 필요한 데이터를 담는 DTO
 */
@Getter
@AllArgsConstructor
@Builder
public class SongLikeContext {

    private final Song song;
    private final SongLike songLike;

    @Setter
    private Long totalLikes;

    /**
     * 좋아요 추가 시 컨텍스트 생성
     */
    public static SongLikeContext likeAdded(Song song, SongLike songLike) {
        return SongLikeContext.builder()
                .song(song)
                .songLike(songLike)
                .build();
    }

    /**
     * 좋아요 추가 시 컨텍스트 생성 (총 좋아요 수 포함)
     */
    public static SongLikeContext likeAdded(Song song, SongLike songLike, Long totalLikes) {
        return SongLikeContext.builder()
                .song(song)
                .songLike(songLike)
                .totalLikes(totalLikes)
                .build();
    }

    /**
     * 좋아요 취소 시 컨텍스트 생성
     */
    public static SongLikeContext likeRemoved(Song song, Long totalLikes) {
        return SongLikeContext.builder()
                .song(song)
                .totalLikes(totalLikes)
                .build();
    }

    /**
     * 기존 호환성을 위한 메서드 (deprecated 예정)
     */
    @Deprecated
    public static SongLikeContext of(Song song, SongLike songLike) {
        return likeAdded(song, songLike);
    }

    /**
     * 기존 호환성을 위한 메서드 (deprecated 예정)
     */
    @Deprecated
    public static SongLikeContext of(Song song, SongLike songLike, Long totalLikes) {
        return likeAdded(song, songLike, totalLikes);
    }
}