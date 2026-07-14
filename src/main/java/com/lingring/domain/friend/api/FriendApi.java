package com.lingring.domain.friend.api;

import com.lingring.domain.friend.domain.FriendRequestDirection;
import com.lingring.domain.friend.domain.FriendshipStatus;
import com.lingring.domain.friend.dto.request.FriendRequestCreateRequest;
import com.lingring.domain.friend.dto.request.FriendshipUpdateRequest;
import com.lingring.domain.friend.dto.response.FriendSearchResponse;
import com.lingring.domain.friend.dto.response.FriendsResponse;
import com.lingring.domain.friend.dto.response.FriendshipResponse;
import com.lingring.domain.friend.dto.response.ReceivedCountResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Friend", description = "친구 API")
public interface FriendApi {

    @Operation(
            summary = "친구 요청 보내기",
            description = "인증된 사용자가 targetUserId에게 친구 요청을 보낸다(PENDING). "
                    + "상대가 이미 나에게 보낸 대기 요청이 있으면 새 요청 대신 즉시 수락되어 ACCEPTED로 반환된다. "
                    + "응답의 status로 PENDING(요청 전송) / ACCEPTED(즉시 성립)를 구분한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "친구 요청 생성 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 친구이거나 동일한 요청이 이미 존재함"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/friends")
    ApiResponse<FriendshipResponse> sendRequest(
            @AuthUser final Long userId,
            @Valid @RequestBody final FriendRequestCreateRequest request
    );

    @Operation(
            summary = "친구 요청 수락",
            description = "인증된 사용자(요청을 받은 쪽)가 requesterId 사용자로부터 받은 대기 요청을 수락한다(PENDING → ACCEPTED). "
                    + "요청 body의 status는 ACCEPTED만 허용된다. 요청을 보낸 본인(requester)이 호출하면 403."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수락 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 친구 요청을 수락할 권한이 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "수락할 친구 요청이 없음"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/friends/{requesterId}")
    ApiResponse<FriendshipResponse> accept(
            @AuthUser final Long userId,
            @Parameter(description = "친구 요청을 보낸 사용자 id", example = "2")
            @PathVariable("requesterId") final Long requesterId,
            @Valid @RequestBody final FriendshipUpdateRequest request
    );

    @Operation(
            summary = "친구 관계 제거",
            description = "인증된 사용자와 targetUserId 사이의 관계를 제거한다. "
                    + "대기 요청 거절/취소, 성립된 친구 삭제를 모두 처리한다. 관계가 없어도 204를 반환한다(멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "제거 성공 (대상 없음 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/friends/{targetUserId}")
    ApiResponse<Void> remove(
            @AuthUser final Long userId,
            @Parameter(description = "관계를 제거할 상대 사용자 id", example = "2")
            @PathVariable("targetUserId") final Long targetUserId
    );

    @Operation(
            summary = "친구 / 대기 요청 목록 조회",
            description = "인증된 사용자의 친구 관계를 status로 필터링해 최근순으로 반환한다. "
                    + "status=ACCEPTED(기본)는 친구 목록, status=PENDING은 대기 요청 목록(각 항목의 direction으로 받은/보낸 요청 구분). "
                    + "direction을 지정하면 대기 요청을 받은(RECEIVED)/보낸(SENT)으로 한정해 세그먼트별 독립 페이지네이션이 가능하다(생략 시 둘 다). "
                    + "무한 스크롤용 Slice. size는 서버에서 최대 50으로 clamp된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/friends")
    ApiResponse<FriendsResponse> getFriends(
            @AuthUser final Long userId,
            @Parameter(description = "조회할 관계 상태 (ACCEPTED: 친구, PENDING: 대기 요청)", example = "ACCEPTED")
            @RequestParam(defaultValue = "ACCEPTED") final FriendshipStatus status,
            @Parameter(description = "대기 요청 방향 필터 (RECEIVED: 받은, SENT: 보낸). 생략 시 둘 다", example = "RECEIVED")
            @RequestParam(required = false) final FriendRequestDirection direction,
            @Parameter(description = "0-based 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "페이지 크기 (1 ~ 50, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") final int size
    );

    @Operation(
            summary = "닉네임으로 사용자 검색 (친구 추가용)",
            description = "닉네임과 정확히 일치하는 사용자를 조회한다(닉네임은 유니크하므로 0/1건). "
                    + "결과에는 인증된 사용자와의 관계(relation)가 포함되어 FE가 버튼 상태를 그릴 수 있다. "
                    + "일치하는 사용자가 없으면 data는 null이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (미존재 시 data=null)",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/friends/search")
    ApiResponse<FriendSearchResponse> search(
            @AuthUser final Long userId,
            @Parameter(description = "검색할 닉네임 (정확 일치)", example = "지우")
            @RequestParam("nickname") final String nickname
    );

    @Operation(
            summary = "받은 대기 요청 개수 조회",
            description = "인증된 사용자가 받은 PENDING 친구 요청 개수를 반환한다(요청 목록 뱃지용)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/friends/received-count")
    ApiResponse<ReceivedCountResponse> receivedCount(
            @AuthUser final Long userId
    );
}
