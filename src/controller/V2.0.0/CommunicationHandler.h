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
  enum communication_flags{IM_CONTROLLER = 0x80, EMERGENCY_STOP = 0x80, NEW_ID = 0x02, SET_TEMPERATURE = 0x01, EMERGENCY_STOP_RELEASE = 0x40};

  void tcpInit();

  void handleConnection();
  void handleTemperature(uint8_t *buffer);
  void handleEmergency_stop(uint8_t *buffer);
  void handleEmergency_release(uint8_t *buffer);
  void handleDisconnect();
  void handleError();

  uint8_t sendTemperatureFlag();

public:
  bool begin(AsyncUDP *_udp, AsyncClient *_tcp, Status *_status, Preferences *_memory);

  bool startUdp();
  bool stopUdp();

  void sendTemperature();

  void refresh();

};

#endif