@echo off
echo ========================================
echo Compilando Plugin GiftBond con Maven
echo ========================================
echo.

REM Verificar si Maven está instalado
call mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven no está instalado o no está en el PATH
    echo Descarga e instala Maven desde: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo Compilando el proyecto...
call mvn clean package -DskipTests

if errorlevel 1 (
    echo.
    echo ERROR: Falló la compilación del proyecto
    pause
    exit /b 1
)

echo.
echo ========================================
echo COMPILACIÓN COMPLETADA
echo ========================================
echo.
echo El archivo GiftBond.jar está listo en la carpeta target/
echo Puedes copiarlo a la carpeta plugins/ de tu servidor Minecraft.
echo.
pause