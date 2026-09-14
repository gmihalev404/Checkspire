package com.example.chessforge.controller.challenge;

import com.example.chessforge.controller.challenge.dto.ChallengeRequest;
import com.example.chessforge.model.entity.user.User;
import com.example.chessforge.model.enums.challenge.ColorPreference;
import com.example.chessforge.model.enums.timeControl.TimeControl;
import com.example.chessforge.service.challenge.ChallengeApplicationService;
import com.example.chessforge.service.challenge.dto.ChallengePageResponse;
import com.example.chessforge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/challenges")
public class ChallengeController {

    private final ChallengeApplicationService challengeApplicationService;
    private final UserService userService;


    @GetMapping
    public String challenges(
            Principal principal,
            Model model
    ) {

        User currentUser =
                getCurrentUser(
                        principal
                );


        ChallengePageResponse page =
                challengeApplicationService
                        .getChallenges(
                                currentUser
                        );


        model.addAttribute(
                "incoming",
                page.incoming()
        );

        model.addAttribute(
                "outgoing",
                page.outgoing()
        );

        model.addAttribute(
                "username",
                currentUser.getUsername()
        );


        return "challenge/challenges";
    }


    @GetMapping("/new")
    public String newChallenge(
            Principal principal,
            Model model
    ) {

        User currentUser =
                getCurrentUser(
                        principal
                );


        if (!model.containsAttribute(
                "challengeRequest"
        )) {

            model.addAttribute(
                    "challengeRequest",
                    new ChallengeRequest()
            );
        }


        prepareChallengeForm(
                model,
                currentUser
        );


        return "challenge/new-challenge";
    }


    @PostMapping
    public String sendChallenge(
            @ModelAttribute
            ChallengeRequest request,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        User challenger =
                getCurrentUser(
                        principal
                );


        try {

            if (request.getOpponentUsername() == null
                    || request.getOpponentUsername()
                    .isBlank()) {

                throw new IllegalArgumentException(
                        "Opponent username is required."
                );
            }


            String opponentUsername =
                    request
                            .getOpponentUsername()
                            .trim();


            User opponent =
                    userService
                            .findByUsername(
                                    opponentUsername
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "User not found."
                                    )
                            );


            challengeApplicationService
                    .sendChallenge(
                            challenger,
                            opponent,
                            request.getTimeControl(),
                            request.getColorPreference(),
                            request.isRated()
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "success",
                            "Challenge sent to "
                                    + opponent.getUsername()
                                    + "."
                    );


            return "redirect:/challenges";

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );

            model.addAttribute(
                    "challengeRequest",
                    request
            );

            prepareChallengeForm(
                    model,
                    challenger
            );


            return "challenge/new-challenge";
        }
    }


    @PostMapping("/{challengeId}/accept")
    public String accept(
            @PathVariable
            Long challengeId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User currentUser =
                getCurrentUser(
                        principal
                );


        try {

            Long gameId =
                    challengeApplicationService
                            .acceptAndStart(
                                    challengeId,
                                    currentUser
                            );


            return "redirect:/games/"
                    + gameId;

        } catch (
                IllegalArgumentException
                | IllegalStateException exception
        ) {

            redirectAttributes
                    .addFlashAttribute(
                            "error",
                            exception.getMessage()
                    );

            return "redirect:/challenges";
        }
    }


    @PostMapping("/{challengeId}/decline")
    public String decline(
            @PathVariable
            Long challengeId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User currentUser =
                getCurrentUser(
                        principal
                );


        try {

            challengeApplicationService
                    .decline(
                            challengeId,
                            currentUser
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "success",
                            "Challenge declined."
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


        return "redirect:/challenges";
    }


    @PostMapping("/{challengeId}/cancel")
    public String cancel(
            @PathVariable
            Long challengeId,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {

        User currentUser =
                getCurrentUser(
                        principal
                );


        try {

            challengeApplicationService
                    .cancel(
                            challengeId,
                            currentUser
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "success",
                            "Challenge cancelled."
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


        return "redirect:/challenges";
    }


    private User getCurrentUser(
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


    private void prepareChallengeForm(
            Model model,
            User currentUser
    ) {

        model.addAttribute(
                "timeControls",
                TimeControl.values()
        );

        model.addAttribute(
                "colorPreferences",
                ColorPreference.values()
        );

        model.addAttribute(
                "username",
                currentUser.getUsername()
        );
    }
}