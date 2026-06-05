package com.lingring.domain.matching.dao.dto;

import com.lingring.domain.matching.domain.MatchConfirmation;

public record AcceptResult(AcceptOutcome outcome, MatchConfirmation confirmation) {
}
