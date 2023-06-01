//********************************************************
// Class to handle all status of controller
//
//  Status:
//
//  DAC not found
//  Thermometer not found  
//  
//  TCP:
//  searching for server
//  server find
//  conecting to server
//  connected to server
//  lost connection to server
//  Emergency stop ??
//  
//  Heating process:
//  get job from server / heating
//  cooling down
//  Heater overheat
//********************************************************
#include "status.h"


Status::Status() {
}

bool Status::begin(){
  return true;
}

void Status::clearPID(){
  
}