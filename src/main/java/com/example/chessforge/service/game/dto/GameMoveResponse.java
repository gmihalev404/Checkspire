package com.example.chessforge.service.game.dto;

public record GameMoveResponse(
        int plyNumber,
        String san,
        String fenAfter
) {
}