(function (window, document) {
    updateOne();
    updateEvery1sec();
    updateEvery5sec();
    
    setInterval(function(){
        updateEvery1sec();
    }, 1000);

    setInterval(function(){
        updateEvery5sec();
    }, 5000);

}(this, this.document));

function updateEvery5sec() {
    getTextFromAPI("heatingt", document.getElementById("heating"));
    getTextFromAPI("air", document.getElementById("air"));
    getTextFromAPI("serverip", document.getElementById("serverIP"));
}

function updateEvery1sec() {
    getTextFromAPI("t", document.getElementById("temp"));
    getTextFromAPI("actualpower", document.getElementById("power"));
    getTextFromAPI("serverstat", document.getElementById("server"));
}

function updateOne(){
    getTextFromAPI("eepromstat", document.getElementById("eeprom"));
    getTextFromAPI("dacstat", document.getElementById("dac"));
    getTextFromAPI("thermometerstat", document.getElementById("thermometer"));
    getTextFromAPI("controlleridcko", document.getElementById("controllerID"));
}