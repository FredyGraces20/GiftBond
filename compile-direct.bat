@echo off
echo ========================================
echo Compiling GiftBond Plugin Directly
echo ========================================

REM Set up environment
set JAVA_HOME=C:\Program Files\Java\jdk-25
set PATH=%JAVA_HOME%\bin;%PATH%

REM Clean previous build
if exist "target\classes" rmdir /s /q "target\classes"
mkdir "target\classes"

REM Compile Java files
echo Compiling Java source files...
javac -source 17 -target 17 ^
  -cp "lib/*" ^
  -d "target/classes" ^
  -encoding UTF-8 ^
  src\main\java\com\fredygraces\giftbond\*.java ^
  src\main\java\com\fredygraces\giftbond\**\*.java

if %errorlevel% neq 0 (
  echo Compilation failed!
  pause
  exit /b 1
)

echo Compilation successful!

REM Copy resources
echo Copying resources...
xcopy /s /y "src\main\resources" "target\classes" >nul

REM Create JAR
echo Creating JAR file...
jar cf target\GiftBond-latest.jar -C target\classes .

if %errorlevel% neq 0 (
  echo JAR creation failed!
  pause
  exit /b 1
)

echo.
echo ========================================
echo Plugin compiled successfully!
echo Output: target\GiftBond-latest.jar
echo ========================================
pause