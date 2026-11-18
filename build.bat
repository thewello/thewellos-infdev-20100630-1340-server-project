@echo off
rem Builds TheWelloDev on Windows. Requires JDK 8 (set JAVA_HOME or put javac on PATH).
rem Inputs:
rem   CLIENT_JAR   original Infdev jar, default: original\inf-20100630-1340.jar
rem   LWJGL_JAR    LWJGL 2 jar (compile-time only), default: lib\lwjgl.jar
rem   RELEASE_DIR  release bundle folder, default: ..\TheWelloDev-Release
setlocal EnableDelayedExpansion
cd /d "%~dp0"

if defined JAVA_HOME (set "JB=%JAVA_HOME%\bin\") else (set "JB=")
set "JAVAC=%JB%javac"
set "JAR=%JB%jar"
set "JAVA=%JB%java"
if not defined CLIENT_JAR set "CLIENT_JAR=original\inf-20100630-1340.jar"
if not defined LWJGL_JAR set "LWJGL_JAR=lib\lwjgl.jar"
if not defined RELEASE_DIR set "RELEASE_DIR=..\TheWelloDev-Release"
set "JOPT=-nowarn -encoding UTF-8 -source 1.8 -target 1.8"
set "ASM=lib\asm-9.8.jar;lib\asm-tree-9.8.jar"

if not exist "%CLIENT_JAR%" (echo Missing %CLIENT_JAR%. Put the original inf-20100630-1340.jar there or set CLIENT_JAR. & goto fail)
if not exist "%LWJGL_JAR%" (echo Missing %LWJGL_JAR%. Put an LWJGL 2 jar there or set LWJGL_JAR. & goto fail)

if exist build rmdir /s /q build
mkdir build\common build\patcher build\mod build\api build\api-src build\server build\bridge\bridge build\plugins build\patcher-all build\server-all 2>nul
if not exist dist mkdir dist

echo [1/8] common (protocol and ASM hook tool)
call :srclist common\src build\s1.txt
"%JAVAC%" %JOPT% -cp "%ASM%" -d build\common @build\s1.txt || goto fail

echo [2/8] client patcher
call :srclist client\patcher\src build\s2.txt
"%JAVAC%" %JOPT% -cp "%ASM%;build\common" -d build\patcher @build\s2.txt || goto fail

echo [3/8] public client jar (for compiling)
"%JAVA%" -cp "%ASM%;build\common;build\patcher" net.thewellodev.patcher.ClientPatcher "%CLIENT_JAR%" build\client-public.jar --publicize-only || goto fail

echo [4/8] client mod, patcher jar and patched client
call :srclist client\mod build\s4.txt
"%JAVAC%" %JOPT% -cp "%LWJGL_JAR%;build\client-public.jar;build\common" -d build\mod @build\s4.txt || goto fail
mkdir build\patcher\mod 2>nul
xcopy /s /y /q /i build\mod build\patcher\mod >nul
xcopy /s /y /q /i build\common\net\thewellodev\protocol build\patcher\mod\net\thewellodev\protocol >nul
pushd build\patcher\mod
if exist index.txt del index.txt
for /r %%F in (*.class) do (set "R=%%F" & set "R=!R:%CD%\=!" & set "R=!R:\=/!" & >> index.txt echo !R!)
popd
xcopy /s /y /q /i build\common build\patcher-all >nul
xcopy /s /y /q /i build\patcher build\patcher-all >nul
pushd build\patcher-all
"%JAR%" xf "..\..\lib\asm-9.8.jar" || (popd & goto fail)
"%JAR%" xf "..\..\lib\asm-tree-9.8.jar" || (popd & goto fail)
if exist META-INF rmdir /s /q META-INF
if exist module-info.class del module-info.class
popd
> build\patcher.mf echo Main-Class: net.thewellodev.patcher.ClientPatcher
"%JAR%" cfm dist\TheWelloDev-ClientPatcher.jar build\patcher.mf -C build\patcher-all . || goto fail
"%JAVA%" -jar dist\TheWelloDev-ClientPatcher.jar "%CLIENT_JAR%" dist\inf-20100630-1340-mp.jar || goto fail

echo [5/8] API
call :srclist api\src build\s5.txt
"%JAVAC%" %JOPT% -d build\api @build\s5.txt || goto fail
xcopy /s /y /q /i api\src build\api-src >nul
"%JAR%" cf dist\TheWelloDev-API.jar -C build\api . || goto fail
"%JAR%" cf dist\TheWelloDev-API-sources.jar -C build\api-src . || goto fail

echo [6/8] server
call :srclist server\src build\s6.txt
"%JAVAC%" %JOPT% -cp "%ASM%;build\common;build\api" -d build\server @build\s6.txt || goto fail
call :srclist server\bridge build\s6b.txt
"%JAVAC%" %JOPT% -cp "build\client-public.jar;build\common;build\server" -d build\bridge\bridge @build\s6b.txt || goto fail
xcopy /s /y /q /i build\common build\server-all >nul
xcopy /s /y /q /i build\api build\server-all >nul
xcopy /s /y /q /i build\server build\server-all >nul
xcopy /s /y /q /i build\bridge build\server-all >nul
pushd build\server-all
"%JAR%" xf "..\..\lib\asm-9.8.jar" || (popd & goto fail)
"%JAR%" xf "..\..\lib\asm-tree-9.8.jar" || (popd & goto fail)
if exist META-INF rmdir /s /q META-INF
if exist module-info.class del module-info.class
popd
> build\server.mf echo Main-Class: net.thewellodev.server.Main
"%JAR%" cfm dist\TheWelloDev-Server.jar build\server.mf -C build\server-all . || goto fail

echo [7/8] plugins
if not exist dist\plugins mkdir dist\plugins
for /d %%P in (plugins\*) do (
  set "PN=%%~nxP"
  mkdir "build\plugins\!PN!" 2>nul
  call :srclist "%%P\src" build\splug.txt
  "%JAVAC%" %JOPT% -cp dist\TheWelloDev-API.jar -d "build\plugins\!PN!" @build\splug.txt || goto fail
  copy /y "%%P\src\plugin.yml" "build\plugins\!PN!\" >nul
  for /f "tokens=2" %%N in ('findstr /b "name:" "%%P\src\plugin.yml"') do set "JN=%%N"
  "%JAR%" cf "dist\plugins\!JN!.jar" -C "build\plugins\!PN!" . || goto fail
)

echo [8/8] release bundle: %RELEASE_DIR%
if exist "%RELEASE_DIR%" rmdir /s /q "%RELEASE_DIR%"
mkdir "%RELEASE_DIR%\client" "%RELEASE_DIR%\server\plugins" "%RELEASE_DIR%\server\optional-plugins" "%RELEASE_DIR%\plugin-api\example-plugin" 2>nul
copy /y release\README.txt "%RELEASE_DIR%\" >nul
copy /y release\THIRD-PARTY-LICENSES.txt "%RELEASE_DIR%\" >nul
copy /y lib\ASM-LICENSE.txt "%RELEASE_DIR%\" >nul
copy /y LICENSE "%RELEASE_DIR%\" >nul
copy /y dist\TheWelloDev-ClientPatcher.jar "%RELEASE_DIR%\client\" >nul
copy /y release\client\patch-client.bat "%RELEASE_DIR%\client\" >nul
copy /y release\client\inf-20100630-1340-mp.json "%RELEASE_DIR%\client\" >nul
copy /y release\client\inf-20100630-1340-mp.info "%RELEASE_DIR%\client\" >nul
copy /y dist\TheWelloDev-Server.jar "%RELEASE_DIR%\server\" >nul
copy /y release\server\start-server.bat "%RELEASE_DIR%\server\" >nul
copy /y release\server\server.properties "%RELEASE_DIR%\server\" >nul
copy /y dist\plugins\*.jar "%RELEASE_DIR%\server\optional-plugins\" >nul
del /q "%RELEASE_DIR%\server\optional-plugins\HelloPlugin.jar" 2>nul
copy /y dist\TheWelloDev-API.jar "%RELEASE_DIR%\plugin-api\" >nul
copy /y dist\TheWelloDev-API-sources.jar "%RELEASE_DIR%\plugin-api\" >nul
copy /y docs\PLUGIN-API.md "%RELEASE_DIR%\plugin-api\" >nul
xcopy /s /y /q /i plugins\example\src "%RELEASE_DIR%\plugin-api\example-plugin\source" >nul
copy /y dist\plugins\HelloPlugin.jar "%RELEASE_DIR%\plugin-api\example-plugin\" >nul

echo Done. Development jars are in dist\, the release bundle is in %RELEASE_DIR%.
endlocal
exit /b 0

:srclist
rem Writes the quoted path of every .java file under %1 into the file %2 (quotes allow spaces in paths).
if exist "%~2" del "%~2"
for /r "%~1" %%F in (*.java) do (set "P=%%F" & set "P=!P:\=/!" & >> "%~2" echo "!P!")
exit /b 0

:fail
echo Build failed.
endlocal
exit /b 1
