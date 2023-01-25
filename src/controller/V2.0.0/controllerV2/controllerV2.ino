//*************************************************************
//  ,-----.  ,--. ,--.,------. ,--.  ,--.,--.,------.,------.
//  |  |) /_ |  | |  ||  .--. '|  ,'.|  |`--'|  .---'|  .---'
//  |  .-.  \|  | |  ||  '--'.'|  |' '  |,--.|  `--, |  `--,
//  |  '--' /'  '-'  '|  |\  \ |  | `   ||  ||  `---.|  `---.
//  `------'  `-----' `--' '--'`--'  `--'`--'`------'`------'
//                                                 V.2.0.0
//*************************************************************


  // test 01 = extremne pomale ale ide
  // test 02 = rychlejsie ale o 8 stupnov prestreli a dlho mu trva pokial sa to stabilizuje
  // test 03 = na zaciatku to islo extremne rychlo ale potom to spomalilo a islo to opatrne hore. MEGA super, funguje to aj pri 280 teplote
  // ---------------------------------
  // test  |01     |02     | 03      |
  // ---------------------------------
  // air   |5 000  |5 000  | 10 000  |
  // P     |4      |4      | 40-60   | 
  // D     |10     |5      | 5       |
  // I     |10     |10     | 40-60   |
  // temp  |180    |30-400 | 80-180  |
  // Alpha |1 000  |1 000  | 1 000   |
  // Delay |250    |250    | 250     |
  // ---------------------------------


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
#include <math.h>

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
  
  serverComm.begin(&udp, &tcp, &status, &memory);

  Serial.print("\nChecking hardware\n");
  Wire.beginTransmission(DAC_address);
  Wire.endTransmission() == 0 ? status.dac_connected = true : status.dac_connected = false;

  #ifndef SIMULATION
    Serial.print("Thermometer booting up");

    for (uint8_t i = 0; i < 5; i++) {
      Serial.print(".");
      status.actual_temperature = thermocouple.readCelsius();
      delay(250);
    }
    //isnan(status.actual_temperature) ? status.thermometer_connected = false : status.thermometer_connected = true;
    status.thermometer_connected = true;
  #else
    status.thermometer_connected = false ;
  #endif 

  // info output to serial.
  Serial.println();
  Serial.print("DAC: ");
  status.dac_connected ? Serial.println("OK") : Serial.println("ERROR");
  Serial.print("Thermometer: ");
  status.thermometer_connected ? Serial.println("OK") : Serial.println("ERROR");


  
  if (status.dac_connected == true){
    dac.begin();
    dac.setDACOutRange(dac.eOutputRange10V);
}
  }
  

void dacPower(uint8_t power){
  if (power > 100){
    power = 100;
  }
  dac.outputSquare(power * 100, 10000, 0, 100, 0);
}

void dacAir(uint8_t air_power){
  if (air_power > 100){
    air_power = 100;
  }
  dac.outputSquare(air_power * 100, 10000, 0, 100, 1);
}

void handleDac(){
  if (status.dac_connected == false || status.thermometer_connected == false){
    return;
  }

  if (status.connected_server == false && status.disconnected_time_out == false){
    status.disconnected_time_out_milis = millis();
    status.disconnected_time_out = true;
    return;
  }

  if (status.disconnected_time_out == true && millis() - status.disconnected_time_out_milis > 100000 ){ // #55 Ak vypadne sieť tak vypnúť testovanie po 100 sekund
    dacPower(0);
    dacAir(100);
    status.set_temperature = 0;
    return;
  }

  if (status.emergency_stop == true){
    status.set_temperature = 0;
    return;
  }

  if (status.actual_power >= 0 && status.actual_power <= 100){
    dacPower(status.actual_power);
  }

  if (status.set_airflow >= 0 && status.set_airflow <= 0){
    dacAir(status.set_airflow);
  }

  // if (status.actual_temperature < 30 && status.set_temperature == 0){
  //   dacAir(50);
  // }
  if (status.actual_temperature <= 0){
    dacAir(50); //chladenie po 30C
  }

  if (status.set_temperature > 0){
    dacAir(100);
  }
}

void handleTemperature(int update_time){
  if (milis_temperature > millis()){
    milis_temperature = 0;
  }

  if (millis() - milis_temperature > update_time){

    #ifdef SIMULATION
    if (status.set_temperature > 0 && status.actual_temperature < status.set_temperature){
      status.actual_temperature += 0.25;
    } else if (status.set_temperature >= 0 && status.actual_temperature > status.set_temperature){
      status.actual_temperature -= 0.25;
    }
    #else
      status.actual_temperature = thermocouple.readCelsius();
    #endif

    milis_temperature = millis();
  }

  if (status.actual_temperature != status.last_temperature){
    //serverComm.sendTemperature();//todo kazdu sec
    status.last_temperature = status.actual_temperature;
  } 



  #ifdef SIMULATION
    if (millis() - status.pid_delay_millis > memory.getDelay()){
      pd_step();
      status.pid_delay_millis = millis();
    }
  #endif


}


// PDI reg

void pd_step()
{   

    if (status.connected_server == false){
      return;
    }
    
    if (status.set_temperature == 0 || status.emergency_stop == true) {
      dacPower(0);
      return;
    }
    static float last_err = 0;
    static float suma = 0;

    //unsigned long tm = millis();
    //if (tm < last_time_measured_temp) last_time_measured_temp = 0;
    
    // if (tm - last_time_measured_temp > 250)
    // {
    //   temperature = thermocouple.readCelsius();
    //   last_time_measured_temp = tm;      
    // }    
    
    //delay(memory.getDelay());

        
    float p_err = status.set_temperature - status.actual_temperature;
    float d_err = p_err - last_err;
    last_err = p_err;

    //Serial.print("err: ");

    //Serial.print("p err");
    //Serial.println()
    // Serial.print("t: ");
    // Serial.println(status.actual_temperature);


    // Serial.print(p_err);
    // Serial.print(" ");
    // Serial.print(d_err);
    // Serial.print(" ");

    suma += p_err * memory.getI();
    float delta_amount = 1000 * (suma + memory.getP() * p_err + memory.getD() * d_err);
    float current_power = delta_amount;
    if (current_power > 10000) current_power = 10000;
    if (current_power < 0) current_power = 0;

    status.actual_power = ((1 - memory.getA()) * status.actual_power + memory.getA() * current_power) / 100.0;
    
    // Serial.print((int)current_power);
    // Serial.print(" ");
    // Serial.print((int)delta_amount);
    // Serial.print(" ");
    // Serial.println((int)status.actual_power);

    //dac.outputSquare((int)status.actual_power, 10000, 0, 100, 0);
    
    
}

void loop() {
  if (status.pid_delay_millis > millis()){
    status.pid_delay_millis = 0;
  }


  handleTemperature(THERMOMETER_UPDATING_TIME);
  serverComm.refresh();
  serverComm.sendTemperatureTimeout(millis(), THERMOMETER_SENDING_INTERVAL);
  
  



  handleDac();

}
