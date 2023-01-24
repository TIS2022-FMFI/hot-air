#include "Socket.h"

#define MESSAGE_SIZE_BYTES 4
#define SERVER_RECOGNIZE_EXE_MESSAGE 'a'
#define SERVER_RECOGNIZE_EXE_MESSAGE_LENGTH 1
#define SERVER_PASSWORD "abcd"

Socket::Socket(in_addr ip, const char *port) {
    char stringIp[INET_ADDRSTRLEN];
    sock = INVALID_SOCKET;
    errorFlag = false;
    WSADATA wsaData;
    struct addrinfo *result = nullptr,
            *ptr = nullptr,
            hints {};
    int iResult;

    inet_ntop(AF_INET, (&ip), stringIp, INET_ADDRSTRLEN);
    printf("[TCP] Trying to connect to the server on ip = %s, port = %s\n", stringIp, port);
    iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("[TCP] WSAStartup failed with error: %d\n", iResult);
        errorFlag = true;
        return;
    }

    ZeroMemory( &hints, sizeof(hints) );
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    iResult = getaddrinfo(stringIp, port, &hints, &result);
    if (iResult != 0) {
        printf("[TCP] getaddrinfo failed with error: %d\n", iResult);
        WSACleanup();
        errorFlag = true;
        return;
    }

    for(ptr=result; ptr != nullptr ;ptr=ptr->ai_next) {
        sock = socket(ptr->ai_family, ptr->ai_socktype,
                               ptr->ai_protocol);
        if (sock == INVALID_SOCKET) {
            printf("[TCP] socket failed with error: %d\n", WSAGetLastError());
            WSACleanup();
            errorFlag = true;
            return;
        }

        iResult = connect( sock, ptr->ai_addr, (int)ptr->ai_addrlen);
        if (iResult == SOCKET_ERROR) {
            closesocket(sock);
            sock = INVALID_SOCKET;
            errorFlag = true;
            continue;
        }
        break;
    }

    freeaddrinfo(result);

    if (sock == INVALID_SOCKET) {
        printf("[TCP] Unable to connect to server!\n");
        WSACleanup();
        return;
    }
    printf("[TCP] Socket successfully established!\n");

    const char * msg = readMessage();
    if (msg == nullptr) {
        errorFlag = true;
        return;
    }
    const char * serverPassword = SERVER_PASSWORD;
    if (strcmp(msg, serverPassword) != 0) {
        printf("[TCP] Server password does not match\n");
        stopConnection();
    }
    delete msg;
}

char *Socket::readMessage() {
    printf("[TCP] reading a message\n");
    int32_t bytesReceived;
    char msgLengthBuffer[MESSAGE_SIZE_BYTES];
    int32_t buffLen;

    bytesReceived = recv(sock, msgLengthBuffer, MESSAGE_SIZE_BYTES, 0);
    if (bytesReceived == 0) {
        printf("[TCP] Socket connection closed\n");
        errorFlag = true;
        return nullptr;
    } else if (bytesReceived < 0) {
        printf("[TCP] recv failed with error: %d\n", WSAGetLastError());
        errorFlag = true;
        return nullptr;
    }

    buffLen = ((uint8_t)(msgLengthBuffer[3])) + ((uint8_t)(msgLengthBuffer[2]) << 8) + ((uint8_t)(msgLengthBuffer[1]) << 16) + ((uint8_t)(msgLengthBuffer[0]) << 24);
    char *buff = new char[buffLen+1]; //+1 -> for end of string -> \0
    ZeroMemory(buff, buffLen+1);

    bytesReceived = recv(sock, buff, buffLen, 0);
    if (bytesReceived > 0) {
        return buff;
    } else if (bytesReceived == 0) {
        printf("[TCP] Socket connection closed\n");
        errorFlag = true;
        return nullptr;
    }
    printf("[TCP] recv failed with error: %d\n", WSAGetLastError());
    errorFlag = true;
    return nullptr;
}

void Socket::sendRecognizeMeMessage() {
    int32_t bytesSent;
    char sendBuff[SERVER_RECOGNIZE_EXE_MESSAGE_LENGTH] {SERVER_RECOGNIZE_EXE_MESSAGE};

    bytesSent = send(sock, sendBuff, SERVER_RECOGNIZE_EXE_MESSAGE_LENGTH, 0);
    if (bytesSent == SOCKET_ERROR) {
        printf("[TCP] send failed with error: %d\n", WSAGetLastError());
        stopConnection();
        errorFlag = true;
        return;
    }

//    printf("[TCP] Bytes Sent: %d\n", bytesSent);
}

void Socket::sendMessage(const char *msg, uint32_t msgLen) {
    int32_t bytesSent;
    char* sendBuff = (char *)malloc(msgLen + 4); 
    if (sendBuff == 0) return;
    printf("[TCP] Sending a message!\n");
    sendBuff[0] = (msgLen >> 24) & 0xFF;
    sendBuff[1] = (msgLen >> 16) & 0xFF;
    sendBuff[2] = (msgLen >> 8) & 0xFF;
    sendBuff[3] = msgLen & 0xFF;

    for (uint32_t i = 0; i < msgLen; i++) {
        sendBuff[4+i] = msg[i];
    }

    bytesSent = send(sock, sendBuff, msgLen+4, 0);
    if (bytesSent == SOCKET_ERROR) {
        printf("[TCP] send failed with error: %d\n", WSAGetLastError());
        stopConnection();
        errorFlag = true;
        free(sendBuff);
        return;
    }
    if (bytesSent != msgLen+4) {
        printf("[TCP] Less data than expected has been sent");

    }

    printf("[TCP] Bytes Sent: %d\n", bytesSent);
    free(sendBuff);
}

void Socket::stopConnection() const {
    shutdown(sock, SD_BOTH);
    closesocket(sock);
    WSACleanup();
}