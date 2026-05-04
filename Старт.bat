@echo off
chcp 65001 >nul
title Wave 1.5 Reworked

echo.
echo   Wave 1.5 Reworked
echo   ==================
echo.

:: Check Java
where java >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Java not found. Install JDK 11+:
    echo   https://adoptium.net/temurin/releases/
    echo.
    pause
    exit /b 1
)

:: Check JAR
if not exist "Wave.jar" (
    echo   [ERROR] Wave.jar not found.
    echo   Download from: https://github.com/JustNanix/Wave-Multibot/releases
    echo.
    pause
    exit /b 1
)

:: Auto-create config from example if missing
if not exist "config.yml" (
    if exist "config.example.yml" (
        echo   [INFO] config.yml not found, copying from config.example.yml...
        copy "config.example.yml" "config.yml" >nul
        echo   [INFO] Edit config.yml before running again.
        echo.
        pause
        exit /b 0
    ) else (
        echo   [ERROR] config.yml not found.
        echo.
        pause
        exit /b 1
    )
)

:: Run
java -Xmx2G -server -jar Wave.jar

echo.
pause
