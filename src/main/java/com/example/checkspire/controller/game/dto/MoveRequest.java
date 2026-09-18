package com.example.checkspire.controller.game.dto;

import com.example.checkspire.service.game.engine.model.PieceType;

public record MoveRequest(
        String from,
        String to,
        PieceType promotion
) {
}