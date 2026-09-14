package com.example.chessforge.controller.game;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GameMoveResponse;
import com.example.chessforge.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameHistoryControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private GameHistoryController controller;

    private User currentUser;


    @BeforeEach
    void setUp() {

        currentUser =
                User.builder()
                        .username("challenger")
                        .build();
    }


    @Test
    void moveHistoryShouldReturnCurrentPlayersMoves() {

        Long gameId = 1L;

        List<GameMoveResponse> moves =
                List.of(
                        new GameMoveResponse(
                                1,
                                "e4"
                        ),
                        new GameMoveResponse(
                                2,
                                "e5"
                        ),
                        new GameMoveResponse(
                                3,
                                "Nf3"
                        )
                );


        when(
                principal.getName()
        ).thenReturn(
                "challenger"
        );

        when(
                userService.findByUsername(
                        "challenger"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                gameService.getMoveHistory(
                        gameId,
                        currentUser
                )
        ).thenReturn(
                moves
        );


        List<GameMoveResponse> result =
                controller.moveHistory(
                        gameId,
                        principal
                );


        assertSame(
                moves,
                result
        );

        assertEquals(
                3,
                result.size()
        );


        verify(
                userService
        ).findByUsername(
                "challenger"
        );

        verify(
                gameService
        ).getMoveHistory(
                gameId,
                currentUser
        );
    }


    @Test
    void moveHistoryShouldRejectMissingAuthenticatedUser() {

        Long gameId = 1L;


        when(
                principal.getName()
        ).thenReturn(
                "missing-user"
        );

        when(
                userService.findByUsername(
                        "missing-user"
                )
        ).thenReturn(
                Optional.empty()
        );


        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                controller.moveHistory(
                                        gameId,
                                        principal
                                )
                );


        assertEquals(
                "Authenticated user was not found.",
                exception.getMessage()
        );


        verifyNoInteractions(
                gameService
        );
    }
}