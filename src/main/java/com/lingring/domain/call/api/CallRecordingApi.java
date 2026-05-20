package com.lingring.domain.call.api;

import com.lingring.domain.call.dto.request.CallRecordingCreateRequest;
import com.lingring.domain.call.dto.request.CallRecordingPresignedUrlRequest;
import com.lingring.domain.call.dto.response.CallRecordingCreateResponse;
import com.lingring.domain.call.dto.response.CallRecordingPresignedUrlResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "CallRecording", description = "통화 녹음 업로드 API")
public interface CallRecordingApi {

    @Operation(
            summary = "녹음 업로드 presigned URL 발급",
            description = "통화 종료 후 본인 녹음 파일을 S3에 PUT할 수 있는 일회용 URL을 발급한다."
                    + " contentType/contentLength는 서버 허용 정책으로 검증된다."
                    + " 통화가 아직 진행 중이거나 본인이 참여자가 아니면 거절된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/calls/{callId}/recordings/presigned-url")
    ApiResponse<CallRecordingPresignedUrlResponse> createPresignedUrl(
            @AuthUser final Long userId,
            @Parameter(description = "통화 ID", example = "42")
            @PathVariable final Long callId,
            @Valid @RequestBody final CallRecordingPresignedUrlRequest request
    );

    @Operation(
            summary = "녹음 업로드 완료 알림",
            description = "S3 업로드 완료 후 호출. CallRecording 메타데이터를 영속화한다."
                    + " (callId, userId)당 한 row만 유지되며, 재요청은 기존 row를 그대로 반환해 멱등하게 동작한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/calls/{callId}/recordings")
    ApiResponse<CallRecordingCreateResponse> create(
            @AuthUser final Long userId,
            @Parameter(description = "통화 ID", example = "42")
            @PathVariable final Long callId,
            @Valid @RequestBody final CallRecordingCreateRequest request
    );
}
