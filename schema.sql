-- FitMap Database Schema
-- PostgreSQL 16 + PostGIS 3.x

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    email                 VARCHAR(255) UNIQUE NOT NULL,
    name                  VARCHAR(100),
    profile_image_url     TEXT,

    -- 소셜 로그인
    provider              VARCHAR(20)  NOT NULL,   -- kakao | naver | google
    provider_id           VARCHAR(100) NOT NULL,
    provider_access_token TEXT,
    provider_refresh_token TEXT,
    UNIQUE (provider, provider_id),

    -- 약관 동의
    terms_agreed_at       TIMESTAMPTZ,
    privacy_agreed_at     TIMESTAMPTZ,

    role                  VARCHAR(20)  NOT NULL DEFAULT 'USER',

    -- 온보딩 정보
    interest_category     VARCHAR(100),              -- 관심 업종
    interest_sido         VARCHAR(50),               -- 관심 지역 (시도)
    budget_range          VARCHAR(50),               -- 창업 예산 범위

    created_at            TIMESTAMPTZ DEFAULT now(),
    last_login_at         TIMESTAMPTZ,
    deleted_at            TIMESTAMPTZ
);

-- ============================================================
-- 2. FACILITIES
-- ============================================================
CREATE TABLE facilities (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(100),                    -- 체력단련장, 태권도 등
    category      VARCHAR(100),                    -- 체력단련장업, 체육교습업 등
    status        VARCHAR(20),                     -- 정상운영 | 폐업
    closed_at     DATE,

    lat           DOUBLE PRECISION,
    lng           DOUBLE PRECISION,
    geom          GEOGRAPHY(POINT, 4326),          -- PostGIS 공간 컬럼

    sido          VARCHAR(50),
    sigungu       VARCHAR(50),
    road_addr     TEXT,
    floor         INTEGER,
    area_m2       BIGINT,

    is_public     BOOLEAN DEFAULT false,
    is_free       BOOLEAN,
    open_weekday  VARCHAR(30),                     -- '09:00~22:00'
    open_weekend  VARCHAR(30),
    capacity      INTEGER,

    source        VARCHAR(20),                     -- sfms | public_open
    source_id     VARCHAR(100),                    -- 원본 데이터 ID (upsert 기준)
    kakao_place_id VARCHAR(50),

    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);

-- 공간 인덱스 (반경/bbox 쿼리)
CREATE INDEX idx_facilities_geom    ON facilities USING GIST(geom);
-- 일반 쿼리용
CREATE INDEX idx_facilities_sido    ON facilities(sido, sigungu);
CREATE INDEX idx_facilities_status  ON facilities(status);
CREATE INDEX idx_facilities_category ON facilities(category);

-- ============================================================
-- 3. REGION_STATS (배치 집계 캐시)
-- ============================================================
CREATE TABLE region_stats (
    id            BIGSERIAL PRIMARY KEY,
    sido          VARCHAR(50) NOT NULL,
    sigungu       VARCHAR(50) NOT NULL,
    category      VARCHAR(100) NOT NULL,
    total_count   INTEGER DEFAULT 0,
    active_count  INTEGER DEFAULT 0,
    closed_count  INTEGER DEFAULT 0,
    closure_rate  NUMERIC(5,2),                   -- 0.00 ~ 100.00
    public_ratio  NUMERIC(5,2),
    updated_at    TIMESTAMPTZ DEFAULT now(),
    UNIQUE (sido, sigungu, category)
);

-- ============================================================
-- 4. ANALYSES (분석 입력)
-- ============================================================
CREATE TABLE analyses (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    category    VARCHAR(100),                      -- 분석 업종
    lat         DOUBLE PRECISION NOT NULL,
    lng         DOUBLE PRECISION NOT NULL,
    address     TEXT,
    radius_m    INTEGER NOT NULL,                  -- 500 | 1000 | 3000 | 5000
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_analyses_user ON analyses(user_id);

-- ============================================================
-- 5. REPORTS (입지 리포트)
-- ============================================================
CREATE TABLE reports (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    analysis_id       UUID NOT NULL REFERENCES analyses(id),
    score             SMALLINT,                    -- 0 ~ 100
    grade             CHAR(1),                     -- A | B | C | D | E
    competitor_count  INTEGER,
    closure_rate      NUMERIC(5,2),
    public_ratio      NUMERIC(5,2),
    summary_json      JSONB,                       -- 항목별 점수 상세
    pdf_url           TEXT,
    data_snapshot_at  TIMESTAMPTZ,                 -- 리포트 생성 당시 데이터 기준일
    is_paid           BOOLEAN DEFAULT false,
    created_at        TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_reports_analysis ON reports(analysis_id);

-- ============================================================
-- 6. REPORT_COMPETITORS (경쟁 시설 상세)
-- ============================================================
CREATE TABLE report_competitors (
    id          BIGSERIAL PRIMARY KEY,
    report_id   UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    facility_id BIGINT NOT NULL REFERENCES facilities(id),
    distance_m  INTEGER
);

CREATE INDEX idx_report_competitors_report ON report_competitors(report_id);

-- ============================================================
-- 7. PAYMENTS (결제)
-- ============================================================
CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    bundle_type     SMALLINT NOT NULL,             -- 1 | 3 | 5
    total_credits   SMALLINT NOT NULL,             -- bundle_type과 동일
    amount          INTEGER NOT NULL,              -- 원 단위
    pg_tx_id        VARCHAR(200),                  -- PG 트랜잭션 ID (더미 단계: UUID)
    pg_provider     VARCHAR(50) DEFAULT 'dummy',   -- toss | iamport | dummy
    status          VARCHAR(20) DEFAULT 'paid',    -- paid | failed | refunded
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_payments_user ON payments(user_id);

-- ============================================================
-- 8. CREDITS (크레딧 행 단위 추적 - Option A)
-- ============================================================
CREATE TABLE credits (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     BIGINT NOT NULL REFERENCES users(id),
    payment_id  UUID NOT NULL REFERENCES payments(id),
    report_id   UUID REFERENCES reports(id),       -- null = 미사용
    used_at     TIMESTAMPTZ                        -- null = 미사용
);

CREATE INDEX idx_credits_user        ON credits(user_id);
CREATE INDEX idx_credits_payment     ON credits(payment_id);
CREATE INDEX idx_credits_unused      ON credits(user_id) WHERE used_at IS NULL;


-- ============================================================
-- 9. TERMS (약관)
-- ============================================================
CREATE TABLE IF NOT EXISTS terms (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    version     VARCHAR(20)  NOT NULL DEFAULT 'v1.0',
    is_required BOOLEAN NOT NULL DEFAULT true,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ============================================================
-- 10. USER_TERMS_AGREEMENTS (유저-약관 동의 내역)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_terms_agreements (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    terms_id   BIGINT NOT NULL REFERENCES terms(id),
    agreed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, terms_id)
);

CREATE INDEX IF NOT EXISTS idx_user_terms_user ON user_terms_agreements(user_id);
