// alert(
//     "challenges.js loaded"
// );

document.addEventListener(
    "DOMContentLoaded",
    () => {

        connectChallengeWebSocket();
    }
);


function connectChallengeWebSocket() {

    const protocol =
        window.location.protocol === "https:"
            ? "wss"
            : "ws";


    const client =
        new StompJs.Client({

            brokerURL:
                `${protocol}://${window.location.host}/ws`,

            reconnectDelay:
                3000,

            debug:
                () => {
                }
        });


    client.onConnect =
        () => {

            // alert(
            //     "Challenge WebSocket connected"
            // );

            client.subscribe(
                "/user/queue/game-started",
                message => {

                    const event =
                        JSON.parse(
                            message.body
                        );


                    if (!event.gameId) {
                        return;
                    }

                    const gameUrl =
                        `/games/${event.gameId}`;

                    // alert(
                    //     `Game started: ${event.gameId}`
                    // );
                    //
                    // alert(
                    //     `Redirecting to: ${gameUrl}`
                    // );

                    window.location.assign(
                        gameUrl
                    );
                }
            );
        };


    client.onStompError =
        frame => {

            console.error(
                "STOMP error:",
                frame
            );
        };


    client.onWebSocketError =
        error => {

            console.error(
                "WebSocket error:",
                error
            );
        };

    client.onWebSocketClose =
        event => {

            // alert(
            //     `Challenge WebSocket closed: ${event.code} ${event.reason}`
            // );
        };

    client.onDisconnect =
        () => {

            // alert(
            //     "Challenge STOMP disconnected"
            // );
        };


    client.activate();
}