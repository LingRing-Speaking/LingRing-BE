package com.lingring.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.lingring.global.common.response.ApiResponse;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.DomainException;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.global.error.exception.IdpUnavailableException;
import com.lingring.global.error.exception.InternalServerException;
import com.lingring.global.error.exception.InvalidValueException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BadRequestException은 ErrorCode의 HttpStatus를 HTTP 상태와 본문에 함께 담는다")
    void handleBadRequestException_usesErrorCodeStatus() {
        // given
        final BadRequestException exception =
                new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "잘못된 요청입니다.");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadRequestException(exception);

        // then
        assertResponse(response, ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException은 400 INVALID_INPUT_VALUE로 변환한다")
    void handleMethodArgumentNotValidException_returns400() {
        // given
        final MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        given(exception.getMessage()).willReturn("validation failed");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleMethodArgumentNotValidException(exception);

        // then
        assertResponse(response, ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("UnauthorizedException은 401 상태를 반환한다")
    void handleUnauthorizedException_returns401() {
        // given
        final UnauthorizedException exception =
                new UnauthorizedException(ErrorCode.UNAUTHORIZED_ERROR, "인증이 필요합니다.");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnauthorizedException(exception);

        // then
        assertResponse(response, ErrorCode.UNAUTHORIZED_ERROR);
    }

    @Test
    @DisplayName("ForbiddenException은 403 상태를 반환한다")
    void handleForbiddenException_returns403() {
        // given
        final ForbiddenException exception =
                new ForbiddenException(ErrorCode.FORBIDDEN_ERROR, "권한이 없습니다.");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleForbiddenException(exception);

        // then
        assertResponse(response, ErrorCode.FORBIDDEN_ERROR);
    }

    @Test
    @DisplayName("NotFoundException은 404 상태를 반환한다")
    void handleNotFoundException_returns404() {
        // given
        final NotFoundException exception =
                new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "자원을 찾을 수 없습니다.");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotFoundException(exception);

        // then
        assertResponse(response, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("InvalidValueException은 ErrorCode의 HttpStatus를 반환한다")
    void handleInvalidValueException_usesErrorCodeStatus() {
        // given
        final InvalidValueException exception =
                new InvalidValueException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 값입니다.");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalArgumentException(exception);

        // then
        assertResponse(response, ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("DomainException은 하위 ErrorCode의 HttpStatus(409 등)를 동적으로 반환한다")
    void handleDomainException_usesErrorCodeStatus() {
        // given
        final DomainException exception =
                new TestDomainException(ErrorCode.NICKNAME_CONFLICT, "이미 사용 중인 닉네임입니다.");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleDomainException(exception);

        // then
        assertResponse(response, ErrorCode.NICKNAME_CONFLICT);
    }

    @Test
    @DisplayName("IdpUnavailableException은 502 상태를 반환한다")
    void handleIdpUnavailableException_returns502() {
        // given
        final IdpUnavailableException exception =
                new IdpUnavailableException(ErrorCode.IDP_UNAVAILABLE, "IdP 통신 실패");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleIdpUnavailableException(exception);

        // then
        assertResponse(response, ErrorCode.IDP_UNAVAILABLE);
    }

    @Test
    @DisplayName("InternalServerException은 500 상태를 반환한다")
    void handleInternalServerException_returns500() {
        // given
        final InternalServerException exception =
                new InternalServerException(ErrorCode.SERVER_ERROR, "서버 오류");

        // when
        final ResponseEntity<ApiResponse<Void>> response =
                handler.handleInternalServerException(exception);

        // then
        assertResponse(response, ErrorCode.SERVER_ERROR);
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500 SERVER_ERROR로 변환한다")
    void handleException_returns500() {
        // given
        final Exception exception = new RuntimeException("unexpected");

        // when
        final ResponseEntity<ApiResponse<Void>> response = handler.handleException(exception);

        // then
        assertResponse(response, ErrorCode.SERVER_ERROR);
    }

    private void assertResponse(
            final ResponseEntity<ApiResponse<Void>> response,
            final ErrorCode expected
    ) {
        final HttpStatus expectedStatus = expected.getHttpStatus();
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().message()).isEqualTo(expected.getMessage());
    }

    private static final class TestDomainException extends DomainException {

        private TestDomainException(final ErrorCode errorCode, final String message) {
            super(errorCode, message);
        }
    }
}
