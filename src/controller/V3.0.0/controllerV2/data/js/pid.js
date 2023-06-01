(function (window, document) {
    getValueFromAPI("p", document.getElementById("p"), true);
    getValueFromAPI("i", document.getElementById("i"), true);
    getValueFromAPI("d", document.getElementById("d"), true);

    getValueFromAPI("alpha", document.getElementById("alpha"), true);
    getValueFromAPI("delay", document.getElementById("delay"), true);

    getValueFromAPI("deltat", document.getElementById("deltat"), true);

    const pidform = document.getElementById("pidsetup");
    pidform.addEventListener("submit", event => {
        event.preventDefault();
        console.log("sending data for PID setup");
        fetchData(pidform, "/data");
    });

}(this, this.document));