# 설계 규칙
- 기본적으로 DDD 방법론을 따른다 
- 같은 Aggregate 내부 객체는 직접참조를 우선 고려한다
- 다른 Aggregate는 기본적으로 식별자(ID)로 간접참조한다

# 날짜 사용 규칙
- `LocalDateTime.now()` / `LocalDate.now()` 등 시스템 시계 직접 호출 금지. `DateTimeProvider`를 주입받아 사용한다
- 기준 타임존은 `Asia/Seoul`로 고정 (서버 OS 기본 타임존 의존 금지)
- DB 저장 대상 `LocalDateTime`은 마이크로초까지 truncate (MySQL DATETIME(6) 한계)
- 테스트에서 시각 의존 로직은 `FixedDateTimeProvider`로 시각을 고정해 검증

# 네이밍 규칙
- 검증/boolean 반환 메서드는 긍정문 사용 (isNotValid, hasNotRank 등 금지)
- dto 네이밍 대신 request/response로 구분
- dto는 record로 생성
        
# 코드 스타일
- Google Code Style 기반 (Tab/Indent: 4, Continuation: 8)
- 클래스와 선언부 필드 사이 개행
- 메서드 매개변수와 지역 변수에 final 사용
- 초기화되지 않은 상태로 변수를 선언하지 않는다
- else 예약어를 쓰지 않는다 (조기 리턴으로 대체)
- switch식 문장이나 삼항 연산자는 사용하지 않는다
- 컬렉션에 도메인 동작(검증/필터링/집계 등)이 붙거나, 불변성을 보장해야 한다면 일급 컬렉션으로 래핑한다
        
# Lombok 사용 규칙:
- @Data 사용 금지
- @Getter, @NoArgsConstructor, @RequiredArgsConstructor, @Builder(필드 4개 이상시), @EqualsAndHashCode 만 허용
- 어노테이션이 여럿 있는 경우, 중요한 것을 가장 상단에 배치한다. (Lombok의 어노테이션을 가장 하단에 배치한다)

# 서비스/컨트롤러
- controller에서 swagger 명시를 하는 대신에, 별도의 api interface를 만든 후 해당 인터페이스에서 swagger 정보를 작성한다
- @RequestMapping을 클래스 레벨에서 사용 금지
- @Transactional을 클래스 레벨에서 사용 금지 — 메서드 단위로 붙이며, 읽기 메서드는 `@Transactional(readOnly = true)`, 쓰기 메서드는 `@Transactional`로 명시한다
        
# 엔티티
- @Column의 nullable=false인 경우 명시
- @Embeddable 내부에 @Embedded 사용 금지
- nullable 하지 않다면, 생성자의 매개변수에도 @NonNull을 붙인다
- @Enumerated는 항상 EnumType.STRING을 사용한다
- @GeneratedValue의 경우, IDENTITY를 사용한다
- 검증 로직이나 도메인 로직이 담길 경우, VO 객체로 감싼다