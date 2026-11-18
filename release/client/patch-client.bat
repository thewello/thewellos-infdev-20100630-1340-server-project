@echo off
rem Patches inf-20100630-1340.jar (the original Infdev client) into inf-20100630-1340-mp.jar.
cd /d "%~dp0"
if not exist "inf-20100630-1340.jar" (
  echo Put the original inf-20100630-1340.jar in this folder first.
  pause
  exit /b 1
)
java -jar TheWelloDev-ClientPatcher.jar inf-20100630-1340.jar inf-20100630-1340-mp.jar
if errorlevel 1 (
  echo Patching failed.
) else (
  echo Done: inf-20100630-1340-mp.jar was created.
)
pause
