(function (window, document) {
    const baseIPregex = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    const maskIPregex = /^(254|252|248|240|224|192|128|0)\.0\.0\.0|255\.(254|252|248|240|224|192|128|0)\.0\.0|255\.255\.(254|252|248|240|224|192|128|0)\.0|255\.255\.255\.(254|252|248|240|224|192|128|0)$/;
    const IDregex = /^[\x00-\x7F]{1,15}$/;
    const PORTregex = /^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$/;
    const rootPORTregex = /^(0|[1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])$/;

    document.getElementById("controllersetup").addEventListener("submit", submitForm);

    getElement("ip").addEventListener("change", function() {
        checkIP(this, getElement("wip"), baseIPregex);
    });

    getElement("serverip").addEventListener("change", function() {
        checkIP(this, getElement("wserverip"), baseIPregex);
    });



    getElement("port").addEventListener("change", function() {
        checkIP(this, getElement("wport"), PORTregex, "Wrong port number<br>A network port is a 16-bit unsigned integer, which means it can have a value between 0 and 65535.");
        //getElement("wrootport").innerHTML = "";
    });
    getElement("port").addEventListener("change", function() {
        checkPort(this, getElement("wrootport"), rootPORTregex, "Friendly reminder<br>On Windows, starting a server on port number less than 1024 typically requires administrator access");
    });



    getElement("gateway").addEventListener("change", function() {
        checkIP(this, getElement("wgateway"), baseIPregex);

    });





    getElement("netmask").addEventListener("change", function() {
        checkIP(this, document.getElementById("wnetmask"), maskIPregex, "Wrong netmask\r\n");
    });

    getElement("idcko").addEventListener("change", function() {
        checkIP(this, document.getElementById("wid"), IDregex, "Wrong ID<br>The ID can contain 1-15 ASCII characters only\r\n");
    });

    getValueFromAPI("controllerip", getElement("ip"));
    getValueFromAPI("controllerport", getElement("port"), true);
    getValueFromAPI("controllergateway", getElement("gateway"));
    getValueFromAPI("controllernwtmask", getElement("netmask"));

    getValueFromAPI("controlleridcko", getElement("idcko"));

    getValueFromAPI("serverip", getElement("serverip"));


    const controllerform = getElement("controllersetup");
    controllerform.addEventListener("submit", event => {
        event.preventDefault();
        console.log("sending data for controller setup");
        fetchData(controllerform, "/data");
    });

    const serverform = getElement("serversetup");
    serverform.addEventListener("submit", event => {
        event.preventDefault();
        console.log("sending data for server setup");
        fetchData(serverform, "/data");
    });

    const rebooting = getElement("reboot");
    rebooting.addEventListener("submit", event => {
        event.preventDefault();
        console.log("Rebooting controller");
        getTextFromAPI("reboot",getElement("rebootingtxt"));
    });

}(this, this.document));

function checkIP(elementip, elementwrong, regex, errormsg = "Wrong IP format\r\n"){
    if (regex.test(elementip.value)) {
        elementwrong.innerHTML = "";
        elementip.style.borderColor = "#2e7a29";
    } else {
        elementwrong.innerHTML = errormsg;
        elementip.style.borderColor = "#ce0116";
    }
}

function checkPort(elementip, elementwrong, regex, errormsg = "Wrong IP format\r\n"){
    if (!regex.test(elementip.value)) {
        elementwrong.innerHTML = "";
    } else {
        elementwrong.innerHTML = errormsg;
    }
}

function getElement(element) {
    return document.getElementById(element);
}

function submitForm(event) {
    event.preventDefault();

}