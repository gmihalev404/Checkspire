package com.example.chessforge.service.challenge.dto;

import java.util.List;

public record ChallengePageResponse(
        List<ChallengeSummaryResponse> incoming,
        List<ChallengeSummaryResponse> outgoing
) {
}