package com.example.checkspire.controller.challenge;

import com.example.checkspire.controller.challenge.dto.ChallengeRequest;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.model.enums.challenge.ColorPreference;
import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.service.challenge.ChallengeApplicationService;
import com.example.checkspire.service.challenge.dto.ChallengePageResponse;
import com.example.checkspire.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeControllerTest {

    @Mock
    private ChallengeApplicationService
            challengeApplicationService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private ChallengeController controller;

    private User currentUser;
    private User opponent;


    @BeforeEach
    void setUp() {

        currentUser =
                User.builder()
                        .username("player")
                        .build();

        opponent =
                User.builder()
                        .username("opponent")
                        .build();

        when(
                principal.getName()
        ).thenReturn(
                "player"
        );
    }


    @Test
    void challengesShouldLoadChallengePage() {

        ChallengePageResponse page =
                new ChallengePageResponse(
                        List.of(),
                        List.of()
                );

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                challengeApplicationService
                        .getChallenges(
                                currentUser
                        )
        ).thenReturn(
                page
        );


        String view =
                controller.challenges(
                        principal,
                        model
                );


        assertEquals(
                "challenge/challenges",
                view
        );

        verify(model)
                .addAttribute(
                        "incoming",
                        page.incoming()
                );

        verify(model)
                .addAttribute(
                        "outgoing",
                        page.outgoing()
                );

        verify(model)
                .addAttribute(
                        "username",
                        "player"
                );
    }


    @Test
    void newChallengeShouldCreateRequestWhenMissing() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                model.containsAttribute(
                        "challengeRequest"
                )
        ).thenReturn(
                false
        );


        String view =
                controller.newChallenge(
                        principal,
                        model
                );


        assertEquals(
                "challenge/new-challenge",
                view
        );

        verify(model)
                .addAttribute(
                        eq("challengeRequest"),
                        any(ChallengeRequest.class)
                );

        verify(model)
                .addAttribute(
                        "timeControls",
                        TimeControl.values()
                );

        verify(model)
                .addAttribute(
                        "colorPreferences",
                        ColorPreference.values()
                );

        verify(model)
                .addAttribute(
                        "username",
                        "player"
                );
    }


    @Test
    void newChallengeShouldKeepExistingRequest() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                model.containsAttribute(
                        "challengeRequest"
                )
        ).thenReturn(
                true
        );


        controller.newChallenge(
                principal,
                model
        );


        verify(
                model,
                never()
        ).addAttribute(
                eq("challengeRequest"),
                any()
        );
    }


    @Test
    void sendChallengeShouldSendAndRedirect() {

        ChallengeRequest request =
                createRequest(
                        " opponent "
                );


        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                userService.findByUsername(
                        "opponent"
                )
        ).thenReturn(
                Optional.of(
                        opponent
                )
        );


        String result =
                controller.sendChallenge(
                        request,
                        principal,
                        model,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/challenges",
                result
        );

        verify(challengeApplicationService)
                .sendChallenge(
                        currentUser,
                        opponent,
                        request.getTimeControl(),
                        request.getColorPreference(),
                        request.isRated()
                );

        verify(redirectAttributes)
                .addFlashAttribute(
                        "success",
                        "Challenge sent to opponent."
                );
    }


    @Test
    void sendChallengeShouldRejectBlankOpponentUsername() {

        ChallengeRequest request =
                createRequest(
                        "   "
                );


        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );


        String result =
                controller.sendChallenge(
                        request,
                        principal,
                        model,
                        redirectAttributes
                );


        assertEquals(
                "challenge/new-challenge",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "Opponent username is required."
                );

        verifyNoInteractions(
                challengeApplicationService
        );
    }


    @Test
    void sendChallengeShouldShowErrorWhenOpponentDoesNotExist() {

        ChallengeRequest request =
                createRequest(
                        "missing"
                );


        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                userService.findByUsername(
                        "missing"
                )
        ).thenReturn(
                Optional.empty()
        );


        String result =
                controller.sendChallenge(
                        request,
                        principal,
                        model,
                        redirectAttributes
                );


        assertEquals(
                "challenge/new-challenge",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "User not found."
                );

        verifyNoInteractions(
                challengeApplicationService
        );
    }


    @Test
    void sendChallengeShouldShowServiceError() {

        ChallengeRequest request =
                createRequest(
                        "opponent"
                );


        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                userService.findByUsername(
                        "opponent"
                )
        ).thenReturn(
                Optional.of(
                        opponent
                )
        );

        doThrow(
                new IllegalStateException(
                        "Challenge already exists."
                )
        ).when(
                challengeApplicationService
        ).sendChallenge(
                currentUser,
                opponent,
                request.getTimeControl(),
                request.getColorPreference(),
                request.isRated()
        );


        String result =
                controller.sendChallenge(
                        request,
                        principal,
                        model,
                        redirectAttributes
                );


        assertEquals(
                "challenge/new-challenge",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "Challenge already exists."
                );
    }


    @Test
    void acceptShouldRedirectToCreatedGame() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                challengeApplicationService
                        .acceptAndStart(
                                1L,
                                currentUser
                        )
        ).thenReturn(
                42L
        );


        String result =
                controller.accept(
                        1L,
                        principal,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/games/42",
                result
        );
    }


    @Test
    void acceptShouldRedirectBackWhenServiceFails() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                challengeApplicationService
                        .acceptAndStart(
                                1L,
                                currentUser
                        )
        ).thenThrow(
                new IllegalStateException(
                        "Challenge cannot be accepted."
                )
        );


        String result =
                controller.accept(
                        1L,
                        principal,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/challenges",
                result
        );

        verify(redirectAttributes)
                .addFlashAttribute(
                        "error",
                        "Challenge cannot be accepted."
                );
    }


    @Test
    void declineShouldDeclineAndRedirect() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );


        String result =
                controller.decline(
                        1L,
                        principal,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/challenges",
                result
        );

        verify(challengeApplicationService)
                .decline(
                        1L,
                        currentUser
                );

        verify(redirectAttributes)
                .addFlashAttribute(
                        "success",
                        "Challenge declined."
                );
    }


    @Test
    void declineShouldAddErrorWhenServiceFails() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        doThrow(
                new IllegalStateException(
                        "Cannot decline challenge."
                )
        ).when(
                challengeApplicationService
        ).decline(
                1L,
                currentUser
        );


        String result =
                controller.decline(
                        1L,
                        principal,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/challenges",
                result
        );

        verify(redirectAttributes)
                .addFlashAttribute(
                        "error",
                        "Cannot decline challenge."
                );
    }


    @Test
    void cancelShouldCancelAndRedirect() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );


        String result =
                controller.cancel(
                        1L,
                        principal,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/challenges",
                result
        );

        verify(challengeApplicationService)
                .cancel(
                        1L,
                        currentUser
                );

        verify(redirectAttributes)
                .addFlashAttribute(
                        "success",
                        "Challenge cancelled."
                );
    }


    @Test
    void cancelShouldAddErrorWhenServiceFails() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        doThrow(
                new IllegalArgumentException(
                        "Challenge not found."
                )
        ).when(
                challengeApplicationService
        ).cancel(
                1L,
                currentUser
        );


        String result =
                controller.cancel(
                        1L,
                        principal,
                        redirectAttributes
                );


        assertEquals(
                "redirect:/challenges",
                result
        );

        verify(redirectAttributes)
                .addFlashAttribute(
                        "error",
                        "Challenge not found."
                );
    }


    @Test
    void actionShouldThrowWhenAuthenticatedUserDoesNotExist() {

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.empty()
        );


        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                controller.challenges(
                                        principal,
                                        model
                                )
                );


        assertEquals(
                "Authenticated user was not found.",
                exception.getMessage()
        );

        verifyNoInteractions(
                challengeApplicationService
        );
    }


    private ChallengeRequest createRequest(
            String opponentUsername
    ) {

        ChallengeRequest request =
                new ChallengeRequest();

        request.setOpponentUsername(
                opponentUsername
        );

        request.setTimeControl(
                TimeControl.RAPID_10_0
        );

        request.setColorPreference(
                ColorPreference.RANDOM
        );

        request.setRated(
                true
        );

        return request;
    }
}