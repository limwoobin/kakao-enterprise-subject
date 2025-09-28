# Spotify Song Subject

## 개요

Spotify 음원의 JSON 파일을 다운받아 기초데이터 생성 및 이를 기반으로 Restful API, 좋아요 기능을 제공합니다.

## 어플리케이션 실행 방법

RDBMS 는 H2 Embedded Database, Redis 는 TestContainer 를 이용해 구성했습니다.  
따라서 실행하는 로컬 DeskTop 환경에 Docker 가 준비되어야 합니다.

서버가 실행되면 어플리케이션이 로드되는 시점에 __900k Definitive Spotify Dataset.json__ 의 데이터를 다운로드 받아 파싱하여  
h2 database 에 데이터를 세팅하게 됩니다.

__900k Definitive Spotify Dataset.json__ 기초데이터 처리는 아래와 같은 순서로 진행됩니다. 
1. Google Drive 에 등록된 __900k Definitive Spotify Dataset.json__ 압축파일 다운로드 
2. 다운로드가 완료된 압축파일은 프로젝트의 /data 디렉토리에 저장 
3. 압축파일에 대해 압축 해제
4. __900k Definitive Spotify Dataset.json__ 를 스트리밍하며 h2 database 에 저장

(압축파일로 데이터 처리를 진행하기 위해 Google Drive 를 활용하였습니다.)

어플리케이션 실행시 로컬 서버 접속은 8080, 데이터 확인은 8082 port 에 접속하면 확인할 수 있습니다.

<br />

## 프로젝트 구조

```
|── application/ # 비즈니스 서비스 로직 처리
├── config/ # 프로젝트 설정 관련
├── controller/ # API 엔드포인트 정의
    ├── response/ # API 반환타입 위치
    ├── utils/ # API 에 필요한 util class 위치
├── domain/ # 도메인 엔티티
├── dto/ # 데이터 전송 객체
└── exception/ # 예외 처리
├── loader/ # 데이터 초기화
├── mapper/ # 객체 변환 로직
├── repository/ # 데이터 접근 계층
    ├── bulk/ # bulk repository 처리
```

### __application__: 비즈니스 흐름을 담당하고 도메인 레이어와 컨트롤러 레이어를 연결합니다.
- 데이터 초기화, 좋아요, 통계 조회와 같은 서비스 객체가 위치합니다.
- 데이터 초기화의 bulk 처리를 위한 Processor 객체가 위치합니다.

### __config__: 프로젝트에 필요한 모든 설정을 관리합니다.
- DatabaseClientConfig, H2, Redis 연결 설정을 관리합니다.

### __controller__: API 엔드포인트 정의합니다.
- REST API 엔드포인트를 정의하고 HTTP 요청/응답을 처리합니다.

### __domain__: 핵심 비즈니스 엔티티와 도메인 규칙을 정의합니다.
- **기본 엔티티**:
    - `Song`: 노래 정보 (제목, 가사, 템포, 감정 등 30개 이상 속성)
    - `Album`: 앨범 정보 (제목, 발매일, 이미지 URL)
    - `Artist`: 아티스트 정보 (이름)
    - `SongLike`: 좋아요 정보 (사용자-노래 매핑)
- **관계 엔티티**:
    - `ArtistSong`: 아티스트-노래 M:N 관계
    - `ArtistAlbum`: 아티스트-앨범 M:N 관계
    - `SimilarSong`: 유사 노래 정보
- **Enum 타입**:
    - `InclusionStatus`: 포함 상태 (INCLUDE, EXCLUDE)
    - `ActivitySuitability`: 활동 적합성 (VERY_HIGH, HIGH, MEDIUM, LOW)
- **`BaseDomain`**: 공통 필드

### __DTO__:  계층 간 데이터 전송 및 데이터 값 객체를 정의합니다.
계층 간 데이터 전송을 위한 객체들입니다.

### __Exception Layer__: 도메인 특화 예외를 정의합니다. 
- **`ResourceNotFoundException`**: 리소스 없음
- **`DuplicateLikeException`**: 중복 좋아요

### __Loader Layer__:  애플리케이션 시작 시 데이터를 초기화합니다.

- **`DataInitializationRunner`**: 초기화 진입점
    - `@EventListener(ApplicationReadyEvent.class)` 사용
    - 조건부 실행 (`@ConditionalOnProperty`)
    - 다운로드 및 처리 오케스트레이션

- **`GoogleDriveDownloader`**: 파일 다운로드
    - Google Drive API 없이 직접 다운로드
    - ZIP 파일 자동 압축 해제
    - 재시작 시 중복 다운로드 방지

- **`SpotifyDataStreamReader`**: JSON 스트리밍
    - Jackson Streaming API 사용
    - 메모리 효율적인 대용량 파일 처리
    - 백프레셔 지원 (`Flux.generate`)

### __Mapper Layer__ : 객체 간 변환 로직을 담당합니다.
- **`SpotifyDataMapper`**: Map → DTO 변환
    - JSON 파싱 데이터를 DTO로 변환
- **`SpotifyDomainMapper`**: DTO → Domain 변환
    - SpotifySongDto → Song/Album/Artist 변환
    - Enum 변환 로직

### __Repository Layer__: 데이터베이스와 캐시 접근을 담당합니다.
- **기본 Repository** (R2DBC Repository 인터페이스):
    - `SongRepository`: 노래 CRUD
    - `AlbumRepository`: 앨범 CRUD
    - `ArtistRepository`: 아티스트 CRUD
    - `SongLikeRepository`: 좋아요 CRUD
- **Custom Repository** (복잡한 쿼리):
    - `SongLikeCustomRepository`: 좋아요 관련 커스텀 쿼리
    - `AlbumStatisticsCustomRepository`: 통계 집계 쿼리
- **Redis Repository**:
    - `SongLikeRedisRepository`: Redis 버킷 관리
        - 5분 단위 버킷 생성/조회
        - ZUNIONSTORE를 통한 트렌딩 계산
        - 캐시 TTL 관리

<br />

## __Redis 좋아요 / 최근 1시간 동안 '좋아요' 증가 Top 10 확인__

### 좋아요 Toggle 기능
- 좋아요 등록/취소시 userId 로 유효성 검증 후 h2, redis 에 반영
- Redis 에는 ZSET 으로 likes:bucket:{bucket} 의 Key 를 가지고 있음
- bucket 은 YYYYMMDDHHMM 의 타입을 가지고 5분 단위로 time slice 되어 저장됨
  - member: song_id, score: 좋아요
  - e.g) 2025/09/18 15시 52분 이라면 bucket 은 202509181550 으로 저장됨
  - TTL 은 70분 단위로 설정 (최근 1시간 조회이니 10분 버퍼를 포함해 70분)


### 최근 1시간 동안 '좋아요' 증가 Top 10 조회

- 현재 시간을 기준으로 bucket 을 구한 후 이전 한시간 범위까지 조회한다.
- e.g) 현재시간 bucket 이 202509181550 이라면 202509181450 ~ 202509181550 까지의 bucket 조회 후 ZUNIONSTORE 반환

<br />

## __API EndPoint__

### __SongLikeController__

### __POST /api/v1/songs/{songId}/likes__ 

노래에 대한 좋아요를 추가하거나 취소하는 토글 API입니다.

request
  - songId (Path Variable): 좋아요할 노래 ID
  - userId (Request Param): 사용자 ID

response
```json
{
    "songId": 1001,
    "songTitle": "Bohemian Rhapsody",
    "totalLikes": 1543,
    "liked": true,
    "actionAt": "2024-01-15T14:35:22"
}
```

<br />

### __GET /api/v1/songs/trending/likes__

최근 1시간 동안 좋아요가 가장 많이 증가한 노래 Top 10을 조회합니다.

response

```json
[
    {
        "songId": 3003,
        "title": "Shape of You",
        "artistName": "Ed Sheeran",
        "likeIncreaseCount": 234,
        "currentTotalLikes": 5678
    },
    {
        "songId": 1001,
        "title": "Bohemian Rhapsody", 
        "artistName": "Queen",
        "likeIncreaseCount": 189,
        "currentTotalLikes": 1543
    }
    // ... 최대 10개
]
```

<br />

### __AlbumStatisticsController__

### __GET /api/v1/album-statistics?year=2023&page=0&size=20__

특정 연도의 아티스트별 앨범 발매 수를 조회합니다.

request
- year (Required): 조회할 연도
- page (Optional): 페이지 번호 (기본값: 0)
- size (Optional): 페이지 크기 (기본값: 10, 최대: 100)

response 

```json
{
    "content": [
        {
            "artistName": "Taylor Swift",
            "albumCount": 3,
            "year": 2023
        },
        {
            "artistName": "Drake",
            "albumCount": 2,
            "year": 2023
        },
        {
            "artistName": "BTS",
            "albumCount": 2,
            "year": 2023
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20,
        "sort": {
            "sorted": true,
            "ascending": false
        }
    },
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "numberOfElements": 20
}
```