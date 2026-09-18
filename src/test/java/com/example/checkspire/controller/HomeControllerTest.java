package com.example.checkspire.controller;

import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HomeControllerTest {

    private HomeController controller;

    private UserService userService;

    private Model model;

    @BeforeEach
    void setUp() {

        userService =
                mock(UserService.class);

        controller =
                new HomeController(
                        userService
                );

        model =
                mock(Model.class);
    }

    @Test
    void homeShouldAddAuthenticatedUserData() {

        Principal principal =
                () -> "testplayer";

        User user =
                User.builder()
                        .username("testplayer")
                        .bulletRating(400)
                        .blitzRating(500)
                        .rapidRating(600)
                        .classicalRating(700)
                        .build();

        when(
                userService.findByUsername(
                        "testplayer"
                )
        ).thenReturn(
                Optional.of(user)
        );

        String result =
                controller.home(
                        principal,
                        model
                );

        assertEquals(
                "home",
                result
        );

        verify(model)
                .addAttribute(
                        "username",
                        "testplayer"
                );

        verify(model)
                .addAttribute(
                        "currentUser",
                        user
                );

        verify(userService)
                .findByUsername(
                        "testplayer"
                );
    }

    @Test
    void homeShouldNotAddUserDataForAnonymousUser() {

        String result =
                controller.home(
                        null,
                        model
                );

        assertEquals(
                "home",
                result
        );

        verifyNoInteractions(
                model
        );

        verifyNoInteractions(
                userService
        );
    }

    @Test
    void homeShouldStillLoadWhenAuthenticatedUserIsNotFound() {

        Principal principal =
                () -> "missingplayer";

        when(
                userService.findByUsername(
                        "missingplayer"
                )
        ).thenReturn(
                Optional.empty()
        );

        String result =
                controller.home(
                        principal,
                        model
                );

        assertEquals(
                "home",
                result
        );

        verify(model)
                .addAttribute(
                        "username",
                        "missingplayer"
                );

        verify(model, never())
                .addAttribute(
                        eq("currentUser"),
                        any()
                );
    }
}