@echo off
set PORT=4002
set arg1=%1
set arg2=%2

if "%arg2%"=="" goto use_default_port
start java -jar .\EXE.jar %arg1% %arg2%
exit /b 1

:use_default_port
start java -jar .\EXE.jar %arg1% %PORT%