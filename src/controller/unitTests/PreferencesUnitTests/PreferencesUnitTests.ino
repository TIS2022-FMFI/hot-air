

#include "preferences.cpp"
#include <IPAddress.h>

#define ON_ONE_LINE 0x10
//#define EEPROM_SIZE 64

Preferences pr;

void setup() {
   Serial.begin(115200);
   
   pr = Preferences();
   Serial.println("Preference Begin");
    
   IPAddress ipserver(192,13,152,2);
   Serial.print("IP ");
   Serial.print(ipserver);
   Serial.println(" init");
   uint16_t port = 4002;
   Serial.print("PORT ");
   Serial.print(port);
   Serial.println(" init");

   
   //{'','','','','','','','','','','','','','',''};
   char id_name_full[16] = {'A','H','O','J','c','n','t','r','o','l','l','e','r','0','1'};
   char id_name_empty[16] = {};
   char id_name_half[16] = {'A','H','O','J'};
   char id_name_space[16] = {'A','H','O','J',' ','C','O','N','T','R','O','L','L','E','R'};
   char id_name_IP[16] = {'1','9','2','.','1','6','8','.','0','0','1','.','1','2','3'};
   char id_name_special_simbol[16] = {'á','ľ','š','ť','ž','ý','á','í','é','ú','ä','§','ô','ň','¶'};
   Serial.println("id names init");
   
   // IP test

   testIP(ipserver);
   testPORT(port);

   // ID test
   //testID(id_name_full);
   //testID(id_name_empty);
   testID(id_name_half);
   //testID(id_name_space);
   //testID(id_name_IP);
    //testID(id_name_special_simbol);

   EEPROMmemoryMap();

   
}

void loop() {
 

}


void testIP(IPAddress input){
   if (pr.setIP(input) == false){
      Serial.print("setIP ERROR");
   }
   
   Serial.print("Testing. setIP("); 
   Serial.print(input);
   Serial.print(") getIP: ");

   IPAddress eeprom_ip = pr.getIP();
   
   for (int i = 0; i < 4; i++){
    if (input[i] != eeprom_ip[i]){
      Serial.print("ERROR Input: ");
      Serial.print(input);
      Serial.print(" Get:");
      Serial.println(eeprom_ip);
      return;
    }
    
   }
   Serial.print("OK Input: ");
   Serial.print(input);
   Serial.print(" Get:");
   Serial.println(eeprom_ip);
   //Serial.println((pr.getIP() == input) ? "OK" : "ERROR");
  
}

void testPORT(uint16_t input){
   if (pr.setPORT(input) == false){
      Serial.print("setPORT ERROR");
   }
   
   Serial.print("Testing. setPORT("); 
   Serial.print(input);
   Serial.print(") getPORT: ");
   Serial.println((pr.getPORT() == input) ? "OK" : "ERROR");
  
}

void testID(char *input){
   if (pr.setID(input) == false){
      Serial.print("setID ERROR");
   }
   
   Serial.print("Testing. setID("); 
   Serial.print(input);
   Serial.print(") getID = ");
   Serial.println(pr.getID());
   //Serial.println((strcmp(pr.getID(), input) == 0) ? "OK" : "ERROR");
  
}

void EEPROMmemoryMap(){
  uint16_t index = 0;
  char hex_string[5];
  char ascii[ON_ONE_LINE];
  uint8_t eeprom_byte;
  uint8_t modulo;

  EEPROM.begin(EEPROM_SIZE);

  // Print header
  Serial.print("Address |");
  for (uint8_t i = 0; i < ON_ONE_LINE; i++){
        sprintf(hex_string, "%X", i);
    Serial.print(hex_string);
    Serial.print(" |");
  }
  Serial.print(F("ASCII "));

  
  while (index < EEPROM_SIZE){ // for main iteration
    modulo = index % ON_ONE_LINE;
    
    if (modulo == 0){ // every 16 bytes put on next line
      Serial.println(ascii);
      
      sprintf(hex_string, "%X", index);
      Serial.print(hex_string);

      // printing space after address
      for (uint8_t i = 0; i <= 9 - strlen(hex_string) - 2; i++){
        Serial.print(" ");
      }
      Serial.print("|");
    }
    
    eeprom_byte = EEPROM.readByte(index);
    sprintf(hex_string, "%X", eeprom_byte);
    Serial.print(hex_string);
    Serial.print("|");

    if (eeprom_byte > 0x20 && eeprom_byte > 0x7e ){
      ascii[modulo] = '.';
    } else {
      ascii[modulo] = eeprom_byte;
    }
    index++;
  }
}
