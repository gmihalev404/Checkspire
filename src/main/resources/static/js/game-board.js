let boardElement;
let currentFen;
let orientation;
let gameId;
let gameStatus;

let moveHistory = [];

let reviewPly = null;

let reviewFirstButton;
let reviewPreviousButton;
let reviewNextButton;
let reviewLiveButton;
let reviewPositionLabel;


const INITIAL_FEN =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

let currentPgn = "";
let copyPgnButton;
let copyPgnResetTimeout = null;

let gameResult;
let gameTermination;

let gameResultElement;
let gameResultScore;
let gameResultReason;

let viewerUserId;
let drawOfferByUserId = null;

let drawButton;
let resignButton;
let abortButton;
let acceptDrawButton;
let rejectDrawButton;

let drawStatus;
let drawDefaultActions;
let drawResponseActions;

let selectedSquare = null;
let stompClient = null;

let whiteClockMillis = 0;
let blackClockMillis = 0;

let clockAnchor = 0;
let clockInterval = null;


const PIECES = {

    K: "♚",
    Q: "♛",
    R: "♜",
    B: "♝",
    N: "♞",
    P: "♟",

    k: "♚",
    q: "♛",
    r: "♜",
    b: "♝",
    n: "♞",
    p: "♟"
};


document.addEventListener(
    "DOMContentLoaded",
    () => {

        boardElement =
            document.getElementById(
                "chess-board"
            );

        if (!boardElement) {
            return;
        }


        gameId =
            boardElement.dataset.gameId;

        currentFen =
            boardElement.dataset.fen;

        orientation =
            boardElement.dataset.orientation;

        gameStatus =
            boardElement.dataset.status;

        gameResult =
            boardElement.dataset.result
            || null;

        gameTermination =
            boardElement.dataset.termination
            || null;


        gameResultElement =
            document.getElementById(
                "game-result"
            );

        gameResultScore =
            document.getElementById(
                "game-result-score"
            );

        gameResultReason =
            document.getElementById(
                "game-result-reason"
            );

        reviewFirstButton =
            document.getElementById(
                "review-first-button"
            );

        reviewPreviousButton =
            document.getElementById(
                "review-previous-button"
            );

        reviewNextButton =
            document.getElementById(
                "review-next-button"
            );

        reviewLiveButton =
            document.getElementById(
                "review-live-button"
            );

        reviewPositionLabel =
            document.getElementById(
                "review-position-label"
            );


        const gameActions =
            document.getElementById(
                "game-actions"
            );

        viewerUserId =
            Number(
                gameActions.dataset.viewerUserId
            );

        const initialDrawOffer =
            gameActions
                .dataset
                .drawOfferByUserId;

        drawOfferByUserId =
            initialDrawOffer
                ? Number(initialDrawOffer)
                : null;


        drawButton =
            document.getElementById(
                "draw-button"
            );

        resignButton =
            document.getElementById(
                "resign-button"
            );

        abortButton =
            document.getElementById(
                "abort-button"
            );

        acceptDrawButton =
            document.getElementById(
                "accept-draw-button"
            );

        rejectDrawButton =
            document.getElementById(
                "reject-draw-button"
            );

        drawStatus =
            document.getElementById(
                "draw-status"
            );

        drawDefaultActions =
            document.getElementById(
                "draw-default-actions"
            );

        drawResponseActions =
            document.getElementById(
                "draw-response-actions"
            );


        copyPgnButton =
            document.getElementById(
                "copy-pgn-button"
            );


        initializeGameActions();
        initializePgnCopy();
        initializeMoveReview();

        updateGameActions();
        updateGameResult();


        renderBoard();

        loadMoveHistory();

        initializeClocks();

        if (
            gameStatus
            === "IN_PROGRESS"
        ) {

            startClockTicker();
        }

        connectWebSocket();
    }
);


function connectWebSocket() {

    const protocol =
        window.location.protocol
        === "https:"
            ? "wss"
            : "ws";


    stompClient =
        new StompJs.Client({

            brokerURL:
                `${protocol}://${window.location.host}/ws`,

            reconnectDelay:
                3000,

            debug:
                () => {
                }
        });


    stompClient.onConnect =
        () => {

            stompClient.subscribe(
                `/topic/games/${gameId}`,
                message => {

                    const gameState =
                        JSON.parse(
                            message.body
                        );

                    handleGameState(
                        gameState
                    );
                }
            );
        };


    stompClient.onStompError =
        frame => {

            console.error(
                "STOMP error:",
                frame
            );
        };


    stompClient.onWebSocketError =
        error => {

            console.error(
                "WebSocket error:",
                error
            );
        };


    stompClient.activate();
}


function handleGameState(
    gameState
) {

    const previousFen =
        currentFen;

    const previousStatus =
        gameStatus;


    /*
     * Preserve the values currently visible
     * to the user before changing game state.
     *
     * For actions such as resignation,
     * accepted draw or abort there is no move,
     * so the stored server clocks may represent
     * the beginning of the current turn.
     */
    const displayedClocks =
        getDisplayedClockValues();


    currentFen =
        gameState.currentFen;

    gameStatus =
        gameState.status;

    gameResult =
        gameState.result;

    gameTermination =
        gameState.termination;

    drawOfferByUserId =
        gameState.drawOfferByUserId;


    boardElement.dataset.fen =
        currentFen;

    boardElement.dataset.status =
        gameStatus;


    selectedSquare =
        null;


    /*
     * The backend PGN is authoritative.
     * Use it immediately when available.
     */
    if (gameState.pgn) {

        currentPgn =
            gameState.pgn;

        updatePgnButton();
    }


    renderBoard();


    const positionChanged =
        previousFen !== currentFen;

    const gameJustFinished =
        previousStatus === "IN_PROGRESS"
        && gameStatus !== "IN_PROGRESS";


    if (positionChanged) {

        /*
         * A chess move was made.
         *
         * Server clock values already contain
         * the result of that move and are
         * authoritative.
         */
        updateClocks(
            gameState.whiteTimeRemainingMillis,
            gameState.blackTimeRemainingMillis
        );

        /*
         * Refresh SAN history after every
         * actual board position change.
         */
        loadMoveHistory();

    } else if (gameJustFinished) {

        /*
         * No move caused the game to finish.
         *
         * Examples:
         * - resignation
         * - accepted draw
         * - abort
         *
         * Keep exactly the clock values that
         * were visible when the action happened.
         */
        updateClocks(
            displayedClocks.white,
            displayedClocks.black
        );
    }


    updateGameActions();
    updateGameResult();


    if (
        gameStatus
        !== "IN_PROGRESS"
    ) {

        stopClockTicker();

        renderClocks();
    }
}


function getDisplayedClockValues() {

    let displayedWhite =
        whiteClockMillis;

    let displayedBlack =
        blackClockMillis;


    if (
        gameStatus
        === "IN_PROGRESS"
    ) {

        const elapsed =
            performance.now()
            - clockAnchor;

        const sideToMove =
            getSideToMove(
                currentFen
            );


        if (
            sideToMove
            === "WHITE"
        ) {

            displayedWhite -=
                elapsed;

        } else {

            displayedBlack -=
                elapsed;
        }
    }


    return {

        white:
            Math.max(
                0,
                displayedWhite
            ),

        black:
            Math.max(
                0,
                displayedBlack
            )
    };
}


function renderBoard() {

    boardElement.innerHTML =
        "";


    const displayedFen =
        getDisplayedBoardFen();


    if (!displayedFen) {

        boardElement.textContent =
            "Position unavailable.";

        return;
    }


    const position =
        parseFen(
            displayedFen
        );


    const files =
        orientation === "BLACK"
            ? [
                "h",
                "g",
                "f",
                "e",
                "d",
                "c",
                "b",
                "a"
            ]
            : [
                "a",
                "b",
                "c",
                "d",
                "e",
                "f",
                "g",
                "h"
            ];


    const ranks =
        orientation === "BLACK"
            ? [
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
            ]
            : [
                8,
                7,
                6,
                5,
                4,
                3,
                2,
                1
            ];


    ranks.forEach(
        (rank, rowIndex) => {

            files.forEach(
                (file, columnIndex) => {

                    const squareName =
                        `${file}${rank}`;


                    const square =
                        document.createElement(
                            "div"
                        );


                    square.classList.add(
                        "chess-square"
                    );


                    const fileIndex =
                        file.charCodeAt(0)
                        - "a".charCodeAt(0);


                    const isLight =
                        (fileIndex + rank) % 2
                        === 0;


                    square.classList.add(
                        isLight
                            ? "light"
                            : "dark"
                    );


                    square.dataset.square =
                        squareName;


                    const piece =
                        position[
                            squareName
                            ];


                    if (piece) {

                        const pieceElement =
                            document.createElement(
                                "span"
                            );

                        pieceElement.classList.add(
                            "chess-piece",
                            getPieceColor(
                                piece
                            ).toLowerCase()
                        );

                        pieceElement.textContent =
                            PIECES[piece];

                        square.appendChild(
                            pieceElement
                        );
                    }


                    square.addEventListener(
                        "click",
                        () => {

                            handleSquareClick(
                                squareName,
                                piece
                            );
                        }
                    );


                    if (
                        selectedSquare
                        === squareName
                    ) {

                        square.classList.add(
                            "selected"
                        );
                    }


                    if (
                        columnIndex === 0
                    ) {

                        const rankLabel =
                            document.createElement(
                                "span"
                            );

                        rankLabel.classList.add(
                            "rank-label"
                        );

                        rankLabel.textContent =
                            rank;

                        square.appendChild(
                            rankLabel
                        );
                    }


                    if (
                        rowIndex === 7
                    ) {

                        const fileLabel =
                            document.createElement(
                                "span"
                            );

                        fileLabel.classList.add(
                            "file-label"
                        );

                        fileLabel.textContent =
                            file;

                        square.appendChild(
                            fileLabel
                        );
                    }


                    boardElement.appendChild(
                        square
                    );
                }
            );
        }
    );
}


function handleSquareClick(
    square,
    piece
) {
    if (
        reviewPly !== null
    ) {

        return;
    }

    if (
        gameStatus
        !== "IN_PROGRESS"
    ) {

        return;
    }


    if (!selectedSquare) {

        if (!piece) {
            return;
        }


        if (
            getPieceColor(
                piece
            )
            !== orientation
        ) {

            return;
        }


        if (
            getSideToMove(
                currentFen
            )
            !== orientation
        ) {

            return;
        }


        selectedSquare =
            square;

        renderBoard();

        return;
    }


    if (
        square
        === selectedSquare
    ) {

        selectedSquare =
            null;

        renderBoard();

        return;
    }


    if (
        piece
        && getPieceColor(
            piece
        ) === orientation
    ) {

        selectedSquare =
            square;

        renderBoard();

        return;
    }


    const from =
        selectedSquare;

    const to =
        square;


    selectedSquare =
        null;

    renderBoard();


    sendMove(
        from,
        to
    );
}


function sendMove(
    from,
    to
) {

    if (
        !stompClient
        || !stompClient.connected
    ) {

        console.error(
            "WebSocket is not connected."
        );

        return;
    }


    const promotion =
        resolvePromotion(
            from,
            to
        );


    stompClient.publish({

        destination:
            `/app/games/${gameId}/move`,

        body:
            JSON.stringify({

                from:
                from,

                to:
                to,

                promotion:
                promotion
            })
    });
}


function resolvePromotion(
    from,
    to
) {

    const position =
        parseFen(
            currentFen
        );

    const piece =
        position[from];


    if (
        piece !== "P"
        && piece !== "p"
    ) {

        return null;
    }


    const targetRank =
        Number(
            to[1]
        );


    const isPromotion =
        piece === "P"
            ? targetRank === 8
            : targetRank === 1;


    if (!isPromotion) {
        return null;
    }


    const answer =
        window.prompt(
            "Promote to Q, R, B or N:",
            "Q"
        );


    return switchPromotion(
        answer
    );
}


function switchPromotion(
    value
) {

    switch (
        value
            ?.trim()
            .toUpperCase()
        ) {

        case "R":
            return "ROOK";

        case "B":
            return "BISHOP";

        case "N":
            return "KNIGHT";

        case "Q":
        default:
            return "QUEEN";
    }
}


function parseFen(
    fen
) {

    const position = {};


    const placement =
        fen.split(" ")[0];


    const rows =
        placement.split("/");


    rows.forEach(
        (row, rowIndex) => {

            const rank =
                8 - rowIndex;

            let fileIndex =
                0;


            for (
                const symbol
                of row
                ) {

                if (
                    /\d/.test(
                        symbol
                    )
                ) {

                    fileIndex +=
                        Number(
                            symbol
                        );

                    continue;
                }


                const file =
                    String.fromCharCode(
                        "a".charCodeAt(0)
                        + fileIndex
                    );


                position[
                    `${file}${rank}`
                    ] = symbol;


                fileIndex++;
            }
        }
    );


    return position;
}


function getPieceColor(
    piece
) {

    return piece
    === piece.toUpperCase()
        ? "WHITE"
        : "BLACK";
}


function getSideToMove(
    fen
) {

    const side =
        fen.split(" ")[1];

    return side === "w"
        ? "WHITE"
        : "BLACK";
}


function initializeClocks() {

    const topClock =
        document.getElementById(
            "top-clock"
        );

    const bottomClock =
        document.getElementById(
            "bottom-clock"
        );


    if (
        !topClock
        || !bottomClock
    ) {

        return;
    }


    const topMillis =
        Number(
            topClock.dataset.time
        );

    const bottomMillis =
        Number(
            bottomClock.dataset.time
        );


    if (
        orientation
        === "WHITE"
    ) {

        blackClockMillis =
            topMillis;

        whiteClockMillis =
            bottomMillis;

    } else {

        whiteClockMillis =
            topMillis;

        blackClockMillis =
            bottomMillis;
    }


    clockAnchor =
        performance.now();


    renderClocks();
}


function updateClocks(
    whiteMillis,
    blackMillis
) {

    whiteClockMillis =
        Number(
            whiteMillis
        );

    blackClockMillis =
        Number(
            blackMillis
        );


    clockAnchor =
        performance.now();


    renderClocks();
}


function startClockTicker() {

    if (
        gameStatus
        !== "IN_PROGRESS"
    ) {

        return;
    }


    if (clockInterval) {

        clearInterval(
            clockInterval
        );
    }


    clockInterval =
        setInterval(
            renderClocks,
            100
        );
}


function stopClockTicker() {

    if (!clockInterval) {
        return;
    }


    clearInterval(
        clockInterval
    );


    clockInterval =
        null;
}


function renderClocks() {

    const topClock =
        document.getElementById(
            "top-clock"
        );

    const bottomClock =
        document.getElementById(
            "bottom-clock"
        );


    if (
        !topClock
        || !bottomClock
    ) {

        return;
    }


    let displayedWhite =
        whiteClockMillis;

    let displayedBlack =
        blackClockMillis;


    if (
        gameStatus
        === "IN_PROGRESS"
    ) {

        const elapsed =
            performance.now()
            - clockAnchor;


        const sideToMove =
            getSideToMove(
                currentFen
            );


        if (
            sideToMove
            === "WHITE"
        ) {

            displayedWhite -=
                elapsed;

        } else {

            displayedBlack -=
                elapsed;
        }
    }


    displayedWhite =
        Math.max(
            0,
            displayedWhite
        );

    displayedBlack =
        Math.max(
            0,
            displayedBlack
        );


    if (
        orientation
        === "WHITE"
    ) {

        topClock.textContent =
            formatClock(
                displayedBlack
            );

        bottomClock.textContent =
            formatClock(
                displayedWhite
            );

    } else {

        topClock.textContent =
            formatClock(
                displayedWhite
            );

        bottomClock.textContent =
            formatClock(
                displayedBlack
            );
    }
}


function formatClock(
    milliseconds
) {

    if (
        !Number.isFinite(
            milliseconds
        )
    ) {

        return "--:--";
    }


    milliseconds =
        Math.max(
            0,
            milliseconds
        );


    if (
        milliseconds
        < 20_000
    ) {

        const seconds =
            Math.floor(
                milliseconds / 1000
            );

        const tenths =
            Math.floor(
                milliseconds % 1000
                / 100
            );


        return `0:${seconds
            .toString()
            .padStart(
                2,
                "0"
            )}.${tenths}`;
    }


    const totalSeconds =
        Math.ceil(
            milliseconds / 1000
        );


    const minutes =
        Math.floor(
            totalSeconds / 60
        );


    const seconds =
        totalSeconds % 60;


    return `${minutes}:${seconds
        .toString()
        .padStart(
            2,
            "0"
        )}`;
}


function initializeGameActions() {

    drawButton.addEventListener(
        "click",
        () => {

            if (
                gameStatus
                !== "IN_PROGRESS"
                || drawOfferByUserId
                !== null
            ) {

                return;
            }


            publishGameAction(
                `/app/games/${gameId}/draw/offer`
            );
        }
    );


    acceptDrawButton.addEventListener(
        "click",
        () => {

            if (
                gameStatus
                !== "IN_PROGRESS"
            ) {

                return;
            }


            publishGameAction(
                `/app/games/${gameId}/draw/accept`
            );
        }
    );


    rejectDrawButton.addEventListener(
        "click",
        () => {

            if (
                gameStatus
                !== "IN_PROGRESS"
            ) {

                return;
            }


            publishGameAction(
                `/app/games/${gameId}/draw/reject`
            );
        }
    );


    abortButton.addEventListener(
        "click",
        () => {

            if (
                gameStatus
                !== "IN_PROGRESS"
                || !canAbortGame()
            ) {

                return;
            }


            const confirmed =
                window.confirm(
                    "Abort this game? The game will end without a result."
                );


            if (!confirmed) {
                return;
            }


            publishGameAction(
                `/app/games/${gameId}/abort`
            );
        }
    );


    resignButton.addEventListener(
        "click",
        () => {

            if (
                gameStatus
                !== "IN_PROGRESS"
            ) {

                return;
            }


            const confirmed =
                window.confirm(
                    "Are you sure you want to resign?"
                );


            if (!confirmed) {
                return;
            }


            publishGameAction(
                `/app/games/${gameId}/resign`
            );
        }
    );
}


function publishGameAction(
    destination
) {

    if (
        !stompClient
        || !stompClient.connected
    ) {

        console.error(
            "WebSocket is not connected."
        );

        return;
    }


    stompClient.publish({

        destination:
        destination
    });
}


function canAbortGame() {

    const initialFen =
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";


    return gameStatus
        === "IN_PROGRESS"
        && currentFen
        === initialFen;
}


function updateGameActions() {

    const gameInProgress =
        gameStatus
        === "IN_PROGRESS";


    resignButton.disabled =
        !gameInProgress;


    const abortAllowed =
        canAbortGame();


    abortButton.disabled =
        !abortAllowed;

    abortButton.classList.toggle(
        "d-none",
        !abortAllowed
    );


    if (!gameInProgress) {

        drawButton.disabled =
            true;

        acceptDrawButton.disabled =
            true;

        rejectDrawButton.disabled =
            true;


        drawDefaultActions.classList.remove(
            "d-none"
        );


        drawResponseActions.classList.remove(
            "d-flex"
        );

        drawResponseActions.classList.add(
            "d-none"
        );


        drawButton.classList.remove(
            "d-none"
        );


        drawStatus.textContent =
            gameStatus === "ABORTED"
                ? "Game aborted."
                : "Game finished.";


        return;
    }


    if (
        drawOfferByUserId
        === null
    ) {

        drawDefaultActions.classList.remove(
            "d-none"
        );


        drawResponseActions.classList.remove(
            "d-flex"
        );

        drawResponseActions.classList.add(
            "d-none"
        );


        drawButton.classList.remove(
            "d-none"
        );

        drawButton.disabled =
            false;

        drawButton.textContent =
            "Offer Draw";

        drawStatus.textContent =
            "";


        return;
    }


    if (
        drawOfferByUserId
        === viewerUserId
    ) {

        drawDefaultActions.classList.remove(
            "d-none"
        );


        drawResponseActions.classList.remove(
            "d-flex"
        );

        drawResponseActions.classList.add(
            "d-none"
        );


        drawButton.classList.remove(
            "d-none"
        );

        drawButton.disabled =
            true;

        drawButton.textContent =
            "Draw Offered";

        drawStatus.textContent =
            "Waiting for opponent to respond.";


        return;
    }


    drawDefaultActions.classList.remove(
        "d-none"
    );


    drawButton.classList.add(
        "d-none"
    );


    drawResponseActions.classList.remove(
        "d-none"
    );

    drawResponseActions.classList.add(
        "d-flex"
    );


    acceptDrawButton.disabled =
        false;

    rejectDrawButton.disabled =
        false;


    drawStatus.textContent =
        "Opponent offered a draw.";
}


function updateGameResult() {

    if (
        gameStatus
        === "ABORTED"
    ) {

        gameResultElement.classList.remove(
            "d-none"
        );

        gameResultScore.textContent =
            "";

        gameResultReason.textContent =
            "Game aborted";

        return;
    }


    if (
        gameStatus
        !== "FINISHED"
        || !gameResult
    ) {

        gameResultElement.classList.add(
            "d-none"
        );

        return;
    }


    gameResultElement.classList.remove(
        "d-none"
    );


    switch (
        gameResult
        ) {

        case "WHITE_WIN":

            gameResultScore.textContent =
                "1–0";

            break;


        case "BLACK_WIN":

            gameResultScore.textContent =
                "0–1";

            break;


        case "DRAW":

            gameResultScore.textContent =
                "½–½";

            break;


        default:

            gameResultScore.textContent =
                "";
    }


    gameResultReason.textContent =
        getTerminationText(
            gameTermination
        );
}


function getTerminationText(
    termination
) {

    switch (
        termination
        ) {

        case "CHECKMATE":
            return "Checkmate";

        case "RESIGNATION":
            return "Resignation";

        case "AGREEMENT":
            return "Draw by agreement";

        case "STALEMATE":
            return "Stalemate";

        case "INSUFFICIENT_MATERIAL":
            return "Draw by insufficient material";

        case "THREEFOLD_REPETITION":
            return "Draw by threefold repetition";

        case "FIVEFOLD_REPETITION":
            return "Draw by fivefold repetition";

        case "FIFTY_MOVE_RULE":
            return "Draw by fifty-move rule";

        case "SEVENTY_FIVE_MOVE_RULE":
            return "Draw by seventy-five-move rule";

        case "TIMEOUT":
            return "Time expired";

        case "TIMEOUT_INSUFFICIENT_MATERIAL":
            return "Draw on time due to insufficient mating material";

        case "ABORTED":
            return "Game aborted";

        default:

            return termination
                ? termination
                    .replaceAll(
                        "_",
                        " "
                    )
                    .toLowerCase()
                : "";
    }
}


async function loadMoveHistory() {

    const moveHistoryElement =
        document.getElementById(
            "move-history"
        );


    if (!moveHistoryElement) {
        return;
    }


    try {

        const response =
            await fetch(
                `/games/${gameId}/moves`
            );


        if (!response.ok) {

            console.error(
                "Failed to load move history."
            );

            return;
        }


        const moves =
            await response.json();

        moveHistory =
            moves;


        /*
         * On page load we may not yet have
         * received a WebSocket GameState.
         *
         * Build a valid movetext PGN from
         * persisted SAN moves in that case.
         */
        currentPgn =
            buildPgnFromMoves(
                moves
            );


        updatePgnButton();


        renderMoveHistory(
            moves
        );

        updateMoveReviewControls();

        updateMoveSelection();

    } catch (error) {

        console.error(
            "Failed to load move history:",
            error
        );
    }
}


function renderMoveHistory(
    moves
) {

    const moveHistoryElement =
        document.getElementById(
            "move-history"
        );


    if (!moveHistoryElement) {
        return;
    }


    moveHistoryElement.innerHTML =
        "";


    if (
        !moves
        || moves.length === 0
    ) {

        const emptyElement =
            document.createElement(
                "div"
            );

        emptyElement.classList.add(
            "move-history-empty"
        );

        emptyElement.textContent =
            "No moves yet.";

        moveHistoryElement.appendChild(
            emptyElement
        );

        return;
    }


    const moveRows =
        new Map();


    moves.forEach(
        move => {

            const moveNumber =
                Math.ceil(
                    move.plyNumber / 2
                );


            if (
                !moveRows.has(
                    moveNumber
                )
            ) {

                moveRows.set(
                    moveNumber,
                    {
                        white: null,
                        black: null
                    }
                );
            }


            const row =
                moveRows.get(
                    moveNumber
                );


            if (
                move.plyNumber % 2
                === 1
            ) {

                row.white =
                    move;

            } else {

                row.black =
                    move;
            }
        }
    );


    moveRows.forEach(
        (
            movesForTurn,
            moveNumber
        ) => {

            const rowElement =
                document.createElement(
                    "div"
                );

            rowElement.classList.add(
                "move-history-row"
            );


            const numberElement =
                document.createElement(
                    "span"
                );

            numberElement.classList.add(
                "move-number"
            );

            numberElement.textContent =
                `${moveNumber}.`;


            const whiteMoveElement =
                createMoveHistoryElement(
                    movesForTurn.white
                );

            const blackMoveElement =
                createMoveHistoryElement(
                    movesForTurn.black
                );


            rowElement.appendChild(
                numberElement
            );

            rowElement.appendChild(
                whiteMoveElement
            );

            rowElement.appendChild(
                blackMoveElement
            );


            moveHistoryElement.appendChild(
                rowElement
            );
        }
    );


    if (
        reviewPly === null
    ) {

        moveHistoryElement.scrollTop =
            moveHistoryElement.scrollHeight;
    }


    updateMoveSelection();
}


function initializePgnCopy() {

    if (!copyPgnButton) {
        return;
    }


    copyPgnButton.addEventListener(
        "click",
        async () => {

            if (!currentPgn) {
                return;
            }


            try {

                await navigator.clipboard
                    .writeText(
                        currentPgn
                    );


                if (copyPgnResetTimeout) {

                    clearTimeout(
                        copyPgnResetTimeout
                    );
                }


                copyPgnButton.textContent =
                    "Copied!";

                copyPgnButton.disabled =
                    true;


                copyPgnResetTimeout =
                    setTimeout(
                        () => {

                            copyPgnButton.textContent =
                                "Copy PGN";

                            copyPgnResetTimeout =
                                null;

                            updatePgnButton();
                        },
                        1500
                    );


            } catch (error) {

                console.error(
                    "Failed to copy PGN:",
                    error
                );
            }
        }
    );


    updatePgnButton();
}


function updatePgnButton() {

    if (!copyPgnButton) {
        return;
    }


    copyPgnButton.disabled =
        !currentPgn
        || copyPgnResetTimeout
        !== null;
}


function buildPgnFromMoves(
    moves
) {

    if (
        !moves
        || moves.length === 0
    ) {

        return "";
    }


    const turns = [];


    for (
        let index = 0;
        index < moves.length;
        index += 2
    ) {

        const whiteMove =
            moves[index];

        const blackMove =
            moves[index + 1];


        const moveNumber =
            Math.ceil(
                whiteMove.plyNumber / 2
            );


        let turn =
            `${moveNumber}. ${whiteMove.san}`;


        if (blackMove) {

            turn +=
                ` ${blackMove.san}`;
        }


        turns.push(
            turn
        );
    }


    turns.push(
        getPgnResultToken()
    );


    return turns.join(
        " "
    );
}


function getPgnResultToken() {

    switch (
        gameResult
        ) {

        case "WHITE_WIN":
            return "1-0";

        case "BLACK_WIN":
            return "0-1";

        case "DRAW":
            return "1/2-1/2";

        default:
            return "*";
    }
}

function getDisplayedBoardFen() {

    if (
        reviewPly === null
    ) {

        return currentFen;
    }


    if (
        reviewPly === 0
    ) {

        return INITIAL_FEN;
    }


    const move =
        moveHistory.find(
            currentMove =>
                currentMove.plyNumber
                === reviewPly
        );


    return move
            ?.fenAfter
        || currentFen;
}

function createMoveHistoryElement(
    move
) {

    if (!move) {

        const emptyElement =
            document.createElement(
                "span"
            );

        emptyElement.classList.add(
            "move-san"
        );

        return emptyElement;
    }


    const moveElement =
        document.createElement(
            "button"
        );


    moveElement.type =
        "button";


    moveElement.classList.add(
        "move-san",
        "btn",
        "btn-sm",
        "btn-link",
        "text-body",
        "text-decoration-none",
        "border-0",
        "p-0",
        "text-start"
    );


    moveElement.dataset.ply =
        String(
            move.plyNumber
        );


    moveElement.textContent =
        move.san;


    moveElement.addEventListener(
        "click",
        () => {

            goToReviewPly(
                move.plyNumber
            );
        }
    );


    return moveElement;
}

function initializeMoveReview() {

    if (
        !reviewFirstButton
        || !reviewPreviousButton
        || !reviewNextButton
        || !reviewLiveButton
    ) {

        return;
    }


    reviewFirstButton.addEventListener(
        "click",
        () => {

            if (
                moveHistory.length === 0
            ) {
                return;
            }

            goToReviewPly(
                0
            );
        }
    );


    reviewPreviousButton.addEventListener(
        "click",
        () => {

            const lastPly =
                getLastPly();


            if (
                lastPly === 0
            ) {
                return;
            }


            if (
                reviewPly === null
            ) {

                goToReviewPly(
                    Math.max(
                        0,
                        lastPly - 1
                    )
                );

                return;
            }


            if (
                reviewPly > 0
            ) {

                goToReviewPly(
                    reviewPly - 1
                );
            }
        }
    );


    reviewNextButton.addEventListener(
        "click",
        () => {

            const lastPly =
                getLastPly();


            if (
                reviewPly === null
                || lastPly === 0
            ) {

                return;
            }


            if (
                reviewPly < lastPly
            ) {

                goToReviewPly(
                    reviewPly + 1
                );

                return;
            }


            returnToLivePosition();
        }
    );


    reviewLiveButton.addEventListener(
        "click",
        returnToLivePosition
    );


    updateMoveReviewControls();
}


function goToReviewPly(
    ply
) {

    const lastPly =
        getLastPly();


    reviewPly =
        Math.max(
            0,
            Math.min(
                ply,
                lastPly
            )
        );


    selectedSquare =
        null;


    renderBoard();

    updateMoveReviewControls();

    updateMoveSelection();
}


function returnToLivePosition() {

    reviewPly =
        null;


    selectedSquare =
        null;


    renderBoard();

    updateMoveReviewControls();

    updateMoveSelection();
}


function getLastPly() {

    if (
        moveHistory.length === 0
    ) {

        return 0;
    }


    return moveHistory[
    moveHistory.length - 1
        ].plyNumber;
}


function updateMoveReviewControls() {

    if (
        !reviewFirstButton
        || !reviewPreviousButton
        || !reviewNextButton
        || !reviewLiveButton
        || !reviewPositionLabel
    ) {

        return;
    }


    const lastPly =
        getLastPly();

    const hasMoves =
        lastPly > 0;


    reviewFirstButton.disabled =
        !hasMoves
        || reviewPly === 0;


    reviewPreviousButton.disabled =
        !hasMoves
        || reviewPly === 0;


    reviewNextButton.disabled =
        !hasMoves
        || reviewPly === null;


    reviewLiveButton.disabled =
        reviewPly === null;


    if (
        reviewPly === null
    ) {

        reviewPositionLabel.textContent =
            "Live";

        return;
    }


    reviewPositionLabel.textContent =
        `${reviewPly} / ${lastPly}`;
}


function updateMoveSelection() {

    const moveElements =
        document.querySelectorAll(
            ".move-san[data-ply]"
        );


    moveElements.forEach(
        element => {

            const ply =
                Number(
                    element.dataset.ply
                );


            const selected =
                reviewPly !== null
                && ply === reviewPly;


            element.classList.toggle(
                "fw-bold",
                selected
            );

            element.classList.toggle(
                "text-decoration-underline",
                selected
            );
        }
    );
}
