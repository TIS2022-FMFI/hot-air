#ifndef DEFINES_h
#define DEFINES_h

//********************************************************
//  All important system functions that cannot be 
//  modified in RunTime are set in this file.
//
//                                               V2.0.0
//********************************************************
#include <IPAddress.h>

// comment if you want to use static IP
// uncomment if you want to get IP from DHCP
//#define _DHCP set

// simulation
//#define SIMULATION 

// Select the IP address according to your local network, works only if _DHCP is commented
// all info taks form memory
// #ifndef _DHCP
//   IPAddress myIP(10, 1, 1, 105);
//   IPAddress myGW(10, 1, 1, 1);
//   IPAddress mySN(255, 0, 0, 0);
// #endif

//********************************************************
//
//                        SERVER
//
//********************************************************

#define TIMEOUT_SERVER_STOP 2000 //ms // cas za ktory sa vypne ohrev duchadla po prepnuti do stavu Odpojene
#define TIMEOUT_UDP_CONNECTION 10000 //ms //za aky cas sa prepnem do stavu Odpojene
#define TIMEOUT_UDP_ACK_TIME 5000 //ms // po akom case controller vyhlasi error ze sa nevie pripojit na server.

//********************************************************
//
//                     THERMOMETER
//
//********************************************************
#define THERMOMETER_UPDATING_TIME 500 // in milliseconds // min updating time is 250ms
#define THERMOMETER_SENDING_INTERVAL 1000 // min 500ms max 5000ms
//********************************************************
//
//                     PIN OUT
//
//********************************************************
// thermocouple
#define PIN_thermoDO 14
#define PIN_thermoCS 12
#define PIN_thermoCLK 15

// I2C
#define PIN_SDA 2
#define PIN_SCL 4

// DAC controller
#define DAC_address 0x5F
#define DAC_POEWR_PORT 0
#define DAC_AIRFLOW_PORT 1

// LED 
#define RGB_LED_PIN 17
#define NUMBER_OF_PINS 1

//Preferences 
#define EEPROM_SIZE 128


//********************************************************
//
//                      DEBUG
//
//********************************************************
// for getting ethernet debug msg on Serial
#define DEBUG_ETHERNET_WEBSERVER_PORT       Serial

// Debug Level from 0 to 4
#define _ETHERNET_WEBSERVER_LOGLEVEL_       3

// USE ONLY FOR TESTING. BYPASS ALL SAFTY
#define _DEBUG

#endif
