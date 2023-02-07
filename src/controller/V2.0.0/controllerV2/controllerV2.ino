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
  // P     |4      |4      | 40-60   | / 1000
  // D     |10     |5      | 5       | / 1000
  // I     |10     |10     | 40-60   | / 100000
  // temp  |180    |30-400 | 80-180  |
  // Alpha |1 000  |1 000  | 1 000   | / 1000
  // Delay |250    |250    | 250     | 
  // ---------------------------------


#include "defines.h"
#include "preferences.h"
#include "status.h"
#include "statusled.h"
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
StatusLed pixels;
IPAddress CONTROLLER_DNS(1, 1, 1, 10);

//hardwer
MAX6675 thermocouple(PIN_thermoCLK, PIN_thermoCS, PIN_thermoDO);
DFRobot_GP8403 dac(&Wire, DAC_address);

// global variable.
unsigned long millis_temperature = 0;


void dacPower(uint16_t power){
  if (power > 10000){
  power = 10000;
  } else if (power < 0){
  power = 0;
  }
  dac.outputSquare(power, 10000, 0, 100, DAC_POEWR_PORT);
}

void dacAir(uint8_t air_power){
  if (air_power > 100){
    air_power = 100;
  } else if (air_power < 0){
    air_power = 0;
  }
  dac.outputSquare(air_power * 100, 10000, 0, 100, DAC_AIRFLOW_PORT);
}

void setup() {
  Serial.begin(115200);
  Serial.println("Booting up...");
  
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
  status.ack_server_time_out = millis();

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

  Serial.print("ETH0 broadcast IP:");
  Serial.println(ETH.broadcastIP());

  Serial.println("Starting Web server on port 80");

  webserver.begin(server, &memory, &status);
  
  serverComm.begin(&udp, &tcp, &status, &memory);

  Serial.print("\nChecking hardware\n");
  pixels.begin();
  
  Wire.beginTransmission(DAC_address);
  Wire.endTransmission() == 0 ? status.dac_connected = true : status.dac_connected = false;

  // if (status.dac_connected = true){
  //   dacAir(50);
  // }

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

  // do not change!
  for (uint8_t i = 0; i < 5; i++){
    Serial.print("*");
    delay(1000);
  }
  Serial.println();
  // status.connection_error = true; // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // VYMAZAT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

}
  

void handleDac(){
if ((status.dac_connected == false) || status.thermometer_connected == false){
  return;
}

if (status.emergency_stop == true){
  dacPower(0);
  dacAir(100);
  status.set_power = 0;
  status.actual_power = 0;
  status.set_airflow = 100;    
  return;
}

if (status.connected_server == false && status.disconnected_time_out == false){
  status.disconnected_time_out_millis = millis();
  status.disconnected_time_out = true;
  // return;
}

if (status.disconnected_time_out == true && status.connected_server == false && millis() - status.disconnected_time_out_millis > TIMEOUT_SERVER_STOP){ // #55 Ak vypadne sieť tak vypnúť testovanie po 100 sekund
  dacPower(0);
  dacAir(100);
  status.actual_power = 0;
  status.set_airflow = 100;
  
  status.set_temperature = 0;
  return;
}

if (status.set_temperature > 0){
  status.set_airflow = 100;
  dacAir(100);
} else {
  status.actual_power = 0;
  dacPower(0);
}

if (status.actual_power >= 0 && status.actual_power <= 10000){
  dacPower(status.actual_power);
}

if (status.set_temperature == 0 && status.actual_temperature < 30){
  status.set_airflow = 50;
  dacAir(50);
  return;
}

  if (status.set_temperature == 0 && status.actual_temperature > 50){
  status.set_airflow = 100;
  dacAir(100);
  return;
}

if (status.set_airflow >= 0 && status.set_airflow <= 100){
  dacAir(status.set_airflow);
}
}

void handleTemperature(int update_time){
if (millis_temperature > millis()){
  millis_temperature = 0;
}

if (millis() - millis_temperature > update_time){

  #ifdef SIMULATION
    if (status.set_temperature > 0 && status.actual_temperature < status.set_temperature){
      status.actual_temperature += 0.25;
    } else if (status.set_temperature >= 0 && status.actual_temperature > status.set_temperature){
      status.actual_temperature -= 0.25;
    }
    if (status.emergency_stop == true){
      status.actual_temperature = 0;
    }
  #else
    status.actual_temperature = thermocouple.readCelsius();
  #endif

  millis_temperature = millis();
}

if (status.actual_temperature != status.last_temperature){
  //serverComm.sendTemperature();//todo kazdu sec
  status.last_temperature = status.actual_temperature;
} 

#ifndef SIMULATION
  if (status.pid_delay_millis > millis()){
    status.pid_delay_millis = 0;
  }


  if (millis() - status.pid_delay_millis > memory.getDelay()){
    pd_step();
    status.pid_delay_millis = millis();
  }
#endif
}

// PDI reg

void pd_step()
{   
      
  if (status.set_temperature == 0 || status.emergency_stop == true) {
    return;
  }

  static float last_err = 0;
  static float suma = 0;
  
  float p_err = status.set_temperature - status.actual_temperature;
  float d_err = p_err - last_err;
  last_err = p_err;

  Serial.print("t:");
  Serial.println(status.actual_temperature);

  //printf("%-6s%-6s%-6s%-13s%-5s\n", "temp", "t-err", "d-err", "required-power", "" , "delta");
  //printf("%-6s%-6s%-6s%-13s%-5s\n", "temp", "t-err", "d-err", "required-power", "" , "delta");

  Serial.print(p_err);
  Serial.print(" ");
  Serial.print(d_err);
  Serial.print(" ");

  suma += p_err * memory.getI();
  float delta_amount = 1000 * (suma + memory.getP() * p_err + memory.getD() * d_err);
  float current_power = delta_amount;
  if (current_power > 10000) current_power = 10000;
  if (current_power < 0) current_power = 0;

  status.actual_power = ((1 - memory.getA()) * status.actual_power + memory.getA() * current_power);
  
  Serial.print((int)current_power);
  Serial.print(" ");
  Serial.print((int)delta_amount);
  Serial.print(" ");
  Serial.println((int)status.actual_power);

  //dac.outputSquare((int)status.actual_power, 10000, 0, 100, 0);
  
  
}

void loop() {
  if (status.pid_delay_millis > millis()){
    status.pid_delay_millis = 0;
  }


  handleTemperature(THERMOMETER_UPDATING_TIME);
  serverComm.refresh();
  serverComm.sendTemperatureTimeout(millis(), THERMOMETER_SENDING_INTERVAL);
  
  #ifndef SIMULATION
    handleDac();
  #endif

}
