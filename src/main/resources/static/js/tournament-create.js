document.addEventListener(
    "DOMContentLoaded",
    () => {

        const formatSelect =
            document.getElementById(
                "format"
            );

        const swissOptions =
            document.getElementById(
                "swiss-options"
            );

        const numberOfRounds =
            document.getElementById(
                "numberOfRounds"
            );


        if (
            !formatSelect
            || !swissOptions
            || !numberOfRounds
        ) {

            return;
        }


        function updateSwissOptions() {

            const swissSelected =
                formatSelect.value
                === "SWISS";


            swissOptions.classList.toggle(
                "d-none",
                !swissSelected
            );


            numberOfRounds.required =
                swissSelected;


            if (!swissSelected) {

                numberOfRounds.value =
                    "";
            }
        }


        formatSelect.addEventListener(
            "change",
            updateSwissOptions
        );


        updateSwissOptions();
    }
);