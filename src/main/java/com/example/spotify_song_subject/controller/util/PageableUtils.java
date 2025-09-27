package com.example.spotify_song_subject.controller.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Pageable 관련 유틸리티 클래스
 * WebFlux에서 Pageable 파라미터 처리를 위한 헬퍼 메서드 제공
 */
public class PageableUtils {
    
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    
    /**
     * 요청 파라미터로부터 Pageable 객체 생성
     */
    public static Pageable createPageable(Integer page, Integer size) {
        int pageNumber = page != null ? page : DEFAULT_PAGE;
        int pageSize = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        return PageRequest.of(pageNumber, pageSize);
    }
}
