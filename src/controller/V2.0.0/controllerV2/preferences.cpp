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
#include "preferences.h"
#include <sys/_stdint.h>
#include "IPAddress.h"
#include "defines.h"

void Preferences::trim(char *str) {
  uint8_t index = 15;
  while (index >= 0 && (str[index] == ' ' || str[index] == '\0'))  {
    index--;
  }
}

Preferences::Preferences() {
}

bool Preferences::begin(uint16_t size) {
  eeprom_size = size;
  mem_flags = 0;



  if (EEPROM.begin(size) == false) {
    Serial.println("EEPROM load error.\n Rebooting...");
    //ESP.restart();
    return false; 
  } else {
    Serial.println("EEPROM load OK");
  }

  readFlags();
  if (mem_flags == 0xFF) {
    Serial.println("deleting EEPROM");
    erasureAll();
  }

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

  #ifdef _DEBUG
    IPAddress ip = IPAddress(10,1,1,105);
    IPAddress GW = IPAddress(10,1,1,1);
    // setMASK(24);
    // char idcko[] = "id05           ";
    // setID(idcko);
    setCONTROLLERIP(ip, GW);
    // setPORT(4002);
    // PID SETUP
    // setP(40/1000.0);
    // setD(5/1000.0);
    // setI(40/ 100000.0);
    // setA(1000/ 1000.0);
    // setDelay(250);
    

    IPAddress ip_addres = IPAddress();  
    Serial.println("Preferences BEGIN in debug mode.");
    Serial.print("FLAGS: ");
    Serial.printf("%X\n", mem_flags);

    Serial.print("Server IP: ");
    this->getSERVERIP(ip_addres);
    Serial.println(ip_addres);

    Serial.print("PORT: ");
    Serial.println(this->getPORT());

    Serial.print("ID: ");
    char id[16];
    this->getID(id);
    Serial.write(id, 15);
    Serial.println();

    Serial.print("Controller IP: ");
    this->getCONTROLLERIP(ip_addres);
    Serial.println(ip_addres);

    Serial.print("Controller GW: ");
    IPAddress ip_mask = IPAddress(); 
    this->getCONTROLLERGW(ip_mask);
    Serial.println(ip_mask);

    Serial.print("Controller MASK: ");
    this->getMASK(ip_addres);
    Serial.println(ip_addres);

    Serial.println("PDI setup: ");

    Serial.print("P: ");
    Serial.printf("%F\n", this->getP());

    Serial.print("D: ");
    Serial.printf("%F\n", this->getD());

    Serial.print("I: ");
    Serial.printf("%F\n", this->getI());

    Serial.print("Alpha: ");
    Serial.printf("%F\n", this->getA());

    Serial.print("Delay: ");
    Serial.println(this->getDelay());
  #endif

  return 1;
}


bool Preferences::loadEEPROM(){

  return false;
}

void Preferences::convertNetMask(uint8_t mm, IPAddress &addr){
  uint8_t mask = mm;
  uint8_t ip[4] = {0,0,0,0};

  for (uint8_t i = 0; i < 4; i++){
      uint8_t oneip = 0;
      for (uint8_t m = 0; m < 8; m++){
        oneip = oneip << 1;
        if (mask > 0){
          oneip++;
          mask--;
        }
      }
      ip[i] = oneip;
  }

  addr = IPAddress(ip[0], ip[1], ip[2], ip[3]);
}

uint8_t Preferences::convertNetMask(IPAddress mm){
uint8_t mask = 0;
for (uint8_t i = 0; i < 4; i++){
  uint8_t ip = mm[i];
  for (uint8_t m = 0; m < 8; m++){
    if ((ip & 128) != 128){
      mask++;
      ip = ip << 1;
    } else {
      return mask;
    }
  }
}
  return mask;    
}

bool Preferences::isServerIPset() {
  return (mem_flags & flag::serverip) == flag::serverip;
}

bool Preferences::isPORTset() {
  return (mem_flags & flag::port) == flag::port;
}

bool Preferences::isIDset() {
  return (mem_flags & flag::id) == flag::id;
}

bool Preferences::isControllerIPset() {
  return (mem_flags & flag::controllerip) == flag::controllerip;
}


bool Preferences::setSERVERIP(IPAddress address) {
  return setIP(addresses::SERVER_IP, address);
}

void Preferences::getSERVERIP(IPAddress &ip) {
  return getIP(addresses::SERVER_IP, ip);
}

bool Preferences::setCONTROLLERIP(IPAddress IPaddress, IPAddress GWaddress) {
  setFlag(flag::controllerip);
  return setIP(addresses::CONTROLLER_IP, IPaddress) && setIP(addresses::CONTROLLER_GW, GWaddress);

}

void Preferences::getCONTROLLERIP(IPAddress &ip){
  getIP(addresses::CONTROLLER_IP, ip);
}

void Preferences::getCONTROLLERGW(IPAddress &ip) {
  return getIP(addresses::CONTROLLER_GW, ip);
}

bool Preferences::setPORT(uint16_t port) {
  EEPROM.writeShort(addresses::PORT, port);
  setFlag(flag::port);
  return EEPROM.commit();
}

uint16_t Preferences::getPORT() {
  if (isPORTset() == false) {
    return DEFAULT_PORT;
  }
  return EEPROM.readShort(addresses::PORT);
}

bool Preferences::setID(const char *id_name) {
  char c = 0;
  Serial.print("SAVEING TO EEPROM: ");
  for (uint8_t i = 0; i < 16; i++) {
    c = id_name[i];
    
    if (c < 0x20 || c > 0x7e) {
      c = 0;  // A8 = ¿
    }
    Serial.print(c);
    EEPROM.writeChar(addresses::ID + i, c);

    if (c == 0 || c == ' '){
      Serial.println("END OF STRING");
      break;
    }
  }
  //15 is last char in EEPROM
  EEPROM.writeByte(addresses::ID + 15, '\0');
  setFlag(flag::id);
  return EEPROM.commit();
}

uint8_t Preferences::getID(char* id) {
  
  if (isIDset() == false) {
    Serial.println("ID is not set");
    // id[0] = 'i';
    // id[1] = 'd';
    // IPAddress addr = IPAddress();
    // this->getCONTROLLERIP(addr);   
    // itoa(addr[3], &id[2], 3);

    // for (uint8_t i = 3; i < 15; i++) {
    //   id[i] = ' ';
    // }
    char idnotset[] = "idNOTset";
    strncpy(id, idnotset, 8);
    return 9;
  }
  
  uint8_t len = 0;
  Serial.println("READING FORM EEPROM: ");

  for (uint8_t i = 0; i < 15; i++) {
    id[i] = EEPROM.readByte(addresses::ID + i);
    Serial.print(id[i]);
    len++;
    if (id[i] == 0 || id[i] == ' '){
      Serial.println("\nEND OF STRING");
      break;
    }
  }

  //id[15] = 0;
  trim(id);
  return len;
}

// PID regulator

void Preferences::getMASK(IPAddress &ipaddr) {
  if (isControllerIPset() == false) {
    ipaddr = IPAddress(255,255,255,0);
    return;
  }
  ipaddr = IPAddress(255,255,255,0);

  //convertNetMask(EEPROM.readByte(addresses::CONTROLLER_MASK), ipaddr);
}

bool Preferences::setMASK(IPAddress mask) {
uint8_t prefix = convertNetMask(mask);    
EEPROM.writeByte(addresses::CONTROLLER_MASK, prefix);
setFlag(flag::controllerip);
return EEPROM.commit();
}
bool Preferences::setMASKprefix(uint8_t mask) {  
EEPROM.writeByte(addresses::CONTROLLER_MASK, mask);
setFlag(flag::controllerip);
return EEPROM.commit();
}

float Preferences::getP(){
return getPID(addresses::P_reg);
}
bool Preferences::setP(float val){
setFlag(flag::pid);
return setPID(addresses::P_reg, val);
}

float Preferences::getI(){
return getPID(addresses::I_reg);
}
bool Preferences::setI(float val){
return setPID(addresses::I_reg, val);
}

float Preferences::getD(){
return getPID(addresses::D_reg);
}
bool Preferences::setD(float val){
return setPID(addresses::D_reg, val);
}

float Preferences::getA(){
return getPID(addresses::ALPHA_reg);
}
bool Preferences::setA(float val){
return setPID(addresses::ALPHA_reg, val);
}

uint16_t Preferences::getDelay(){
return EEPROM.readUShort(addresses::DELAY_reg);
}

bool Preferences::setDelay(uint16_t val){
EEPROM.writeUShort(addresses::DELAY_reg, val);
return EEPROM.commit();
}

void Preferences::setFlag(flag mask) {
mem_flags = mem_flags | mask;
writeFlags();
}

void Preferences::readFlags() {
mem_flags = EEPROM.readByte(FLAGS);
}

void Preferences::writeFlags() {
EEPROM.writeByte(FLAGS, mem_flags);
}

void Preferences::erasureAll() {
for (int i = 0; i < eeprom_size; i++) {
  EEPROM.writeByte(i, 0x00);
}
EEPROM.commit();

readFlags();
}

void Preferences::getIP(addresses memory_address, IPAddress &ipaddr) {
  uint8_t ip[4] = {0,0,0,0};
  if (memory_address == addresses::SERVER_IP && isServerIPset() == false){
    //IPAddress returnIP(0, 0, 0, 0);
    ipaddr = IPAddress(ip);
    return;
  } else if ((memory_address == addresses::CONTROLLER_IP || memory_address == addresses::CONTROLLER_GW) && isControllerIPset() == false){
    //IPAddress returnIP(0, 0, 0, 0);
    ipaddr = IPAddress(ip);
    return;
  }

  //uint8_t ip[4];
  for (uint8_t i = 0; i < 4; i++) {
    ip[i] = EEPROM.readByte(memory_address + i);
  };

  //IPAddress returnIP(ip[0], ip[1], ip[2], ip[3]);
  //return ip;
  ipaddr = IPAddress(ip[0], ip[1], ip[2], ip[3]);
}

bool Preferences::setIP(addresses memory_address, IPAddress address) {
  for (uint8_t i = 0; i < 4; i++) {
    EEPROM.writeByte(memory_address + i, (uint8_t)address[i]);
  };
  setFlag(flag::serverip);
  return EEPROM.commit();
}

float Preferences::getPID(addresses reg){
  if (reg < addresses::P_reg || reg > addresses::ALPHA_reg){
    return 0;
  }
  float pid = EEPROM.readFloat(reg);
  if (pid != pid){ // if is NAN return 0;
    return 0;
  }
  return pid;
}

bool Preferences::setPID(addresses reg, float val){
if (reg < addresses::P_reg || reg > addresses::ALPHA_reg){
    return false;
  }
Serial.print("sedPID ");
Serial.println(val);
EEPROM.writeFloat(reg, val);
return EEPROM.commit();
}

