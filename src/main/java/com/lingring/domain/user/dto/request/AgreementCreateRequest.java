package com.lingring.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AgreementCreateRequest(
        @NotBlank
        @Size(max = 20)
        String termsVersion,

        @NotEmpty
        Set<@NotBlank String> agreedItems
) {
}
