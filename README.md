# hot-air
Control, monitoring, and scheduling of hot-air fans

# Spustenie
Najskôr je potrebné spustiť server dvojklikom na `Server.jar` hot-air/src/server/Server/Server.jar.  <br>

Ak je potrebné prepísať XML na náš formát, dvojklikom otvoríme `GUI.jar`. Následne v *Settings* doplníme adresu `EXECPP.exe` (kliknutím na lupu sa otvorí prieskumník súborov). Ak sa *Settings* uložia, pri spustení GUI sa načítaju posledné uložené nastavenia. Ďalej v *Blowers* alebo *Projects* zadáme cestu k XML (`SEARCH FILE` uľahčí hľadanie). Ak chceme k fázam priradiť konkrétne dúchadlo, v *Blowers* zaklikneme príslušné checkboxy a následne stlačíme `UPLOAD BUTTON` <image src= 'https://user-images.githubusercontent.com/95253218/217627263-2f2012fd-2a74-4d66-92b6-87cc590b25f2.jpg' widht=15 height=15> . Týmto sa vytvorí kópia zvoleného XML, upravená o spúšťanie nášho EXE súboru a do názvov blokov sa pridá teplota a zvolené ID dúchadiel.  <br>

Ak
- názvy blokov v XML už obsahujú `@...` doplnia sa iba o zvolené ID dúchadiel
- neobsahujú `@...` doplnia sa o template `@temperature` a následne ID dúchadiel. V tomto prípade treba `temperature` prepísať na želanú teplotu. 

# Flashovanie arduina
