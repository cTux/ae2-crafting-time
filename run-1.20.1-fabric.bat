@echo off
set "MODS=versions\1.20.1-fabric\run\mods"
set "JEI=jei-1.20.1-fabric-15.20.0.132.jar"
set "JEI_URL=https://cdn.modrinth.com/data/u6dRKJwZ/versions/dI7d5ZeA/%JEI%"
if not exist "%MODS%" mkdir "%MODS%"
for %%F in ("%MODS%\jei-*.jar") do if /I not "%%~nxF"=="%JEI%" del "%%~fF"
if not exist "%MODS%\%JEI%" curl.exe -fsSL -o "%MODS%\%JEI%" "%JEI_URL%" || exit /b 1
call gradlew.bat :fabric_1_20_1:runClient %*
