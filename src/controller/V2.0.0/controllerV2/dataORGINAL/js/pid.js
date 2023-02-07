(function (window, document) {
    getValueFromAPI("p", document.getElementById("p"), true);
    getValueFromAPI("i", document.getElementById("i"), true);
    getValueFromAPI("d", document.getElementById("d"), true);

    getValueFromAPI("alpha", document.getElementById("alpha"), true);
    getValueFromAPI("delay", document.getElementById("delay"), true);

}(this, this.document));