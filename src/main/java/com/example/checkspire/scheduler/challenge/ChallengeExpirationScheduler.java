package com.example.checkspire.scheduler.challenge;

import com.example.checkspire.service.challenge.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChallengeExpirationScheduler {

    private final ChallengeService challengeService;

    @Scheduled(fixedRate = 60_000) //checks for expired challenges every 60 seconds
    public void expireChallenges() {
        challengeService.expireChallenges();
    }
}