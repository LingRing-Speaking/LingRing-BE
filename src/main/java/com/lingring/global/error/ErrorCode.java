package com.lingring.global.error;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
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

    // Auth Error
    INVALID_TOKEN(UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    INVALID_ID_TOKEN(UNAUTHORIZED, "유효하지 않은 IdP 토큰입니다."),
    IDP_UNAVAILABLE(BAD_GATEWAY, "IdP 인증 서버 통신에 실패했습니다. 잠시 후 다시 시도해주세요."),
    NICKNAME_CONFLICT(CONFLICT, "이미 사용 중인 닉네임입니다."),
    NICKNAME_REQUIRED(BAD_REQUEST, "신규 가입 시 nickname은 필수입니다."),

    // User Error
    INVALID_USER_NAME(BAD_REQUEST, "유효하지 않은 사용자 이름입니다."),
    USER_NOT_FOUND(NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // UserStats Error
    USER_STATS_NOT_FOUND(NOT_FOUND, "사용자 통계를 찾을 수 없습니다."),

    // UserExpression Error
    INVALID_EXPRESSION(BAD_REQUEST, "유효하지 않은 표현입니다."),
    INVALID_MEANING(BAD_REQUEST, "유효하지 않은 뜻입니다."),

    // RecommendedExpression Error
    INVALID_RECOMMENDED_EXPRESSION(BAD_REQUEST, "유효하지 않은 추천 표현입니다."),
    INVALID_RECOMMENDED_MEANING(BAD_REQUEST, "유효하지 않은 추천 뜻입니다."),
    RECOMMENDED_EXPRESSION_NOT_FOUND(NOT_FOUND, "오늘의 추천 표현을 찾을 수 없습니다."),

    // Icebreaker Error
    INVALID_ICEBREAKER_EXPRESSION(BAD_REQUEST, "유효하지 않은 아이스브레이커 표현입니다."),
    INVALID_ICEBREAKER_MEANING(BAD_REQUEST, "유효하지 않은 아이스브레이커 뜻입니다."),
    ICEBREAKER_NOT_FOUND(NOT_FOUND, "아이스브레이커를 찾을 수 없습니다."),

    // UserBlock Error
    SELF_BLOCK_NOT_ALLOWED(BAD_REQUEST, "자기 자신을 차단할 수 없습니다."),

    // UserReport Error
    SELF_REPORT_NOT_ALLOWED(BAD_REQUEST, "자기 자신을 신고할 수 없습니다."),
    INVALID_REPORT_DESCRIPTION(BAD_REQUEST, "유효하지 않은 신고 상세 내용입니다."),

    // Matching Error
    MATCH_NOT_FOUND(NOT_FOUND, "매칭을 찾을 수 없습니다."),
    MATCH_PARTICIPANT_MISMATCH(FORBIDDEN, "해당 매칭의 참여자가 아닙니다."),

    // Call Error
    CALL_HISTORY_NOT_FOUND(NOT_FOUND, "통화 기록을 찾을 수 없습니다."),
    CALL_PARTICIPANT_MISMATCH(FORBIDDEN, "해당 통화의 참여자가 아닙니다."),

    // Signaling Error
    SIGNALING_INVALID_USER(BAD_REQUEST, "시그널링 연결에 유효하지 않은 사용자입니다."),
    SIGNALING_INVALID_PAYLOAD(BAD_REQUEST, "유효하지 않은 시그널링 메시지입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
