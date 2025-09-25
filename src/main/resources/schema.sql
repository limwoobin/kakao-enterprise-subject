-- 1. Artists 테이블: 아티스트 정보를 저장하는 테이블
CREATE TABLE IF NOT EXISTS artists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '아티스트 고유 ID',
    name VARCHAR(255) NOT NULL COMMENT '아티스트명',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_artist_name ON artists(name);
CREATE INDEX IF NOT EXISTS idx_artist_deleted_at ON artists(deleted_at);

-- 2. Albums 테이블: 앨범 정보를 저장하는 테이블
CREATE TABLE IF NOT EXISTS albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '앨범 고유 ID',
    title VARCHAR(255) NOT NULL COMMENT '앨범 제목',
    release_date DATE COMMENT '발매일',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_album_release_date ON albums(release_date);
CREATE INDEX IF NOT EXISTS idx_album_deleted_at ON albums(deleted_at);

-- 3. Artist_Albums 테이블: 아티스트와 앨범의 관계를 저장하는 테이블
CREATE TABLE IF NOT EXISTS artist_albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '관계 고유 ID',
    artist_id BIGINT NOT NULL COMMENT '아티스트 ID',
    album_id BIGINT NOT NULL COMMENT '앨범 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_artist_albums_album_artist ON artist_albums(album_id, artist_id);
CREATE INDEX IF NOT EXISTS idx_artist_albums_deleted_at ON artist_albums(deleted_at);

-- 4. Songs 테이블: 곡 정보를 저장하는 테이블
CREATE TABLE IF NOT EXISTS songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '곡 고유 ID',
    album_id BIGINT COMMENT '앨범 ID',
    title VARCHAR(500) NOT NULL COMMENT '곡 제목',
    lyrics CLOB COMMENT '가사',
    length TIME COMMENT '곡 길이',
    music_key VARCHAR(20) COMMENT '음악 키 (예: C major)',
    tempo DECIMAL(10,2) COMMENT '템포 (BPM)',
    loudness_db DECIMAL(10,2) COMMENT '음량 (데시벨)',
    time_signature VARCHAR(10) COMMENT '박자 (예: 4/4)',
    explicit_content VARCHAR(20) DEFAULT 'NOT_INCLUDED' COMMENT '노골적 내용 포함 상태',
    emotion VARCHAR(50) COMMENT '곡의 감정',
    genre VARCHAR(100) COMMENT '장르',
    popularity INT COMMENT '인기도 (0-100)',
    energy INT COMMENT '에너지 (0-100)',
    danceability INT COMMENT '춤추기 적합도 (0-100)',
    positiveness INT COMMENT '긍정성 (0-100)',
    speechiness INT COMMENT '말하기 비중 (0-100)',
    liveness INT COMMENT '라이브감 (0-100)',
    acousticness INT COMMENT '어쿠스틱 정도 (0-100)',
    instrumentalness INT COMMENT '악기 비중 (0-100)',
    activity_suitability_party VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '파티 적합도',
    activity_suitability_work VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '작업 적합도',
    activity_suitability_relaxation VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '휴식 적합도',
    activity_suitability_exercise VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '운동 적합도',
    activity_suitability_running VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '러닝 적합도',
    activity_suitability_yoga VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '요가 적합도',
    activity_suitability_driving VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '운전 적합도',
    activity_suitability_social VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '사교 적합도',
    activity_suitability_morning VARCHAR(20) DEFAULT 'NOT_SUITABLE' COMMENT '아침 적합도',
    like_count BIGINT DEFAULT 0 COMMENT '좋아요 수 (역정규화)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_song_album ON songs(album_id);
CREATE INDEX IF NOT EXISTS idx_song_genre ON songs(genre);
CREATE INDEX IF NOT EXISTS idx_song_like_count ON songs(like_count DESC);
CREATE INDEX IF NOT EXISTS idx_song_deleted_at ON songs(deleted_at);

-- 5. Artist_Songs 테이블: 아티스트와 곡의 관계를 저장하는 테이블
CREATE TABLE IF NOT EXISTS artist_songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '관계 고유 ID',
    artist_id BIGINT NOT NULL COMMENT '아티스트 ID',
    song_id BIGINT NOT NULL COMMENT '곡 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_artist_songs_song_artist ON artist_songs(song_id, artist_id);
CREATE INDEX IF NOT EXISTS idx_artist_songs_deleted_at ON artist_songs(deleted_at);

-- 6. Song_Likes 테이블: 곡에 대한 사용자의 좋아요를 저장하는 테이블
CREATE TABLE IF NOT EXISTS song_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '좋아요 고유 ID',
    song_id BIGINT NOT NULL COMMENT '곡 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_like_song ON song_likes(song_id);
CREATE INDEX IF NOT EXISTS idx_like_user ON song_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_like_created ON song_likes(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_like_deleted_at ON song_likes(deleted_at);

-- 7. Similar_Songs 테이블: 유사한 곡들의 관계를 저장하는 테이블
CREATE TABLE IF NOT EXISTS similar_songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '유사 관계 고유 ID',
    song_id BIGINT NOT NULL COMMENT '기준 곡 ID',
    similar_artist_name VARCHAR(1000) NOT NULL COMMENT '유사 곡의 아티스트명',
    similar_song_title VARCHAR(500) NOT NULL COMMENT '유사 곡의 제목',
    similarity_score DECIMAL(20,18) NOT NULL COMMENT '유사도 점수 (0-1)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    deleted_at TIMESTAMP NULL COMMENT '삭제일시 (논리 삭제용)',
    created_by VARCHAR(100) COMMENT '생성자',
    updated_by VARCHAR(100) COMMENT '수정자'
);

CREATE INDEX IF NOT EXISTS idx_similar_score ON similar_songs(song_id, similarity_score DESC);
CREATE INDEX IF NOT EXISTS idx_similar_artist_song ON similar_songs(similar_artist_name, similar_song_title);
CREATE INDEX IF NOT EXISTS idx_similar_deleted_at ON similar_songs(deleted_at);
