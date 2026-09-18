package com.example.checkspire.service.game;

import com.example.checkspire.model.entity.game.Game;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.game.dto.GameStateResponse;
import com.example.checkspire.service.game.GameActionApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameActionApplicationServiceTest {

    @Mock
    private GameService gameService;

    @Mock
    private GameStateMapper gameStateMapper;

    @InjectMocks
    private GameActionApplicationService
            gameActionApplicationService;

    private User player;
    private Game game;
    private GameStateResponse response;


    @BeforeEach
    void setUp() {

        player =
                User.builder()
                        .username("player")
                        .build();

        game =
                Game.builder()
                        .build();

        response =
                mock(
                        GameStateResponse.class
                );
    }


    @Test
    void resignAndGetStateShouldReturnMappedState() {

        Long gameId = 1L;

        when(
                gameService.resignGame(
                        gameId,
                        player
                )
        ).thenReturn(game);

        when(
                gameStateMapper.toResponse(
                        game
                )
        ).thenReturn(response);


        GameStateResponse result =
                gameActionApplicationService
                        .resignAndGetState(
                                gameId,
                                player
                        );


        assertSame(
                response,
                result
        );

        verify(gameService)
                .resignGame(
                        gameId,
                        player
                );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }


    @Test
    void offerDrawAndGetStateShouldReturnMappedState() {

        Long gameId = 1L;

        when(
                gameService.offerDraw(
                        gameId,
                        player
                )
        ).thenReturn(game);

        when(
                gameStateMapper.toResponse(
                        game
                )
        ).thenReturn(response);


        GameStateResponse result =
                gameActionApplicationService
                        .offerDrawAndGetState(
                                gameId,
                                player
                        );


        assertSame(
                response,
                result
        );

        verify(gameService)
                .offerDraw(
                        gameId,
                        player
                );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }


    @Test
    void acceptDrawAndGetStateShouldReturnMappedState() {

        Long gameId = 1L;

        when(
                gameService.acceptDraw(
                        gameId,
                        player
                )
        ).thenReturn(game);

        when(
                gameStateMapper.toResponse(
                        game
                )
        ).thenReturn(response);


        GameStateResponse result =
                gameActionApplicationService
                        .acceptDrawAndGetState(
                                gameId,
                                player
                        );


        assertSame(
                response,
                result
        );

        verify(gameService)
                .acceptDraw(
                        gameId,
                        player
                );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }


    @Test
    void rejectDrawAndGetStateShouldReturnMappedState() {

        Long gameId = 1L;

        when(
                gameService.rejectDraw(
                        gameId,
                        player
                )
        ).thenReturn(game);

        when(
                gameStateMapper.toResponse(
                        game
                )
        ).thenReturn(response);


        GameStateResponse result =
                gameActionApplicationService
                        .rejectDrawAndGetState(
                                gameId,
                                player
                        );


        assertSame(
                response,
                result
        );

        verify(gameService)
                .rejectDraw(
                        gameId,
                        player
                );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }

    @Test
    void abortAndGetStateShouldReturnMappedState() {

        Long gameId = 1L;

        when(
                gameService.abortGame(
                        gameId,
                        player
                )
        ).thenReturn(
                game
        );

        when(
                gameStateMapper.toResponse(
                        game
                )
        ).thenReturn(
                response
        );


        GameStateResponse result =
                gameActionApplicationService
                        .abortAndGetState(
                                gameId,
                                player
                        );


        assertSame(
                response,
                result
        );

        verify(gameService)
                .abortGame(
                        gameId,
                        player
                );

        verify(gameStateMapper)
                .toResponse(
                        game
                );
    }
}