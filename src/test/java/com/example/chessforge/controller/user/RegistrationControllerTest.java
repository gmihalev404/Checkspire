package com.example.chessforge.controller.user;

import com.example.chessforge.controller.user.dto.RegisterRequest;
import com.example.chessforge.service.user.ChessForgeUserDetailsService;
import com.example.chessforge.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ChessForgeUserDetailsService
            userDetailsService;

    @Mock
    private Model model;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpSession session;

    private RegistrationController controller;

    @BeforeEach
    void setUp() {

        controller =
                new RegistrationController(
                        userService,
                        userDetailsService
                );
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder
                .clearContext();
    }

    @Test
    void showRegisterPageShouldReturnRegisterView() {

        String result =
                controller.showRegisterPage(
                        model
                );

        assertEquals(
                "register",
                result
        );

        verify(model)
                .addAttribute(
                        eq("registerRequest"),
                        any(RegisterRequest.class)
                );
    }

    @Test
    void registerShouldCreateAuthenticateAndRedirectHome() {

        RegisterRequest request =
                createRequest();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername("georgi")
                        .password("encoded-password")
                        .authorities("USER")
                        .build();

        when(
                userDetailsService
                        .loadUserByUsername(
                                "georgi"
                        )
        ).thenReturn(
                userDetails
        );

        when(
                httpRequest.getSession(
                        true
                )
        ).thenReturn(
                session
        );

        String result =
                controller.register(
                        request,
                        model,
                        httpRequest
                );

        assertEquals(
                "redirect:/",
                result
        );

        verify(userService)
                .register(
                        "georgi",
                        "georgi@test.com",
                        "secret123"
                );

        verify(userDetailsService)
                .loadUserByUsername(
                        "georgi"
                );

        verify(httpRequest)
                .changeSessionId();

        verify(session)
                .setAttribute(
                        eq(
                                HttpSessionSecurityContextRepository
                                        .SPRING_SECURITY_CONTEXT_KEY
                        ),
                        any(SecurityContext.class)
                );

        assertNotNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        assertEquals(
                "georgi",
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName()
        );
    }

    @Test
    void registerShouldRejectDifferentPasswords() {

        RegisterRequest request =
                createRequest();

        request.setConfirmPassword(
                "different"
        );

        String result =
                controller.register(
                        request,
                        model,
                        httpRequest
                );

        assertEquals(
                "register",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "Passwords do not match."
                );

        verifyNoInteractions(
                userService
        );

        verifyNoInteractions(
                userDetailsService
        );
    }

    @Test
    void registerShouldReturnRegisterPageWhenServiceRejectsUser() {

        RegisterRequest request =
                createRequest();

        doThrow(
                new IllegalStateException(
                        "Username is already taken."
                )
        ).when(userService)
                .register(
                        "georgi",
                        "georgi@test.com",
                        "secret123"
                );

        String result =
                controller.register(
                        request,
                        model,
                        httpRequest
                );

        assertEquals(
                "register",
                result
        );

        verify(model)
                .addAttribute(
                        "error",
                        "Username is already taken."
                );

        verifyNoInteractions(
                userDetailsService
        );
    }

    private RegisterRequest createRequest() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername(
                "georgi"
        );

        request.setEmail(
                "georgi@test.com"
        );

        request.setPassword(
                "secret123"
        );

        request.setConfirmPassword(
                "secret123"
        );

        return request;
    }
}