(function (window, document) {
    getValueFromAPI("heatingt", document.getElementById("temp"), true);
    getValueFromAPI("air", document.getElementById("airflow1"), true);
    getValueFromAPI("air", document.getElementById("airflow2"), true);
    getValueFromAPI("power", document.getElementById("power"), true);

    const powerform = document.getElementById("powerform");
    powerform.addEventListener("submit", event => {
        event.preventDefault();
        console.log("sending data for power setup");
        fetchData(powerform, "/data");
    });

    const temperatureform = document.getElementById("temperatureform");
    temperatureform.addEventListener("submit", event => {
        event.preventDefault();
        console.log("sending data for temperature set");
        fetchData(temperatureform, "/data");
    });



}(this, this.document));

function toggleForm() {
    var toggle = document.getElementById("toggle");
    var formManual = document.getElementById("manualform");
    var formPid = document.getElementById("pidform");
    var manual = document.getElementById("manualtext");
    var pid = document.getElementById("pidtext");

    if (toggle.checked) {
        manual.style.fontWeight = "normal";
        pid.style.fontWeight = "bold"

        formManual.style.display = "none";
        formPid.style.display = "block";

    } else {
        manual.style.fontWeight = "bold";
        pid.style.fontWeight = "normal"
        formManual.style.display = "block";
        formPid.style.display = "none";

    }
}