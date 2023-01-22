//*************************************************************
//  ,-----.  ,--. ,--.,------. ,--.  ,--.,--.,------.,------.
//  |  |) /_ |  | |  ||  .--. '|  ,'.|  |`--'|  .---'|  .---'
//  |  .-.  \|  | |  ||  '--'.'|  |' '  |,--.|  `--, |  `--,
//  |  '--' /'  '-'  '|  |\  \ |  | `   ||  ||  `---.|  `---.
//  `------'  `-----' `--' '--'`--'  `--'`--'`------'`------'
//                                                 V.2.0.0
//*************************************************************
#include "defines.h"
#include "preferences.h"
#include "status.h"
#include "CommunicationHandler.h"
#include "webServerHandlers.cpp"

#include <Wire.h>
// comunication
#include <IPAddress.h>
#include <AsyncTCP.h>
#include <AsyncUDP.h>
#include <AsyncWebServer_WT32_ETH01.h>

#include <max6675.h>         // thermometer
#include <DFRobot_GP8403.h>  // DAC controller

#include <EEPROM.h>

AsyncWebServer server(80);  // init webserver on port 80
AsyncUDP udp;
AsyncClient tcp;
Preferences memory;
Status status;
WebHandler webserver;
ServerCommunication serverComm;
IPAddress CONTROLLER_DNS(1, 1, 1, 10);

//hardwer
MAX6675 thermocouple(PIN_thermoCLK, PIN_thermoCS, PIN_thermoDO);
DFRobot_GP8403 dac(&Wire, DAC_address);

// global variable.
unsigned long milis_temperature = 0;

void setup() {
  Serial.begin(115200);
  Serial.println("Booting...");
  status.begin();

  Wire.begin(PIN_SDA, PIN_SCL) ? Serial.println("Wire: OK") : Serial.println("Wire: ERROR");

  memory.begin(EEPROM_SIZE) ? status.eeprom_begin = true : status.eeprom_begin = false;
  status.eeprom_begin ? Serial.println("Preferences: OK") : Serial.println("Preferences: ERROR");

  serverComm.begin(&udp, &tcp, &status, &memory);

  Serial.println("\n\nStarting ethernet");
  WT32_ETH01_onEvent();
  ETH.begin(ETH_PHY_ADDR, ETH_PHY_POWER);
#ifndef _DHCP
  IPAddress controller_IP = IPAddress();
  IPAddress controller_GW = IPAddress();
  IPAddress controller_MASK = IPAddress();
  memory.getCONTROLLERIP(controller_IP);
  memory.getCONTROLLERGW(controller_GW);
  memory.getMASK(controller_MASK);

  Serial.print("IP: ");
  Serial.println(controller_IP);
  Serial.print("GW: ");
  Serial.println(controller_GW);
  Serial.print("MASK: ");
  Serial.println(controller_MASK);

  ETH.config(controller_IP, controller_GW, controller_MASK, CONTROLLER_DNS);
#endif

  Serial.println("waiting for ethernet connection ");
#ifdef _DHCP
  Serial.println("and for DHCP. ");
#endif

  unsigned long eth_not_connected_timeout = millis();
  while (WT32_ETH01_isConnected() == false) {
    if (millis() - eth_not_connected_timeout < 60000) {
      Serial.print("*");
      delay(1000);
    } else {
      // turn on WIFI.
    }
  }
  Serial.println();

  //WT32_ETH01_waitForConnect();
  Serial.print("ETH0 IP:");
  Serial.println(ETH.localIP());

  Serial.println("Starting Web server on port 80");

  webserver.begin(server, &memory, &status);

  Serial.print("\n\nChecking hardware\n");
  Wire.beginTransmission(DAC_address);
  Wire.endTransmission() == 0 ? status.dac_connected = true : status.dac_connected = false;

  Serial.print("Thermometer booting up");

  for (uint8_t i = 0; i < 5; i++) {
    Serial.print(".");
    status.actual_temperature = thermocouple.readCelsius();
    delay(250);
  }
  status.actual_temperature != status.actual_temperature ? status.thermometer_connected = false : status.thermometer_connected = true;

  // info output to serial.
  Serial.println();
  Serial.print("DAC: ");
  status.dac_connected ? Serial.println("OK") : Serial.println("ERROR");
  Serial.print("Thermometer: ");
  status.thermometer_connected ? Serial.println("OK") : Serial.println("ERROR");


  dac.begin();
  dac.setDACOutRange(dac.eOutputRange10V);
}

void dacPower(uint8_t power){
  if (power > 100){
    power = 100;
  }
  dac.outputSquare(power * 100, 10000, 0, 100, 1);
}

void dacAir(uint8_t air_power){
  if (air_power > 100){
    air_power = 100;
  }
  dac.outputSquare(air_power * 100, 10000, 0, 100, 0);
}

void handleDac(){
  if (status.dac_connected == false || status.thermometer_connected == false){
    return;
  }

  if (status.emergency_stop == true){
    dacPower(0);
    dacAir(100);
  }

  if (status.actual_temperature <= 0){
    dacAir(50); //chladenie po 30C
  }

  if (status.actual_power > 0){
    dacPower(status.actual_power);
  }

  if (status.set_airflow > 0){
    dacPower(status.set_airflow);
  }
}

void handleTemperature(int update_time){
  if (milis_temperature > millis()){
    milis_temperature = 0;
  }

  if (millis() - milis_temperature > update_time){
    status.actual_temperature = thermocouple.readCelsius();
    milis_temperature = millis();
  }

  if (status.actual_temperature != status.last_temperature){
    //Serial.println(status.actual_temperature);
    status.last_temperature = status.actual_temperature;
  }
}

void loop() {
  
  handleTemperature(THERMOMETER_UPDATING_TIME);
  serverComm.refresh();
  handleDac();

}
