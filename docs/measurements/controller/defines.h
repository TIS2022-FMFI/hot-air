//********************************************************
// This is for definig all stuff
// TODO
//********************************************************

// comment if you want to use static IP
// uncomment if you want to get IP from DHCP

//#define _DHCP set

// Select the IP address according to your local network, works only if _DHCP is commented
#ifndef _DHCP
  IPAddress myIP(192, 168, 0, 232);
  IPAddress myGW(192, 168, 0, 1);
  IPAddress mySN(255, 255, 255, 0);
#endif

// DNS Server IP
IPAddress myDNS(1, 1, 1, 10);


// for getting ethernet debug msg on Serial
#define DEBUG_ETHERNET_WEBSERVER_PORT       Serial

// Debug Level from 0 to 4
#define _ETHERNET_WEBSERVER_LOGLEVEL_       3

// USE ONLY FOR TESTING. BYPASS ALL SAFTY
#define _DEBUG 
