# 패키지 구조
- 공통 글로벌 예외 (`ErrorCode`, `GlobalExceptionHandler`, 범용 예외 5종): `com.lingring.global.error` 하위
- 커스텀 도메인 예외: `domain/{도메인}/exception` (예: `domain/member/exception`)

# ErrorCode
- enum + `HttpStatus httpStatus`, `String message` 두 필드로 고정
- `@RequiredArgsConstructor`, `@Getter`만 사용
- `HttpStatus`는 static import로 작성
- 도메인/영역별로 주석으로 그룹 구분 (`// Global Error`, `// Member Error`)
- 상수명은 `UPPER_SNAKE_CASE`, 메시지는 클라이언트 노출 가능한 한국어 문장

# 예외 클래스 계층
- **공통 글로벌 예외**: `BadRequestException`, `NotFoundException`, `UnauthorizedException`, `ForbiddenException`, `InvalidValueException` — `RuntimeException` 직접 상속. HTTP 의미 기반 범용 예외
- **커스텀 도메인 예외**: `DomainException`(abstract) 반드시 상속. 클라이언트에는 고정 메시지를 반환하면서 타입별 로그 레벨을 분리하기 위함

# 예외 필드/생성자 (모든 예외 공통)
```java
private final ErrorCode errorCode;
private final String message;

public XxxException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
    this.message = message;
}
```
- `@Getter`만 사용 (`@AllArgsConstructor` 등 생성자 자동 생성 금지 — 시그니처 고정)

# 예외 선택 기준
- 잘못된 요청 → `BadRequestException`
- 도메인 값 유효성 위반 → `InvalidValueException`
- 리소스 미존재 → `NotFoundException`
- 인증 실패 → `UnauthorizedException`
- 권한 부족 → `ForbiddenException`
- 도메인 규칙/불변식 위반 → `DomainException` 하위 타입

# 예외 던지기
- 서비스/도메인 레이어에서 `IllegalArgumentException` 등 자바 기본 예외 직접 사용 금지
- 메시지는 원인을 식별할 수 있는 구체적인 한국어로 작성 (예: `"ID가 %d인 회원을 찾을 수 없습니다".formatted(id)`)
- `errorCode.getMessage()`를 그대로 재사용하지 않는다 (로그 식별성 유지)