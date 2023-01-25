#ifndef EXE_SOCKET_H
#define EXE_SOCKET_H

#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <cstdio>
#include <cstdint>

class Socket {
public:
    Socket(in_addr ip, const char* port);

    [[nodiscard]] inline bool getErrorFlag() const {return errorFlag;}
    inline void removeErrorFlag() {errorFlag = false;}

    void sendMessage(const char*, uint32_t);
    void sendRecognizeMeMessage();
    char* readMessage();

    void stopConnection() const;
private:
    SOCKET sock;
    bool errorFlag;
};

#endif //EXE_SOCKET_H
