package com.example.checkspire.scheduler.challenge;

import com.example.checkspire.service.challenge.ChallengeService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ChallengeExpirationSchedulerTest {

    @Test
    void expireChallengesShouldDelegateToService() {

        ChallengeService challengeService =
                mock(ChallengeService.class);

        ChallengeExpirationScheduler scheduler =
                new ChallengeExpirationScheduler(
                        challengeService
                );

        scheduler.expireChallenges();

        verify(challengeService).expireChallenges();
    }
}