package com.example.checkspire.service.game.dto;

public record GameMoveResponse(
        int plyNumber,
        String san,
        String fromSquare,
        String toSquare,
        String fenAfter
) {
}