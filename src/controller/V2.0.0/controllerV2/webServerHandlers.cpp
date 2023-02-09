#include "esp32-hal.h"
#include <sys/types.h>
#include "pgmspace.h"

#include <sys/_stdint.h>
#include <string>
#include "WString.h"
#include "IPAddress.h"
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
  Preferences *memory;
  Status *status;
  volatile bool lock;

public:
  WebHandler() {
  }

  void string2ip(IPAddress &ipaddr, String ipString) {
    int parts[4];
    for (int j = 0; j < 4; j++) {
      int dot_index = ipString.indexOf('.');
      if (dot_index == -1) {
        parts[j] = ipString.toInt();
        break;
      }
      parts[j] = ipString.substring(0, dot_index).toInt();
      ipString = ipString.substring(dot_index + 1);
    }
    ipaddr = IPAddress(parts[0], parts[1], parts[2], parts[3]);
  }

  bool handleData(AsyncWebServerRequest *request) {
    bool restart = false;
    Serial.print(request->args());
    Serial.println("new dates arrived.");

    if (request->hasParam("controllerIP", true) && request->hasParam("controllerGW", true)) {
      String IP;
      String GW;
      IP = request->getParam("controllerIP", true)->value();
      GW = request->getParam("controllerGW", true)->value();
      IPAddress controllerIP = IPAddress();
      IPAddress controllerGW = IPAddress();
      string2ip(controllerIP, IP);
      string2ip(controllerGW, GW);
      memory->setCONTROLLERIP(controllerIP, controllerGW);
      Serial.println("got IP:");
      Serial.println(controllerIP);
      Serial.println(controllerGW);
      restart = true;
    }
    if (request->hasParam("controllerMASK", true)) {
      String stringMask;
      stringMask = request->getParam("controllerMASK", true)->value();
      IPAddress controllerMask = IPAddress();
      string2ip(controllerMask, stringMask);
      memory->setMASK(controllerMask);
      Serial.print("New controller Mask: ");
      Serial.println(controllerMask);
      restart = true;
    }
    if (request->hasParam("controllerID", true)) {
      String idstr;
      idstr = request->getParam("controllerID", true)->value();
      Serial.write(idstr.c_str(), 15);
      memory->setID(idstr.c_str());
      status->id_has_change = true;
    }
    if(request->hasParam("serverIP",true)){
      String stringIP;
      stringIP = request->getParam("serverIP", true)->value();
      IPAddress serverIP = IPAddress();
      string2ip(serverIP, stringIP);
      memory->setSERVERIP(serverIP);
      Serial.print("New Server IP: ");
      Serial.println(serverIP);
    }
    if(request->hasParam("port",true)){
      String port = request->getParam("port", true)->value();
      memory->setPORT(port.toInt());
    }
    // PID
    if(request->hasParam("p",true)){
      String p = request->getParam("p", true)->value();
      Serial.print("P: ");
      Serial.println(p);
      memory->setP(p.toFloat());
    }
    if(request->hasParam("i",true)){
      String i = request->getParam("i", true)->value();
      Serial.print("I: ");
      Serial.println(i);
      memory->setI(i.toFloat());
    }
    if(request->hasParam("d",true)){
      String d = request->getParam("d", true)->value();
      Serial.print("D: ");
      Serial.println(d);
      memory->setD(d.toFloat());
    }
    if(request->hasParam("alpha",true)){
      String a = request->getParam("alpha", true)->value();
      Serial.print("A: ");
      Serial.println(a);
      memory->setA(a.toFloat());
    }
    if(request->hasParam("delay",true)){
      String delay = request->getParam("delay", true)->value();
      Serial.print("Delay: ");
      Serial.println(delay);
      memory->setDelay(delay.toInt());
    }
    if(request->hasParam("deltat",true)){
      String temp = request->getParam("deltat", true)->value();
      memory->setDeltaT(temp.toInt());
      Serial.print("Delta T: ");
      Serial.println(memory->getDeltaT());
    }

    //manualcontroll
    if(request->hasParam("power",true)){
      String powerstr = request->getParam("power", true)->value();
      status->actual_power = powerstr.toInt();
      Serial.print("Get new POWER: ");
      Serial.println(status->actual_power);
    }
    if(request->hasParam("airflow",true)){
      String airflowstr = request->getParam("airflow", true)->value();
      status->set_airflow = airflowstr.toInt();
      Serial.print("Get new AIRFLOW: ");
      Serial.println(status->set_airflow);
    }
    if(request->hasParam("temp",true)){
      String temp = request->getParam("temp", true)->value();
      status->set_temperature = temp.toInt();
      Serial.print("Get new TEMPERATURE: ");
      Serial.println(status->set_temperature);
    }

    if (restart == true){
      ESP.restart();
    }

    return false;
  };

  void waitforlock(bool _lock){
    while (_lock) {
      delayMicroseconds(100);
    }
  }

  bool begin(AsyncWebServer &server, Preferences *m, Status *s) {
    memory = m;
    status = s;
    lock = false;
    !SPIFFS.begin(true) ? status->spiffs_begin = false: status->spiffs_begin = true;
    !status->spiffs_begin ? Serial.println("An Error has occurred while mounting SPIFFS") : Serial.println("SPIFFS OK");


    // STATIC WEB PAGES
    // html
    server.on("/", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("root index.html");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/index.html", "text/html");
      lock = false;
      Serial.println("root index.html");
    });

    server.on("/index.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("index.html");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/index.html", "text/html");
      lock = false; 
      Serial.println("index.html");
    });

    server.on("/manualcontrol.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("manualcontrol.html");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/manualcontrol.html", "text/html");
      lock = false; 
      Serial.println("manualcontrol.html");
    });


    server.on("/settings.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("settings.html");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/settings.html", "text/html");
      lock = false;
      Serial.println("settings.html");
    });

    server.on("/pid.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("pid.html");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/pid.html", "text/html");
      lock = false;
      Serial.println("pid.html");
    });

    // css
    server.on("/style.css", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("style.css");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/style.css", "text/css");
      lock = false;
      Serial.println("style END.css");
    });

    server.on("/main.css", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("main.css");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/main.css", "text/css");
      lock = false;
      Serial.println("main END.css");
    });

    // js
    server.on("/js/ajax.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("ajax.js");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/ajax.js", "application/javascript");
      lock = false;
      Serial.println("ajax.js");
    });

    server.on("/js/dashboard.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("dashboard.js");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/dashboard.js", "application/javascript");
      Serial.println("dashboard.js");
    });

    server.on("/js/form.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("form.js");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/form.js", "application/javascript");
      lock = false;
      Serial.println("form.js");
    });

    server.on("/js/manualcontrol.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("manualcontrol.js");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/manualcontrol.js", "application/javascript");
      lock = false; 
      Serial.println("manualcontrol.js");
    });

    server.on("/js/pid.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("pid.js");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/pid.js", "application/javascript");
      lock = false; 
      Serial.println("pid.js");
    });

    server.on("/js/ui.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("ui.js");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/ui.js", "application/javascript");
      lock = false;
      Serial.println("ui.js");
    });

    //ico
    server.on("/favicon.ico", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("favicon.ico");
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/favicon.ico", "image/vnd.microsoft.icon");
      lock = false; 
      Serial.println("favicon.ico");
    });

    // REQUESTS
    server.on("/t", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("t");
      waitforlock(lock);
      lock = true;
      char str[20];
      sprintf(str, "%f", status->actual_temperature);
      request->send(200, "text/plain", str);
      lock = false;
      Serial.println("t");
    });

    //PID
    server.on("/p", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("p");
      waitforlock(lock);
      lock = true;  
      char str[20];
      sprintf(str, "%f", memory->getP());
      request->send(200, "text/plain", str);
      lock = false; 
      Serial.println("p");
    });

    server.on("/i", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("i");
      waitforlock(lock);
      lock = true;  
      char str[20];
      sprintf(str, "%f", memory->getI());
      request->send(200, "text/plain", str);
      lock = false;
      Serial.println("i");
    });

    server.on("/d", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[20];
      sprintf(str, "%f", memory->getD());
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/alpha", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[11];
      sprintf(str, "%10f", memory->getA());
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/delay", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[20];
      sprintf(str, "%d", memory->getDelay());
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/deltat", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[11];
      sprintf(str, "%10f", memory->getDeltaT());
      request->send(200, "text/plain", str);
      lock = false; 
    });
    // end PID

    // Controller settings
    server.on("/controllerip", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("controllerip");
      waitforlock(lock);
      lock = true;  
      IPAddress controller_IP = IPAddress();
      memory->getCONTROLLERIP(controller_IP);
      request->send(200, "text/plain", controller_IP.toString());
      lock = false; 
      Serial.println("controllerip");
    });

    server.on("/controllergateway", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("gs");
      waitforlock(lock);
      lock = true;  
      IPAddress controller_GW = IPAddress();
      memory->getCONTROLLERGW(controller_GW);
      request->send(200, "text/plain", controller_GW.toString());
      lock = false; 
      Serial.println("gs");
    });

    server.on("/controllernwtmask", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("mask");
      waitforlock(lock);
      lock = true;  
      IPAddress controller_MASK = IPAddress();
      memory->getMASK(controller_MASK);
      request->send(200, "text/plain", controller_MASK.toString());
      lock = false; 
      Serial.println("mask");
    });

    server.on("/serverip", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("serverip");
      waitforlock(lock);
      lock = true;  
      IPAddress server_IP = IPAddress();
      memory->getSERVERIP(server_IP);
      request->send(200, "text/plain", server_IP.toString());
      lock = false; 
      Serial.println("serverup");
    });

    server.on("/controlleridcko", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("idcko");
      waitforlock(lock);
      lock = true;  
      char id[16];
      uint8_t len = memory->getID(id);
      char idsend[len];
      memcpy(idsend, id, len);
      request->send(200, "text/plain", idsend);
      lock = false; 
      Serial.println("idecko");
    });

    server.on("/controllerport", HTTP_GET, [this](AsyncWebServerRequest *request) {
      Serial.println("port");
      waitforlock(lock);
      lock = true;  
      char str[10];
      sprintf(str, "%d", memory->getPORT());
      request->send(200, "text/plain", str);
      lock = false; 
      Serial.println("port");
    });

    //Status
    server.on("/eepromstat", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      if (status->eeprom_begin) {
        request->send(200, "text/plain", OK_RESPONSE);
      } else {
        request->send(200, "text/plain", ERROR_RESPONSE);
      }
      lock = false; 
    });

    server.on("/dacstat", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      if (status->dac_connected) {
        request->send(200, "text/plain", OK_RESPONSE);
      } else {
        request->send(200, "text/plain", ERROR_RESPONSE);
      }
      lock = false; 
    });

    server.on("/thermometerstat", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      if (status->thermometer_connected) {
        request->send(200, "text/plain", OK_RESPONSE);
      } else {
        request->send(200, "text/plain", ERROR_RESPONSE);
      }
      lock = false; 
    });

    server.on("/serverstat", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      if (status->connected_server) {
        request->send(200, "text/plain", "connected");
      } else if (status->searching_server) {
        request->send(200, "text/plain", "searching for server");
      } else if (status->connection_error) {
        request->send(200, "text/plain", "connection error");
      } else if (status->lost_connection) {
        request->send(200, "text/plain", "connection lost");
      } else if (status->connecting_server) {
        request->send(200, "text/plain", "connecting to server");
      }
      request->send(200, "text/plain", "ERROR");
      lock = false; 
    });

    server.on("/air", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[4];
      sprintf(str, "%d", status->set_airflow);
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/airset", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[4];
      sprintf(str, "%d", status->set_airflow);
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/heatingt", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[10];
      sprintf(str, "%d", status->set_temperature);
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/power", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[4];
      sprintf(str, "%d", status->set_power);
      request->send(200, "text/plain", str);
      lock = false; 
    });

    server.on("/actualpower", HTTP_GET, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      char str[6];
      sprintf(str, "%d", status->actual_power);
      request->send(200, "text/plain", str);
      lock = false; 
    });
    //END Status

    server.on("/data", HTTP_POST, [this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      handleData(request);
      request->send(200, "text/plain", "DATAOK");
      lock = false; 
    });

    server.on("/reboot", HTTP_GET, [this](AsyncWebServerRequest *request) {
      
      request->send(200, "text/plain", "REBOOTING!");
      delay(1000);
      ESP.restart();
    });

    server.onNotFound([this](AsyncWebServerRequest *request) {
      waitforlock(lock);
      lock = true;  
      request->send(SPIFFS, "/404.html", "text/html");
      lock = false;  
    });
    server.begin();
    return status->spiffs_begin;
  }
};