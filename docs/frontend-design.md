# PNN App 프론트엔드 설계 및 아키텍처 가이드

## 1. 개요 (Overview)
본 문서는 React Native (Expo) 기반의 PNN(Pill & Nutrient Navigator) 모바일 앱 개발을 위한 프론트엔드 설계 문서입니다. UI보다 '정확한 구조(Layout)', '확실한 동작(Functionality)', '코드의 재사용성(Reusability)'에 초점을 맞추어 설계되었습니다.

---

## 2. 기술 스택 (Tech Stack)
* **프레임워크**: Expo (React Native) + TypeScript
* **라우팅/네비게이션**: React Navigation (Native Stack)
* **스타일링**: React Native `StyleSheet.create` - *각 화면/컴포넌트 하단에 StyleSheet.create로 스타일 객체를 정의하여 사용함.*
* **HTTP 통신**: Axios (Spring Boot 및 FastAPI 연동)
* **주요 라이브러리**: 
  * `expo-image-picker`: 갤러리 접근 및 카메라 촬영 (Case A 영양제 분석용)
  * `zustand` (예정): 전역 상태 관리 (검색한 처방약 정보, 촬영한 영양제 데이터 유지용)

---

## 3. 폴더 구조 (Directory Structure)
```text
pnn-app/
├── src/
│   ├── api/          # 백엔드/AI 서버 통신 (Axios instance 및 엔드포인트별 함수)
│   │   ├── client.ts # Axios 기본 설정 (Base URL, Timeout 등)
│   │   └── ...
│   ├── components/   # 재사용 가능한 UI 컴포넌트 (디자인보다 레이아웃과 재사용성 중점)
│   │   ├── ActionCard.tsx # 메인 화면의 큰 버튼 (아이콘, 제목, 설명 포함)
│   │   ├── InfoRow.tsx    # 약품 상세 정보 표시에 쓰이는 행(Row) 컴포넌트
│   │   └── ...
│   ├── navigation/   # 화면 라우팅 설정
│   │   └── AppNavigator.tsx # Stack Navigator 정의
│   ├── screens/      # 개별 화면 컴포넌트 (비즈니스 로직과 UI 결합)
│   │   ├── HomeScreen.tsx           # 메인 홈 화면
│   │   ├── DrugSearchScreen.tsx     # 처방약 검색
│   │   ├── InteractionCheckScreen.tsx # 충돌 검사 (Case A)
│   │   └── RecommendationScreen.tsx   # 안전 영양제 추천 (Case B)
│   ├── types/        # TypeScript 인터페이스 및 타입 정의 (백엔드 DTO와 동기화)
│   └── utils/        # 공통 유틸리티 함수 (날짜 포맷팅, 데이터 가공 등)
├── App.tsx           # 앱 최상위 진입점
└── package.json
```

---

## 4. 핵심 화면 및 데이터 흐름 (Core Flows)

### 4.1 메인 홈 화면 (HomeScreen)
* **역할**: 앱의 진입점. 사용자가 원하는 기능으로 이동할 수 있는 내비게이션 허브.
* **구조**: `ActionCard` 컴포넌트를 재사용하여 세 가지 주요 기능(약 검색, Case A, Case B)을 수직 배치.
* **특징**: `StyleSheet.create`로 정의한 스타일을 적용하여 `flex: 1`, `justifyContent: 'center'` 등으로 레이아웃 구성.

### 4.2 Case A: 충돌 검사 (InteractionCheck Flow)
* **흐름**: 
  1. `InteractionCheckScreen`: 기능 소개 및 '처방약 추가', '영양제 사진 업로드' 버튼 제공.
  2. `Camera/ImagePicker`: 영양제 성분표 촬영 → FastAPI(`/supplement/extract`) 호출 → JSON 파싱 데이터 상태 저장.
  3. `InteractionAddDrugScreen`: 처방약 검색 후 선택 → `drugId` 상태 저장.
  4. `InteractionResultScreen`: 수집된 `drugId`와 영양제 JSON을 Spring Boot(`/api/interaction/check`)로 전송 → 분석 결과(SAFE/CAUTION/WARNING) 렌더링.

### 4.3 Case B: 안전 영양제 추천 (Recommendation Flow)
* **흐름**:
  1. `RecommendationScreen`: 기능 소개 및 '기복용 처방약 추가' 버튼 제공.
  2. 처방약 검색 및 선택 → 다수의 `drugId` 상태 저장.
  3. `RecommendationResultScreen`: 수집된 `drugIds`를 Spring Boot(`/api/v1/recommendations/safe-nutrients`)로 전송 → 추천 성분 2가지 및 금기 경고 렌더링.

---

## 5. 설계 원칙 (Design Principles)

### 5.1 스타일 관리
* 각 화면 및 컴포넌트 하단에 `StyleSheet.create`로 스타일 객체를 정의합니다.
* `style={styles.xxx}` 형태로 적용하여 일관된 스타일링과 타입 안정성을 유지합니다.

### 5.2 컴포넌트 모듈화
* 한 번 이상 쓰이는 UI 요소(예: 버튼, 정보 표시 박스)는 반드시 `src/components/`로 분리하여 인자(props)만 넘겨주어 재사용합니다.

### 5.3 관심사 분리 (SoC)
* **통신 로직**: `src/screens` 내부에서 직접 `fetch`나 `axios`를 호출하지 않고, `src/api` 폴더에 정의된 함수를 가져와서 사용합니다.
* **상태 관리**: 화면을 전환해도 유지되어야 하는 데이터(예: 선택한 처방약 목록)는 전역 상태 관리 도구(`zustand` 등)를 사용하여 컴포넌트 간 데이터 전달(Props Drilling)을 최소화합니다.

---
*작성일: 2026-03-17*
*작성자: Cursor Agent*