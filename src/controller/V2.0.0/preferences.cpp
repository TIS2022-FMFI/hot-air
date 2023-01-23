#include <sys/_stdint.h>
#include "IPAddress.h"
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

void Preferences::trim(char *str) {
  int i, begin, end;
  for (begin = 0; begin < strlen(str) && str[begin] == ' '; begin++);

  for (end = strlen(str) - 1; end >= 0 && str[end] == ' '; end--);

  for (i = begin; i <= end; i++) {
      str[i - begin] = str[i];
  }
  str[end - begin + 1] = '\0';
}

Preferences::Preferences() {
}

bool Preferences::begin(uint16_t size) {
eeprom_size = size;
flags = 0;

if (EEPROM.begin(size) == false) {
  Serial.println("EEPROM load error.\n Rebooting...");
  //ESP.restart();
  return false; 
} else {
  Serial.println("EEPROM load OK");
}

readFlags();
if (flags == 0xFF) {
  Serial.println("deleting EEPROM");
  erasureAll();
}

#ifdef _DEBUG
  // IPAddress ip = IPAddress(10,1,1,105);
  // IPAddress GW = IPAddress(10,1,1,1);
  // setMASK(24);
  // char idcko[] = "id05           ";
  // setID(idcko);
  // setCONTROLLERIP(ip, GW);
  // setPORT(4002);

  IPAddress ip_addres = IPAddress();  
  Serial.println("Preferences BEGIN in debug mode.");
  Serial.print("FLAGS: ");
  Serial.printf("%X\n", flags);

  Serial.print("Server IP: ");
  this->getSERVERIP(ip_addres);
  Serial.println(ip_addres);

  Serial.print("PORT: ");
  Serial.println(this->getPORT());

  Serial.print("ID: ");
  char id[15];
  Serial.write(this->getID(id), 15);
  Serial.println();

  Serial.print("Controller IP: ");
  this->getCONTROLLERIP(ip_addres);
  Serial.println(ip_addres);

  Serial.print("Controller GW: ");
  this->getCONTROLLERGW(ip_addres);
  Serial.println(ip_addres);

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
  return (flags & flag::serverip) == flag::serverip;
}

bool Preferences::isPORTset() {
return (flags & flag::port) == flag::port;
}

bool Preferences::isIDset() {
return (flags & flag::id) == flag::id;
}

bool Preferences::isControllerIPset() {
return (flags & flag::controllerip) == flag::controllerip;
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

bool Preferences::setID(char id_name[]) {
char c = 0;

for (uint8_t i = 0; i < 16; i++) {
  c = id_name[i];
  if (c < 0x20 || c > 0x7e) {
    c = ' ';  // A8 = ¿
  }
  EEPROM.writeChar(addresses::ID + i, c);
}

setFlag(flag::id);
return EEPROM.commit();
}

char* Preferences::getID(char* id) {
if (isIDset() == false) {
  Serial.println("ID is not set");
  id[0] = 'i';
  id[1] = 'd';
  IPAddress addr = IPAddress();
  this->getCONTROLLERIP(addr);   
  itoa(addr[3], &id[2], 10);

  for (uint8_t i = 3; i < 15; i++) {
    id[i] = ' ';
  }
  trim(id);

  return id;
}
//char* id[15];
for (uint8_t i = 0; i < 15; i++) {

  id[i] = EEPROM.readByte(addresses::ID + i);
}
return id;
}

// PID regulator

void Preferences::getMASK(IPAddress &ipaddr) {
if (isControllerIPset() == false) {
  ipaddr = IPAddress(255,255,255,0);
  return;
}
convertNetMask(EEPROM.readByte(addresses::CONTROLLER_MASK), ipaddr);
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
flags = flags | mask;
writeFlags();
}

void Preferences::readFlags() {
flags = EEPROM.readByte(FLAGS);
}

void Preferences::writeFlags() {
EEPROM.writeByte(FLAGS, flags);
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
ipaddr = IPAddress(ip);
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

EEPROM.writeFloat(reg, val);
return EEPROM.commit();
}

