#include <math.h>
#include <sys/_stdint.h>
#include "defines.h"

#ifndef STATUS_h
#define STATUS_h
class Status{
  public:
    bool wire_begin = false;
    bool eeprom_begin = false;
    bool dac_connected = false;
    bool thermometer_connected = false;

    bool searching_server = false; // listening for UDP
    bool server_find = false;
    bool connecting_server = false;
    bool connected_server = false;
    bool connection_error = false;
    bool lost_connection = false;

    bool heating = false;
    bool cooling_down = false;
    bool overheat = false;

    float actual_temperature = NAN;
    float last_temperature = NAN;

    bool emergency_stop = false;
    uint16_t set_temperature = 0;
    uint8_t set_airflow = 0;
    uint8_t set_power = 0;
    uint8_t actual_power = 0;

    // UDP server password
    uint8_t server_password[5] = {0x41, 0x48, 0x4f, 0x4a, 0x2b};

    Status();

    bool begin();


};

#endif
