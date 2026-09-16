package com.example.chessforge.controller;

import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping("/")
    public String home(
            Principal principal,
            Model model
    ) {

        if (principal != null) {

            model.addAttribute(
                    "username",
                    principal.getName()
            );

            userService
                    .findByUsername(
                            principal.getName()
                    )
                    .ifPresent(user ->
                            model.addAttribute(
                                    "currentUser",
                                    user
                            )
                    );
        }

        return "home";
    }
}