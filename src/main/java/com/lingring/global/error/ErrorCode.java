package com.lingring.global.error;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    // Global Error
    SERVER_ERROR(INTERNAL_SERVER_ERROR,  "예상치 못한 서버 에러가 발생하였습니다"),
    RESOURCE_NOT_FOUND(NOT_FOUND, "요청한 자원을 찾을 수 없습니다"),
    NOT_SUPPORTED(BAD_REQUEST,  "지원하지 않는 요청입니다"),
    RESOURCE_EXISTS(CONFLICT,  "이미 존재하는 자원입니다."),
    INVALID_INPUT_VALUE(BAD_REQUEST, "유효하지 않은 입력값입니다."),
    UNAUTHORIZED_ERROR(UNAUTHORIZED, "인증이 필요합니다. 로그인 후 다시 시도해주세요."),
    FORBIDDEN_ERROR(FORBIDDEN, "접근 권한이 없습니다."),
    LOGIN_INVALID_CREDENTIALS(BAD_REQUEST, "아이디 또는 비밀번호가 일치하지 않습니다."),

    // User Error
    INVALID_USER_NAME(BAD_REQUEST, "유효하지 않은 사용자 이름입니다."),
    USER_NOT_FOUND(NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // UserStats Error
    USER_STATS_NOT_FOUND(NOT_FOUND, "사용자 통계를 찾을 수 없습니다."),

    // UserExpression Error
    INVALID_EXPRESSION(BAD_REQUEST, "유효하지 않은 표현입니다."),
    INVALID_MEANING(BAD_REQUEST, "유효하지 않은 뜻입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
