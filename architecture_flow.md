# 🏢 기업 정보 검색 서비스 흐름도 (Architecture & Data Flow)

지금까지 개발하신 시스템은 단일 앱을 넘어 여러 **마이크로서비스 및 공공데이터를 결합(Orchestration)**하는 뛰어난 구조를 갖추고 있습니다. 전체적인 흐름을 두 가지 다이어그램으로 정리했습니다.

---

## 1. 시스템 구조도 (System Architecture)
각 프로그램(프론트엔드, 백엔드, DB)이 배포된 위치와 역할 분담을 보여줍니다.

```mermaid
graph TD
    User([사용자]) --> |웹 브라우저 접속| Frontend
    
    subgraph "Frontend Layer (Vercel 배포 예정)"
        Frontend[Next.js]
        UI[웹 화면 / 모달창]
    end
    
    Frontend --> |REST API 호출<br>HTTP/JSON| Backend
    
    subgraph "Backend Layer (Render 배포 완료)"
        Backend[Spring Boot]
        Controller[CompanyController<br>데이터 병합 및 로직 처리]
        DB_Repo[SearchHistoryRepository<br>DB 통신 모듈]
    end
    
    Backend --> Controller
    Controller --> DB_Repo
    
    DB_Repo --> |검색 이력 보관| MongoDB[(MongoDB Atlas<br>클라우드 DB)]
    
    Controller -.-> |1. 상태 체킹| API_NTS[국세청 API]
    Controller -.-> |2. 상호/연락처| API_BIZ[Bizno API]
    Controller -.-> |3. 재무/직원| API_DART[DART API]
    Controller -.-> |4. 보완 데이터| API_SUB[국민연금 API & CSV]
```

---

## 2. 데이터 흐름도 (Sequence Diagram)
사용자가 검색 버튼을 눌렀을 때 내부적으로 일어나는 수많은 백그라운드 작업을 순서대로 나열했습니다.

```mermaid
sequenceDiagram
    participant U as 사용자 (브라우저)
    participant F as 프론트엔드 (Next.js)
    participant B as 백엔드 (Spring Boot)
    participant DB as MongoDB Atlas
    
    box 외부 API 연동
    participant NTS as 국세청
    participant Biz as 비즈노
    participant Dart as DART
    participant Sub as 국민연금 및 CSV
    end

    U->>F: 검색어 입력 (ex: "삼성")
    F->>B: GET /api/company/search?q=삼성
    
    Note over B, Sub: 🔄 검색 시작 및 데이터 병합 (Orchestration)
    
    B->>Biz: 1. 검색어로 기초 정보 요청
    Biz-->>B: 상호명, 주소, 사업자번호 목록 반환
    
    B->>Dart: 2. 확보된 사업자번호들로 DART 조회
    Dart-->>B: 2024~2022년 매출액 및 직원수 응답
    
    opt DART에 데이터 누락 시 (Fallback 방어선)
        B->>Sub: 3. 국민연금/산재보험 데이터 호출
        Sub-->>B: 직원수 및 상세 주소 보완 데이터 응답
    end

    B->>NTS: 4. 국세청 영업 상태 (폐업 여부) 최종 확인
    NTS-->>B: 계속사업자 / 폐업자 상태 응답
    
    B->>DB: 5. SearchHistory 컬렉션에 사용자 검색 이력 저장
    DB-->>B: 저장 완료 성공
    
    Note over B: 수집된 파편화된 데이터들을<br>1개의 깔끔한 JSON으로 포장
    B-->>F: JSON 데이터 응답 완료
    F-->>U: 화면에 표 및 상세 모달창으로 예쁘게 렌더링 완료✨
```

### 💡 파워풀한 아키텍처의 핵심
* **무중단 폴백(Fallback) 방어선 구축**: 단순히 DART(상장사) 한 곳만 바라보지 않습니다. 그곳에 정보가 비어있다면, 즉시 국민연금 API와 200만 건의 로컬 CSV 기반 로직(고용산재보험)을 2차, 3차로 탐색하는 **강력한 방어형(Fault-Tolerant)** 로직이 들어있습니다.
* **보안 관심사 분리(SoC)**: API 인증키나 MongoDB 비밀번호 같은 초민감 정보는 오직 렌더(Render)라는 단단한 백엔드 공간에만 존재합니다. 프론트엔드는 통신만 주고받으므로 완벽한 보안 분리가 달성되었습니다.
