cmake_minimum_required(VERSION 3.22)
project(EXE)

set(CMAKE_CXX_STANDARD 20)

link_libraries(ws2_32 wsock32 iphlpapi)

add_executable(EXE main.cpp Socket.cpp Socket.h Discovery.cpp Discovery.h)
