@echo off
set "MODS=versions\1.21.1-neoforge\run\mods"
set "AE2CT=ae2ct-1.21.1-1.1.1.jar"
set "AE2CT_URL=https://edge.forgecdn.net/files/7182/163/%AE2CT%"
set "JEI=jei-1.21.1-neoforge-19.27.0.343.jar"
set "JEI_URL=https://cdn.modrinth.com/data/u6dRKJwZ/versions/iiCpE7cU/%JEI%"
if not exist "%MODS%" mkdir "%MODS%"
for %%F in ("%MODS%\ae2ct-*.jar") do if /I not "%%~nxF"=="%AE2CT%" del "%%~fF"
if not exist "%MODS%\%AE2CT%" curl.exe -fsSL -o "%MODS%\%AE2CT%" "%AE2CT_URL%" || exit /b 1
for %%F in ("%MODS%\jei-*.jar") do if /I not "%%~nxF"=="%JEI%" del "%%~fF"
if not exist "%MODS%\%JEI%" curl.exe -fsSL -o "%MODS%\%JEI%" "%JEI_URL%" || exit /b 1
call gradlew.bat :mc_1_21_1_neoforge:runClient %*
