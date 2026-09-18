package com.example.checkspire.controller.user;

import com.example.checkspire.controller.user.dto.RegisterRequest;
import com.example.checkspire.service.user.CheckspireUserDetailsService;
import com.example.checkspire.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

    private final CheckspireUserDetailsService
            userDetailsService;

    @GetMapping("/register")
    public String showRegisterPage(
            Model model
    ) {

        model.addAttribute(
                "registerRequest",
                new RegisterRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute RegisterRequest request,
            Model model,
            HttpServletRequest httpRequest
    ) {

        if (!passwordsMatch(request)) {

            model.addAttribute(
                    "error",
                    "Passwords do not match."
            );

            return "register";
        }

        try {

            userService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            authenticateRegisteredUser(
                    request.getUsername(),
                    httpRequest
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );

            return "register";
        }

        return "redirect:/";
    }

    private void authenticateRegisteredUser(
            String username,
            HttpServletRequest request
    ) {

        UserDetails userDetails =
                userDetailsService
                        .loadUserByUsername(
                                username
                        );

        UsernamePasswordAuthenticationToken
                authentication =
                UsernamePasswordAuthenticationToken
                        .authenticated(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

        SecurityContext securityContext =
                SecurityContextHolder
                        .createEmptyContext();

        securityContext.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                securityContext
        );

        HttpSession session =
                request.getSession(
                        true
                );

        request.changeSessionId();

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );
    }

    private boolean passwordsMatch(
            RegisterRequest request
    ) {

        if (request.getPassword() == null) {
            return request.getConfirmPassword() == null;
        }

        return request.getPassword()
                .equals(
                        request.getConfirmPassword()
                );
    }
}