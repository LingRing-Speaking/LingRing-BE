package com.lingring.domain.useragreement.dto.request;

import com.lingring.domain.useragreement.domain.AgreementItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AgreementCreateRequest(
        @NotBlank
        @Size(max = 20)
        String termsVersion,

        @NotEmpty
        Set<AgreementItem> agreedItems
) {
}
