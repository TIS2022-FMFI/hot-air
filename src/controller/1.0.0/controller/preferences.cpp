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
//  │││└─────── NOT USE
//  ││└──────── NOT USE
//  │└───────── NOT USE
//  └────────── NOT USE
//********************************************************



#include <EEPROM.h>
#include <IPAddress.h>

#define EEPROM_SIZE 32
#define MEMORY_START 0
#define DEFAULT_PORT 4002

class Preferences {
  private:
    enum addresses{FLAGS = MEMORY_START + 0, SERVER_IP = MEMORY_START + 1, PORT = MEMORY_START + 5, ID = MEMORY_START + 16}; //todo zmenene
    enum flag{ip = 0x01, port = 0x02, id = 0x04};
    uint8_t flags;
     
  public:
    Preferences(){
      flags = 0;
      EEPROM.begin(EEPROM_SIZE);
      readFlags();
      if (flags == 0xFF){
        erasureAll();
      }
      //Serial.println("Preferences:");
      //Serial.print("ID: ");
      //Serial.write(getID(), 15);
    }

    bool isIPset(){
      return flags & flag::ip == flag::ip;
    }

    bool isPORTset(){
      return flags & flag::port == flag::port;
    }

    bool isIDset(){
      return flags & flag::id == flag::id;
    }

    
    bool setIP(IPAddress address){ 
      for (uint8_t i = 0; i < 4; i++){
        EEPROM.writeByte(addresses::SERVER_IP + i, (uint8_t)address[i]);
      };
      setFlag(flag::ip);
      return EEPROM.commit();
    }

    IPAddress getIP(){   
      if (isIPset() == false){
        IPAddress returnIP(127,0,0,1);
        return returnIP;
      }
      
      uint8_t ip[4];
      for (uint8_t i = 0; i < 4; i++){
        ip[i] = EEPROM.readByte(addresses::SERVER_IP + i);
      };
      
      IPAddress returnIP(ip[0],ip[1], ip[2], ip[3]);
      return returnIP; 
      
    }

    
    bool setPORT(uint16_t port){
      EEPROM.writeShort(addresses::PORT, port);
      setFlag(flag::port);
      return EEPROM.commit();
    }

    uint16_t getPORT(){
      if (isPORTset() == false){
        return DEFAULT_PORT;
      }
      return EEPROM.readShort(addresses::PORT);
    }

    bool setID(char id_name[]){
      char c = 0;
      
      for (uint8_t i = 0; i < 16; i++){
        c = id_name[i];
        if (c < 0x20 || c > 0x7e){
          c = ' '; // A8 = ¿
        }
        EEPROM.writeChar(addresses::ID + i, c);
      }

      setFlag(flag::id);
      return EEPROM.commit();
    }

    char* getID(){
      if (isIDset() == false){
        return "";
      }

      char id[15];
      for (uint8_t i = 0; i < 15; i++){
        id[i] = EEPROM.readChar(addresses::ID + i);
      }
      return id;
    }
    
  private:
    void setFlag(flag mask){
      flags = flags | mask;
      writeFlags();
    }
    
    void readFlags(){
      flags = EEPROM.readByte(FLAGS);
    }
  
    void writeFlags(){
      EEPROM.writeByte(FLAGS, flags);
    }

    void erasureAll(){
      for(int i = 0; i < EEPROM_SIZE; i++){
        EEPROM.writeByte(i, 0x00);
      }
      EEPROM.commit();

      readFlags();
    }
  
};
