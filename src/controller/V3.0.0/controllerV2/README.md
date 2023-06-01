# Program for controlling the HOTWIND SYSTEM air heater
This program functions as a thermostat that can control the HOTWIND SYSTEM air heater through the DAC module. *More information about the control protocol of the air heater can be found in the [documentation](https://github.com/TIS2022-FMFI/hot-air/blob/main/docs/HOTWIND_SYSTEM_datasheet.pdf).* It measures the temperature using the MAX6675 chip and communicates via Ethernet using UDP and HTTP.

## Hardware Used
1. [WT32-ETH01](https://files.seeedstudio.com/products/102991455/WT32-ETH01_datasheet_V1.1-%20en.pdf)
2. [DFRobot Gravity 2 Channel I2C DAC Module 0V to 10V](https://www.dfrobot.com/product-2613.html)
3. [MAX31855](https://pdf1.alldatasheet.com/datasheet-pdf/view/415787/MAXIM/MAX31855.html)

### <a id="WT32"> 1. WT32-ETH01 </a>
[Datasheet](https://files.seeedstudio.com/products/102991455/WT32-ETH01_datasheet_V1.1-%20en.pdf) for WT32-ETH01

#### PIN OUT
1. For DAC Module

| WT32-ETH01 Pin | DAC Module Pin |
| :-------- | :-------: | 
| SDA `D2` | `D` |
| SDA `D4` | `C` |

2. For MAX31855

| WT32-ETH01 Pin | MAX31855 Pin |
|:-----------| :------------:| 
| `D14`		   | 		`S0` 	|
| `D12` 		 | 		`CS` 	|
| `D15` 		 | 		`SCK` 	|
