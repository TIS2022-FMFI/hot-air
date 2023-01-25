#ifndef EXE_DISCOVERY_H
#define EXE_DISCOVERY_H

#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <cstdint>

#define DEFAULT_PORT 4002

class UDPSocket {
public:
    inline UDPSocket() : UDPSocket(DEFAULT_PORT) {};
    explicit UDPSocket(short port);
    struct in_addr listenForMessage();
    void sendMessage(char *message, int32_t messageLength, in_addr ip);
    void stopSocket() const;
    [[nodiscard]] inline bool getErrorFlag() const {return errorFlag;}
private:
    SOCKET sock;
    bool errorFlag;
    struct sockaddr_in si_template {};
};

//class UDPServer {
//public:
//    inline UDPServer() : UDPServer(DEFAULT_PORT) {};
//    explicit UDPServer(short port);
//    struct in_addr listenForMessage();
//    void stopSocket() const;
//    [[nodiscard]] inline bool getErrorFlag() const {return errorFlag;}
//
//private:
//    SOCKET sock;
//    bool errorFlag;
//};
//
//class UDPClient {
//public:
//    inline UDPClient() : UDPClient(DEFAULT_PORT) {};
//    explicit UDPClient(short port);
//    void sendMessage(char *message, int32_t messageLength, in_addr ip);
//    void stopSocket() const;
//    [[nodiscard]] inline bool getErrorFlag() const {return errorFlag;}
//
//private:
//    SOCKET sock;
//    bool errorFlag;
//    struct sockaddr_in si_other {};
//};

class Discovery {
public:
    inline Discovery() : Discovery(DEFAULT_PORT) {};
    explicit Discovery(short port);
    in_addr discover(); //returns TCP server ip
private:
    static in_addr* getBroadcastAddresses(uint32_t &len);
    UDPSocket socket;
};

#endif //EXE_DISCOVERY_H
