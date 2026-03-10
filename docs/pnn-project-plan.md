# 프로젝트 계획서 (v0.1)

# PNN Project - 프로젝트 계획서 (v0.1)

## 1. 프로젝트 개요 (Project Overview)

### 1.1 서비스 정의

본 프로젝트(PNN: Pill & Nutrient Navigator)는 사용자가 복용 중인 **전문/일반 의약품(Prescription Drugs)**과 **건강기능식품(Supplements)** 간의 데이터 교차 분석을 통해, **약물-영양제 상호작용(부작용/시너지)**을 사전에 판별하고 **개인화된 영양 성분 섭취 가이드**를 제공하는 하이브리드(Rule-based + LLM) 웹 서비스이다.

### 1.2 핵심 가치 (Value Proposition)

- **안전**: 처방약과 영양제의 충돌(흡수 방해, 과잉 작용 등)을 사전에 경고
- **편의**: 영양성분표 촬영만으로 보유 영양제 등록, 처방약은 약 모양/색상으로 쉽게 검색
- **개인화**: 장기 처방약으로 인한 영양소 결핍을 분석하여 맞춤 성분 추천 + 구매 연결

### 1.3 면책 조항 (Disclaimer)

> 본 서비스는 정보 제공 목적이며, 의료·약사 상담을 대체하지 않습니다. 복용 결정은 반드시 의료진과 상담하세요.
> 

*(상세 문구는 추후 법률/규제 검토 후 확정)*

---

## 2. 핵심 비즈니스 로직 (Core Business Logic)

### 2.1 Case A: 기복용 영양제 기반 신규 처방약 충돌 검사 (Personalized Conflict Check)

### 개념

평소 영양제를 섭취하던 사용자가 질병으로 인해 새로운 처방약을 복용하게 되었을 때, 두 약물 간의 상호작용을 분석하여 **일시적 복용 중단 또는 병용 지침(Action Plan)**을 제공한다.

### 데이터 흐름 (Protocol Flow)

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
  │       ├─ Google Cloud Vision API → 텍스트 추출
  │       └─ OpenAI GPT-4o → JSON 정형화 (성분명은 반드시 영문으로 출력)
  │
  ├─ (3) 정형화된 영양제 데이터(JSON) 수신 및 저장 (user_supplements.nutrients, 영문)
  │
  ├─ (4) 사용자가 식별/선택한 신규 처방약 → drug_id 확보
  │
  ├─ (5) drug_id → drugs.main_ingr_eng 조회 → "/" split → [영문 성분 목록]
  │       └─ main_ingr_eng 없음(상세정보 미적재) → 약품명만 LLM에 전달 → 안내문 제공
  │
  ├─ (6) Rule-based 1차 필터링 (Spring 내부)
  │       └─ (drug_ingredient, nutrient) × interaction_rules 매칭
  │       └─ 미매칭 시 성분만 모아서 LLM으로 전달 (약 2.5만 drug vs 2천 성분 룰)
  │
  ├─ (7) 1차 필터에서 미결정인 건 → Python AI 서버로 심층 분석 요청
  │       └─ LLM 상호작용 분석 (RAG 미사용)
  │
  ▼
[응답]
  └─ (8) 결과 반환: '병용 안전' / '시간 간격 필요' / '복용 기간 내 중단 권장' 등
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

처방약을 복용 중인 사용자가 새로운 영양제를 구매하고자 할 때, **interaction_rules**를 활용하여 **피해야 할 영양 성분을 경고**하고, 해당 정보를 참고하여 LLM이 **안전하고 유익한 영양 성분을 추천** + 외부 커머스 검색 결과로 **딥링크(Deep Linking)**한다.

### 데이터 흐름 (Protocol Flow)

```
[사용자]
  │
  ├─ (1) 기복용 처방약 리스트 확인 요청 + 영양제 구매 의도
  │
  ▼
[Spring Boot - 메인 백엔드]
  │
  ├─ (2) user_drugs에서 drug_id 목록 조회 (복수 drug — 추후 별도 설계)
  │
  ├─ (3) 각 drug_id → drugs.main_ingr_eng 조회 → "/" split → [영문 성분 목록]
  │
  ├─ (4) 각 drug 성분에 대해 interaction_rules 조회 (findByDrugIngredient)
  │       └─ CAUTION/WARNING nutrient 수집 → 경고 목록 생성
  │
  ├─ (5) 금기 성분 정보 + 처방약 정보 → Python AI 서버로 추천 요청
  │       └─ LLM 기반 안전 영양 성분 추천 + 사유 생성
  │
  ├─ (6) 추천 성분명 → URLEncoder → 제휴 쇼핑몰 동적 검색 URL 생성
  │
  ▼
[응답]
  └─ (7) 금기 성분 경고 + 추천 성분/사유 + 외부 커머스 딥링크 제공
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

- 인증/사용자 관리
- 처방약 검색 (낱알식별: 모양/색/각인)
- 프로필/이력 관리 (기복용 처방약, 영양제)
- 비즈니스 룰 엔진 (1차 필터: 확정 충돌/안전 판별)
- RestClient (Raw HTTP)로 Python AI 서버 호출
- RestClient로 외부 API 호출 (공공데이터 등)

**↓** (내부망 REST API)

**Python AI Server (FastAPI 추천)**

- OCR 파이프라인: Google Vision API로 텍스트 추출 → GPT-4o로 JSON 정형화
- RAG 파이프라인: RAG/pgvector 임베딩을 제거하고, 1차 룰(DB)을 중심으로 분석. 미결정 건만 LLM 프롬프트로 넘겨 심층 분석
- 영양제 제품명 매칭: 사용자 입력/OCR 결과 → LLM으로 제품 후보 추론
- (향후) 전용 경량 모델 (NER, 분류 등)

↓

**PostgreSQL (16+)**

- 정형 테이블: 의약품 마스터, 사용자, 프로필, 상호작용 룰
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
| **Spring Boot** | 인증/권한, 프로필 CRUD, 처방약 검색, 비즈니스 룰(1차 필터), Python 호출 오케스트레이션, 응답 조합/가공, DB 트랜잭션 | 클라이언트의 **유일한 진입점** |
| **Python AI Server** | OCR 파이프라인(Vision+GPT-4o 정형화), RAG 파이프라인(pgvector+LLM 분석), 영양제 제품명 매칭, 향후 전용 모델 | Spring에서만 호출 (내부망) |
| **PostgreSQL** | 정형 데이터(의약품, 사용자, 프로필), JSONB(OCR 결과, 분석 로그), pgvector(상호작용/영양소 고갈 지식 임베딩) | Spring·Python 양쪽에서 접근 |

### 3.3 설계 원칙

- **단일 진입점**: 클라이언트는 Spring Boot API만 호출하며, Python 서버 URL은 외부에 노출하지 않는다.
- **외부 라이브러리 최소 의존**: Spring 측은 RestClient를 활용한 Raw HTTP 통신으로, 변동성이 큰 외부 AI 라이브러리 의존성을 배제하고 시스템 안정성을 극대화한다.
- **관심사 분리**: 도메인/트랜잭션은 Spring, AI/ML은 Python으로 명확히 분리한다.
- **Graceful Degradation**: Python 서버 장애/타임아웃 시 Spring은 1차 룰 결과 또는 캐시된 결과로 fallback한다.

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
| 인증/보안 | Spring Security + JWT | Access/Refresh Token 기반 인증, BCrypt 비밀번호 해싱 |

### 4.2 AI Server (Python)

| 항목 | 선택 | 이유 |
| --- | --- | --- |
| Framework | FastAPI (추천) 또는 Flask | 비동기 지원, 자동 OpenAPI 문서, 타입 힌트 |
| Language | Python 3.14.2 (로컬 확인 완료) | AI/ML 생태계 최적 |
| OpenAI SDK | openai (공식) | GPT-4o 호출, 프롬프트 관리 |
| OCR | Google Cloud Vision API | 다국어/표 구조 인식 강점 |
| 벡터 검색 | psycopg + pgvector | PostgreSQL 내 벡터 검색, 별도 DB 불필요 |
| 임베딩 | OpenAI text-embedding-3-small/large | pgvector에 저장할 임베딩 생성 |

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
| Google Cloud Vision API | 영양성분표 이미지 → 텍스트 추출 | Python |
| OpenAI GPT-4o | OCR 텍스트 → JSON 정형화, 상호작용 심층 분석, 제품명 추론 | Python |
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

### 5.1 처방약 등록 (약학정보원 약검색 방식)

### 검색 조건 (UI)

| 조건 | 설명 | 예시 |
| --- | --- | --- |
| 모양 | 알약 외형 (원형, 타원형, 장방형 등) | 원형 |
| 색상 | 앞/뒤 색상 | 흰색, 노란색 |
| 각인 | 알약 표면 문자/숫자 | "A", "325" |
| 이니셜 | 제약사 로고/이니셜 | "HW" |
| 분할선 | 분할선 유무/형태 | +자, -자, 없음 |
| 제형 | 정제, 캡슐, 연질캡슐 등 | 정제 |

### 플로우

```
사용자가 조건 선택 → Spring API 조회 → 후보 약 목록 반환
→ 사용자가 "이 약이다" 선택 → 프로필에 추가 (기복용 처방약)
```

### 5.2 영양제 등록

### 제품 식별 (2가지 경로)

| 경로 | 방법 | 처리 |
| --- | --- | --- |
| **텍스트 입력** | 사용자가 제품명 직접 입력 | Spring → Python `/match-supplement` → LLM이 제품 후보 추론 → 사용자 확인 |
| **제품명 촬영** | 제품 라벨/포장 사진 촬영 | Spring → Python → Vision OCR → LLM 제품명 추출 → 사용자 확인 |

### 영양성분 등록

```
영양성분표 촬영 (Client)
→ Spring → Python OCR Pipeline
  → Google Vision API: 이미지 → Raw Text
  → GPT-4o: Raw Text → 정형 JSON (성분명은 반드시 영문으로 출력)
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
    "drug": { "id": "string", "name": "string", "ingredients": ["string"] },  // ingredients: drugs.main_ingr_eng split (영문)
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

### 7.0 데이터 소스 현황

| 소스 | 파일/API | 건수 | 핵심 필드 |
| --- | --- | --- | --- |
| 병용금기약물 CSV | `한국의약품안전관리원_병용금기약물_20240625.csv` | ~54만건 | 성분명1, 성분코드1, 제품코드1, 제품명1, 업체명1, 급여구분1, 성분명2, 성분코드2, 제품코드2, 제품명2, 업체명2, 급여구분2, 공고번호, 공고일자, 금기사유 |
| 약가마스터 CSV | `건강보험심사평가원_약가마스터_의약품표준코드_20251031.csv` | ~30만건 | 한글상품명, 업체명, 약품규격, 제형구분, 포장형태, 품목기준코드, 품목허가일자, 전문일반구분, 대표코드, 표준코드, 일반명코드(성분명코드), 국제표준코드(ATC코드), 특수관리약품구분 |
| 의약품 허가정보 API | 식약처 DrugPrdtPrmsnInfoService07 | API | ITEM_SEQ, ITEM_NAME, ENTP_NAME, ITEM_INGR_NAME, PRDUCT_TYPE, BIG_PRDT_IMG_URL, EDI_CODE |
| 의약품 허가 상세 API | 식약처 getDrugPrdtPrmsnDtlInq06 | API | ITEM_SEQ, MATERIAL_NAME, ETC_OTC_CODE, CHART, MAIN_ITEM_INGR, INGR_NAME, ATC_CODE, TOTAL_CONTENT |
| 의약품 주성분 상세 API | 식약처 getDrugPrdtMcpnDtlInq07 | API | ITEM_SEQ, MTRAL_CODE, MTRAL_NM, QNT, INGD_UNIT_CD, MAIN_INGR_ENG |
| 의약품 낱알식별 API | 식약처 MdcinGrnIdntfcInfoService03 | API | ITEM_SEQ, ITEM_NAME, DRUG_SHAPE, COLOR_CLASS1, COLOR_CLASS2, PRINT_FRONT, PRINT_BACK, LINE_FRONT, LINE_BACK, FORM_CODE_NAME, ITEM_IMAGE, CLASS_NAME, ETC_OTC_NAME |
| 건강기능식품 API | 식약안전처 I0030 | API | PRDLST_NM(품목명), BSSH_NM(업소명), PRIMARY_FNCLTY(주된기능성), RAWMTRL_NM(기능지표성분), INDIV_RAWMTRL_NM(기능성원재료), ETC_RAWMTRL_NM(기타원재료), NTK_MTHD(섭취방법), IFTKN_ATNT_MATR_CN(섭취시주의사항), STDR_STND(기준규격) |

### 7.1 정형 테이블 (PostgreSQL)

| 테이블 | 주요 필드 | 데이터 소스 | 비고 |
| --- | --- | --- | --- |
| `users` | id, email, password_hash, created_at | 직접 입력 | 사용자 계정 |
| `drugs` | id, item_seq, item_name, entp_name, etc_otc_code, chart, material_name, main_item_ingr, ingr_name, **main_ingr_eng**, atc_code, total_content, big_prdt_img_url | 의약품 허가정보/상세 API, 주성분 상세 API | 의약품 마스터. main_ingr_eng는 drug당 1건(주성분 영문, "/" 구분). Case A/B 1차 필터용 |
| `drug_identification` | id, drug_id(FK), drug_shape, color_class1, color_class2, print_front, print_back, line_front, line_back, form_code_name, item_image, class_name | 낱알식별 API | 낱알 외형정보. drugs와 ITEM_SEQ로 연결 |
| `drug_ingredients` | id, drug_id(FK), mtral_code, mtral_nm, qnt, ingd_unit_cd, main_ingr_eng | 주성분 상세 API | 의약품별 주성분 목록. 1:N 관계 |
| `contraindications` | id, ingredient_name1, ingredient_code1, product_code1, product_name1, company_name1, ingredient_name2, ingredient_code2, product_code2, product_name2, company_name2, notice_no, notice_date, reason | 병용금기 CSV (~54만건) | 약물 간 병용금기 조합. 1차 필터에 활용 |
| `drug_price_master` | id, item_name, entp_name, drug_spec, form_type, pkg_type, std_code, permit_date, etc_otc_type, represent_code, bar_code, ingr_code, atc_code | 약가마스터 CSV (~30만건) | 표준코드·ATC코드 매핑용 |
| `supplements` | id, prdlst_nm, bssh_nm, primary_fnclty, rawmtrl_nm, indiv_rawmtrl_nm, etc_rawmtrl_nm, ntk_mthd, iftkn_atnt_matr_cn, stdr_stnd | 건강기능식품 API | 건강기능식품 마스터 |
| `user_drugs` | id, user_id(FK), drug_id(FK), start_date, is_active | 직접 입력 | 사용자 기복용 처방약 |
| `user_supplements` | id, user_id(FK), supplement_name, nutrients(JSONB), registered_at | OCR + 직접 입력 | 사용자 기복용 영양제. nutrients는 LLM이 영문으로 출력 (Vitamin C, Magnesium 등) |
| `interaction_rules` | id, drug_ingredient, nutrient, level(SAFE/CAUTION/WARNING/SYNERGY), description, action | SUPP.AI JSON 적재 | Rule-based 1차 필터용. drug_ingredient, nutrient 모두 영문 |
| `analysis_logs` | id, user_id(FK), case_type(A/B), request(JSONB), response(JSONB), created_at | 시스템 생성 | 분석 이력 로그 |

### 7.2 벡터 테이블 (pgvector)

| 테이블 | 주요 필드 | 비고 |
| --- | --- | --- |
| `knowledge_embeddings` | id, content, embedding(vector), source, category, metadata(JSONB) | 상호작용 문헌 임베딩 |

- **임베딩 모델**: OpenAI `text-embedding-3-small` (1536차원) 또는 `text-embedding-3-large` (3072차원)
- **인덱스**: HNSW (데이터 규모에 따라 m/ef_construction 튜닝)

### 7.3 테이블 간 관계 요약

```
users ─┬─ user_drugs ── drugs ─┬─ drug_identification (1:1)
       │                       ├─ drug_ingredients (1:N, RAG용)
       │                       └─ drugs.main_ingr_eng (drug당 1건, 1차 필터용)
       └─ user_supplements     └─ interaction_rules (영문 drug_ingredient × nutrient)
                                  drug_price_master (표준코드 매핑)
supplements (건강기능식품 마스터, 독립)
knowledge_embeddings (벡터 검색, 독립)
```

### 7.4 JSONB 사용 규칙

- `user_supplements.nutrients`: OCR→LLM 정형화 결과. **name은 반드시 영문** (Vitamin C, Coenzyme Q10 등). interaction_rules 조회 시 그대로 사용
- `analysis_logs.request/response`: 분석 요청/응답 원문 보존
- JSONB 내부 필드 검색이 빈번한 경우 GIN 인덱스 추가

---

## 8. 분석 파이프라인 상세

### 8.1 Rule-based 1차 필터 (Spring)

```
[Case A] drug_id → drugs.main_ingr_eng → "/" split → [영문 성분]
         user_supplements.nutrients → [영문]
         (drug_ingredient, nutrient) × interaction_rules 매칭

[Case B] user_drugs → drug_id들 → drugs.main_ingr_eng split
         findByDrugIngredient → CAUTION/WARNING nutrient 수집

  ├─ 매칭 있음 → 즉시 결과 반환 (확정적 충돌/안전)
  └─ 매칭 없음 → 성분만 모아서 Python LLM으로 전달 (약 2.5만 drug vs 2천 성분 룰)
```

- **데이터**: drug_ingredient, nutrient 모두 **영문**. 한/영 매핑 테이블(ingredient_mapping) 사용하지 않음
- **Fallback**: drugs.main_ingr_eng 없으면(상세정보 미적재) 약품명만 LLM에 전달 → 안내문 제공
- **관리**: `interaction_rules`는 SUPP.AI JSON 배치 적재

### 8.2 LLM 2차 분석 (Python)

```
(1) 입력: 처방약 성분 + 영양제 성분 조합
(2) 프롬프트 구성: 시스템 프롬프트 + 분석 요청
(3) GPT-4o 호출 → 구조화된 JSON 응답 생성
(4) 결과 파싱 및 Spring으로 반환
```

---

## 9. 비기능 요구사항 (Non-Functional Requirements)

### 9.1 성능/안정성

| 항목 | 목표 |
| --- | --- |
| Spring → Python 타임아웃 | OCR: 30초, 분석: 15초, 매칭: 10초 |
| Python 장애 시 fallback | Spring 1차 룰 결과 반환 또는 "일시적 오류" 안내 |
| 재시도 정책 | 최대 2회, 지수 백오프 (1초, 2초) |

### 9.2 보안

| 항목 | 방침 |
| --- | --- |
| Python 서버 접근 | 내부망 전용, 외부 직접 접근 차단 |
| Spring ↔ Python 인증 | API Key 또는 내부 JWT (추후 확정) |
| 사용자 인증 | Spring Security + JWT (Access Token / Refresh Token) |

### 9.3 확장성

| 항목 | 방향 |
| --- | --- |
| AI 모델 교체 | Python 서버만 재배포, Spring 무변경 |
| 전용 경량 모델 추가 | Python 서버 내 엔드포인트 추가 |
| 트래픽 증가 시 | Python 서버 수평 확장 (GPU 인스턴스 별도 스케일) |

---

## 10. 개발 로드맵 (초안)

### Phase 1: 기반 구축

- [ ]  Spring Boot 프로젝트 초기 세팅 (Gradle, 의존성, 패키지 구조)
- [ ]  PostgreSQL 스키마 설계 및 초기 마이그레이션
- [ ]  pgvector 확장 설치 및 벡터 테이블 생성
- [ ]  Python AI 서버 프로젝트 초기 세팅 (FastAPI)
- [ ]  Spring ↔ Python 간 기본 REST 통신 확인 (Health Check)
- [ ]  공공데이터포털 의약품 낱알식별 데이터 배치 적재 파이프라인

### Phase 2: Case A 구현 (충돌 검사)

- [ ]  처방약 검색 API (모양/색/각인 기반)
- [ ]  OCR 파이프라인 (Vision + GPT-4o 정형화, **성분명 영문 출력**)
- [ ]  영양성분 등록/조회 API
- [ ]  Rule-based 1차 필터 (drugs.main_ingr_eng split × interaction_rules)
- [ ]  상세정보 없는 drug fallback (약품명 → LLM → 안내문)
- [ ]  LLM 2차 분석 파이프라인
- [ ]  충돌 검사 통합 API

### Phase 3: Case B 구현 (추천)

- [ ]  상호작용 DB 기반 금기 성분 추출 로직 (drugs.main_ingr_eng split × findByDrugIngredient)
- [ ]  복수 drug 처리 로직 설계
- [ ]  LLM 기반 안전 영양 성분 추천 API
- [ ]  딥링크 URL 생성 로직
- [ ]  추천 결과 API

### Phase 4: 사용자/프로필

- [ ]  Spring Security + JWT 인증 설정 (Filter Chain, Access/Refresh Token)
- [ ]  회원가입 API (`/api/auth/signup`) — BCrypt 비밀번호 해싱
- [ ]  로그인 API (`/api/auth/login`) — JWT 발급
- [ ]  로그아웃 / 토큰 갱신 API
- [ ]  회원정보 수정 / 회원 탈퇴 API
- [ ]  기복용 처방약/영양제 프로필 CRUD
- [ ]  분석 이력 조회

### Phase 5: 프론트엔드 (추후 결정)

- [ ]  기술 스택 결정 (React/Next.js/기타)
- [ ]  주요 화면 설계 및 구현

---

## 11. 설계 결정 사항 (2026-03-08 확정)

| 항목 | 결정 |
| --- | --- |
| nutrient 저장 형식 | LLM이 OCR→JSON 변환 시 **영문으로 출력** (Vitamin C, Magnesium 등). ingredient_mapping 불필요 |
| drug 성분 조회 | drugs.main_ingr_eng split("/") → interaction_rules 직접 비교 |
| 상세정보 없는 drug | main_ingr_eng 없으면(낱알 2.5만 vs 상세 3,800건) 약품명만 LLM에 전달 → 안내문 제공 |
| interaction_rules 미매칭 | 성분만 모아서 LLM으로 전달. 수작업 정제 불가 수준의 데이터 |
| 복수 drug (Case B) | 처방은 대부분 복수 → 추후 별도 설계 |
| 표기 불일치 (Vitamin C vs Ascorbic acid) | 수작업 정제 불가, LLM 비용 부담 → 현실적 리스크 수용 |

---

## 12. 미결정 사항 (Open Items)

| # | 항목 | 현재 상태 | 결정 시점 |
| --- | --- | --- | --- |
| 1 | 프론트엔드 기술 스택 | 미정 | Phase 5 전 |
| 2 | 사용자 인증 방식 | Spring Security + JWT 확정 | 확정 |
| 3 | Spring ↔ Python 인증 (API Key vs JWT) | 미정 | Phase 1 |
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

**[적용 제언]**
1. **Rule-based 중심의 보수적 접근**: LLM이 실시간으로 상호작용을 "추론"하게 하는 것은 매우 위험합니다. DB에 적재된 검증된 상호작용 데이터(SUPP.AI 등)를 1차 필터(Rule-based)로 강하게 적용하고, LLM은 단순히 매칭된 결과를 "사용자가 읽기 쉽게 문장으로 다듬는(Formatting)" 역할로만 제한해야 합니다.
2. **강력한 프롬프트 통제 (Guardrails)**: LLM을 사용해야만 한다면 프롬프트에 `[정보 통제]`, `[보수적 해석]`, `[무의미한 데이터 예외 처리]`와 같은 절대 규칙을 명시하여 모델의 자의적인 해석을 원천 차단해야 합니다. "정보가 없으면 없다고 출력하라"는 지시가 필수적입니다.
3. **전문가 검수 프로세스 (Human-in-the-loop)**: 생성된 경고 문구나 가이드는 서비스 배포 전 최소한의 샘플링을 통해 약사 등 도메인 전문가의 검수를 거치는 프로세스가 필요합니다.

### 13.2 데이터 품질과 정제의 중요성
수집된 데이터(예: SUPP.AI)가 '상호작용이 있다'는 사실 자체는 알려주지만, 그 상호작용이 구체적으로 어떤 위험을 초래하는지(description) 명확하지 않은 경우가 많았습니다. (예: 단순히 "작용을 억제한다"고만 되어 있고, 이것이 인체에 어떤 악영향을 미치는지 설명 부재).

**[적용 제언]**
1. **데이터 전처리 강화**: 데이터 적재 시점에 불분명한 데이터는 과감히 'Caution(주의)' 등급으로 일괄 하향하거나, 사용자에게 노출하지 않는 필터링 로직이 필요합니다.
2. **보수적인 기본값(Default) 설정**: 명확한 근거가 없는 성분 조합에 대해서는 "안전하다"가 아니라 "정보가 부족하므로 전문의와 상담하라"는 방어적인 태도를 기본값으로 취해야 법적/윤리적 리스크를 최소화할 수 있습니다.

---

*마지막 수정: 2026-03-10 (LLM 환각 문제 및 데이터 정제 관련 제언 추가)*