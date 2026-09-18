package com.example.checkspire.service.game.dto;

import com.example.checkspire.service.game.engine.model.PieceColor;

public record GamePageResponse(

        GameStateResponse game,

        PieceColor viewerColor,

        long whiteTimeRemainingMillis,

        long blackTimeRemainingMillis,

        Long tournamentId,

        boolean viewerParticipant

) {
}