package com.example.checkspire.service.game.dto;

import com.example.checkspire.model.enums.game.GameResult;
import com.example.checkspire.model.enums.game.GameStatus;
import com.example.checkspire.model.enums.game.GameTermination;
import com.example.checkspire.model.enums.timeControl.TimeControl;

import java.time.LocalDateTime;

public record GameStateResponse(
        Long gameId,

        Long whitePlayerId,
        String whiteUsername,
        Integer whiteRatingBefore,

        Long blackPlayerId,
        String blackUsername,
        Integer blackRatingBefore,

        TimeControl timeControl,
        boolean rated,

        GameStatus status,
        GameResult result,
        GameTermination termination,

        String currentFen,

        Long whiteTimeRemainingMillis,
        Long blackTimeRemainingMillis,

        LocalDateTime turnStartedAt,
        LocalDateTime turnExpiresAt,

        Long drawOfferByUserId,

        String pgn
) {
}