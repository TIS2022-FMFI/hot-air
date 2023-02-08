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

    volatile bool searching_server = false; // listening for UDP
    volatile bool server_find = false;
    volatile bool connecting_server = false;
    volatile bool connected_server = false;
    volatile bool connection_error = false;
    volatile bool lost_connection = false;
    volatile uint8_t request_udp_listening = 2;
    volatile bool id_has_change = false;

    volatile bool disconnected_time_out = false;
    volatile unsigned long disconnected_time_out_millis = 0;
    volatile unsigned long connection_time_out_millis = 0;
    volatile unsigned long ack_server_time_out = 0;
    volatile bool heating = false;
    volatile bool cooling_down = false;
    volatile bool overheat = false;
    volatile unsigned long pid_delay_millis = 0;

    volatile float actual_temperature = 0;
    volatile float last_temperature = 0;
    volatile int phsaeID = 0;

    volatile bool emergency_stop = false;
    volatile uint16_t set_temperature = 0;
    volatile uint8_t set_airflow = 0;
    volatile uint8_t set_power = 0;
    volatile uint16_t actual_power = 0;



    // UDP server password
    uint8_t server_password[5] = {0x41, 0x48, 0x4f, 0x4a, 0x2b}; //


    char data[16];    


    Status();

    bool begin();


};

#endif
