function getTextFromAPI(endpoint, object) {
    fetch(endpoint)
        .then(response => {
            if(response.status !== 200){
                return "NaN";
            }else{
                return response.text();
            }
        })
        .then(text => {
            if (text == "OK"){
                object.style.color = "green";
            } else if (text == "ERROR"){
                object.style.color = "red";
            }

            if (endpoint == "t"){
                object.innerHTML = Number(text).toFixed(2);
            } else {
                object.innerHTML = text;
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

function getValueFromAPI(endpoint, object, number=false) {
    fetch(endpoint)
        .then(response => {
            if(response.status !== 200){
                return "";
            }else{
                return response.text();
            }
        })
        .then(text => {
            if (number == true){
                object.value = Number(text);
            } else {
                object.value = text;
            }

        })
        .catch(error => {
            console.error('Error:', error);
        });
}

function fetchData(form, fetchUrl) {
    const formData = new FormData(form);
    // Add additional data to the form
    //formData.append("AdditionalData", "AdditionalValue");

    fetch(fetchUrl, {
        method: "POST",
        body: formData
    })
        .then(response => response.text())
        .then(text => {
            console.log(text);
        })
        .catch(error => {
            console.error("Error:", error);
        });



}


(function (window, document) {
    getTextFromAPI("controlleridcko", document.getElementById("id_in_h2"));
}(this, this.document));

