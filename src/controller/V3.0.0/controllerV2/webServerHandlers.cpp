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
    
    #ifdef _DEBUG
    Serial.print(request->args());
    Serial.println("new dates arrived.");
    #endif
    
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
      #ifdef _DEBUG
      Serial.println("got IP:");
      Serial.println(controllerIP);
      Serial.println(controllerGW);
      #endif
      restart = true;
    }
    if (request->hasParam("controllerMASK", true)) {
      String stringMask;
      stringMask = request->getParam("controllerMASK", true)->value();
      IPAddress controllerMask = IPAddress();
      string2ip(controllerMask, stringMask);
      memory->setMASK(controllerMask);
      #ifdef _DEBUG
      Serial.print("New controller Mask: ");
      Serial.println(controllerMask);
      #endif
      restart = true;
    }
    if (request->hasParam("controllerID", true)) {
      String idstr;
      idstr = request->getParam("controllerID", true)->value();
      #ifdef _DEBUG
      Serial.write(idstr.c_str(), 15);
      #endif
      memory->setID(idstr.c_str());
      status->id_has_change = true;
    }
    if(request->hasParam("serverIP",true)){
      String stringIP;
      stringIP = request->getParam("serverIP", true)->value();
      IPAddress serverIP = IPAddress();
      string2ip(serverIP, stringIP);
      memory->setSERVERIP(serverIP);
      #ifdef _DEBUG
      Serial.print("New Server IP: ");
      Serial.println(serverIP);
      #endif
    }
    if(request->hasParam("port",true)){
      String port = request->getParam("port", true)->value();
      memory->setPORT(port.toInt());
    }
    // PID
    if(request->hasParam("p",true)){
      String p = request->getParam("p", true)->value();
      #ifdef _DEBUG
      Serial.print("P: ");
      Serial.println(p);
      #endif
      memory->setP(p.toFloat());
    }
    if(request->hasParam("i",true)){
      String i = request->getParam("i", true)->value();
      #ifdef _DEBUG
      Serial.print("I: ");
      Serial.println(i);
      #endif
      memory->setI(i.toFloat());
    }
    if(request->hasParam("d",true)){
      String d = request->getParam("d", true)->value();
      #ifdef _DEBUG
      Serial.print("D: ");
      Serial.println(d);
      #endif
      memory->setD(d.toFloat());
    }
    if(request->hasParam("alpha",true)){
      String a = request->getParam("alpha", true)->value();
      #ifdef _DEBUG
      Serial.print("A: ");
      Serial.println(a);
      #endif
      memory->setA(a.toFloat());
    }
    if(request->hasParam("delay",true)){
      String delay = request->getParam("delay", true)->value();
      #ifdef _DEBUG
      Serial.print("Delay: ");
      Serial.println(delay);
      #endif
      memory->setDelay(delay.toInt());
    }
    if(request->hasParam("deltat",true)){
      String temp = request->getParam("deltat", true)->value();
      memory->setDeltaT(temp.toInt());
      #ifdef _DEBUG
      Serial.print("Delta T: ");
      Serial.println(memory->getDeltaT());
      #endif
    }

    //manualcontroll
    if(request->hasParam("power",true)){
      String powerstr = request->getParam("power", true)->value();
      status->actual_power = powerstr.toInt();
      #ifdef _DEBUG
      Serial.print("Get new POWER: ");
      Serial.println(status->actual_power);
      #endif
    }
    if(request->hasParam("airflow",true)){
      String airflowstr = request->getParam("airflow", true)->value();
      status->set_airflow = airflowstr.toInt();
      #ifdef _DEBUG
      Serial.print("Get new AIRFLOW: ");
      Serial.println(status->set_airflow);
      #endif
    }
    if(request->hasParam("temp",true)){
      String temp = request->getParam("temp", true)->value();
      status->set_temperature = temp.toInt();
      #ifdef _DEBUG
      Serial.print("Get new TEMPERATURE: ");
      Serial.println(status->set_temperature);
      #endif
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
    #ifdef _DEBUG
    !status->spiffs_begin ? Serial.println("An Error has occurred while mounting SPIFFS") : Serial.println("SPIFFS OK");
    #endif

    // STATIC WEB PAGES
    // html
    server.on("/", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("root index.html");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/index.html", "text/html");
      lock = false;
      #ifdef _DEBUG
      Serial.println("root index.html");
      #endif
    });

    server.on("/index.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("index.html");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/index.html", "text/html");
      lock = false; 
      #ifdef _DEBUG
      Serial.println("index.html");
      #endif
    });

    server.on("/manualcontrol.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("manualcontrol.html");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/manualcontrol.html", "text/html");
      lock = false;
      #ifdef _DEBUG 
      Serial.println("manualcontrol.html");
      #endif
    });


    server.on("/settings.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("settings.html");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/settings.html", "text/html");
      lock = false;
      #ifdef _DEBUG
      Serial.println("settings.html");
      #endif
    });

    server.on("/pid.html", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("pid.html");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/pid.html", "text/html");
      lock = false;
      #ifdef _DEBUG
      Serial.println("pid.html");
      #endif
    });

    // css
    server.on("/style.css", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("style.css");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/style.css", "text/css");
      lock = false;
      #ifdef _DEBUG
      Serial.println("style END.css");
      #endif
    });

    server.on("/main.css", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("main.css");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/main.css", "text/css");
      lock = false;
      #ifdef _DEBUG
      Serial.println("main END.css");
      #endif
    });

    // js
    server.on("/js/ajax.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("ajax.js");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/ajax.js", "application/javascript");
      lock = false;
      #ifdef _DEBUG
      Serial.println("ajax.js");
      #endif
    });

    server.on("/js/dashboard.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("dashboard.js");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/dashboard.js", "application/javascript");
      #ifdef _DEBUG
      Serial.println("dashboard.js");
      #endif
    });

    server.on("/js/form.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("form.js");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/form.js", "application/javascript");
      lock = false;
      #ifdef _DEBUG
      Serial.println("form.js");
      #endif
    });

    server.on("/js/manualcontrol.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("manualcontrol.js");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/manualcontrol.js", "application/javascript");
      lock = false; 
      #ifdef _DEBUG
      Serial.println("manualcontrol.js");
      #endif
    });

    server.on("/js/pid.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("pid.js");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/pid.js", "application/javascript");
      lock = false; 
      #ifdef _DEBUG
      Serial.println("pid.js");
      #endif
    });

    server.on("/js/ui.js", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("ui.js");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/js/ui.js", "application/javascript");
      lock = false;
      #ifdef _DEBUG
      Serial.println("ui.js");
      #endif
    });

    //ico
    server.on("/favicon.ico", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("favicon.ico");
      #endif
      waitforlock(lock);
      lock = true;
      request->send(SPIFFS, "/favicon.ico", "image/vnd.microsoft.icon");
      lock = false; 
      #ifdef _DEBUG
      Serial.println("favicon.ico");
      #endif
    });

    // REQUESTS
    server.on("/t", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("t");
      #endif
      waitforlock(lock);
      lock = true;
      char str[20];
      sprintf(str, "%f", status->actual_temperature);
      request->send(200, "text/plain", str);
      lock = false;
      #ifdef _DEBUG
      Serial.println("t");
      #endif
    });

    //PID
    server.on("/p", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("p");
      #endif
      waitforlock(lock);
      lock = true;  
      char str[20];
      sprintf(str, "%f", memory->getP());
      request->send(200, "text/plain", str);
      lock = false; 
      #ifdef _DEBUG
      Serial.println("p");
      #endif
    });

    server.on("/i", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("i");
      #endif
      waitforlock(lock);
      lock = true;  
      char str[20];
      sprintf(str, "%f", memory->getI());
      request->send(200, "text/plain", str);
      lock = false;
      #ifdef _DEBUG
      Serial.println("i");
      #endif
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
      #ifdef _DEBUG
      Serial.println("controllerip");
      #endif
      waitforlock(lock);
      lock = true;  
      IPAddress controller_IP = IPAddress();
      memory->getCONTROLLERIP(controller_IP);
      request->send(200, "text/plain", controller_IP.toString());
      lock = false; 
      #ifdef _DEBUG
      Serial.println("controllerip");
      #endif
    });

    server.on("/controllergateway", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG
      Serial.println("gs");
      #endif
      waitforlock(lock);
      lock = true;  
      IPAddress controller_GW = IPAddress();
      memory->getCONTROLLERGW(controller_GW);
      request->send(200, "text/plain", controller_GW.toString());
      lock = false;
      #ifdef _DEBUG 
      Serial.println("gs");
      #endif
    });

    server.on("/controllernwtmask", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG       
      Serial.println("mask");
      #endif
      waitforlock(lock);
      lock = true;  
      IPAddress controller_MASK = IPAddress();
      memory->getMASK(controller_MASK);
      request->send(200, "text/plain", controller_MASK.toString());
      lock = false; 
      #ifdef _DEBUG 
      Serial.println("mask");
      #endif
    });

    server.on("/serverip", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG 
      Serial.println("serverip");
      #endif
      waitforlock(lock);
      lock = true;  
      IPAddress server_IP = IPAddress();
      memory->getSERVERIP(server_IP);
      request->send(200, "text/plain", server_IP.toString());
      lock = false; 
      #ifdef _DEBUG 
      Serial.println("serverup");
      #endif
    });

    server.on("/controlleridcko", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG 
      Serial.println("idcko");
      #endif
      waitforlock(lock);
      lock = true;  
      char id[16];
      uint8_t len = memory->getID(id);
      char idsend[len];
      memcpy(idsend, id, len);
      request->send(200, "text/plain", idsend);
      lock = false; 
      #ifdef _DEBUG 
      Serial.println("idecko");
      #endif
    });

    server.on("/controllerport", HTTP_GET, [this](AsyncWebServerRequest *request) {
      #ifdef _DEBUG 
      Serial.println("port");
      #endif
      waitforlock(lock);
      lock = true;  
      char str[10];
      sprintf(str, "%d", memory->getPORT());
      request->send(200, "text/plain", str);
      lock = false; 
      #ifdef _DEBUG 
      Serial.println("port");
      #endif
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
