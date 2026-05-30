---                                                                                                                                                                                                                                   
paths:                                                                                                                                                                                                                              
  - "src/test/**/*.java"
  - "src/**/*Test.java"
---

# 적용 범위
- 새로 추가하는 Controller / Service / Repository / Domain 로직에는 대응하는 테스트를 같이 작성한다
- 기존 로직 수정 시, 변경된 동작을 검증하는 테스트를 추가·갱신한다
- 테스트 미작성 상태로 PR을 올리지 않는다
- 테스트 커버리지는 최소 80%를 넘어야한다

# 테스트 프레임워크
- JUnit 5 + AssertJ 사용
- 단언(assertion)은 AssertJ의 `assertThat`으로 통일
- Mockito는 Controller 계층에 한정

# 네이밍
- 메서드명은 영어로 작성 (예: `save_whenValidInput_returnsPersistedEntity`)
- 시나리오 설명은 `@DisplayName`에 한국어로 작성
- 테스트 클래스는 대상 클래스명 + `Test`

# 구조
- Given-When-Then 섹션을 주석(`// given`, `// when`, `// then`)으로 구분
- 하나의 테스트 메서드는 하나의 시나리오만 검증
- 한 대상(메서드/기능)에 대해 여러 시나리오를 테스트할 때는 `@Nested`로 묶는다
  - @Nested 상단에도 @DisplayName을 명시한다
- 테스트 유지보수성을 위해 중복되는 코드가 존재한다면, 내부 helper 메서드로 분리해 사용한다

# Domain 테스트
- 순수 단위 테스트로 작성 (Spring 컨텍스트 미로드, 의존성은 직접 생성)

# Repository 테스트
- JPA 기본 제공 메서드(`save`, `findById` 등)는 테스트하지 않는다
- Custom 구현된 메서드만 테스트 대상
- Mocking 없이 실제 의존성 주입으로 검증 (`@DataJpaTest` 활용)

# Service 테스트
- Mocking 금지 — `@SpringBootTest` + `@Autowired`로 실제 빈 주입
- 실제 Repository를 통해 DB 상태까지 검증
- 테스트 격리를 위해 `@Transactional`로 롤백 (별도 지시가 없는 한)

# Controller 테스트
- MockMvc 기반 (`@WebMvcTest` 권장)
- 하위 의존 빈은 `@MockitoBean`으로 대체 (Spring Boot 3.4+ 표준, 4.x에서도 동일)
- 응답 상태 코드와 body 구조까지 검증