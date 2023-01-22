#include "lwip/arch.h"


//**************************************************************************
//                           WEB SERVER LISTENING
//**************************************************************************

#include <AsyncWebServer_WT32_ETH01.h>
#include "SPIFFS.h"
#include "preferences.h"
#include "status.h"
#include "defines.h"

#define OK_RESPONSE "OK"
#define ERROR_RESPONSE "ERROR"

class WebHandler {
private:
Preferences* memory;
Status* status;

public:
  WebHandler() {
  }


  bool begin(AsyncWebServer &server, Preferences *m, Status *s) {
    memory = m;
    status = s;
    !SPIFFS.begin(true) ? Serial.println("An Error has occurred while mounting SPIFFS") : Serial.println("SPIFFS OK");


    // STATIC WEB PAGES
    // html
    server.on("/", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/index.html", "text/html");
    });

    server.on("/index.html", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/index.html", "text/html");
    });

    server.on("/manualcontrol.html", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/manualcontrol.html", "text/html");
    });

    
    server.on("/settings.html", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/settings.html", "text/html");
    });
    
    server.on("/pid.html", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/pid.html", "text/html");
    });
    
    // css
    server.on("/style.css", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/style.css", "text/css");
    });

    server.on("/main.css", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/main.css", "text/css");
    });

    // js
    server.on("/js/ajax.js", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/js/ajax.js", "application/javascript");
    });
     
    server.on("/js/dashboard.js", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/js/dashboard.js", "application/javascript");
    });
    
    server.on("/js/form.js", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/js/form.js", "application/javascript");
    });
    
    server.on("/js/manualcontrol.js", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/js/manualcontrol.js", "application/javascript");
    });
    
    server.on("/js/pid.js", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/js/pid.js", "application/javascript");
    });
    
    server.on("/js/ui.js", HTTP_GET, [](AsyncWebServerRequest *request) {
      request->send(SPIFFS, "/js/ui.js", "application/javascript");
    });

    //ico
    server.on("/favicon.ico", HTTP_GET, [this](AsyncWebServerRequest * request){
       request->send(SPIFFS, "/favicon.ico", "image/vnd.microsoft.icon");
    });

    // REQUESTS
    server.on("/t", HTTP_GET, [this](AsyncWebServerRequest *request) {
      char str[10];
      sprintf(str, "%f", status->actual_temperature);
      request->send(200, "text/plain", str);
    });

    //PID
    server.on("/p", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%f", memory->getP());
      request->send(200, F("text/plain"), str);
    });
    
    server.on("/i", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%f", memory->getI());
      request->send(200, F("text/plain"), str);
    });

    server.on("/d", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%f", memory->getD());
      request->send(200, F("text/plain"), str);
    });


    server.on("/alpha", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%f", memory->getA());
      request->send(200, F("text/plain"), str);
    });

    server.on("/delay", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%d", memory->getDelay());
      request->send(200, "text/plain", str);
    });
    // end PID
    
    // Controller settings
    server.on("/controllerip", HTTP_GET, [this](AsyncWebServerRequest * request){
      IPAddress controller_IP = IPAddress();
      memory->getCONTROLLERIP(controller_IP);
      request->send(200, F("text/plain"), controller_IP.toString());
    });

    server.on("/controllergateway", HTTP_GET, [this](AsyncWebServerRequest * request){
      IPAddress controller_GW = IPAddress();
      memory->getCONTROLLERGW(controller_GW);
      request->send(200, F("text/plain"), controller_GW.toString());
    });

    server.on("/controllernwtmask", HTTP_GET, [this](AsyncWebServerRequest * request){
      IPAddress controller_MASK = IPAddress();
      memory->getMASK(controller_MASK);
      request->send(200, F("text/plain"), controller_MASK.toString());
    });

    server.on("/serverip", HTTP_GET, [this](AsyncWebServerRequest * request){
      IPAddress server_IP = IPAddress();
      memory->getSERVERIP(server_IP);
      request->send(200, "text/plain", server_IP.toString());
    });

    server.on("/controlleridcko", HTTP_GET, [this](AsyncWebServerRequest * request){
      char id[16];
      memory->getID(id);
      id[15] = '\0';
      request->send(200, "text/plain", id);
    });

    server.on("/controllerport", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%d", memory->getPORT());
      request->send(200, F("text/plain"), str);
    });

    //Status
    server.on("/eepromstat", HTTP_GET, [this](AsyncWebServerRequest * request){
      if (status->eeprom_begin){
        request->send(200, "text/plain", OK_RESPONSE);        
      } else {
        request->send(200, F("text/plain"), ERROR_RESPONSE);    
      }
    });

    server.on("/dacstat", HTTP_GET, [this](AsyncWebServerRequest * request){
      if (status->dac_connected){
        request->send(200, F("text/plain"), OK_RESPONSE);        
      } else {
        request->send(200, F("text/plain"), ERROR_RESPONSE);    
      }
    });

    server.on("/thermometerstat", HTTP_GET, [this](AsyncWebServerRequest * request){
      if (status->thermometer_connected){
        request->send(200, F("text/plain"), OK_RESPONSE);        
      } else {
        request->send(200, F("text/plain"), ERROR_RESPONSE);    
      }
    });
    
    server.on("/serverstat", HTTP_GET, [this](AsyncWebServerRequest * request){
      if (status->connected_server){
        request->send(200, F("text/plain"), "connected");        
      } else if (status->searching_server) {
        request->send(200, F("text/plain"), "searching for server");    
      } else if (status->connection_error) {
        request->send(200, F("text/plain"), "connection error");    
      } else if (status->lost_connection) {
        request->send(200, F("text/plain"), "connection lost");    
      } else if (status->connecting_server) {
        request->send(200, F("text/plain"), "connecting to server");
      }
      request->send(200, F("text/plain"), "ERROR");
    });

    server.on("/air", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[4];
      sprintf(str, "%d", status->set_airflow);
      request->send(200, "text/plain", str);
    });

        server.on("/airset", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[4];
      sprintf(str, "%d", status->set_airflow);
      request->send(200, "text/plain", str);
    });

    server.on("/heatingt", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[10];
      sprintf(str, "%d", status->set_temperature);
      request->send(200, "text/plain", str);
    });

    server.on("/power", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[4];
      sprintf(str, "%d", status->set_power);
      request->send(200, "text/plain", str);
    });

    server.on("/actualpower", HTTP_GET, [this](AsyncWebServerRequest * request){
      char str[4];
      sprintf(str, "%d", status->actual_power);
      request->send(200, "text/plain", str);
    });
    //END Status

    server.onNotFound([this](AsyncWebServerRequest * request){
      request->send(SPIFFS, "/404.html", "text/html");
    });
    server.begin();
    return true;
  }


   


 

};