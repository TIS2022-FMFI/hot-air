#include "Discovery.h"
#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>
#include <cstdio>
#include <iphlpapi.h>
#include <vector>

#define BUFF_SIZE 1024
#define I_AM_THE_SERVER_MESSAGE {72, 65, 76, 76, 79}
#define I_AM_THE_SERVER_MESSAGE_LENGTH 5
#define LOOKING_FOR_SERVER_MESSAGE {83, 89, 83}
#define LOOKING_FOR_SERVER_MESSAGE_LENGTH 3

UDPSocket::UDPSocket(short port) {
    printf("[UDP] creating a socket on port = %d\n", port);
    errorFlag = false;
    si_template = {};
    struct sockaddr_in server{};
    WSADATA wsa;

    if (WSAStartup(MAKEWORD(2,2),&wsa) != 0) {
        printf("[UDP] Failed. Error Code : %d\n",WSAGetLastError());
        errorFlag = true;
        stopSocket();
    }

    if((sock = socket(AF_INET , SOCK_DGRAM ,  IPPROTO_UDP)) == INVALID_SOCKET) {
        printf("[UDP] Could not create socket : %d\n" , WSAGetLastError());
        errorFlag = true;
        stopSocket();
    }
    int optval = 1;
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char *) (&optval), sizeof optval);
    setsockopt(sock, SOL_SOCKET, SO_BROADCAST, (char *) (&optval), sizeof optval);

    ZeroMemory(&server, sizeof server);
    server.sin_family = AF_INET;
    server.sin_addr.s_addr = INADDR_ANY;
    server.sin_port = htons( port );

    if( bind(sock ,(struct sockaddr *)&server , sizeof(server)) == SOCKET_ERROR) {
        printf("[UDP] Bind failed with error code : %d\n" , WSAGetLastError());
        errorFlag = true;
        stopSocket();
    }

    ZeroMemory(&si_template, sizeof(si_template));
    si_template.sin_family = AF_INET;
    si_template.sin_port = htons(port);
    printf("[UDP] Socket successfully created\n");
}

bool isPasswordRight(char* a ,int aSize, char* b, int bSize) {
    for (int i = 0; i < aSize && i < bSize; i++) {
        if (a[i] != b[i]) {
            return false;
        }
    }
    return true;
}

struct in_addr UDPSocket::listenForMessage() {
    printf("[UDP] Listening for a message\n");
    struct sockaddr_in si_other {};
    ZeroMemory(&si_other, sizeof si_other);
    int32_t si_other_len = sizeof si_other;
//    char *buff = new char[BUFF_SIZE];
    char buff[BUFF_SIZE];
    ZeroMemory(buff, BUFF_SIZE);
    char serverPassword[] = I_AM_THE_SERVER_MESSAGE;

    do {
        if (recvfrom(sock, buff, BUFF_SIZE, 0, (sockaddr *) (&si_other), &si_other_len) == SOCKET_ERROR) {
            printf("[UDP] recvfrom() failed with error code : %d\n", WSAGetLastError());
            errorFlag = true;
            stopSocket();
//            delete[] buff;
            return in_addr {};
        }
        printf("[UDP] message received: %s\n", buff);
    } while (isPasswordRight(buff, BUFF_SIZE, serverPassword, I_AM_THE_SERVER_MESSAGE_LENGTH) == 0);

//    delete[] buff;
    return si_other.sin_addr;
}

void UDPSocket::stopSocket() const {
    closesocket(sock);
    WSACleanup();
}

void UDPSocket::sendMessage(char *message, int32_t messageLength, in_addr ip) {
    printf("[UDP] Sending a message\n");
    si_template.sin_addr = ip;
    if (sendto(sock, message, messageLength , 0 , (struct sockaddr *) &si_template, sizeof(si_template)) == SOCKET_ERROR)
    {
        printf("[UDP] sendto() failed with error code : %d\n" , WSAGetLastError());
        errorFlag = true;
        stopSocket();
    }
}

in_addr* Discovery::getBroadcastAddresses(uint32_t &len) {
    ULONG bufSz = 0;
    len = 0;
    if (GetAdaptersInfo(nullptr,&bufSz) == ERROR_BUFFER_OVERFLOW) {
        std::vector<BYTE> buf;
        in_addr *result = new in_addr[bufSz];
        buf.resize(bufSz,0);
        if (GetAdaptersInfo((IP_ADAPTER_INFO*)&buf[0],&bufSz) == ERROR_SUCCESS) {
            IP_ADAPTER_INFO* pAdapterInfo = (IP_ADAPTER_INFO*)&buf[0];
            for (int i = 0; pAdapterInfo != nullptr; pAdapterInfo = pAdapterInfo->Next, i++) {
                unsigned long ip = inet_addr(pAdapterInfo->IpAddressList.IpAddress.String);
                unsigned long mask = inet_addr(pAdapterInfo->IpAddressList.IpMask.String);
                unsigned long bcip = ip | ~mask;
                struct in_addr ia {};
                ia.S_un.S_addr = bcip;
                result[i] = ia;
                len++;
            }
        }
        return result;
    }
    return nullptr;
}

Discovery::Discovery(short port) {
    socket = UDPSocket {port};
//    server = UDPServer {port};
//    client = UDPClient {port};
}

in_addr Discovery::discover() {
    printf("[UDP] starting discovery\n");
    uint32_t broadcastAddressesLen = 0;
    in_addr* broadcastAddresses = getBroadcastAddresses(broadcastAddressesLen);
    if (broadcastAddresses == nullptr) {
        printf("[UDP] No broadcast addresses found!");
        return in_addr {};
    }
    char msg[] = LOOKING_FOR_SERVER_MESSAGE;
    char REMOVE_ME[INET_ADDRSTRLEN];

    for (int i = 0; i < broadcastAddressesLen; i++) {
        inet_ntop(AF_INET, &broadcastAddresses[i], REMOVE_ME, INET_ADDRSTRLEN);
        printf("broadcast Ip = %s\n", REMOVE_ME);
        socket.sendMessage(msg, LOOKING_FOR_SERVER_MESSAGE_LENGTH, broadcastAddresses[i]);
    }

    return socket.listenForMessage();
}

//
//UDPServer::UDPServer(short port) {
//    errorFlag = false;
//    struct sockaddr_in server{};
//    WSADATA wsa;
//
//    if (WSAStartup(MAKEWORD(2,2),&wsa) != 0) {
//        printf("Failed. Error Code : %d\n",WSAGetLastError());
//        errorFlag = true;
//        stopSocket();
//    }
//
//    if((sock = socket(AF_INET , SOCK_DGRAM , 0 )) == INVALID_SOCKET) {
//        printf("Could not create socket : %d\n" , WSAGetLastError());
//        errorFlag = true;
//        stopSocket();
//    }
//
//    ZeroMemory(&server, sizeof server);
//    server.sin_family = AF_INET;
//    server.sin_addr.s_addr = INADDR_ANY;
//    server.sin_port = htons( port );
//
//    if( bind(sock ,(struct sockaddr *)&server , sizeof(server)) == SOCKET_ERROR) {
//        printf("Bind failed with error code : %d\n" , WSAGetLastError());
//        errorFlag = true;
//        stopSocket();
//    }
//}
//
//struct in_addr UDPServer::listenForMessage() {
//    char buff[BUFF_SIZE];
//    struct sockaddr_in si_other {};
//    int32_t si_other_len = 0;
//    ZeroMemory(buff, BUFF_SIZE);
//    char serverPassword[] = I_AM_THE_SERVER_MESSAGE;
//
//    do {
//        if (recvfrom(sock, buff, BUFF_SIZE, 0, (sockaddr *) (&si_other), &si_other_len) == SOCKET_ERROR) {
//            printf("recvfrom() failed with error code : %d", WSAGetLastError());
//            errorFlag = true;
//            stopSocket();
//        }
//    } while (strcmp(buff, serverPassword) != 0);
//
//    return si_other.sin_addr;
//}
//
//void UDPServer::stopSocket() const {
//    closesocket(sock);
//    WSACleanup();
//}
//
//UDPClient::UDPClient(short port) {
//    errorFlag = false;
//    si_other = {};
//    WSADATA wsa;
//
//    if (WSAStartup(MAKEWORD(2,2),&wsa) != 0) {
//        printf("Failed. Error Code : %d\n", WSAGetLastError());
//        errorFlag = true;
//        stopSocket();
//    }
//
//    if ( (sock=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == SOCKET_ERROR) {
//        printf("socket() failed with error code : %d\n" , WSAGetLastError());
//        errorFlag = true;
//        stopSocket();
//    }
//
//    ZeroMemory(&si_other, sizeof(si_other));
//    si_other.sin_family = AF_INET;
//    si_other.sin_port = htons(port);
//}
//
//void UDPClient::sendMessage(char *message, int32_t messageLength, in_addr ip) {
//    si_other.sin_addr = ip;
//    if (sendto(sock, message, messageLength , 0 , (struct sockaddr *) &si_other, sizeof(si_other)) == SOCKET_ERROR)
//    {
//        printf("sendto() failed with error code : %d\n" , WSAGetLastError());
//        errorFlag = true;
//        stopSocket();
//    }
//}
//
//void UDPClient::stopSocket() const {
//    closesocket(sock);
//    WSACleanup();
//}