#include "statusled.h"
#include "defines.h"

bool StatusLed::begin(){
  pixels = Adafruit_NeoPixel(NUMBER_OF_PINS, RGB_LED_PIN, NEO_GRB + NEO_KHZ800);

  pixels.setBrightness(200);
  pixels.setPixelColor(0, 0,255,42);
  pixels.show();
  return true;
}

void StatusLed::refresh(){

}

