package com.example.checkspire.controller.challenge.dto;

import com.example.checkspire.model.enums.challenge.ColorPreference;
import com.example.checkspire.model.enums.timeControl.TimeControl;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengeRequest {

    private String opponentUsername;

    private TimeControl timeControl =
            TimeControl.RAPID_10_0;

    private ColorPreference colorPreference =
            ColorPreference.RANDOM;

    private boolean rated;
}