#ifndef COMMUNICATIONHANDLER_h
#define COMMUNICATIONHANDLER_h

#include <AsyncUDP.h>
#ifndef ASYNCTCP_H_
#include <AsyncTCP.h>
#endif
#include "status.h"
#include "preferences.h"
#include "defines.h"

class ServerCommunication{
private:
  AsyncUDP* udp;
  AsyncClient* tcp;
  Status* status;
  Preferences* memory;
  unsigned long millis_send_temperature = 0;

  enum communication_flags{IM_CONTROLLER = 0x80, EMERGENCY_STOP = 0x80, NEW_ID = 0x02, SET_TEMPERATURE = 0x01, EMERGENCY_STOP_RELEASE = 0x40, ACK = 0x20};

  void tcpInit();

  void handleConnection();
  void handleTemperature(uint8_t *buffer);
  void handleEmergency_stop(uint8_t *buffer);
  void handleEmergency_release(uint8_t *buffer);
  void handleDisconnect();
  void handleError();
  void sendAck(uint8_t *buffer);

  void printRawData(uint8_t *buffer, uint16_t len);

  // DEBUG

  uint8_t sendTemperatureFlag();

public:
  bool begin(AsyncUDP *_udp, AsyncClient *_tcp, Status *_status, Preferences *_memory);

  void sendID();
  void udpHandler(AsyncUDPPacket packet);
  // bool startUdp();
  // bool stopUdp();

  void sendTemperature();
  void sendTemperatureTimeout(unsigned long millis, int refresh);

  void refresh();
  void handlePacket();

};

#endif