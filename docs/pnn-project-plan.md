# 프로젝트 계획서 (v0.1)

# PNN Project - 프로젝트 계획서 (v0.1)

## 1. 프로젝트 개요 (Project Overview)

### 1.1 서비스 정의

본 프로젝트(PNN: Pill & Nutrient Navigator)는 사용자가 복용 중인 **전문/일반 의약품(Prescription Drugs)**과 **건강기능식품(Supplements)** 간의 데이터 교차 분석을 통해, **약물-영양제 상호작용(부작용/시너지)**을 사전에 판별하고 **개인화된 영양 성분 섭취 가이드**를 제공하는 웹 서비스이다. **처방약-영양제 상호작용(Case A/B)**은 LLM 기반으로 분석하며, **병용금기/DUR 데이터**는 낱알식별·약 검색 결과 표시용으로만 사용한다.

### 1.2 핵심 가치 (Value Proposition)

- **안전**: 처방약과 영양제의 충돌(흡수 방해, 과잉 작용 등)을 사전에 경고
- **편의**: 영양성분표 촬영만으로 보유 영양제 등록, 처방약은 약 모양/색상으로 쉽게 검색
- **개인화**: 장기 처방약으로 인한 영양소 결핍을 분석하여 맞춤 성분 추천 + 구매 연결

### 1.3 면책 조항 (Disclaimer)

> 본 서비스는 정보 제공 목적이며, 의료·약사 상담을 대체하지 않습니다. 복용 결정은 반드시 의료진과 상담하세요.
> 

*(상세 문구는 추후 법률/규제 검토 후 확정)*

### 1.4 범위 제외 — 사용자·로그인·보안 (2026-03-28 확정)

본 프로젝트 **현재 릴리스 범위**에는 다음을 **포함하지 않는다**.

- 회원가입·로그인·로그아웃, JWT/세션 기반 **사용자 인증**
- 서버에 사용자 계정을 저장하고 세션을 유지하는 **회원 시스템**
- Spring Security를 이용한 **엔드포인트별 인가(역할·권한)** 를 전제로 한 API 설계

처방약·영양제 목록 등은 **클라이언트 로컬 상태**(예: 모바일 앱 내 스토어)로만 유지한다. DB에 `users`, `user_drugs` 등 **회원·이력용 스키마가 존재하더라도** 본 범위에서는 **비즈니스 로직에 연결하지 않는다**(향후 제품 확장 시 별도 검토).

내부망 **Spring ↔ Python** 연동에 대한 API Key 등은 배포 환경에서 필요 시 별도로 정하되, **사용자 로그인과는 무관**하다.

---

## 2. 핵심 비즈니스 로직 (Core Business Logic)

### 2.1 Case A: 기복용 영양제 기반 신규 처방약 충돌 검사 (Personalized Conflict Check)

### 개념

평소 영양제를 섭취하던 사용자가 질병으로 인해 새로운 처방약을 복용하게 되었을 때, 두 약물 간의 상호작용을 분석하여 **일시적 복용 중단 또는 병용 지침(Action Plan)**을 제공한다.

### 데이터 흐름 (Protocol Flow)

> **설계 결정 (2026-03-13)**: drug-nutrient 상호작용 데이터를 구하는 것이 불가능하므로, Case A는 **LLM 전담**으로 설계한다. dur_rules, drug_contraindication은 drug-drug 데이터이며 처방약-영양제 비교에 사용하지 않는다.

```
[사용자]
  │
  ├─ (1) 영양성분표 이미지 촬영 (Client)
  │       └─ 이미지 압축 후 서버 전송
  │
  ▼
[Spring Boot - 메인 백엔드]
  │
  ├─ (2) Python AI 서버로 OCR + 정형화 요청
  │       ├─ PaddleOCR-VL-1.5 (로컬) → 텍스트 추출
  │       └─ Gemini 3 Flash → JSON 정형화 (성분명은 반드시 영문으로 출력)
  │
  ├─ (3) 정형화된 영양제 데이터(JSON) 수신 및 저장 (user_supplements.nutrients, 영문)
  │
  ├─ (4) 사용자가 식별/선택한 신규 처방약 → drugId(drugs_master.id) 확보
  │       └─ **전제**: 낱알식별 또는 약 검색 API로 drugId를 먼저 획득해야 함
  │
  ├─ (5) drugId → drugs_master 조회 → item_seq → drug_ingredients.ingr_name_eng 조회 → [영문 성분 목록]
  │       └─ drug_ingredients 없음 → 약품명만 LLM에 전달 → 안내문 제공
  │
  ├─ (6) Python AI 서버로 상호작용 분석 요청 (Rule-based 필터 없음)
  │       └─ drug 정보(이름, 성분) + supplements(nutrients) → LLM 분석
  │
  ▼
[응답]
  └─ (7) 결과 반환: '병용 안전' / '시간 간격 필요' / '복용 기간 내 중단 권장' 등
```

### Use Case 예시

| 항목 | 내용 |
| --- | --- |
| **사용자 상태** | 종합비타민(마그네슘, 칼슘 포함) 복용 중 |
| **신규 처방약** | 테트라사이클린계 항생제 (감기/염증) |
| **분석 결과** | "항생제의 흡수율을 마그네슘/칼슘이 방해합니다(Antagonist)." |
| **최종 가이드** | `[주의] 처방약 복용 기간(3일) 동안 해당 영양제 섭취를 일시 중단하거나, 최소 2시간의 간격을 두고 섭취하십시오.` |

### 결과 분류 체계

| 등급 | 라벨 | 설명 | 색상(UI) |
| --- | --- | --- | --- |
| SAFE | 병용 안전 | 알려진 상호작용 없음 | 초록 |
| CAUTION | 시간 간격 필요 | 동시 복용 시 흡수 방해 가능, 시간 분리로 해결 | 노랑 |
| WARNING | 복용 기간 내 중단 권장 | 처방약 복용 기간 동안 해당 영양제 일시 중단 권장 | 빨강 |
| SYNERGY | 시너지 | 병용 시 긍정적 상호작용 기대 | 파랑 |

---

### 2.2 Case B: 기복용 처방약 기반 안전 영양제 추천 (Safe Supplement Recommendation)

### 개념

처방약을 복용 중인 사용자가 새로운 영양제를 구매하고자 할 때, LLM이 **피해야 할 영양 성분을 경고**하고 **안전하고 유익한 영양 성분을 추천** + 외부 커머스 검색 결과로 **딥링크(Deep Linking)**한다.

> **설계 결정 (2026-03-13)**: Case B도 **LLM 전담**으로 설계한다. dur_rules, drug_contraindication은 drug-drug 데이터이며 처방약-영양제 추천에 사용하지 않는다.

### 데이터 흐름 (Protocol Flow)

```
[사용자]
  │
  ├─ (1) 기복용 처방약 리스트 확인 요청 + 영양제 구매 의도
  │
  ▼
[Spring Boot - 메인 백엔드]
  │
  ├─ (2) drugIds(drugs_master.id 목록) 확보
  │       └─ **전제**: 낱알식별 또는 약 검색 API로 drugId를 먼저 획득해야 함
  │
  ├─ (3) 각 drugId → drugs_master → item_seq → drug_ingredients.ingr_name_eng 조회 → [영문 성분 목록]
  │
  ├─ (4) 처방약 정보(이름, 성분) → Python AI 서버로 추천 요청
  │       └─ LLM 기반 금기 성분 경고 + 안전 영양 성분 추천 + 사유 생성
  │
  ├─ (5) 추천 성분명 → URLEncoder → 제휴 쇼핑몰 동적 검색 URL 생성
  │
  ▼
[응답]
  └─ (6) 금기 성분 경고 + 추천 성분/사유 + 외부 커머스 딥링크 제공
```

### Use Case 예시

| 항목 | 내용 |
| --- | --- |
| **사용자 상태** | 고혈압 약(ACE 억제제 계열) 복용 중, 영양제 구매 희망 |
| **경고** | "칼륨 함유 영양제는 ACE 억제제와 병용 시 고칼륨혈증 위험이 있어 피해야 합니다." |
| **추천 성분** | 마그네슘, 오메가-3 |
| **추천 사유** | "현재 복용 중인 약물과 상호작용 없이 안전하게 섭취할 수 있는 성분입니다." |
| **CTA 버튼** | `아이허브에서 마그네슘 검색하기` → `https://kr.iherb.com/search?kw=마그네슘` |

---

## 3. 시스템 아키텍처 (System Architecture)

### 3.1 전체 구조도

**클라이언트 (Web Browser)**

- 사용자가 접근하는 웹 화면
- HTTPS로 Spring Boot API만 호출 (유일한 진입점)

↓

**Spring Boot 3.4.x (Java 17, Gradle) — 메인 백엔드**

- 처방약 검색 (낱알식별: 모양/색/각인) — dur_rules, drug_contraindication 연동
- 기복용 처방약·영양제 목록은 **클라이언트 로컬 상태**로 유지 (서버 회원/프로필 저장 없음)
- Case A/B: drug 정보 수집 후 Python AI 서버로 전달 (LLM 전담)
- RestClient (Raw HTTP)로 Python AI 서버 호출
- RestClient로 외부 API 호출 (공공데이터 등)

**↓** (내부망 REST API)

**Python AI Server (FastAPI)**

- OCR 파이프라인: PaddleOCR-VL-1.5(로컬)로 텍스트 추출 → Gemini 3 Flash로 JSON 정형화
- Case A: 처방약 성분 + 영양제 성분 → LLM 상호작용 분석
- Case B: 처방약 성분 → LLM 금기 경고 + 안전 성분 추천
- 영양제 제품명 매칭: 사용자 입력/OCR 결과 → LLM으로 제품 후보 추론
- (향후) 전용 경량 모델 (NER, 분류 등)

↓

**PostgreSQL (16+)**

- 정형 테이블: 의약품 마스터, 상호작용 룰 등 (회원·이력 테이블은 스키마만 두고 본 범위에서는 미사용 가능)
- JSONB 컬럼: OCR 추출 원문, 분석 결과 로그
- pgvector 확장: 상호작용 문헌의 임베딩 벡터 저장 및 유사도 검색

---

**핵심 포인트 요약**

- 클라이언트 → Spring만 호출 (Python은 외부에 안 보임)
- Spring → Python은 내부망에서만 REST로 통신
- DB는 PostgreSQL 하나로 정형 + JSONB + 벡터 모두 관리

---

### 3.2 레이어별 역할 분담

| 레이어 | 역할 | 비고 |
| --- | --- | --- |
| **Spring Boot** | 처방약 검색·낱알식별(dur_rules/drug_contraindication 연동), drug 정보 수집, Python 호출 오케스트레이션, 응답 조합/가공, DB 트랜잭션 (**회원·JWT 범위 제외**) | 클라이언트의 **유일한 진입점** |
| **Python AI Server** | OCR 파이프라인(PaddleOCR-VL-1.5 + Gemini 3 Flash 정형화), Case A/B LLM 분석(Gemini 3 Flash), 영양제 제품명 매칭, 향후 전용 모델 | Spring에서만 호출 (내부망) |
| **PostgreSQL** | 정형 데이터(의약품 마스터 등), JSONB(OCR 결과 등), pgvector(향후 확장) | Spring·Python 양쪽에서 접근 |

### 3.3 설계 원칙

- **단일 진입점**: 클라이언트는 Spring Boot API만 호출하며, Python 서버 URL은 외부에 노출하지 않는다.
- **외부 라이브러리 최소 의존**: Spring 측은 RestClient를 활용한 Raw HTTP 통신으로, 변동성이 큰 외부 AI 라이브러리 의존성을 배제하고 시스템 안정성을 극대화한다.
- **관심사 분리**: 도메인/트랜잭션은 Spring, AI/ML은 Python으로 명확히 분리한다.
- **Graceful Degradation**: Python 서버 장애/타임아웃 시 Spring은 "의사/약사 상담 권장" 등 안내 메시지로 fallback한다.

---

## 4. 기술 스택 상세 (Technology Stack)

### 4.1 Backend (Spring Boot)

| 항목 | 선택 | 이유 |
| --- | --- | --- |
| Framework | Spring Boot 3.4.x | 안정적 LTS, RestClient 네이티브 지원 |
| Language | Java 17.0.12 LTS (로컬 확인 완료) | 장기 지원, 레코드/패턴매칭 등 모던 문법 |
| Build Tool | Gradle (Kotlin DSL, Wrapper 사용) | 유연한 빌드 설정, 멀티모듈 확장 용이. 시스템 설치 불필요 |
| HTTP Client | RestClient (Spring 6.1+) | 외부 AI 라이브러리 의존 없이 HTTP 직접 통신 |
| ORM | Spring Data JPA + Hibernate | PostgreSQL 매핑, JSONB 타입 지원 |
| 인증/보안 | **사용자 로그인·JWT는 범위 제외** | Spring Security는 최소 설정(예: CSRF 등)만 유지하거나 개발 편의 위주. 배포 시 내부망·API Key 등은 별도 검토 |

### 4.2 AI Server (Python)

| 항목 | 선택 | 이유 |
| --- | --- | --- |
| Framework | FastAPI | 비동기 지원, 자동 OpenAPI 문서, 타입 힌트 |
| Language | Python 3.14.2 (로컬 확인 완료) | AI/ML 생태계 최적 |
| LLM (Case A/B) | Gemini 3 Flash (gemini-3-flash-preview) | google-generativeai SDK. 속도·가성비·추론 균형. Input $0.50/M, Output $3/M |
| OCR | PaddleOCR-VL-1.5 (로컬) | 무료 로컬 모델. 영양성분표 텍스트 추출 |
| 벡터 검색 | psycopg + pgvector | PostgreSQL 내 벡터 검색, 별도 DB 불필요 |
| 임베딩 | (미사용) | 향후 pgvector 확장 시 OpenAI 또는 Google 임베딩 검토 |

### 4.3 Database

| 항목 | 선택 | 이유 |
| --- | --- | --- |
| RDBMS | PostgreSQL 18.1 (로컬 확인 완료) | JSONB 동시 활용, 하나의 DB로 통합 |
| 벡터 확장 | pgvector | (현재 미사용) 향후 기능 확장을 대비해 유지 |
| 인덱스 전략 | HNSW (pgvector) | 근사 최근접 이웃 검색, 속도/정확도 균형 |

### 4.4 외부 API / 데이터 소스

| API | 용도 | 호출 주체 |
| --- | --- | --- |
| 공공데이터포털 - 의약품 낱알식별 정보 | 처방약 DB 배치 적재 (모양/색/각인/이니셜) | Spring (Batch) |
| PaddleOCR-VL-1.5 | 영양성분표 이미지 → 텍스트 추출 (로컬 무료) | Python |
| Google Gemini 3 Flash API | OCR 텍스트 → JSON 정형화, Case A/B 상호작용 심층 분석, 제품명 추론 | Python |
| 제휴 커머스 (아이허브 등) | 추천 성분 → 검색 URL 딥링크 생성 | Spring (URL 생성만) |

### 4.5 로컬 개발 환경 현황 (2026-03-05 확인)

| 도구 | 버전 | 상태 | 비고 |
| --- | --- | --- | --- |
| Java (JDK) | 17.0.12 LTS | 설치 완료 | Oracle JDK |
| Python | 3.14.2 | 설치 완료 | pip 26.0.1 포함 |
| PostgreSQL | 18.1 | 설치 완료, 실행 중 | 포트 5432, 경로: `C:\Program Files\PostgreSQL\18` |
| Node.js | 24.13.0 | 설치 완료 | 향후 프론트엔드(Expo) 대비 |
| Git | 2.51.0 | 설치 완료 | |
| Gradle | 미설치 (정상) | Wrapper 사용 예정 | Spring Boot 프로젝트 생성 시 자동 포함 |
| IDE | Cursor (VS Code 기반) | 사용 중 | 확장: Extension Pack for Java, Spring Boot Extension Pack, Python 설치 필요 |

---

## 5. 사용자 입력 및 프로필 관리

### 5.1 처방약 등록 (약 검색 및 낱알 식별)

처방약-영양제 분석(Case A/B)을 위해서는 처방약의 고유 식별자(`drugId`)가 필수적이므로, 다음과 같은 2가지 검색 방식을 제공한다. (2026-03-13 상세 설계 확정)

#### 5.1.1 약품 상세 검색 API (`GET /api/drugs/search/detail`)

- **목적**: 텍스트 입력 기반 검색
- **검색 조건**: `itemName`(제품명), `entpName`(제조사), `ingredient`(성분명 - 한/영)
  - 최소 1개 이상 조건 필수 입력. 2개 이상 입력 시 **AND 조건**으로 결합.
- **응답**: `drugId`, `itemName`, `entpName`, `itemImageUrl` (리스트 뷰에 필요한 최소 정보만 반환)
- **특징**: 성분명 검색 시 `drug_ingredients` 테이블을 동적으로 조인하여 검색 (QueryDSL 활용).

#### 5.1.2 낱알 식별 검색 API (`GET /api/drugs/search/pillIdentifier`)

- **목적**: 알약의 물리적 외형 정보 기반 검색
- **검색 조건**:
  - `printFront`, `printBack` (식별 문자)
  - `markCode` (식별 마크) — 이미지 URL이 아닌 고유 식별 코드로 검색하여 성능/정확도 확보
  - `drugShape` (모양)
  - `color` (색상)
  - `line` (분할선)
  - `formulation` (제형) — 추후 `form_code_name`을 3~4개 카테고리로 정규화(`normal_form_name` 컬럼 추가) 후 필터로 적용
- **검색 로직**:
  - 서로 다른 카테고리(예: 모양과 색상) 간에는 **AND 결합**.
  - 앞/뒤 구분이 모호한 속성(문자, 색상, 분할선, 마크)은 내부적으로 **OR 결합** (예: `color_front LIKE '%하양%' OR color_back LIKE '%하양%'`).
- **응답**: 상세 검색 API와 동일한 간소화된 결과 반환.

#### 5.1.3 프로필 등록 플로우

```
사용자가 검색 조건 입력 → Spring API(상세 검색 또는 낱알 식별) 호출 → 후보 약 목록(리스트) 반환
→ 사용자가 리스트에서 특정 약 선택 → 상세 페이지 이동 (dur_rules, drug_contraindication 경고 확인)
→ "이 약이다" 선택 → 프로필에 추가 (기복용 처방약)
```

### 5.2 영양제 등록

### 제품 식별 (2가지 경로)

| 경로 | 방법 | 처리 |
| --- | --- | --- |
| **텍스트 입력** | 사용자가 제품명 직접 입력 | Spring → Python `/match-supplement` → LLM이 제품 후보 추론 → 사용자 확인 |
| **제품명 촬영** | 제품 라벨/포장 사진 촬영 | Spring → Python → PaddleOCR-VL-1.5 → Gemini 3 Flash 제품명 추출 → 사용자 확인 |

### 영양성분 등록

```
영양성분표 촬영 (Client)
→ Spring → Python OCR Pipeline
  → PaddleOCR-VL-1.5: 이미지 → Raw Text
  → Gemini 3 Flash: Raw Text → 정형 JSON (성분명은 반드시 영문으로 출력)
    {
      "product_name": "○○ 종합비타민",
      "servings": "1정",
      "nutrients": [
        { "name": "Vitamin C", "amount": 500, "unit": "mg", "daily_value_pct": 500 },
        { "name": "Magnesium", "amount": 150, "unit": "mg", "daily_value_pct": 47 },
        ...
      ]
    }
→ Spring: JSON 수신 → user_supplements 저장 (nutrients JSONB, 영문)
→ interaction_rules 조회 시 그대로 사용 (한/영 매핑 테이블 불필요)
```

---

## 6. Spring ↔ Python API 인터페이스 (초안)

### 6.1 OCR + 정형화

```
POST /api/ai/ocr/extract
Content-Type: multipart/form-data

Request:
  - image: (file) 영양성분표 이미지

Response (200):
  {
    "product_name": "string | null",
    "raw_text": "string",
    "nutrients": [
      { "name": "string", "amount": number, "unit": "string", "daily_value_pct": number }
    ],
    "confidence": 0.95
  }
```

### 6.2 상호작용 분석 (Case A 심층)

```
POST /api/ai/interaction/analyze
Content-Type: application/json

Request:
  {
    "drug": { "id": "string", "name": "string", "ingredients": ["string"] },  // ingredients: drug_ingredients.ingr_name_eng (영문)
    "supplements": [
      { "name": "string", "nutrients": [{ "name": "string", "amount": number, "unit": "string" }] }  // name: 영문
    ]
  }

Response (200):
  {
    "interactions": [
      {
        "nutrient": "Magnesium",
        "contraindicated_drug_ingredient": "Tetracycline",
        "level": "CAUTION | WARNING | SAFE | SYNERGY",
        "description": "string",
        "action_guide": "string",
        "sources": ["string"]
      }
    ]
  }
```

### 6.3 안전 영양 성분 추천 (Case B)

```
POST /api/ai/recommendation/safe-nutrients
Content-Type: application/json

Request:
  {
    "drugs": [
      { "id": "string", "name": "string", "ingredients": ["string"] }
    ],
    "contraindicated_nutrients": [
      { "nutrient": "string", "level": "CAUTION | WARNING", "reason": "string" }
    ]
  }

Response (200):
  {
    "warnings": [
      { "nutrient": "칼륨", "reason": "ACE 억제제와 병용 시 고칼륨혈증 위험", "level": "WARNING" }
    ],
    "recommendations": [
      {
        "nutrient": "마그네슘",
        "reason": "현재 복용 약물과 상호작용 없이 안전하게 섭취 가능한 성분입니다.",
        "priority": "HIGH | MEDIUM | LOW",
        "sources": ["string"]
      }
    ]
  }
```

### 6.4 영양제 제품명 매칭

```
POST /api/ai/supplement/match
Content-Type: application/json

Request:
  {
    "query": "string",         // 사용자 입력 텍스트 또는 OCR 추출 제품명
    "query_type": "TEXT | OCR"
  }

Response (200):
  {
    "candidates": [
      { "name": "string", "brand": "string", "confidence": number }
    ]
  }
```

---

## 7. 데이터베이스 설계 (실제 데이터 기반)

### 7.0 데이터 소스 현황 (2026-03-12 기준, 공공데이터 전환 완료)

| 소스 | 파일/API | 건수 | 핵심 필드 |
| --- | --- | --- | --- |
| 의약품 낱알식별 | `의약품 낱알식별.csv` | 27,686 | item_seq, item_name, drug_shape, color_front/back, print_front/back |
| 의약품 주성분 | `의약품 상세조회(주성분).csv` | 43,584 | item_seq, ingr_name_eng, ingr_name_kr |
| 의약품 제품허가 상세 | `의약품 제품허가 상세정보.csv` | 44,089 | item_seq, efficacy, dosage, caution, ingr_name_eng (PDF 파싱) |
| DUR 유형별 성분 | `DUR유형별 성분 현황_*.csv` 8종 | 4,623 | dur_ingr_name_eng, contraind_dur_ingr_name_eng, dur_type |
| 병용금기약물 | `한국의약품안전관리원_병용금기약물_20240625.csv` | 542,996 | ingr_name_1, ingr_name_2, contraind_reason |
| e약은요정보 | `e약은요정보.csv` | 4,708 | item_seq, efficacy, dosage, caution_use (상비약 복약정보) |
| 약가마스터 | `약가마스터.csv` | 약 300,000 | insur_code, main_ingr_code, atc_code (심평원 기준 코드) |

**폐기**: SUPP.AI 기반 interaction_rules (품질 이슈, 매칭 사각지대 1,165개)

### 7.1 정형 테이블 (PostgreSQL, 2026-03-12 공공데이터 기반)

| 테이블 | 주요 필드 | 데이터 소스 | 비고 |
| --- | --- | --- | --- |
| `drugs_master` | id, item_seq(UNIQUE), item_name, drug_shape, color_front/back, print_front/back, line_front/back, normal_form_name | 의약품 낱알식별.csv | 의약품 마스터. item_seq가 모든 테이블 연결 키 |
| `drug_ingredients` | id, item_seq(FK), ingr_name_eng, ingr_name_kr | 의약품 상세조회(주성분).csv | 주성분. AI 입력용, 낱알식별·검색 시 DUR·병용금기 표시용 |
| `drug_permit_detail` | id, item_seq, efficacy, dosage, caution, ingr_name_eng, item_eng_name | 의약품 제품허가 상세정보.csv | 효능효과·용법용량·주의사항 (PDF 파싱) |
| `dur_rules` | id, dur_ingr_name_eng, contraind_dur_ingr_name_eng, dur_type | DUR유형별 성분 현황 8종 | **낱알식별·검색 결과**에서 DUR 경고 표시용 (Case A/B 미사용) |
| `drug_contraindication` | id, ingr_name_1, ingr_name_2, contraind_reason | 한국의약품안전관리원_병용금기약물 | **낱알식별·검색 결과**에서 병용금기 표시용 (Case A/B 미사용) |
| `drug_easy_info` | id, item_seq(UNIQUE), efficacy, dosage, caution_use | e약은요정보.csv | 상비약 복약정보 |
| `drug_price_master` | id, insur_code, main_ingr_code, atc_code | 약가마스터.csv | 심평원 약가마스터 기준 코드 |
| `users` | id, email, password_hash, created_at | 직접 입력 | 사용자 계정 |
| `user_drugs` | id, user_id(FK), item_seq(FK→drugs_master), start_date, is_active | 직접 입력 | 사용자 기복용 처방약 |
| `user_supplements` | id, user_id(FK), supplement_name, nutrients(JSONB), registered_at | OCR + 직접 입력 | 사용자 기복용 영양제. nutrients는 LLM이 영문으로 출력 |
| `analysis_logs` | id, user_id(FK), case_type(A/B), request(JSONB), response(JSONB), created_at | 시스템 생성 | 분석 이력 로그 |

**DDL**: `data/sql/01_drugs_master.sql` ~ `07_drug_price_master.sql`  
**적재 스크립트**: `data/scripts/load_*.py`, `run_ddl.py`

### 7.2 벡터 테이블 (pgvector, 현재 미사용)

- **현재**: Case A/B는 LLM 전담. dur_rules, drug_contraindication은 낱알식별·검색 전용. pgvector 확장은 향후 기능 확장 대비 유지
- **임베딩 모델**: (미사용) 향후 pgvector 확장 시 OpenAI 또는 Google 임베딩 검토
- **인덱스**: HNSW (데이터 규모에 따라 m/ef_construction 튜닝)

### 7.3 테이블 간 관계 요약

```
drugs_master (item_seq) ─┬─ drug_ingredients (1:N, ingr_name_eng)
                         ├─ drug_permit_detail (1:1)
                         ├─ drug_easy_info (1:1, 상비약만)
                         └─ drug_price_master (1:N, insur_code)

drug_ingredients.ingr_name_eng ─┬─ dur_rules (DUR성분 매칭)
                               └─ drug_contraindication (ingr_name_1/2 매칭)

users ─┬─ user_drugs ── item_seq → drugs_master
       └─ user_supplements (nutrients JSONB)
```

#### 7.3.1 데이터 용도 구분 (2026-03-13 확정)

| 데이터 | 용도 | Case A/B |
|--------|------|----------|
| **drug_contraindication** | 낱알식별·약 검색 결과에서 "이 약과 병용금기인 다른 약" 표시 | 사용 안 함 |
| **dur_rules** | 낱알식별·약 검색 결과에서 DUR 관련 경고 표시 | 사용 안 함 |
| **drugs_master** | 검색, 낱알식별, AI에 약 정보 전달 | 사용 |
| **drug_ingredients** | AI에 약 성분 정보 전달 | 사용 |

> drug-nutrient 상호작용 데이터를 구하는 것이 불가능하므로, Case A/B는 LLM 전담. dur_rules, drug_contraindication은 drug-drug 데이터이며 처방약-영양제 비교에 사용하지 않는다.

### 7.4 JSONB 사용 규칙

- `user_supplements.nutrients`: OCR→LLM 정형화 결과. **name은 반드시 영문** (Vitamin C, Coenzyme Q10 등). interaction_rules 조회 시 그대로 사용
- `analysis_logs.request/response`: 분석 요청/응답 원문 보존
- JSONB 내부 필드 검색이 빈번한 경우 GIN 인덱스 추가

---

## 8. 분석 파이프라인 상세

### 8.1 Case A/B: LLM 전담 (2026-03-13 확정)

```
[Case A] drugId → drugs_master → drug_ingredients.ingr_name_eng → [영문 성분]
         supplements.nutrients → [영문]
         → Python AI 서버로 전달 → LLM 상호작용 분석 → 결과 반환

[Case B] drugIds → drugs_master들 → drug_ingredients.ingr_name_eng
         → Python AI 서버로 전달 → LLM 금기 경고 + 안전 성분 추천 → 결과 반환
```

- **Rule-based 필터 제거**: drug-nutrient 상호작용 데이터를 구하는 것이 불가능하므로 dur_rules, drug_contraindication은 Case A/B에서 사용하지 않음
- **데이터**: drug_ingredients.ingr_name_eng, nutrient 모두 **영문**
- **Fallback**: drug_ingredients 없으면 약품명만 LLM에 전달 → 안내문 제공

### 8.2 dur_rules, drug_contraindication 사용처 (낱알식별·약 검색)

```
[낱알식별/약 검색 결과 화면]
  └─ drugs_master 조회 후, 해당 item_seq의 drug_ingredients.ingr_name_eng로
     dur_rules, drug_contraindication 조회 → "이 약과 병용금기인 다른 약" 등 표시
```

- **용도**: 사용자가 약을 검색하거나 낱알식별로 확인할 때, 해당 약과 병용금기인 다른 의약품 성분을 안내
- **Case A/B와 무관**: 처방약-영양제 비교에는 사용하지 않음

### 8.3 LLM 분석 (Python)

```
(1) 입력: 처방약 정보(이름, 성분) + 영양제 성분 조합
(2) 프롬프트 구성: 시스템 프롬프트 + 분석 요청
(3) Gemini 3 Flash 호출 → 구조화된 JSON 응답 생성
(4) 결과 파싱 및 Spring으로 반환
```

---

## 9. 비기능 요구사항 (Non-Functional Requirements)

### 9.1 성능/안정성

| 항목 | 목표 |
| --- | --- |
| Spring → Python 타임아웃 | OCR: 30초, 분석: 15초, 매칭: 10초 |
| Python 장애 시 fallback | "의사/약사 상담 권장" 또는 "일시적 오류" 안내 |
| 재시도 정책 | 최대 2회, 지수 백오프 (1초, 2초) |

### 9.2 보안

| 항목 | 방침 |
| --- | --- |
| Python 서버 접근 | 내부망 전용, 외부 직접 접근 차단 |
| Spring ↔ Python | 필요 시 API Key 등 **서비스 간** 인증 (배포 환경에서 확정). **사용자 JWT와 무관** |
| 사용자 인증 | **범위 제외** — 회원가입/로그인/JWT 미구현 |

### 9.3 확장성

| 항목 | 방향 |
| --- | --- |
| AI 모델 교체 | Python 서버만 재배포, Spring 무변경 |
| 전용 경량 모델 추가 | Python 서버 내 엔드포인트 추가 |
| 트래픽 증가 시 | Python 서버 수평 확장 (GPU 인스턴스 별도 스케일) |

---

## 10. 개발 로드맵 (초안)

> **구현 순서 (2026-03-13 확정)**: Case A/B는 drugId를 전제로 동작하므로, **검색·낱알식별 → Case A → Case B** 순서로 구현한다.

### Phase 1: 기반 구축 (완료)

- [x]  Spring Boot 프로젝트 초기 세팅 (Gradle, 의존성, 패키지 구조)
- [x]  PostgreSQL 스키마 설계 및 초기 마이그레이션
- [x]  pgvector 확장 설치 및 벡터 테이블 생성
- [x]  Python AI 서버 프로젝트 초기 세팅 (FastAPI)
- [x]  Spring ↔ Python 간 기본 REST 통신 확인 (Health Check)
- [x]  공공데이터포털 의약품 낱알식별 데이터 배치 적재 파이프라인 (2026-03-12 완료: drugs_master 6개 테이블)

### Phase 2: 검색·낱알식별 (Case A/B 전제) (완료)

- [x]  **QueryDSL 환경 구성**: 동적 쿼리 처리를 위한 의존성 및 설정
- [x]  **약 검색 API (`GET /api/drugs/search/detail`)**: 제품명, 제조사, 성분명(drug_ingredients 조인) 기반 검색 기능 구현
- [x]  **낱알 식별 API (`GET /api/drugs/search/pillIdentifier`)**: 모양, 색상, 각인, 마크 코드 등 물리적 특성 기반 동적 쿼리 검색 구현
- [x]  **제형(formulation) 데이터 정규화**: `form_code_name`을 4개 표준 카테고리로 정리하여 DB 업데이트 및 낱알식별 필터에 반영
- [x]  **약품 상세 API (`GET /api/drugs/{drugId}`)**: 검색 목록에서 선택 시 상세 정보 통합 제공 (DB 상호작용 의존성 완전 제거)
- [x]  OCR 파이프라인 (PaddleOCR-VL-1.5 + Qwen 7B 텍스트 정제 + 정규식 JSON 변환)

### Phase 3: Case A 구현 (충돌 검사) (완료)

- [x]  drugId → drugs_master → drug_ingredients 조회 로직 (`InteractionCheckService`)
- [x]  상세정보 없는 drug fallback (AI 서버가 판단)
- [x]  Python AI 서버 상호작용 분석 파이프라인 (`interaction.py`, Gemini 3 Flash 전담)
- [x]  충돌 검사 통합 API (`POST /api/interaction/check`) 연동 완료

### Phase 4: Case B 구현 (추천) (완료)

- [x]  복수 drugIds 처리 로직 (`RecommendationService`)
- [x]  Python AI 서버 안전 영양 성분 추천 파이프라인 (`recommendation.py`, Gemini 3 Flash 전담)
- [x]  환각(Hallucination) 방지 Post-Filter 로직 구현 (추천 목록에서 금기 성분 즉시 폐기)
- [x]  추천 결과 API (`POST /api/v1/recommendations/safe-nutrients`) 연동 완료
- [ ]  딥링크 URL 생성 로직 (프론트엔드 연동 시 최종 형식 확정)

### Phase 5: 사용자/프로필 — **범위 제외 (2026-03-28)**

다음은 **구현하지 않기로 확정**하였다. 과거 로드맵 항목은 참고용으로만 남긴다.

- Spring Security + JWT, 회원가입/로그인/토큰 갱신 API
- 서버 측 기복용 약·영양제 프로필 CRUD, 분석 이력 영구 저장

### Phase 6: 프론트엔드 (추후 결정)

- [ ]  기술 스택 결정 (React/Next.js/기타)
- [ ]  주요 화면 설계 및 구현

---

## 11. 설계 결정 사항

### 2026-03-08 확정

| 항목 | 결정 |
| --- | --- |
| nutrient 저장 형식 | LLM이 OCR→JSON 변환 시 **영문으로 출력** (Vitamin C, Magnesium 등). ingredient_mapping 불필요 |
| 상세정보 없는 drug | drug_ingredients 없으면(상세정보 미적재) 약품명만 LLM에 전달 → 안내문 제공 |
| 복수 drug (Case B) | 여러 처방약 성분에 대해 교집합 회피 목록 생성 |
| 상호작용 등급 (Case B) | 환각 통제 및 보수적 접근을 위해 `WARNING/CAUTION` 필터링 제거. 조회된 모든 룰을 경고 대상으로 간주 |
| 표기 불일치 (Vitamin C vs Ascorbic acid) | 수작업 정제 불가, LLM 비용 부담 → 현실적 리스크 수용 |

### 2026-03-12 확정 (공공데이터 전환)

| 항목 | 결정 |
| --- | --- |
| 기존 DB 폐기 | SUPP.AI 기반 interaction_rules 폐기. Actinium 등 잘못된 데이터, 매칭 사각지대 1,165개 |
| 신규 DB | 공공데이터 6개 테이블: drugs_master, drug_ingredients, drug_permit_detail, dur_rules, drug_contraindication, drug_easy_info |
| 연결 키 | item_seq(품목일련번호) 중심. 모든 테이블 item_seq로 JOIN |
| drug 성분 조회 | drug_ingredients.ingr_name_eng → AI에 전달 (Case A/B) |
| 성분명 정규화 | drug_permit_detail.ingr_name_eng는 `/`로 묶인 복합 문자열 → split 후 단일 성분 단위로 정규화 선행 필요 |

### 2026-03-13 확정 (Case A/B LLM 전담, 데이터 용도 구분)

| 항목 | 결정 |
| --- | --- |
| Case A/B 상호작용 | drug-nutrient 데이터 구득 불가 → **LLM 전담**. Rule-based 필터 제거 |
| dur_rules, drug_contraindication | **낱알식별·약 검색 결과**에서만 사용 (이 약과 병용금기인 다른 약 표시). Case A/B에서는 사용 안 함 |
| 구현 순서 | 검색·낱알식별 → Case A → Case B (drugId 획득 경로가 선행되어야 함) |

### 2026-03-14 ~ 15 확정 (상세 검색 및 데이터 모델 보강)

| 항목 | 결정 |
| --- | --- |
| 제형(form_code_name) 정규화 | 4개 표준 카테고리(정제, 캡슐, 액상, 기타)로 분류. `drugs_master`에 `normal_form_name` 컬럼 추가. |
| 데이터 매핑 세분화 | 상세 페이지 구현을 위해 `drug-detail-page-data-mapping.md` 작성 및 `drug_price_master` 테이블 신규 구축. |

### 2026-03-16 확정 (AI 스택: PaddleOCR + Gemini 3 Flash)

| 항목 | 결정 |
| --- | --- |
| OCR | 로컬 무료 모델 `PaddleOCR-VL-1.5` 사용. Google Vision API 대체. |
| Case A/B LLM | `Gemini 3 Flash` (gemini-3-flash-preview) 사용. Google AI Studio API 키 및 300달러 크레딧 활용. |
| Python SDK | `google-genai` 패키지. Ollama 기반 기존 로직 제거. |

### 2026-03-17 확정 (OCR 파이프라인 최적화 및 DB 상호작용 종속 제거)

| 항목 | 결정 |
| --- | --- |
| OCR 파이프라인 | `PaddleOCR-VL-1.5`(텍스트 스캔) → `Qwen 7B`(텍스트 정제) → `Python 정규식(Regex)`(JSON 파싱)의 3단계로 확정. 성능과 가벼움, 모델의 한계 극복을 위한 최적안. |
| DB 상호작용 로직 제거 | Spring Boot의 `DrugDetailService`에서 `drug_contraindication`, `dur_rules` 테이블을 직접 조회하던 레거시 로직 전면 삭제. **상호작용 검증은 100% LLM 전담**. |
| 영양제 매칭(supplement.py) | 한국 데이터에 국한된 DB 한계로 인해 글로벌 활용성이 떨어지므로, 해당 엔드포인트는 주석 처리 후 추후 고도화 단계로 보류함. |

---

## 12. 미결정 사항 (Open Items)

| # | 항목 | 현재 상태 | 결정 시점 |
| --- | --- | --- | --- |
| 1 | 프론트엔드 기술 스택 | 미정 | Phase 5 전 |
| 2 | 사용자 인증 | **범위 제외** (로그인·JWT 미구현) | 2026-03-28 확정 |
| 3 | Spring ↔ Python 서비스 간 인증 | API Key 등 (배포 시) | 배포 전 |
| 4 | Python 프레임워크 (FastAPI vs Flask) | FastAPI 추천, 미확정 | Phase 1 |
| 5 | 임베딩 모델/차원 (small vs large) | 미정 | Phase 1 |
| 6 | 면책/규제 문구 상세 | 추후 | 서비스 출시 전 |
| 7 | 제휴 커머스 목록 및 딥링크 형식 | 아이허브 가안 | Phase 3 |
| 8 | 배포 환경 (클라우드/온프레미스, 컨테이너) | 미정 | Phase 1~2 |
| 9 | Case B 복수 drug 처리 로직 | 추후 설계 | Phase 3 |

---

## 13. 향후 서비스 적용 시 제언 (Lessons Learned)

### 13.1 LLM 환각(Hallucination) 제어의 한계와 대안
초기 기획에서는 논문 원문(Evidence)을 LLM(특히 경량 로컬 모델)에 제공하여 약학적 상호작용을 추론 및 요약하게 하려 했으나, 실제 테스트 결과 **경량 모델의 심각한 환각 현상**이 발견되었습니다. 논문에 없는 부작용을 지어내거나, 긍정적인 현상을 위험으로 잘못 해석하는 등 의료/약학 도메인에서 치명적인 오류를 발생시켰습니다.

**[적용 제언 및 확정 사항]**
1. **Case A/B LLM 전담 (2026-03-13)**: drug-nutrient 상호작용 데이터를 구하는 것이 불가능하므로, 처방약-영양제 비교는 LLM에 맡긴다. dur_rules, drug_contraindication은 drug-drug 데이터이며, 낱알식별·약 검색 결과 표시용으로만 사용한다.
2. **강력한 프롬프트 통제 (Guardrails)**: LLM을 사용해야만 한다면 프롬프트에 `[정보 통제]`, `[보수적 해석]`, `[무의미한 데이터 예외 처리]`와 같은 절대 규칙을 명시하여 모델의 자의적인 해석을 원천 차단해야 합니다. 특히 "질병을 치료한다"는 뉘앙스는 절대 금지합니다.
3. **면책 문구 필수**: LLM 분석 결과는 참고용이며, 최종 복용 결정은 반드시 의료진과 상담해야 함을 명시한다.

### 13.2 데이터 품질과 정제의 중요성
수집된 데이터(예: SUPP.AI, 2026-03-12 폐기)가 '상호작용이 있다'는 사실 자체는 알려주지만, 그 상호작용이 구체적으로 어떤 위험을 초래하는지(description) 명확하지 않은 경우가 많았습니다. (예: 단순히 "작용을 억제한다"고만 되어 있고, 이것이 인체에 어떤 악영향을 미치는지 설명 부재). **2026-03-12 공공데이터 전환**으로 식약처·의약품안전관리원 공식 출처 데이터(dur_rules, drug_contraindication)로 교체 완료.

**[적용 제언]**
1. **데이터 전처리 강화**: 데이터 적재 시점에 불분명한 데이터는 과감히 'Caution(주의)' 등급으로 일괄 하향하거나, 사용자에게 노출하지 않는 필터링 로직이 필요합니다.
2. **보수적인 기본값(Default) 설정**: 명확한 근거가 없는 성분 조합에 대해서는 "안전하다"가 아니라 "정보가 부족하므로 전문의와 상담하라"는 방어적인 태도를 기본값으로 취해야 법적/윤리적 리스크를 최소화할 수 있습니다.
3. **성분명 정규화**: drug_permit_detail.ingr_name_eng는 `/`로 묶인 복합 문자열. DUR·병용금기 매칭 전 단일 성분 단위로 split 정규화 필요.

---

---

## 14. 2026-03-17 프로젝트 진행 현황 (Next Steps: 프론트엔드 진입)

**배경**: 2026-03-17에 걸쳐 백엔드(Spring Boot)와 AI 서버(FastAPI)의 핵심 분석 로직인 **Case A (상호작용 검사)** 와 **Case B (안전 영양제 추천)** 파이프라인 구축을 모두 성공적으로 완료했습니다. 더불어 과거 DB를 직접 뒤져 상호작용을 찾던 레거시 코드들도 완전히 청산하여 100% LLM 기반 시스템을 확립했습니다.

**구현 순서** (2026-03-17 기준 최신화):

1. **QueryDSL 환경 설정**: 동적 쿼리를 위한 기본 세팅. (완료)
2. **약 검색 및 낱알 식별 API** (Phase 2):
   - `GET /api/drugs/search/detail` (상세 검색) 구현 (완료)
   - `GET /api/drugs/search/pillIdentifier` (낱알 식별) 구현 (완료)
   - 제형(`form_code_name`) 수동 매핑 및 DB 정규화 적용 (`normal_form_name`) (완료)
3. **약품 상세 API** (Phase 2):
   - `GET /api/drugs/{drugId}` 구현 (컨트롤러 분리) (완료)
   - 검색 결과 선택 시 상세 정보 통합 조회 및 DB 상호작용 의존성 제거 (완료)
4. **Case A (충돌 검사)** (Phase 3):
   - PaddleOCR + Qwen 7B + Regex 기반의 가볍고 정확한 영양제 텍스트 추출 API 구축 (완료)
   - 추출된 데이터와 처방약 데이터를 조합하여 Gemini 3 Flash가 상호작용 등급을 판별하는 파이프라인 (완료)
5. **Case B (추천)** (Phase 4):
   - 기복용 처방약을 분석하여 Gemini 3 Flash가 환각 없이 안전한 영양 성분만을 추천하는 파이프라인 (완료)

**Next Steps (프론트엔드 본격 진입)**:

- 백엔드의 모든 엔진(API)이 완성되었으므로, 프론트엔드 UI 화면 개발을 시작합니다.
- 사진 촬영 UI, 처방약 검색 화면, 분석 결과(SAFE/CAUTION 등) 표시 화면을 구축하고 생성된 API들과 통합 연동(E2E) 테스트를 진행합니다.
- 추가 고도화(한국 영양제 데이터 기반 매칭 기능 부활, 딥링크 URL 연결 확정 등)는 연동 테스트 이후 단계에서 검토합니다.

---

*마지막 수정: 2026-03-28 (사용자·로그인·JWT 등 인증 기능 범위 제외 확정 반영)*