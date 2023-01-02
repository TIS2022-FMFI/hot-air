# Program pre ovládanie dúchadla HOTWIND SYSTEM
Tento program funguje ako termostat, ktorý vie ovládať dúchadlo HOTWIND SYSTEM cez DAC modul. *Viac o ovládacom protokole dúchadla v [dokumentacii](https://github.com/TIS2022-FMFI/hot-air/blob/main/docs/HOTWIND_SYSTEM_datasheet.pdf).* Teplotu meria pomocou čipu MAX6675 a komunikuje cez ethernet pomocou TCP, UDP a HTTP.
## Použitý hardvér
1. [WT32-ETH01](#WT32)
2. [DFRobot Gravity 2 Channel I2C DAC Module 0V to 10V](#dac)
3. [max6675](#dac)

### <a id="WT32"> 1. WT32-ETH01 </a>
[datasheet](https://files.seeedstudio.com/products/102991455/WT32-ETH01_datasheet_V1.1-%20en.pdf) pre WT32-ETH01

#### PIN OUT 
1. pre DAC MODUL

| WT32-ETH01 pin | DAC Module     |
| :-------- | :-------: | 
| SDA `D2` | `D` |
| SDA `D4` | `C` |

2. pre max6675

| WT32-ETH01 pin |  max6675 	|
|:-----------| :------------:| 
| `D14`		   | 		`S0` 	|
| `D12` 		 | 		`CS` 	|
| `D15` 		 | 		`SCK` 	|
