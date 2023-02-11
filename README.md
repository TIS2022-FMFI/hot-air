# hot-air
Control, monitoring, and scheduling of hot-air fans

# Spustenie
### Server.jar
Aby fungovala komunikácia s dúchadlami je potrebné <u>spustiť server</u> dvojklikom na `Server.jar` (hot-air/src/server/Server/[Server.jar](https://github.com/TIS2022-FMFI/hot-air/blob/main/src/server/Server/Server.jar)).  <br>
Po spustení sa otvorí Windows PowerShell konzola. <br>
Ak chceme server zastaviť, stačí toto okno zavrieť, prípadne vypnúť `Ctrl + C` príkazom do konzoly.

### GUI.jar

Dvojklikom na `GUI.jar` spustíme GUI. To pozostáva z troch tabov:

 **1. BLOWERS** <br>
 Tu sa zobrazujú aktuálne pripojené dúchadlá.<br>
 Kliknutím na `ID` sa dostaneme na <u>webové rozhranie controllera</u>, kde vieme nastaviť jeho údaje = ID, IP adresu, PID... či zapnúť manuálne riadenie dúchadla.<br>
 Kliknutím na `STOP` pri dúchadle (a potvrdením v potvrdzovacom okne) zastavíme ohrev dúchadla. Teplota teda začne klesať, avšak dúchadlo stále vníma jeho cieľovú teplotu. <br>
 Kliknutím na <image src= 'https://github.com/TIS2022-FMFI/hot-air/blob/main/src/GUI/src/main/resources/GUI/caution.png?raw=true' widht=20 height=20> dúchadlo znova začne hriať na cieľovú teplotu.  <br>
 
 Kliknutím na `STOP ALL` (emergency button) všetky dúchadlá ihneď prestanú hriať a testovacie projekty sa zrušia.
    
 **2. PROJECTS** <br>
Tu sa zobrazujú aktuálne bežiace projekty. <br>
Kliknutím na `NAME` sa zobrazí nové okno s grafom projektu, kde sa zobrazuje celý priebeh testovania, teda teplota jednotlivých dúchadiel a aj ich požadovaná teplota. Graf sa posúva automaticky, ale dá sa tu posúvať scrollovaním myškou.<br>
Kliknutím na `STOP` sa testovanie zastaví a všetky dúchadlá k nemu priradené prestanú ohrievať. <br>

 **3. SETTINGS** <br>
 Tu vieme nastaviť cestu k EXE (kliknutím na lupu <image src= 'https://github.com/TIS2022-FMFI/hot-air/blob/main/src/GUI/src/main/resources/GUI/search.png?raw=true' widht=15 height=15> sa otvorí prieskumník súborov) a port na komunikáciu so serverom (defaultne nastavený na 4002).<br>
 Kliknutím na `SAVE` sa nastavenia uložia a  pri ďalšom spustení GUI sa načítajú.

# Pripojenie nového dúchadla  
- Do controllera zapojíme teplomer, konektor z dúchadla a napájanie, od 15V do 24V.
- Controller má po prvotnom spustení prednastavenú IP adresu `10.2.1.100` a ID `idNOTset`
- V GUI sa controller zobrazí do 30 sek, ak nie stlačíme tlačidlo `SCAN`
- Klikneme na `idNOTset`, aby sa nám otvorilo webové rozhranie controllera.
- Na stránke controllera si v pravej lište zvolíme `Settings`
- V `Settings` nastavíme novú IP adresu a ID controllera.
- Stlačíme `Save`.
- Reštartujeme controller, napr. stlačením `REBOOT`, a zatvoríme web stránku.
- Controller je pripravený na spustenie projektu.

Reštartovaním controllera sa odpojí a znova pripojí k serveru, v GUI môže vyskočiť upozornenie o odpojení dúchadla a  zároveň by sa už mali zobrazovať ID a IP adresa aké sme nastavili (ak by nie stlačíme `SCAN`). 

# Nahratie XML

Ak je potrebné <u>vytvoriť XML</u> podľa nášho formátu

 1. dvojklikom otvoríme `GUI.jar`
 2. v *Settings* tabe nastavíme adresu k `EXECPP.exe`
 3. v *Blowers* (prípadne *Projects*) tabe <u>zadáme cestu k XML</u> (`SEARCH FILE` button uľahčí hľadanie)
 4. Ak chceme k fázam <u>priradiť konkrétne dúchadlo</u>, v *Blowers* zaklikneme príslušné checkboxy
 5. stlačíme upload button <image src= 'https://github.com/TIS2022-FMFI/hot-air/blob/main/src/GUI/src/main/resources/GUI/submit.jpg?raw=true' widht=15 height=15>
 
 Týmto sa vytvorí kópia zvoleného XML. Táto kópia má na konci názvu pridané `_temp_control`  a je uložená na mieste ako pôvodné XML. Je upravená o spúšťanie nášho EXE a do názvov blokov sa pridala teplota a zvolené ID dúchadiel, v tvare `@temperature#id1#id2...`.  
Ak<br>
a) názvy blokov v XML už obsahujú `@...` doplnia sa iba o zvolené ID dúchadiel (ak boli nejaké zvolené)<br>
b) neobsahujú `@...` doplnia sa o template `@temperature` a následne ID dúchadiel. V tomto prípade treba všetky výskyty `temperature` prepísať na želanú teplotu.<br>

# Flashovanie arduina
- todo 
