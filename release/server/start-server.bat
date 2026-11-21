@echo off
rem Starts the server in a window. Java 8 is required.
cd /d "%~dp0"
set "JAVA=javaw"
for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-8*" "%ProgramFiles%\Eclipse Adoptium\jre-8*" "%ProgramFiles%\Java\jre1.8*" "%ProgramFiles(x86)%\Java\jre1.8*") do if exist "%%~D\bin\javaw.exe" set "JAVA=%%~D\bin\javaw.exe"
start "" "%JAVA%" -Xms512M -Xmx1G -jar TheWellosInfdevServer.jar
