/****************************************************************************************************************************
  TODO

  test 01 = extremne pomale ale ide
  test 02 = rychlejsie ale o 8 stupnov prestreli a dlho mu trva pokial sa to stabilizuje
  test 03 = na zaciatku to islo extremne rychlo ale potom to spomalilo a islo to opatrne hore. MEGA super, funguje to aj pri 280 teplote
  ---------------------------------
  test  |01     |02     | 03      |
  ---------------------------------
  air   |5 000  |5 000  | 10 000  |
  P     |4      |4      | 40-60   | 
  D     |10     |5      | 5       |
  I     |10     |10     | 40-60   |
  temp  |180    |30-400 | 80-180  |
  Alpha |1 000  |1 000  | 1 000   |
  Delay |250    |250    | 250     |
  ---------------------------------
 *****************************************************************************************************************************/
#include "defines.h"

// Libs
#include <AsyncTCP.h>
#include <AsyncUDP.h>

//#include "ESPAsyncUDP.h"

#include <AsyncWebServer_WT32_ETH01.h>

#include <max6675.h> // temperature
#include <DFRobot_GP8403.h> // DAC controller
#include <EEPROM.h> // eeprom lib
#include "web_page.h" // web page
#include "web_server_handlers.h"
#include "preferences.cpp"
  
AsyncWebServer server(80); // init webserver on port 80
DFRobot_GP8403 dac(&Wire,0x5F);
AsyncUDP udp;
AsyncClient tcpClient;

// flags for TCP
enum controller_flags{SEND_TEMP = 0x01, ERROR_THERMO = 0x04, ERROR_DAC = 0x08, HEATING = 0x10};
enum communication_flags{IM_CONTROLLER = 0x80, EMERGENCY_STOP = 0x80, NEW_ID = 0x02, SET_TEMPERATURE = 0x01};


Preferences memory;

volatile float temperature;
volatile float last_send_temperature = 0;

// thermocouple
int thermoDO = 14; // SO of  MAX6675 module to D14
int thermoCS = 12; // CS of MAX6675 module to D12
int thermoCLK = 15; // SCK of MAX6675 module to D15
// Init thermocouple
MAX6675 thermocouple(thermoCLK, thermoCS, thermoDO);

// Server listening on port:
IPAddress remote_serverIP(0,0,0,0); //IP for burnee server
uint16_t SERVER_LISTENING_PORT = 4002;
bool remote_server_conected = false;
bool remote_server_connection_error = false;
bool trying_connect_TCP = false;
bool listening_for_UDP = false;

// PID regulator
volatile float p_reg = 40;
volatile float d_reg = 5;
volatile float i_reg = 40;
volatile int temp_reg = 0;
volatile float alpha = 1000;
volatile int delaj = 250;
volatile float current_power = 0;
volatile float suma = 0;


void handleRoot(AsyncWebServerRequest * request)
{
  String html = F("Temperature is: ");

  //html += String(BOARD_NAME); 
  html += String(temperature);
  html += "C";
  
  request->send(200, F("text/plain"), html);
  Serial.println(temperature);
  
}

//**************************************************************************
//
//                                 SETUP
//
//**************************************************************************
  
void setup()
{   
  // Open serial communications and wait for port to open:
  Serial.begin(115200);
  #ifdef _DEBUG
    Serial.println("!!!!!!!!!!!!!!!!!!!!!!\nDEBUG IS ON, BYPASS ALL SAFETY\n!!!!!!!!!!!!!!!!!!!!!!");  
  #endif

  //**************************************************************************
  //                                 DAC
  //**************************************************************************
  // TODO test if is connected DONE
  Wire.begin(2, 4); // SDA, SCL
  //delay(50);

  int address = 120;
  
  #ifndef _DEBUG
      Serial.print("Inicializujem I2C na adrese ");
      Serial.println(address);
      
      uint8_t error = 1;
      
      while (error != 0){
        
        Wire.beginTransmission(address);
        error = Wire.endTransmission();
        Serial.print("*");
        delay(1000);
      }
      Serial.println();
  #endif
  
  if (!dac.begin()) {
    Serial.println("dac init err");
  }

  //Set the output range as 0-10V
  dac.setDACOutRange(dac.eOutputRange10V);
  //delay(50);
  Serial.print("\nDac is set up");

   //**************************************************************************
  //                                 PREFERENCES
  //**************************************************************************
    memory = Preferences();
    Serial.println("Preferences");
    Serial.print("ID: ");
    //Serial.write(memory.getID(), 15);
    Serial.println("");
    
  //**************************************************************************
  //                                 ETH
  //**************************************************************************
  
  //ETH.setHostname("Controller"); //TODO with EPROM name
  WT32_ETH01_onEvent();

  //bool begin(uint8_t phy_addr=ETH_PHY_ADDR, int power=ETH_PHY_POWER, int mdc=ETH_PHY_MDC, int mdio=ETH_PHY_MDIO, 
  //           eth_phy_type_t type=ETH_PHY_TYPE, eth_clock_mode_t clk_mode=ETH_CLK_MODE);
  //ETH.begin(ETH_PHY_ADDR, ETH_PHY_POWER, ETH_PHY_MDC, ETH_PHY_MDIO, ETH_PHY_TYPE, ETH_CLK_MODE);
  ETH.begin(ETH_PHY_ADDR, ETH_PHY_POWER);

  #ifndef _DHCP
    ETH.config(myIP, myGW, mySN, myDNS);
  #endif

  Serial.print("\nBoge Heater controller on " + String(ARDUINO_BOARD));
  Serial.println(" with " + String(SHIELD_TYPE));
  
  WT32_ETH01_waitForConnect();
  
  Serial.print(F(" @ IP : "));
  Serial.println(ETH.localIP()); 

  //**************************************************************************
  //                           WEB SERVER LISTENING
  //**************************************************************************
  
  server.on("/", HTTP_GET, [](AsyncWebServerRequest * request) 
  {
    handleRoot(request);
  });

  server.on("/control", HTTP_GET, [](AsyncWebServerRequest * request)
  {
    request->send(200, F("text/html"), controlwebpage);
  });
  
  server.on("/setdac", HTTP_GET, [](AsyncWebServerRequest * request)
  {
    setDAC(request);
  });
  
  server.on("/t", HTTP_GET, [](AsyncWebServerRequest * request)
  {
    handleTemperature(request);
  });

  // set PD regulator
  server.on("/setpd", HTTP_GET, [](AsyncWebServerRequest * request)
  {
    setpd(request);
  });
  
  server.onNotFound(handleNotFound);
  server.begin();
  
  //**************************************************************************
  //                              DISCOVERY //todo
  //**************************************************************************
//tcpClient.connect(IPAddress(192,168,0,22), (uint16_t)4002);
//  if (memory.isIPset() == false){
//    
//    Serial.println("waiting to be discovered by the server");
//    udpListen(memory.getPORT());
//    
//    while(memory.isIPset() == false){
//      Serial.print("*");
//      delay(10);
//    }
//    Serial.println("");
//    udpListen(0);
//  }
//  Serial.print("connecting to server: ");
//  Serial.println(memory.getIP());
//  
//  tcpClient.connect(memory.getIP(), memory.getPORT());
//  
//  while(remote_server_conected == false){
//    if (remote_server_connection_error == true){
//      Serial.println("\nConnection Error");
//    }
//    Serial.print("*");
//    delay(10);
//    
//  }
//  Serial.println("\nConnected");
  
  
  //**************************************************************************
  //                                 UDP
  //**************************************************************************
//  // waiting for UDP packet from server
//  if (udp.listen(SERVER_LISTENING_PORT)) //for server
//  //if (udp.connect(IPAddress(192,168,0,255), SERVER_LISTENING_PORT)) // for client
//    {
//      
//      Serial.print("UDP Listening on IP: ");
//      Serial.println(ETH.localIP());
//      
//      udp.onPacket([](AsyncUDPPacket packet) 
//      {
//        
//        Serial.print("UDP Packet Type: ");
//        Serial.print(packet.isBroadcast() ? "Broadcast" : packet.isMulticast() ? "Multicast" : "Unicast");
//        Serial.print(", From: ");
//        Serial.print(packet.remoteIP());
//        Serial.print(":");
//        Serial.print(packet.remotePort());
//        Serial.print(", To: ");
//        Serial.print(packet.localIP());
//        Serial.print(":");
//        Serial.print(packet.localPort());
//        Serial.print(", Length: ");
//        Serial.print(packet.length());
//        Serial.print(", Data: ");
//        Serial.write(packet.data(), packet.length());
//
//        uint8_t password[] = {0x41, 0x48, 0x4f, 0x4a, 0x2b};
//        
//         for (int i = 0; i < 5; i++){
//          if (password[i] != packet.data()[i]){
//            Serial.println("PSWD WRONG");
//            return;
//          }
//         }
//         Serial.println("PSWD OK");
//
//        
//        
//        remote_serverIP = packet.remoteIP();
//        remote_serverIP_flag = true;
//
//        //git  #67
//        EEPROM.writeByte(0, remote_serverIP[0]);
//        EEPROM.writeByte(1, remote_serverIP[1]);
//        EEPROM.writeByte(2, remote_serverIP[2]);
//        EEPROM.writeByte(3, remote_serverIP[3]);
//       
//        //reply to the client
//
//        //String data = "Got your UDP. My IP is: ";
//        //data += ETH.localIP();
//        //data += ". Starting TCP";
//        //packet.printf(data, packet.length()); // or data.length()
//      });
//    }
  //**************************************************************************
  //                                 TCP
  //**************************************************************************
    
  tcpClient.onConnect([](void * ctx_ptr, AsyncClient * client) {
    Serial.println("\n\nonConnect successful! sending data...");
    remote_server_conected = true;
    remote_server_connection_error = false;
    trying_connect_TCP = false;
    
    char data[] = {(uint8_t)IM_CONTROLLER}; // send to server that Im controller not a GUI
    tcpClient.add(data, sizeof(data), ASYNC_WRITE_FLAG_COPY);
     
    client->send();

    char data_id[15] = {'i','d','1','\x00','\x00','\x00','\x00','\x00','\x00','\x00','\x00','\x00','\x00','\x00','\x00'};; //memory.getID();
    //if (memory.isIDset() == false){
    //  data_id = {'n','o','n','s','e','t','i','d','1','2','3','4','5','6','7'};
    //}
    
    //char *c = memory.getID();
    /*
    for (int i = 0; i < 15; i++){
      data_id[i] = c[i];
    }
    */
    char buffer[16];
    buffer[15] = communication_flags::NEW_ID;
    for (uint8_t i = 0; i < 15; i++){
      buffer[i] = data_id[i];
    }
    
    tcpClient.add(buffer, sizeof(buffer), ASYNC_WRITE_FLAG_COPY);
    
    
    
  },
  NULL
  );

  
  // Callback on data ready to be processed - MUST BE CONSUMED AT ONCE or will be discarded
  tcpClient.onData([](void * ctx_ptr, AsyncClient * client, void * buf, size_t len) {

    Serial.printf("\n\nonData received data (%u bytes), raw buffer follows:\r\n", len);
    //Serial.write((const uint8_t *)buf, len);
    
//    if (len > 13){
//      return;
//    }
    
    
    uint8_t buffer[len + 1];
    uint8_t *b = (uint8_t *)buf;
    Serial.write(b, len);

    uint8_t offset = 0;    
    uint8_t remain = len;
    uint8_t index = 0;
    
    while(remain > 0){
      buffer[index] = *(uint8_t*)(b + offset);
      index++;
      remain--;
      offset++;
    }

    // debug only
    Serial.println("Buffer in HEX: ");
    for (int i = 0; i < len; i++){
      Serial.printf("%5d",i);
    }
    Serial.println("");
    
    char hex_string[5];
    for (int i = 0; i < len; i++){
      sprintf(hex_string, "%X", buffer[i]);
      Serial.printf("%5s", hex_string);
      Serial.print("");
    }
    
    Serial.println("");
    for (int i = 0; i < len; i++){
      Serial.printf("%5d",buffer[i]); // indexing
    }
    Serial.println("");
    // end debug

    
//    long long binary_data = 0;
//    for (int i = 0; i < len - 1; i++)
//    {
//        auto byteVal = ((buffer[i]) << (8 * i));
//        //auto byteVal = ((*b + offset) << (8 * i));
//        binary_data |= byteVal;
//        offset += 8;
//    }
//    Serial.print("Buffer in long long: ");
//    Serial.println(binary_data);

    
    // Temperature
    if (buffer[15] == communication_flags::SET_TEMPERATURE){
      uint16_t temp = ((((uint16_t)buffer[4]) << 8) | ((uint16_t)buffer[5]));
      uint8_t air_flow = buffer[6];
      long time = 0;
      for (int i = 0; i < 8; i++){
        auto byteVal = ((buffer[14 - i]) << (8 * i));
        time |= byteVal;
      }

      if (temp > 0 && temp < 600){
        temp_reg = temp;
      }

      if (air_flow > 0 && air_flow <= 100){
        dac.outputSquare(air_flow * 100, 10000, 0, 100, 1);
        
      }
      
      Serial.printf("New settings:\nTime: %d\nAirFlow: %u\nTemperature: %u\n", time,air_flow,temp);

      // new ID
    } else if (buffer[15] == communication_flags::NEW_ID){
      char new_id[15];
      for (uint8_t i = 0; i < 15; i++){
        new_id[i] = (char)buffer[i];
      }
      
      memory.setID(new_id);
      Serial.print("Got new ID: ");
      Serial.write(new_id, 15);
      Serial.println("");

      // emergecy stop
    }else if (buffer[15] == communication_flags::EMERGENCY_STOP){
      temp_reg = 0;
      Serial.println("\n\n\nEMERGENY STOP\n\n\n");
    }
    
    
//  too je zle  
//    if (binary_data & 0x01 == 0x01){ // recive new temperature setings
//      long time = binary_data & 0xFFFFFFFFFFFFFFFF00;
//      uint8_t air_flow = binary_data & 0xFF000000000000000000;
//      uint16_t temperatur = binary_data & 0x000000FFFF00000000000000000000;
//      if (temperature > 0 && temperature < 600){
//        //temp_reg = temperature;
//      }
//      Serial.printf("New settings:\nTime: %d\nAirFlow: %u\nTemperature: %u\n", time,air_flow,temperature);
//    }

    
    //temp_reg = binary_data & 

  },
  NULL 
  );

  // Callback on written data being acknowledged as sent. If the data written so far fully covers
  // a buffer added WITHOUT the ASYNC_WRITE_FLAG_COPY flag, now is the first safe moment at
  // which such a buffer area may be discarded or reused.
  tcpClient.onAck([](void * ctx_ptr, AsyncClient * client, size_t len, uint32_t ms_delay) {
    
    Serial.printf("\n\nonAck acknowledged sending next %u bytes after %u ms\r\n", len, ms_delay);

  },
  NULL
  );

  // Callback on socket disconnect, called:
  // - on any socket close event (local or remote) after being connected
  // - on failure to connect, right after the onError callback
  tcpClient.onDisconnect([](void * ctx_ptr, AsyncClient * client) {
    remote_server_conected = false;
    trying_connect_TCP = false;
    Serial.println("\n\nonDisconnect socket disconnected!");

  },
  NULL  // <-- Pointer to application data, accessible within callback through ctx_ptr
  );

  // Callback on error event
  tcpClient.onError([](void * ctx_ptr, AsyncClient * client, int8_t error) {
    remote_server_conected = false;
    remote_server_connection_error = true;
    trying_connect_TCP = false;
    Serial.printf("\n\nonError socket reported error %d\r\n", error);

  },
  NULL  // <-- Pointer to application data, accessible within callback through ctx_ptr
  );

}

//**************************************************************************
//
//                                 SETUP END
//
//**************************************************************************

void udpListen(uint16_t port){
  // waiting for UDP packet from server
  if (udp.listen(port)) //for server
  //if (udp.connect(IPAddress(192,168,0,255), SERVER_LISTENING_PORT)) // for client
    {
      
      Serial.print("UDP Listening on IP: ");
      Serial.println(ETH.localIP());
      
      udp.onPacket([](AsyncUDPPacket packet) 
      {
        
        Serial.print("UDP Packet Type: ");
        Serial.print(packet.isBroadcast() ? "Broadcast" : packet.isMulticast() ? "Multicast" : "Unicast");
        Serial.print(", From: ");
        Serial.print(packet.remoteIP());
        Serial.print(":");
        Serial.print(packet.remotePort());
        Serial.print(", To: ");
        Serial.print(packet.localIP());
        Serial.print(":");
        Serial.print(packet.localPort());
        Serial.print(", Length: ");
        Serial.print(packet.length());
        Serial.print(", Data: ");
        Serial.write(packet.data(), packet.length());

        uint8_t password[] = {0x41, 0x48, 0x4f, 0x4a, 0x2b};
        
         for (int i = 0; i < 5; i++){
          if (password[i] != packet.data()[i]){
            Serial.println("PSWD WRONG");
            return;
          }
         }
         Serial.println("PSWD OK");

        
        memory.setIP(packet.remoteIP());
        
        remote_serverIP = packet.remoteIP();


        //flags
        remote_server_connection_error = false;
        listening_for_UDP = false;
        
        //reply to the client

        //String data = "Got your UDP. My IP is: ";
        //data += ETH.localIP();
        //data += ". Starting TCP";
        //packet.printf(data, packet.length()); // or data.length()
      });
    }
}



void pd_step()
{
    static float last_err = 0;
    static unsigned long last_time_measured_temp = 0;
    static float output_amount = 0;
    

    unsigned long tm = millis();
    if (tm < last_time_measured_temp) last_time_measured_temp = 0;
    
    if (tm - last_time_measured_temp > 250)
    {
      temperature = thermocouple.readCelsius();
      last_time_measured_temp = tm;      
    }    
    
    delay(delaj);
    if (temp_reg == 0) {
      return;
    }
        
    float p_err = temp_reg - temperature;
    float d_err = p_err - last_err;
    last_err = p_err;
    //Serial.print("err: ");
    Serial.print(p_err);
    Serial.print(" ");
    Serial.print(d_err);
    Serial.print(" ");

    suma += p_err * i_reg;
    float delta_amount = 1000 * (suma + p_reg * p_err + d_reg * d_err);
    current_power = delta_amount;
    if (current_power > 10000) current_power = 10000;
    if (current_power < 0) current_power = 0;

    output_amount = (1 - alpha) * output_amount + alpha * current_power;
   
    
    Serial.print((int)current_power);
    Serial.print(" ");
    Serial.print((int)delta_amount);
    Serial.print(" ");
    Serial.println((int)output_amount);

    dac.outputSquare((int)output_amount, 10000, 0, 100, 0);
    
    
}

void float2Bytes(float val,byte* bytes_array){
  // Create union of shared memory space
  union {
    float float_variable;
    byte temp_array[4];
  } u;
  // Overite bytes of union with float variable
  u.float_variable = val;
  // Assign bytes to input array
  memcpy(bytes_array, u.temp_array, 4);
}

void sendNewTempTCP(){
  // FLAGS
  //  000   │0  │0  │0  │01
  //        │   │   │   │
  //        │   │   │   └ staticky kod pre posielanie teploty
  //        │   │   └ IF 1 ERROR Temperature can not be read
  //        │   └  IF 1 ERROR Dac not found
  //        └ IF 1 In progress

  
  char data[16];

  // 2^104 padding
  for (int i = 0; i < 11; i++){
    data[i] = 0;
  }

  union {
    float float_variable;
    byte temp_array[4];
  } u;
  // Overite bytes of union with float variable
  u.float_variable = temperature;
  // Assign bytes to input array
  memcpy(&data[11], u.temp_array, 4);


  /*
  // 2^32 temperature
  data[11] = ((uint8_t)temperature >> 32);
  data[12] = ((uint8_t)temperature >> 16);
  data[13] = ((uint8_t)temperature >> 8);
  data[14] = (uint8_t)temperature;
  */
  // 2^8 flag
  data[15] = controller_flags::SEND_TEMP; //+ flag 


  tcpClient.write(data, sizeof(data), ASYNC_WRITE_FLAG_COPY);
}


void handleNotFound(AsyncWebServerRequest * request)
{
  String message = F("File Not Found\n\n");
  
  message += F("URI: ");
  message += request->url();
  message += F("\nMethod: ");
  message += (request->method() == HTTP_GET) ? F("GET") : F("POST");
  message += F("\nArguments: ");
  message += request->args();
  message += F("\n");
  
  for (uint8_t i = 0; i < request->args(); i++)
  {
    message += " " + request->argName(i) + ": " + request->arg(i) + "\n";
  }
  
  request->send(404, F("text/plain"), message);
}

void handleTemperature(AsyncWebServerRequest * request)
{
  char temperatureBuf[10];
  float tempC = temperature;
  if ((tempC < 10000) && (tempC > 0))
    dtostrf(tempC, 1, 2, temperatureBuf);
  else
  {
    temperatureBuf[0] = '-';
    temperatureBuf[1] = '1';
    temperatureBuf[2] = 0;
  }
  request->send(200, F("text/plain"), temperatureBuf);
}

void setDAC(AsyncWebServerRequest * request)
{
  if (request->args() == 2)
  {
    char arg0[20];
    char arg1[20];
    String sss0 = request->arg((uint8_t)0);
    String sss1 = request->arg((uint8_t)1);
    int len0 = sss0.length();
    int len1 = sss1.length();
    int i;
    for (i = 0; i < len0; i++)
      arg0[i] = sss0[i];
    arg0[i] = 0;
    for (i = 0; i < len1; i++)
      arg1[i] = sss1[i];
    arg1[i] = 0;
    Serial.println(len0);
    Serial.println(len1);
    
    Serial.println(arg0);
    Serial.println(arg1);
        
    int channel = atoi(arg0);
    int value = atoi(arg1);

    Serial.println(value*10);
    
    if ((channel < 0) || (channel > 1) || (value < 0) || (value > 10000)) return;
    Serial.println(value*20);

    dac.outputSquare(value, 10000, 0, 100, channel);
    Serial.println(value*30);

    //dac.setDACOutVoltage(value, channel);
    //delay(1000);
    //dac.store();
    Serial.println(value*40);
    Serial.print(F("\nDac kanal: "));
    Serial.print(channel);
    Serial.print(F(" is set to: "));
    Serial.println(value);
    Serial.println(value*50);

    request->send(200, F("text/plain"), F("OK"));
    Serial.println(value*60);
  } else {
    request->send(404, F("text/plain"), F("ERROR"));
  }
}

void setpd(AsyncWebServerRequest * request){
  //*  
    //suma = 0;
    char arg0[20];
    char arg1[20];
    char arg2[20];
    char arg3[20];
    char arg4[20];
    char arg5[20];
    String sss0 = request->arg((uint8_t)0);
    String sss1 = request->arg((uint8_t)1);
    String sss2 = request->arg((uint8_t)2);
    String sss3 = request->arg((uint8_t)3);
    String sss4 = request->arg((uint8_t)4);
    String sss5 = request->arg((uint8_t)5);

    // string to char
    
    
    int len0 = sss0.length();
    int len1 = sss1.length();
    int len2 = sss2.length();
    int len3 = sss3.length();
    int len4 = sss4.length();
    int len5 = sss5.length();
    int i;    
    for (i = 0; i < len0; i++)
      arg0[i] = sss0[i];
    arg0[i] = 0;
    for (i = 0; i < len1; i++)
      arg1[i] = sss1[i];
    arg1[i] = 0;    
    for (i = 0; i < len2; i++)
      arg2[i] = sss2[i];
    arg2[i] = 0;
    for (i = 0; i < len3; i++)
      arg3[i] = sss3[i];
    arg3[i] = 0;
    for (i = 0; i < len4; i++)
      arg4[i] = sss4[i];
    arg4[i] = 0;
    for (i = 0; i < len5; i++)
      arg5[i] = sss5[i];
    arg5[i] = 0;

    Serial.println("pred atoi");
    p_reg = atoi(arg0) / 1000.0;
    d_reg = atoi(arg1) / 1000.0;
    temp_reg= atoi(arg2);
    alpha = atoi(arg3) / 1000.0;
    delaj = atoi(arg4);
    i_reg = atoi(arg5) / 100000.0;
    Serial.println("po atoi");
    request->send(200, F("text/plain"), F("OK"));
    

    // string to chars //TODO - priamo pouzit string cez pointer *str
    /*
    char args[6][11];

    for (int i = 0; i < 6; i++){
      String get_argument = request->arg((uint8_t)0);
      if (get_argument.length() >= 10){
        request->send(404, F("text/plain"), F("ERROR: argument is larger then 10 chars"));
        break;
      }
      strcpy(args[i], get_argument.c_str());
    }
   
    Serial.println("pred atoi");
    p_reg = atoi(args[0]) / 1000.0;
    d_reg = atoi(args[1]) / 1000.0;
    temp_reg= atoi(args[2]);
    alpha = atoi(args[3]) / 1000.0;
    delaj = atoi(args[4]);
    i_reg = atoi(args[5]) / 1000.0;
    Serial.println("po atoi");
    request->send(200, F("text/plain"), F("OK"));
    */
}

void loop()
{
  if (remote_server_conected == false){
    Serial.printf("is IP set: %i Conn_err: %i Try to conn: %i UDPlis: %i", memory.isIPset(), remote_server_connection_error, trying_connect_TCP, listening_for_UDP);
    if (/*memory.isIPset() == true &&*/ remote_server_connection_error == false && trying_connect_TCP == false && listening_for_UDP == false){
      // pripoj sa ku serveru bez hladania IP
      udpListen(0); // stop udp listener
      tcpClient.connect(memory.getIP(), memory.getPORT());
      trying_connect_TCP = true;
      listening_for_UDP = false;
      
    } else if (((listening_for_UDP == false && remote_server_connection_error == true && trying_connect_TCP == false)) || memory.isIPset() == false) {
      // hladaj server
      udpListen(memory.getPORT()); // start udp listener
      listening_for_UDP = true;
    }
    
  }
  
  pd_step();

  if (last_send_temperature != temperature){
      sendNewTempTCP();
      Serial.print("temp: ");
      Serial.println(temperature);
      last_send_temperature = temperature;
  }
  
}
