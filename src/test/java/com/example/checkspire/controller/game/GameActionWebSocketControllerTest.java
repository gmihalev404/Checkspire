package com.example.checkspire.controller.game;

import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.game.GameActionApplicationService;
import com.example.checkspire.service.game.dto.GameStateResponse;
import com.example.checkspire.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameActionWebSocketControllerTest {

    @Mock
    private GameActionApplicationService
            gameActionApplicationService;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Principal principal;

    @InjectMocks
    private GameActionWebSocketController controller;

    private User player;
    private GameStateResponse response;


    @BeforeEach
    void setUp() {

        player =
                User.builder()
                        .username("player")
                        .build();

        response =
                mock(
                        GameStateResponse.class
                );

        when(
                principal.getName()
        ).thenReturn(
                "player"
        );
    }


    @Test
    void resignShouldResignAndPublishState() {

        Long gameId = 1L;

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(player)
        );

        when(
                gameActionApplicationService
                        .resignAndGetState(
                                gameId,
                                player
                        )
        ).thenReturn(response);


        controller.resign(
                gameId,
                principal
        );


        verify(
                gameActionApplicationService
        ).resignAndGetState(
                gameId,
                player
        );

        verify(
                messagingTemplate
        ).convertAndSend(
                "/topic/games/1",
                response
        );
    }


    @Test
    void offerDrawShouldOfferAndPublishState() {

        Long gameId = 1L;

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(player)
        );

        when(
                gameActionApplicationService
                        .offerDrawAndGetState(
                                gameId,
                                player
                        )
        ).thenReturn(response);


        controller.offerDraw(
                gameId,
                principal
        );


        verify(
                gameActionApplicationService
        ).offerDrawAndGetState(
                gameId,
                player
        );

        verify(
                messagingTemplate
        ).convertAndSend(
                "/topic/games/1",
                response
        );
    }


    @Test
    void acceptDrawShouldAcceptAndPublishState() {

        Long gameId = 1L;

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(player)
        );

        when(
                gameActionApplicationService
                        .acceptDrawAndGetState(
                                gameId,
                                player
                        )
        ).thenReturn(response);


        controller.acceptDraw(
                gameId,
                principal
        );


        verify(
                gameActionApplicationService
        ).acceptDrawAndGetState(
                gameId,
                player
        );

        verify(
                messagingTemplate
        ).convertAndSend(
                "/topic/games/1",
                response
        );
    }


    @Test
    void rejectDrawShouldRejectAndPublishState() {

        Long gameId = 1L;

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(player)
        );

        when(
                gameActionApplicationService
                        .rejectDrawAndGetState(
                                gameId,
                                player
                        )
        ).thenReturn(response);


        controller.rejectDraw(
                gameId,
                principal
        );


        verify(
                gameActionApplicationService
        ).rejectDrawAndGetState(
                gameId,
                player
        );

        verify(
                messagingTemplate
        ).convertAndSend(
                "/topic/games/1",
                response
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


        assertThrows(
                IllegalStateException.class,
                () ->
                        controller.resign(
                                1L,
                                principal
                        )
        );


        verifyNoInteractions(
                gameActionApplicationService
        );

        verifyNoInteractions(
                messagingTemplate
        );
    }

    @Test
    void abortShouldAbortAndPublishState() {

        Long gameId = 1L;

        when(
                userService.findByUsername(
                        "player"
                )
        ).thenReturn(
                Optional.of(
                        player
                )
        );

        when(
                gameActionApplicationService
                        .abortAndGetState(
                                gameId,
                                player
                        )
        ).thenReturn(
                response
        );


        controller.abort(
                gameId,
                principal
        );


        verify(
                gameActionApplicationService
        ).abortAndGetState(
                gameId,
                player
        );

        verify(
                messagingTemplate
        ).convertAndSend(
                "/topic/games/1",
                response
        );
    }
}