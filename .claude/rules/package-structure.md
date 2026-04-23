# 최상위 구조

```
com.lingring
├── domain/          — 비즈니스 도메인별 기능 모듈
├── global/          — 프로젝트 전역 횡단 관심사
└── infrastructure/  — 외부 시스템 어댑터 구현체
```

- `domain/`: 각 도메인은 독립된 모듈로 취급한다. 도메인 간 의존은 최소화한다.
- `global/`: 특정 도메인에 귀속되지 않는 공통 코드만 둔다. 도메인 로직이 섞이면 안 된다.
- `infrastructure/`: 외부 시스템(PG, 메시징, 외부 API 등)과의 연동 구현체를 둔다. 

# 도메인 패키지 표준 구조

각 도메인은 아래 구조를 따른다. **대표 예시: `member` 도메인.**

```
domain/<도메인>/
├── api/                             — Controller (HTTP 진입점)
├── service/                         — Application Service (트랜잭션 경계)
├── facade/                          (선택) 여러 service 오케스트레이션
├── dao/
│   ├── XxxRepository.java
│   └── dto/                         (선택) JPQL/QueryDSL 프로젝션
├── dto/
│   ├── request/                     — API 요청 DTO
│   └── response/                    — API 응답 DTO
├── domain/
│   ├── Xxx.java                     — 엔티티
│   ├── Xxxs.java                    — 일급 컬렉션
│   ├── vo/                          (선택) 값 객체
│   ├── service/                     — 순수 도메인 서비스
│   ├── policy/                      (선택) 전략/정책 객체
│   └── exception/                   — 도메인 예외
```