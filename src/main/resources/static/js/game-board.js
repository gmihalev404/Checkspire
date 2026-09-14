let boardElement;
let currentFen;
let orientation;
let gameId;
let gameStatus;

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


        renderBoard();

        initializeClocks();

        startClockTicker();

        connectWebSocket();
    }
);


function connectWebSocket() {

    const protocol =
        window.location.protocol === "https:"
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

    currentFen =
        gameState.currentFen;

    gameStatus =
        gameState.status;


    boardElement.dataset.fen =
        currentFen;

    boardElement.dataset.status =
        gameStatus;


    selectedSquare =
        null;


    renderBoard();


    updateClocks(
        gameState.whiteTimeRemainingMillis,
        gameState.blackTimeRemainingMillis
    );
}


function renderBoard() {

    boardElement.innerHTML = "";


    if (!currentFen) {

        boardElement.textContent =
            "Position unavailable.";

        return;
    }


    const position =
        parseFen(
            currentFen
        );


    const files =
        orientation === "BLACK"
            ? ["h", "g", "f", "e", "d", "c", "b", "a"]
            : ["a", "b", "c", "d", "e", "f", "g", "h"];


    const ranks =
        orientation === "BLACK"
            ? [1, 2, 3, 4, 5, 6, 7, 8]
            : [8, 7, 6, 5, 4, 3, 2, 1];


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
                            getPieceColor(piece)
                                .toLowerCase()
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


                    if (columnIndex === 0) {

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


                    if (rowIndex === 7) {

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
            getPieceColor(piece)
            !== orientation
        ) {

            return;
        }


        if (
            getSideToMove(currentFen)
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
        && getPieceColor(piece)
        === orientation
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

        // alert(
        //     "WebSocket is not connected."
        // );

        return;
    }


    const promotion =
        resolvePromotion(
            from,
            to
        );


    // alert(
    //     `Sending move: ${from} -> ${to}`
    // );

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