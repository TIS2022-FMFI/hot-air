#include <iostream>
#include <fstream>
#include "Socket.h"
#include "Discovery.h"

#define I_AM_EXE 'a'
#define I_BRING_XML_FILE 'a'
#define I_BRING_END_OF_PHASE 'b'
#define DEFAULT_PORT_STR "4002"
#define DEFAULT_IP "127.0.0.1"

#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")

char * getFileBytes(const char* filePath, int64_t &fileLength) {
    std::ifstream infile(filePath, std::ios::binary);

    infile.seekg(0, std::ios::end);
    fileLength = infile.tellg();
    infile.seekg(0, std::ios::beg);
    char *buffer = new char[fileLength];
    infile.read(buffer, fileLength);
    infile.close();

    return buffer;
}

bool fileExists(const char* filePath) {
    std::ifstream f(filePath);
    return f.good();
}

int __cdecl main(int argc, char **argv) {
    if (argc > 1) {
        in_addr localhost {};
        inet_pton(AF_INET, DEFAULT_IP, &localhost);
        Socket s = (argc > 2) ? Socket {localhost, argv[2]} : Socket {localhost, DEFAULT_PORT_STR};
        if (s.getErrorFlag()) {
            printf("[TCP] Server not on localhost, trying UDP discovery\n");
            Discovery d {};
            in_addr serverIp = d.discover();
            s = (argc > 2) ? Socket {serverIp, argv[2]} : Socket {serverIp, DEFAULT_PORT_STR};
        }
        if (s.getErrorFlag()) {
            printf("[UDP] discovery failed!\n");
            return 1;
        }
        printf("[TCP] Successfully connected to server!\n");
        s.sendRecognizeMeMessage();
        if (argv[1][0] == '¿') {
            printf("[TCP] Sending end of segment message to server\n");
            const char message[2] {I_AM_EXE, I_BRING_END_OF_PHASE};
            s.sendMessage(message, 2);
            s.sendMessage(argv[1]+1, (uint32_t)strlen(argv[1]+1)); //send path to xml
        } else {
            printf("[TCP] Sending xml to server and starting a new project\n");
            if (!fileExists(argv[1])) {
                printf("%s No such file found!\n", argv[1]);
                return 1;
            }
            const char message[2] {I_AM_EXE, I_BRING_XML_FILE};
            s.sendMessage(message, 2);
            s.sendMessage(argv[1], (uint32_t)strlen(argv[1])); //send path to xml
            std::string pathToFileString {argv[1]};
            std::string fileName = pathToFileString.substr(pathToFileString.find_last_of("/\\")+1);
            s.sendMessage(fileName.c_str(), (uint32_t)fileName.size());
            int64_t fileLength {0};
            char * a = getFileBytes(argv[1], fileLength);
            printf("%lld\n", fileLength);
            s.sendMessage(a, (uint32_t)fileLength);
        }
        s.stopConnection();
        if (s.getErrorFlag()) {
            printf("Something failed");
            return 1;
        }
    } else {
        printf("Usage:\n.\\EXE.exe \"path_to_xml\" [port] -> to start a new project\n.\\EXE.exe \"¿path_to_xml\" [port] -> to tell server that a phase has come to an end\n");
    }
    return 0;
}