package com.example.chessforge.controller.tournament;

import com.example.chessforge.controller.tournament.dto.TournamentCreateRequest;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.model.enums.tournament.TieBreakType;
import com.example.chessforge.model.enums.tournament.TournamentFormat;
import com.example.chessforge.service.tournament.TournamentApplicationService;
import com.example.chessforge.service.tournament.dto.TournamentDetailsResponse;
import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/tournaments")
public class TournamentController {

    private final TournamentApplicationService
            tournamentApplicationService;

    private final UserService userService;


    @GetMapping
    public String tournaments(
            Model model
    ) {

        model.addAttribute(
                "tournaments",
                tournamentApplicationService
                        .getTournaments()
        );

        return "tournament/tournaments";
    }

    @GetMapping("/create")
    public String createTournamentPage(
            Model model
    ) {

        model.addAttribute(
                "request",
                new TournamentCreateRequest()
        );

        addCreateFormOptions(
                model
        );

        return "tournament/create-tournament";
    }


    @PostMapping
    public String createTournament(
            TournamentCreateRequest request,
            Principal principal,
            Model model
    ) {

        User creator =
                userService
                        .findByUsername(
                                principal.getName()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Authenticated user was not found."
                                )
                        );


        try {

            Long tournamentId =
                    tournamentApplicationService
                            .createTournament(
                                    creator,
                                    request
                            );

            return "redirect:/tournaments/"
                    + tournamentId;

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            model.addAttribute(
                    "request",
                    request
            );

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );

            addCreateFormOptions(
                    model
            );

            return "tournament/create-tournament";
        }
    }

    @GetMapping("/{tournamentId}")
    public String tournamentDetails(
            @PathVariable Long tournamentId,
            Principal principal,
            Model model
    ) {

        User viewer =
                getAuthenticatedUser(
                        principal
                );


        TournamentDetailsResponse tournament =
                tournamentApplicationService
                        .getTournamentDetails(
                                tournamentId,
                                viewer
                        );


        model.addAttribute(
                "tournament",
                tournament
        );


        return "tournament/tournament-details";
    }


    @PostMapping("/{tournamentId}/join")
    public String joinTournament(
            @PathVariable Long tournamentId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User user =
                getAuthenticatedUser(
                        principal
                );


        try {

            tournamentApplicationService
                    .joinTournament(
                            tournamentId,
                            user
                    );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "You joined the tournament."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }


        return "redirect:/tournaments/"
                + tournamentId;
    }


    @PostMapping("/{tournamentId}/withdraw")
    public String withdrawTournament(
            @PathVariable Long tournamentId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User user =
                getAuthenticatedUser(
                        principal
                );


        try {

            tournamentApplicationService
                    .withdrawFromTournament(
                            tournamentId,
                            user
                    );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "You left the tournament."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }


        return "redirect:/tournaments/"
                + tournamentId;
    }

    @PostMapping("/{tournamentId}/start")
    public String startTournament(
            @PathVariable Long tournamentId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User requester =
                getAuthenticatedUser(
                        principal
                );


        try {

            tournamentApplicationService
                    .startTournament(
                            tournamentId,
                            requester
                    );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Tournament started."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }


        return "redirect:/tournaments/"
                + tournamentId;
    }

    @PostMapping(
            "/{tournamentId}/rounds/{roundNumber}/start"
    )
    public String startRoundNow(
            @PathVariable Long tournamentId,
            @PathVariable Integer roundNumber,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User requester =
                getAuthenticatedUser(
                        principal
                );


        try {

            tournamentApplicationService
                    .startScheduledRoundNow(
                            tournamentId,
                            roundNumber,
                            requester
                    );

            redirectAttributes
                    .addFlashAttribute(
                            "success",
                            "Round started."
                    );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            exception.getMessage()
                    );
        }


        return "redirect:/tournaments/"
                + tournamentId;
    }


    @PostMapping("/{tournamentId}/cancel")
    public String cancelTournament(
            @PathVariable Long tournamentId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User requester =
                getAuthenticatedUser(
                        principal
                );


        try {

            tournamentApplicationService
                    .cancelTournament(
                            tournamentId,
                            requester
                    );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Tournament cancelled."
            );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }


        return "redirect:/tournaments/"
                + tournamentId;
    }

    @PostMapping("/{tournamentId}/forfeit")
    public String forfeitTournament(
            @PathVariable Long tournamentId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User user =
                getAuthenticatedUser(
                        principal
                );


        try {

            tournamentApplicationService
                    .forfeitTournament(
                            tournamentId,
                            user
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "success",
                            "You forfeited the tournament."
                    );

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            exception.getMessage()
                    );
        }


        return "redirect:/tournaments/"
                + tournamentId;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void addCreateFormOptions(
            Model model
    ) {

        model.addAttribute(
                "formats",
                TournamentFormat.values()
        );

        model.addAttribute(
                "timeControls",
                TimeControl.values()
        );

        model.addAttribute(
                "tieBreaks",
                TieBreakType.values()
        );
    }

    private User getAuthenticatedUser(
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
}