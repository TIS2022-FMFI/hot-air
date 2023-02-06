#include <sys/_intsup.h>
#include "esp32-hal.h"
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
        

// bool ServerCommunication::startUdp(){
//   //IPAddress updIP = IPAddress(0,0,0,0);
  
//   if (){
//       status->searching_server = true;

//       // udp->onPacket([this](AsyncUDPPacket packet){
//       //   udpHandler(packet);
//       // });
//     return true;
//   }

//   Serial.println("UDP fail");
//   return false;
// };

// bool ServerCommunication::stopUdp(){
//   //IPAddress updIP = IPAddress(0,0,0,0);
//   if (udp->listen(0)){
//     status->searching_server = false;
//     return true;
//   }
//   return false;
// };

void ServerCommunication::sendTemperature(){
  uint8_t data[16];
  memset(data, 0x00, 11); // add padding

  union {
    float float_variable;
    byte temp_array[4];
  } u;
  
  u.float_variable = status->actual_temperature;
  memcpy(&data[11], u.temp_array, 4);
  data[15] = sendTemperatureFlag();

  udp->write(data, 16);
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

  if (temp >= 0 && temp <= 1000){
    status->set_temperature = temp;
  }

  if (air_flow >= 0 && air_flow <= 100){
    status->set_airflow = air_flow;
  }

  Serial.printf("New settings:\nTemperature: %u\nAirFlow: %u\n",status->set_temperature, status->set_airflow);
}

void ServerCommunication::handleEmergency_stop(uint8_t *buffer){
  status->emergency_stop = true;
  Serial.println("\n\n\nEMERGENY STOP\n\n\n");
  sendAck(buffer);
}

void ServerCommunication::handleEmergency_release(uint8_t *buffer){
  status->emergency_stop = false;
  Serial.println("\n\n\nEMERGENY RELEASE\n\n\n");
  sendAck(buffer);
}

void ServerCommunication::sendID(){
  // send server my ID.
  static uint8_t controller_id[16];
  memory->trim((char*)controller_id);
  memory->getID((char*)controller_id);
  controller_id[15] = communication_flags::NEW_ID;
  udp->write(controller_id, 16);
}

void ServerCommunication::handleConnection(){
  status->connected_server = true;
  status->connecting_server = false;
  status->connection_error = false;
  status->disconnected_time_out = false;
  
  #ifdef _DEBUG
    Serial.println("\nUDP connected to server!");
  #endif
}

void ServerCommunication::handleDisconnect(){
  status->connected_server = false;
  status->connecting_server = false;
  status->server_find = false;
  #ifdef _DEBUG
      Serial.println("\nUDP socket disconnected!");
  #endif
}

void ServerCommunication::handleError(){
  status->connected_server = false; 
  status->connecting_server = false;
  status->connection_error = true;
  status->server_find = false;
  #ifdef _DEBUG
    Serial.println("\nUDP Connection ERROR");
  #endif
}

void ServerCommunication::sendAck(uint8_t *buffer){
  buffer[15] &= communication_flags::ACK;

  udp->write(buffer, 16);
}

void ServerCommunication::tcpInit(){
  // tcp->onConnect([this](void * ctx_ptr, AsyncClient * client) {
  //   handleConnection();
  // },NULL);

  udp->listen(memory->getPORT());


  udp->onPacket([this](AsyncUDPPacket packet) {
    if (status->searching_server == true){
      udpHandler(packet);
      return;
    }

    // todo zbavit sa pointera na b, a prekopirovavanie dat.
    uint16_t len = packet.length();
    uint8_t buffer[len + 1];
    uint8_t *b = (uint8_t *)packet.data();
    Serial.print("dlzka Buffer-a ");
    Serial.println(len);

    for (int i = 0; i < len; i++){
      buffer[i] = *((uint8_t*)(b + i));
    }

    if (status->connecting_server == true && buffer[15] == (communication_flags::NEW_ID & communication_flags::ACK)){
      handleConnection();
    }


    #ifdef _DEBUG
     printRawData(buffer, len);
    #endif

    if (len == 1 && buffer[0] == 0xA8){ // 0xA8 = ¿
      status->ack_server_time_out = millis();     
    }


    if (len < 16){
      Serial.print("Dostal som data, dlzka: ");
      Serial.println(len);
      return;
    }

    if (buffer[15] == communication_flags::EMERGENCY_STOP){
      //Serial.println("EMERGENCY STOP");
      handleEmergency_stop(buffer);
    }else if (buffer[15] == communication_flags::SET_TEMPERATURE){
      //Serial.println("NEW TEMPERATURE");
      handleTemperature(buffer);
    }else if (buffer[15] == communication_flags::EMERGENCY_STOP_RELEASE){
      //Serial.println("EMERGENCY STOP RELEASE");
      handleEmergency_release(buffer);
    }
  });

  // tcp->onDisconnect([this](void * ctx_ptr, AsyncClient * client) {
  //   handleDisconnect();
  // },NULL);

  // tcp->onError([this](void * ctx_ptr, AsyncClient * client, int8_t error) {
  //   handleError();
  // },NULL);

  // tcp->onAck([this](void * ctx_ptr, AsyncClient * client, size_t len, uint32_t ms_delay) {
  //   #ifdef _DEBUG
  //     //Serial.printf("\nAcknowledged sending next %u bytes after %u ms\r\n", len, ms_delay);
  //   #endif
  // },NULL);
}

void ServerCommunication::refresh(){

  if (status->connecting_server == true && millis() - status->connection_time_out_millis >= 1000){
    handleError(); // If I don't get ACK on ID msg from server.
  }

  if (status->connected_server == false && status->connecting_server == false){ 
    if (status->searching_server == false  && status->connection_error == false){
     
      //status->request_udp_listening = 0;
      Serial.println("UDP-stop");
      // stopUdp();
      status->searching_server = false;

      IPAddress server_IP = IPAddress();
      memory->getSERVERIP(server_IP);

      //todo add if(udp.connect(...
      Serial.print("Connecting to server: ");
      Serial.println(server_IP);

      if (udp->connect(server_IP, memory->getPORT())){
        status->connecting_server = true;
        sendID();
        status->connection_time_out_millis = millis();
        //handleConnection();
      }

      status->connecting_server = true;
      status->searching_server = false;

    } else if (status->searching_server == false && status->connection_error == true){
      Serial.println("Start searching for a server");
      //status->request_udp_listening = 1;
      // startUdp();
      status->searching_server = true;
    }
  } else if (status->connected_server == true){
    
    if (status->id_has_change == true){
      sendID();
      status->id_has_change = true;
    }


    if (millis() - status->ack_server_time_out >= TIMEOUT_UDP_CONNECTION){
     // handleDisconnect();
    }
  }
}

void ServerCommunication::printRawData(uint8_t *buffer, uint16_t len){
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

  // write in CHAR
  Serial.print("\nDEC:   ");
  for (int i = 0; i < len; i++){
    Serial.printf("%5c",(char)buffer[i]);
  }
  Serial.println("");
}
