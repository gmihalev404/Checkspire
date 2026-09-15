package com.example.chessforge.controller.game;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GamePageResponse;
import com.example.chessforge.service.game.dto.GameSummaryResponse;
import com.example.chessforge.service.game.engine.model.PieceColor;
import com.example.chessforge.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private User user;

    private GameController controller;


    @BeforeEach
    void setUp() {

        controller =
                new GameController(
                        gameService,
                        userService
                );
    }


    @Test
    void gamesShouldLoadAuthenticatedUsersGames() {

        Principal principal =
                () -> "testplayer";

        List<GameSummaryResponse> games =
                List.of();


        when(
                userService.findByUsername(
                        "testplayer"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getUsername()
        ).thenReturn(
                "testplayer"
        );

        when(
                gameService.getGameSummariesForUser(
                        user
                )
        ).thenReturn(
                games
        );


        String result =
                controller.games(
                        principal,
                        model
                );


        assertEquals(
                "game/games",
                result
        );

        verify(model)
                .addAttribute(
                        "games",
                        games
                );

        verify(model)
                .addAttribute(
                        "username",
                        "testplayer"
                );
    }


    @Test
    void gameShouldLoadGameForAuthenticatedParticipant() {

        Long gameId = 10L;
        Long tournamentId = 20L;
        Long userId = 1L;

        Principal principal =
                () -> "testplayer";


        GamePageResponse page =
                new GamePageResponse(
                        null,
                        PieceColor.WHITE,
                        540_000L,
                        515_000L,
                        tournamentId,
                        true
                );


        when(
                userService.findByUsername(
                        "testplayer"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getUsername()
        ).thenReturn(
                "testplayer"
        );

        when(
                user.getId()
        ).thenReturn(
                userId
        );

        when(
                gameService.getGamePage(
                        gameId,
                        user
                )
        ).thenReturn(
                Optional.of(page)
        );


        String result =
                controller.game(
                        gameId,
                        principal,
                        model
                );


        assertEquals(
                "game/game",
                result
        );


        verify(model)
                .addAttribute(
                        "game",
                        null
                );

        verify(model)
                .addAttribute(
                        "viewerColor",
                        "WHITE"
                );

        verify(model)
                .addAttribute(
                        "whiteTimeRemainingMillis",
                        540_000L
                );

        verify(model)
                .addAttribute(
                        "blackTimeRemainingMillis",
                        515_000L
                );

        verify(model)
                .addAttribute(
                        "username",
                        "testplayer"
                );

        verify(model)
                .addAttribute(
                        "currentUserId",
                        userId
                );

        verify(model)
                .addAttribute(
                        "tournamentId",
                        tournamentId
                );

        verify(model)
                .addAttribute(
                        "viewerParticipant",
                        true
                );
    }


    @Test
    void gameShouldLoadGameForAuthenticatedSpectator() {

        Long gameId = 10L;
        Long tournamentId = 20L;
        Long userId = 99L;

        Principal principal =
                () -> "spectator";


        GamePageResponse page =
                new GamePageResponse(
                        null,
                        PieceColor.WHITE,
                        500_000L,
                        480_000L,
                        tournamentId,
                        false
                );


        when(
                userService.findByUsername(
                        "spectator"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getUsername()
        ).thenReturn(
                "spectator"
        );

        when(
                user.getId()
        ).thenReturn(
                userId
        );

        when(
                gameService.getGamePage(
                        gameId,
                        user
                )
        ).thenReturn(
                Optional.of(page)
        );


        String result =
                controller.game(
                        gameId,
                        principal,
                        model
                );


        assertEquals(
                "game/game",
                result
        );

        verify(model)
                .addAttribute(
                        "viewerParticipant",
                        false
                );

        verify(model)
                .addAttribute(
                        "viewerColor",
                        "WHITE"
                );

        verify(model)
                .addAttribute(
                        "tournamentId",
                        tournamentId
                );
    }


    @Test
    void gameShouldReturnNotFoundWhenGameDoesNotExist() {

        Long gameId = 999L;

        Principal principal =
                () -> "testplayer";


        when(
                userService.findByUsername(
                        "testplayer"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                gameService.getGamePage(
                        gameId,
                        user
                )
        ).thenReturn(
                Optional.empty()
        );


        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.game(
                                        gameId,
                                        principal,
                                        model
                                )
                );


        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }
}