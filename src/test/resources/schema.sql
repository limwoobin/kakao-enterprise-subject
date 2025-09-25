-- 1. Artists 테이블: 아티스트 정보를 저장하는 테이블
CREATE TABLE IF NOT EXISTS artists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_artist_name ON artists(name);
CREATE INDEX IF NOT EXISTS idx_artist_deleted_at ON artists(deleted_at);

-- 2. Albums 테이블: 앨범 정보를 저장하는 테이블
CREATE TABLE IF NOT EXISTS albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    release_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_album_release_date ON albums(release_date);
CREATE INDEX IF NOT EXISTS idx_album_deleted_at ON albums(deleted_at);

-- 3. Artist_Albums 테이블: 아티스트와 앨범의 관계를 저장하는 테이블
CREATE TABLE IF NOT EXISTS artist_albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    album_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_artist_albums_album_artist ON artist_albums(album_id, artist_id);
CREATE INDEX IF NOT EXISTS idx_artist_albums_deleted_at ON artist_albums(deleted_at);

-- 4. Songs 테이블: 곡 정보를 저장하는 테이블
CREATE TABLE IF NOT EXISTS songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT,
    title VARCHAR(500) NOT NULL,
    lyrics CLOB,
    length TIME,
    music_key VARCHAR(20),
    tempo DECIMAL(10,2),
    loudness_db DECIMAL(10,2),
    time_signature VARCHAR(10),
    explicit_content VARCHAR(20) DEFAULT 'NOT_INCLUDED',
    emotion VARCHAR(50),
    genre VARCHAR(100),
    popularity INT,
    energy INT,
    danceability INT,
    positiveness INT,
    speechiness INT,
    liveness INT,
    acousticness INT,
    instrumentalness INT,
    activity_suitability_party VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_work VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_relaxation VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_exercise VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_running VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_yoga VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_driving VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_social VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    activity_suitability_morning VARCHAR(20) DEFAULT 'NOT_SUITABLE',
    like_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_song_album ON songs(album_id);
CREATE INDEX IF NOT EXISTS idx_song_genre ON songs(genre);
CREATE INDEX IF NOT EXISTS idx_song_like_count ON songs(like_count DESC);
CREATE INDEX IF NOT EXISTS idx_song_deleted_at ON songs(deleted_at);

-- 5. Artist_Songs 테이블: 아티스트와 곡의 관계를 저장하는 테이블
CREATE TABLE IF NOT EXISTS artist_songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_artist_songs_song_artist ON artist_songs(song_id, artist_id);
CREATE INDEX IF NOT EXISTS idx_artist_songs_deleted_at ON artist_songs(deleted_at);

-- 6. Song_Likes 테이블: 곡에 대한 사용자의 좋아요를 저장하는 테이블
CREATE TABLE IF NOT EXISTS song_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_like_song ON song_likes(song_id);
CREATE INDEX IF NOT EXISTS idx_like_user ON song_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_like_created ON song_likes(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_like_deleted_at ON song_likes(deleted_at);

-- 7. Similar_Songs 테이블: 유사한 곡들의 관계를 저장하는 테이블
CREATE TABLE IF NOT EXISTS similar_songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL,
    similar_artist_name VARCHAR(1000) NOT NULL,
    similar_song_title VARCHAR(500) NOT NULL,
    similarity_score DECIMAL(20,18) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_similar_score ON similar_songs(song_id, similarity_score DESC);
CREATE INDEX IF NOT EXISTS idx_similar_artist_song ON similar_songs(similar_artist_name, similar_song_title);
CREATE INDEX IF NOT EXISTS idx_similar_deleted_at ON similar_songs(deleted_at);