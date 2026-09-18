package com.example.checkspire.controller.game;

import com.example.checkspire.service.game.GameService;
import com.example.checkspire.service.game.dto.GameMoveResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameHistoryControllerTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameHistoryController controller;


    @Test
    void moveHistoryShouldReturnGameMoves() {

        Long gameId = 1L;


        List<GameMoveResponse> moves =
                List.of(
                        new GameMoveResponse(
                                1,
                                "e4",
                                "e2",
                                "e4",
                                "fen-1"
                        ),
                        new GameMoveResponse(
                                2,
                                "e5",
                                "e7",
                                "e5",
                                "fen-2"
                        ),
                        new GameMoveResponse(
                                3,
                                "Nf3",
                                "g1",
                                "f3",
                                "fen-3"
                        )
                );


        when(
                gameService.getMoveHistory(
                        gameId
                )
        ).thenReturn(
                moves
        );


        List<GameMoveResponse> result =
                controller.moveHistory(
                        gameId
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
                gameService
        ).getMoveHistory(
                gameId
        );
    }
}