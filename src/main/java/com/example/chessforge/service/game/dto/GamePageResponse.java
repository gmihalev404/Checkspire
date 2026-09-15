package com.example.chessforge.service.game.dto;

import com.example.chessforge.service.game.engine.model.PieceColor;

public record GamePageResponse(

        GameStateResponse game,

        PieceColor viewerColor,

        long whiteTimeRemainingMillis,

        long blackTimeRemainingMillis,

        Long tournamentId,

        boolean viewerParticipant

) {
}