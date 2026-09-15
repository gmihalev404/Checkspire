package com.example.chessforge.controller.game;

import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.service.game.GameService;
import com.example.chessforge.service.game.dto.GameMoveResponse;
import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/games")
public class GameHistoryController {

    private final GameService gameService;


    @GetMapping("/{gameId}/moves")
    public List<GameMoveResponse> moveHistory(
            @PathVariable Long gameId
    ) {

        return gameService
                .getMoveHistory(
                        gameId
                );
    }
}