package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.request.PresignedUrlRequest;
import com.lingring.domain.user.dto.request.UpdateProfileRequest;
import com.lingring.domain.user.dto.request.WithdrawRequest;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.PresignedUrlResponse;
import com.lingring.domain.user.dto.response.UpdateProfileResponse;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

    @Operation(
            summary = "내 정보 조회",
            description = "Access token 유효성 검증과 함께 소유자 정보(id, nickname)를 반환한다. FE 부팅 시 자동 로그인 흐름에서 사용."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    ApiResponse<MeResponse> getMe(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "타인 프로필 조회",
            description = "userId에 해당하는 사용자의 공개 프로필(id, nickname, level, mannerTemperature)을 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{userId}")
    ApiResponse<UserProfileResponse> getUserProfile(
            @Parameter(description = "조회 대상 user PK", example = "7")
            @PathVariable final Long userId
    );

    @Operation(
            summary = "회원탈퇴",
            description = "인증된 사용자의 계정을 탈퇴 처리한다. body에 reason(enum)을 필수로 동봉하며, reason이 OTHER일 때만 description(<=200자)을 함께 보낸다. "
                    + "User/UserStats/UserBlock(본인이 한 차단)/UserExpression은 hard-delete, "
                    + "Call·UserReport는 탈퇴자 식별자 컬럼만 NULL로 익명화한다. RefreshToken은 폐기된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "탈퇴 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/withdraw")
    ApiResponse<Void> withdraw(
            @AuthUser final Long userId,
            @Valid @RequestBody final WithdrawRequest request
    );

    @Operation(
            summary = "프로필 이미지 업로드용 presigned URL 발급",
            description = "S3에 직접 PUT 업로드할 수 있는 presigned URL과 객체 키를 발급한다. 키는 서버가 발급하며 본인 user prefix로 고정된다. "
                    + "발급된 키로 업로드를 마친 뒤 PATCH /me/profile에 profileImageKey로 동봉해 변경을 확정한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/me/profile/image/presigned-url")
    ApiResponse<PresignedUrlResponse> createProfileImageUploadUrl(
            @AuthUser final Long userId,
            @Valid @RequestBody final PresignedUrlRequest request
    );

    @Operation(
            summary = "프로필 변경",
            description = "닉네임 또는 프로필 이미지 키(혹은 둘 다)를 받아 변경한다. 둘 중 하나는 반드시 포함되어야 한다. "
                    + "profileImageKey는 사전에 /me/profile/image/presigned-url로 받은 키를 사용한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    ref = "#/components/responses/Forbidden"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    ref = "#/components/responses/Conflict"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/me/profile")
    ApiResponse<UpdateProfileResponse> updateProfile(
            @AuthUser final Long userId,
            @Valid @RequestBody final UpdateProfileRequest request
    );
}
