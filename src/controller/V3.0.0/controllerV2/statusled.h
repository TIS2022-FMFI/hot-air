#ifndef STATULEDS_h
#define STATUSLED_h

#include <Adafruit_NeoPixel.h>


class StatusLed{
  Adafruit_NeoPixel pixels;


  public:
  bool begin();

  void refresh();

};


#endif