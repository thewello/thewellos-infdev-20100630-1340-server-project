@echo off
rem Builds the project with build.sh. This needs "sh": it comes with Git for Windows (Git Bash).
rem See docs\BUILD.md for the inputs and the output.
cd /d "%~dp0"
where sh >nul 2>nul
if errorlevel 1 (
  if exist "%ProgramFiles%\Git\bin\sh.exe" (set "SH=%ProgramFiles%\Git\bin\sh.exe") else (
    echo "sh" was not found. Install Git for Windows or run build.sh from a shell.
    exit /b 1
  )
) else (
  set "SH=sh"
)
"%SH%" build.sh %*
