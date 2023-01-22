//********************************************************
// Class to handle EEPROM memory and save preferences
//
// MEMORY FLAG
//  0000 0000
//  ││││ │││└── SERVER IP is set
//  ││││ ││└─── PORT is set
//  ││││ │└──── ID is set
//  ││││ └───── NOT USE
//  ││││
//  │││└─────── CONTROLLER IP with MASK is set //todo
//  ││└──────── PID controller has set values //todo
//  │└───────── NOT USE
//  └────────── NOT USE
//
//********************************************************

#ifndef PREFERENCES_h
#define PREFERENCES_h

#include "defines.h"
#include <IPAddress.h>
#include <math.h>

#ifndef EEPROM_h
#include <EEPROM.h>
#endif



#define MEMORY_START 0
#define DEFAULT_PORT 4002
#define DEFAULT_MASK 24

class Preferences {
private:
  // FLAGS = 1Byte
  // SERVER_IP = 4Byte
  // PORT = 2Byte
  // ID = 15Byte
  // CONTROLLER_IP = 4Byte
  // CONTROLLER_MASK = 1Byte
  // P/D/I/ALPHA_reg = 8Byte (float)
  // DELAY = 4Byte (uint16)

  enum addresses { FLAGS = MEMORY_START + 0,
                   SERVER_IP = MEMORY_START + 1,
                   PORT = MEMORY_START + 5,
                   ID = MEMORY_START + 7,
                   CONTROLLER_IP = MEMORY_START + 23,
                   CONTROLLER_GW = MEMORY_START + 27,
                   CONTROLLER_MASK = MEMORY_START + 31,
                   P_reg = MEMORY_START + 32,
                   D_reg = MEMORY_START + 41,
                   I_reg = MEMORY_START + 50,
                   ALPHA_reg = MEMORY_START + 59,
                   DELAY_reg = MEMORY_START + 68,};

  enum flag { serverip = 0x01,
              port = 0x02,
              id = 0x04,
              controllerip = 0x10,
              pid = 0x20};
  uint8_t flags;
  uint16_t eeprom_size;

public:
  Preferences();

  bool begin(uint16_t size);

  void convertNetMask(uint8_t mm, IPAddress &addr);

  uint8_t convertNetMask(IPAddress mm);

  bool isServerIPset();

  bool isPORTset();
  
  bool isIDset();

  bool isControllerIPset();


  bool setSERVERIP(IPAddress address);

  void getSERVERIP(IPAddress &ip);

  bool setCONTROLLERIP(IPAddress IPaddress, IPAddress GWaddress);

  void getCONTROLLERIP(IPAddress &ip);
  
  void getCONTROLLERGW(IPAddress &ip);

  bool setPORT(uint16_t port);

  uint16_t getPORT();

  bool setID(char id_name[]);

  char* getID(char* id);

  // PID regulator

  void getMASK(IPAddress &ipaddr);
  bool setMASK(IPAddress mask);

  float getP();
  bool setP(float val);

  float getI();
  bool setI(float val);

  float getD();
  bool setD(float val);

  float getA();
  bool setA(float val);

  uint16_t getDelay();

  bool setDelay(uint16_t val);

private:
  void setFlag(flag mask);

  void readFlags();

  void writeFlags();

  void erasureAll();

  void getIP(addresses memory_address, IPAddress &ipaddr);

  bool setIP(addresses memory_address, IPAddress address);

  float getPID(addresses reg);

  bool setPID(addresses reg, float val);
};

#endif