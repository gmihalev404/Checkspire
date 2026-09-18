package com.example.checkspire.controller.tournament.dto;

import com.example.checkspire.model.enums.timeControl.TimeControl;
import com.example.checkspire.model.enums.tournament.TieBreakType;
import com.example.checkspire.model.enums.tournament.TournamentFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TournamentCreateRequest {

    private String name;

    private TournamentFormat format;

    private TimeControl timeControl;

    private boolean rated;

    private Integer maxPlayers;

    @DateTimeFormat(
            pattern = "yyyy-MM-dd'T'HH:mm"
    )
    private LocalDateTime startsAt;

    private boolean automaticStart;

    private Integer roundBreakMinutes = 5;

    private double byePoints = 1.0;

    private List<TieBreakType> tieBreaks =
            new ArrayList<>();

    private boolean armageddonForFirstPlaceTie;

    private Integer numberOfRounds;
}