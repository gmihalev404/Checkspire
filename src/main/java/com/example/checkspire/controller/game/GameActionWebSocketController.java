package com.example.checkspire.controller.game;

import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.service.game.GameActionApplicationService;
import com.example.checkspire.service.game.dto.GameStateResponse;
import com.example.checkspire.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameActionWebSocketController {

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameActionApplicationService gameActionApplicationService;


    @MessageMapping(
            "/games/{gameId}/resign"
    )
    public void resign(
            @DestinationVariable Long gameId,
            Principal principal
    ) {

        User player =
                getCurrentUser(
                        principal
                );

        GameStateResponse response =
                gameActionApplicationService
                        .resignAndGetState(
                                gameId,
                                player
                        );

        publishGameState(
                gameId,
                response
        );
    }

    @MessageMapping(
            "/games/{gameId}/abort"
    )
    public void abort(
            @DestinationVariable Long gameId,
            Principal principal
    ) {

        User player =
                getCurrentUser(
                        principal
                );

        GameStateResponse response =
                gameActionApplicationService
                        .abortAndGetState(
                                gameId,
                                player
                        );

        publishGameState(
                gameId,
                response
        );
    }


    @MessageMapping(
            "/games/{gameId}/draw/offer"
    )
    public void offerDraw(
            @DestinationVariable Long gameId,
            Principal principal
    ) {

        User player =
                getCurrentUser(
                        principal
                );

        GameStateResponse response =
                gameActionApplicationService
                        .offerDrawAndGetState(
                                gameId,
                                player
                        );

        publishGameState(
                gameId,
                response
        );
    }


    @MessageMapping(
            "/games/{gameId}/draw/accept"
    )
    public void acceptDraw(
            @DestinationVariable Long gameId,
            Principal principal
    ) {

        User player =
                getCurrentUser(
                        principal
                );

        GameStateResponse response =
                gameActionApplicationService
                        .acceptDrawAndGetState(
                                gameId,
                                player
                        );

        publishGameState(
                gameId,
                response
        );
    }


    @MessageMapping(
            "/games/{gameId}/draw/reject"
    )
    public void rejectDraw(
            @DestinationVariable Long gameId,
            Principal principal
    ) {

        User player =
                getCurrentUser(
                        principal
                );

        GameStateResponse response =
                gameActionApplicationService
                        .rejectDrawAndGetState(
                                gameId,
                                player
                        );

        publishGameState(
                gameId,
                response
        );
    }


    private User getCurrentUser(
            Principal principal
    ) {

        return userService
                .findByUsername(
                        principal.getName()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found."
                        )
                );
    }


    private void publishGameState(
            Long gameId,
            GameStateResponse response
    ) {

        messagingTemplate.convertAndSend(
                "/topic/games/"
                        + gameId,
                response
        );
    }
}