---
name: supplements 테이블 적재
overview: 건강기능식품 API(식품안전나라)를 통해 supplements 테이블에 44,436건의 데이터를 적재한다. 기존 의약품 API와 완전히 다른 API 구조이므로, 별도의 클라이언트와 DTO를 생성한다.
todos:
  - id: s1
    content: application.properties — 식품안전나라 API 키/URL 추가
    status: completed
  - id: s2
    content: Supplement.java — prdlstReportNo, capRawmtrlNm 필드 추가
    status: completed
  - id: s3
    content: SupplementRepository.java — 중복 체크 메서드 추가
    status: completed
  - id: s4
    content: SupplementResponse.java — 응답 DTO 신규 생성
    status: completed
  - id: s5
    content: FoodSafetyClient.java — 식품안전나라 API 클라이언트 신규 생성
    status: completed
  - id: s6
    content: SupplementDataService.java — 적재 서비스 신규 생성
    status: completed
  - id: s7
    content: BatchController.java — supplement 배치 엔드포인트 추가
    status: completed
isProject: false
---

# supplements 테이블 적재 (건강기능식품 API)

## 배경

- API 제공처: **식품안전나라** (openapi.foodsafetykorea.go.kr) — 기존 data.go.kr과 다름
- 총 데이터: **44,436건**
- URL 패턴: `http://openapi.foodsafetykorea.go.kr/api/{인증키}/I0030/json/{startIdx}/{endIdx}`
- 응답 구조: `{"I0030": {"total_count": "44436", "row": [...], "RESULT": {...}}}`

## 변경 파일 목록

### 1. Supplement 엔티티 수정 — `prdlstReportNo` 추가

[backend/src/main/java/com/pnn/backend/domain/Supplement.java](backend/src/main/java/com/pnn/backend/domain/Supplement.java)

- `prdlstReportNo` (품목제조번호) 필드 추가 — **unique 제약조건** (중복 적재 방지 키)
- `capRawmtrlNm` (캡슐 원재료) 필드 추가 — 캡슐 성분 정보 (상호작용 분석에 유용)

### 2. SupplementRepository 수정 — 중복 체크 메서드

[backend/src/main/java/com/pnn/backend/repository/SupplementRepository.java](backend/src/main/java/com/pnn/backend/repository/SupplementRepository.java)

- `existsByPrdlstReportNo(String prdlstReportNo)` 메서드 추가

### 3. SupplementResponse DTO 신규 생성

`backend/src/main/java/com/pnn/backend/dto/SupplementResponse.java`

- 식품안전나라 API 응답 구조에 맞는 DTO
- 루트: `I0030` 키 → `total_count`(String), `row`(List), `RESULT`
- Jackson `@JsonProperty` 사용하여 UPPER_SNAKE_CASE 필드 매핑

### 4. FoodSafetyClient 신규 생성

`backend/src/main/java/com/pnn/backend/client/FoodSafetyClient.java`

- 기존 `PublicDataClient`와 분리 (다른 API 제공처, 다른 URL 구조, 다른 인증키)
- `fetchSupplements(int startIdx, int endIdx)` 메서드
- URL: `{baseUrl}/{apiKey}/I0030/json/{startIdx}/{endIdx}`

### 5. SupplementDataService 신규 생성

`backend/src/main/java/com/pnn/backend/service/SupplementDataService.java`

- `fetchAllAndSaveSupplements()`: 전체 순회 (500건 단위, 총 ~89페이지)
- `fetchAndSaveSupplements(int startIdx, int endIdx)`: 단일 범위 테스트용
- 중복 체크: `existsByPrdlstReportNo()`
- 페이지별 트랜잭션 분리 (`TransactionTemplate`)

### 6. BatchController 수정 — 엔드포인트 추가

[backend/src/main/java/com/pnn/backend/controller/BatchController.java](backend/src/main/java/com/pnn/backend/controller/BatchController.java)

- `GET /api/batch/supplement?startIdx=1&endIdx=10` — 단일 범위 테스트
- `GET /api/batch/supplement/all` — 전체 적재 (비동기)

### 7. application.properties 수정

[backend/src/main/resources/application.properties](backend/src/main/resources/application.properties)

- `api.foodsafety.key=5e718ecbe7eb41f1a1ba` 추가
- `api.foodsafety.supplement.url=http://openapi.foodsafetykorea.go.kr/api` 추가

## 실행 순서 (사용자 직접 수행)

1. 서버 재시작 (`./gradlew bootRun`)
2. 단일 테스트: `curl.exe http://localhost:8080/api/batch/supplement?startIdx=1&endIdx=5`
3. DB 확인: `SELECT COUNT(*) FROM supplements;`
4. 전체 적재: `curl.exe http://localhost:8080/api/batch/supplement/all`
5. 서버 로그로 진행 확인, 완료 후 DB 건수 확인
