package com.example.checkspire.service.challenge.dto;

import java.util.List;

public record ChallengePageResponse(
        List<com.example.checkspire.service.challenge.dto.ChallengeSummaryResponse> incoming,
        List<com.example.checkspire.service.challenge.dto.ChallengeSummaryResponse> outgoing
) {
}