package com.example.checkspire.controller.game;

import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.game.GameService;
import com.example.checkspire.service.game.dto.GamePageResponse;
import com.example.checkspire.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final UserService userService;

    @GetMapping("/games")
    public String games(
            Principal principal,
            Model model
    ) {

        User currentUser =
                userService.findByUsername(
                        principal.getName()
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found."
                        )
                );

        model.addAttribute(
                "games",
                gameService.getGameSummariesForUser(
                        currentUser
                )
                        .stream().limit(100).toList()
        );

        model.addAttribute(
                "username",
                currentUser.getUsername()
        );

        return "game/games";
    }

    @GetMapping("/games/{gameId}")
    public String game(
            @PathVariable Long gameId,
            Principal principal,
            Model model
    ) {

        User currentUser =
                userService.findByUsername(
                        principal.getName()
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found."
                        )
                );

        GamePageResponse page =
                gameService.getGamePage(
                        gameId,
                        currentUser
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND
                        )
                );

        model.addAttribute(
                "viewerParticipant",
                page.viewerParticipant()
        );

        model.addAttribute(
                "tournamentId",
                page.tournamentId()
        );

        model.addAttribute(
                "game",
                page.game()
        );

        model.addAttribute(
                "viewerColor",
                page.viewerColor()
                        .name()
        );

        model.addAttribute(
                "username",
                currentUser.getUsername()
        );

        model.addAttribute(
                "whiteTimeRemainingMillis",
                page.whiteTimeRemainingMillis()
        );

        model.addAttribute(
                "blackTimeRemainingMillis",
                page.blackTimeRemainingMillis()
        );

        model.addAttribute(
                "currentUserId",
                currentUser.getId()
        );

        return "game/game";
    }
}