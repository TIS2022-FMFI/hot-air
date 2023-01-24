#include <sys/_stdint.h>
#include "CommunicationHandler.h"
#include <IPAddress.h>

bool ServerCommunication::begin(AsyncUDP *_udp, AsyncClient *_tcp, Status *_status, Preferences *_memory){
  udp = _udp;
  tcp = _tcp;
  status = _status;
  memory = _memory;
  tcpInit();
  return true;
};

uint8_t ServerCommunication::sendTemperatureFlag(){
  // FLAGS
  //  000   │0  │0  │0  │01
  //        │   │   │   │
  //        │   │   │   └ staticky kod pre posielanie teploty
  //        │   │   └ IF 1 ERROR Temperature can not be read
  //        │   └  IF 1 ERROR Dac not found
  //        └ IF 1 In progress

  uint8_t flag = 1; // one, because we are sending temperature to the server
  #ifndef SIMULATION
    if (status->thermometer_connected == false){
      flag |= 0x04;    
    }
    if (status->dac_connected == false){
      flag |= 0x08;
    }
  #endif
  
  if (status->set_temperature > 0){
    flag |= 0x10;
  }
  

  return flag;     
};

void ServerCommunication::udpHandler(AsyncUDPPacket packet){
  Serial.print("New UDP packet from: ");
  Serial.println(packet.remoteIP());
  Serial.print("Data: ");
  Serial.write(packet.data(), packet.length());
  Serial.println(" Password: ");
  for (int i = 0; i < 5; i++){
    if (status->server_password[i] != packet.data()[i]){
      Serial.println("WRONG PASSWORD");
      return;
    }
    }
  Serial.println("OK");
  
  memory->setSERVERIP(packet.remoteIP());
  status->searching_server = false;
  status->server_find = true;
  status->connection_error = false;
}
        

bool ServerCommunication::startUdp(){
  //IPAddress updIP = IPAddress(0,0,0,0);
  
  if (udp->listen(memory->getPORT())){
      status->searching_server = true;

      udp->onPacket([this](AsyncUDPPacket packet){
        udpHandler(packet);
      });
    return true;
  }

  Serial.println("UDP fail");
  return false;
};

bool ServerCommunication::stopUdp(){
  //IPAddress updIP = IPAddress(0,0,0,0);
  if (udp->listen(0)){
    status->searching_server = false;
    return true;
  }
  return false;
};

void ServerCommunication::sendTemperature(){
  memset(status->data, 0x00, 11); // add padding

  union {
    float float_variable;
    byte temp_array[4];
  } u;
  
  u.float_variable = status->actual_temperature;
  memcpy(&status->data[11], u.temp_array, 4);
  status->data[15] = sendTemperatureFlag();

  tcp->write(status->data, 16, 0x00); //ASYNC_WRITE_FLAG_COPY 
};

void ServerCommunication::sendTemperatureTimeout(unsigned long millis, int refresh){
  if (millis_send_temperature > millis){
    millis_send_temperature = 0;    
  }

  if (millis - millis_send_temperature > refresh){
    sendTemperature();
    millis_send_temperature = millis;
  } 

}

void ServerCommunication::handleTemperature(uint8_t *buffer){
  uint16_t temp = ((((uint16_t)buffer[4]) << 8) | ((uint16_t)buffer[5]));
  uint8_t air_flow = buffer[6];

  // during development it was decided that time would not be needed for the controller
  // long time = 0;
  //     for (int i = 0; i < 8; i++){
  //       auto byteVal = ((buffer[14 - i]) << (8 * i));
  //       time |= byteVal;
  //     }

  if (temp > 0 && temp < 1000){
    status->set_temperature = temp;
  }

  if (air_flow > 0 && air_flow <= 100){
    status->set_airflow = air_flow;
  }

  Serial.printf("New settings:\nTemperature: %u\nAirFlow: %u\n",status->set_temperature, status->set_airflow);
}

void ServerCommunication::handleEmergency_stop(uint8_t *buffer){
  status->emergency_stop = true;
  Serial.println("\n\n\nEMERGENY STOP\n\n\n");
}

void ServerCommunication::handleEmergency_release(uint8_t *buffer){
  status->emergency_stop = false;
  Serial.println("\n\n\nEMERGENY RELEASE\n\n\n");
}

void ServerCommunication::handleConnection(){
  status->connected_server = true;
  status->connecting_server = false;
  status->connection_error = false;
  status->disconnected_time_out = false;

  // send to server identifier that I'm controller not a GUI
  static char data = (uint8_t)IM_CONTROLLER;
  tcp->write(&data, 1, 0x00);//ASYNC_WRITE_FLAG_COPY

  // send server my ID.
  
  static char controller_id[16];
  memory->trim(controller_id);
  memory->getID(controller_id);
  controller_id[15] = communication_flags::NEW_ID;
  tcp->write(controller_id, 16, 0x00);
    
  #ifdef _DEBUG
      Serial.println("\nTCP connected to server!");
  #endif
}

void ServerCommunication::handleDisconnect(){
  status->connected_server = false;
  status->connecting_server = false;
  status->server_find = false;
  status->set_temperature = 0;
  #ifdef _DEBUG
      Serial.println("\nTCP socket disconnected!");
  #endif
}

void ServerCommunication::handleError(){
  status->connected_server = false; 
  status->connecting_server = false;
  status->connection_error = true;
  status->server_find = false;
  status->set_temperature = 0;

  #ifdef _DEBUG
    Serial.println("\nTCP Connection ERROR");
  #endif
}

void ServerCommunication::tcpInit(){
  tcp->onConnect([this](void * ctx_ptr, AsyncClient * client) {
    handleConnection();
  },NULL);

  tcp->onData([this](void * ctx_ptr, AsyncClient * client, void * buf, size_t len) {
    uint8_t buffer[len + 1];
    uint8_t *b = (uint8_t *)buf;

    Serial.print("dlzka Buffer-a ");
    Serial.println(len);

    for (int i = 0; i < len; i++){
      buffer[i] = *((uint8_t*)(b + i));
    }

    #ifdef _DEBUG
      //make table of 3 row 
      //1. Index, 2. data in HEX, 3. data in DEC
      //write index
      Serial.print("INDEX: ");
      for (int i = 0; i < len; i++){
        Serial.printf("%5d",i);
      }
      
      //write in HEX
      Serial.print("\nHEX:   ");
      char hex_string[5];
      for (int i = 0; i < len; i++){
        sprintf(hex_string, "%X", buffer[i]);
        Serial.printf("%5s", hex_string);
        Serial.print("");
      }
      
      // write in DEC
      Serial.print("\nDEC:   ");
      for (int i = 0; i < len; i++){
        Serial.printf("%5d",buffer[i]);
      }
      Serial.println("");

      // write in CHAR
      Serial.print("\nDEC:   ");
      for (int i = 0; i < len; i++){
        Serial.printf("%5c",(char)buffer[i]);
      }
      Serial.println("");
    #endif

    if (len < 16){
      Serial.print("Dostal som data, dlzka: ");
      Serial.println(len);
      return;
    }

    if (buffer[15] == communication_flags::EMERGENCY_STOP){
      Serial.println("EMERGENCY STOP");
      handleEmergency_stop(buffer);
    }else if (buffer[15] == communication_flags::SET_TEMPERATURE){
      Serial.println("NEW TEMPERATURE");
      handleTemperature(buffer);
    }else if (buffer[15] == communication_flags::EMERGENCY_STOP_RELEASE){
      Serial.println("EMERGENCY STOP RELEASE");
      handleEmergency_release(buffer);
    }
  },NULL);

  tcp->onDisconnect([this](void * ctx_ptr, AsyncClient * client) {
    handleDisconnect();
  },NULL);

  tcp->onError([this](void * ctx_ptr, AsyncClient * client, int8_t error) {
    handleError();
  },NULL);

  tcp->onAck([this](void * ctx_ptr, AsyncClient * client, size_t len, uint32_t ms_delay) {
    #ifdef _DEBUG
      //Serial.printf("\nAcknowledged sending next %u bytes after %u ms\r\n", len, ms_delay);
    #endif
  },NULL);
}

void ServerCommunication::refresh(){
  if (status->connected_server == false){
    if (status->searching_server == false && status->connecting_server == false && status->connection_error == false){
      Serial.print("Connecting to server: ");
      //status->request_udp_listening = 0;
      stopUdp() ? Serial.println("UDP STOP OK") : Serial.println("UDP STOP ERROR"); 

      IPAddress server_IP = IPAddress();
      memory->getSERVERIP(server_IP);
      Serial.println(server_IP);
      
      tcp->connect(server_IP, memory->getPORT());

      status->connecting_server = true;
      status->searching_server = false;
    } else if (status->searching_server == false && status->connecting_server == false && status->connection_error == true){
      Serial.println("Start searching for a server");
      //status->request_udp_listening = 1;
      startUdp() ? Serial.println("UDP START OK") : Serial.println("UDP START ERROR"); 
      status->searching_server = true;
    }
  }
}
