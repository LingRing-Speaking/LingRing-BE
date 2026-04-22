# 네이밍 규칙
- 검증/boolean 반환 메서드는 긍정문 사용 (isNotValid, hasNotRank 등 금지)
- dto 네이밍 대신 request/response로 구분
- dto는 record로 생성
        
# 코드 스타일
- Google Code Style 기반 (Tab/Indent: 4, Continuation: 8)
- 클래스와 선언부 필드 사이 개행
- 메서드 매개변수와 지역 변수에 final 사용
- Enum 값 정의 시 후행 쉼표 사용
- 초기화되지 않은 상태로 변수를 선언하지 않는다
        
# Lombok 사용 규칙:
- @Data 사용 금지
- @Getter, @NoArgsConstructor, @RequiredArgsConstructor, @Builder(필드 4개 이상시)만 허용
- 어노테이션이 여럿 있는 경우, 중요한 것을 가장 상단에 배치한다. (Lombok의 어노테이션을 가장 하단에 배치한다)

# 서비스/컨트롤러
- controller에서 swagger 명시를 하는 대신에, 별도의 api interface를 만든 후 해당 인터페이스에서 swagger 정보를 작성한다
- @RequestMapping을 클래스 레벨에서 사용 금지
        
# 엔티티
- @Column의 nullable=false인 경우 명시
- @Embeddable 내부에 @Embedded 사용 금지
- nullable 하지 않다면, 생성자의 매개변수에도 @NonNull을 붙인다

