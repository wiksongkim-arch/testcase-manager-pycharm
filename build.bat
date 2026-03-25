@echo off
REM TestCase Manager Build Script with Logging

setlocal EnableDelayedExpansion

REM 日志文件
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do set mydate=%%c%%a%%b
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do set mytime=%%a%%b
set LOG_FILE=build-!mydate!-!mytime!.log

REM 初始化日志
echo Build started at !date! !time! > !LOG_FILE!

echo ========================================
echo TestCase Manager Build Script
echo ========================================
echo.
echo [INFO] Log file: !LOG_FILE!
echo [INFO] Log file: !LOG_FILE! >> !LOG_FILE!

REM Check Java
echo [INFO] Checking Java...
echo [INFO] Checking Java... >> !LOG_FILE!

java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    echo [ERROR] Java not found >> !LOG_FILE!
    echo [INFO] Please install Java 17: https://adoptium.net/
    echo [INFO] Please install Java 17: https://adoptium.net/ >> !LOG_FILE!
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%g"
    set "JAVA_VERSION=!JAVA_VERSION:"=!"
)

echo [INFO] Java version: !JAVA_VERSION!
echo [INFO] Java version: !JAVA_VERSION! >> !LOG_FILE!

REM Extract major version
for /f "tokens=1 delims=." %%v in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%v"
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2 delims=." %%v in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%v"
)

if !JAVA_MAJOR! LSS 17 (
    echo [ERROR] Java version too old: !JAVA_VERSION!
    echo [ERROR] Java version too old: !JAVA_VERSION! >> !LOG_FILE!
    echo [INFO] Java 17 or higher is required
    echo [INFO] Java 17 or higher is required >> !LOG_FILE!
    pause
    exit /b 1
)

echo [SUCCESS] Java version OK
echo [SUCCESS] Java version OK >> !LOG_FILE!

REM Check Gradle Wrapper
echo [INFO] Checking Gradle Wrapper...
echo [INFO] Checking Gradle Wrapper... >> !LOG_FILE!

if not exist "gradlew.bat" (
    echo [ERROR] Gradle Wrapper not found
    echo [ERROR] Gradle Wrapper not found >> !LOG_FILE!
    pause
    exit /b 1
)

echo [SUCCESS] Gradle Wrapper found
echo [SUCCESS] Gradle Wrapper found >> !LOG_FILE!

REM Clean
echo [INFO] Cleaning...
echo [INFO] Cleaning... >> !LOG_FILE!
echo [CMD] gradlew clean --quiet >> !LOG_FILE!
call gradlew.bat clean --quiet >> !LOG_FILE! 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Clean warning
    echo [INFO] Clean warning >> !LOG_FILE!
)

REM Compile
echo [INFO] Compiling...
echo [INFO] Compiling... >> !LOG_FILE!
echo [CMD] gradlew compileKotlin --quiet >> !LOG_FILE!
call gradlew.bat compileKotlin --quiet >> !LOG_FILE! 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Compile failed
    echo [ERROR] Compile failed >> !LOG_FILE!
    echo [INFO] See !LOG_FILE! for details
    pause
    exit /b 1
)

echo [SUCCESS] Compile OK
echo [SUCCESS] Compile OK >> !LOG_FILE!

REM Build
echo [INFO] Building plugin...
echo [INFO] Building plugin... >> !LOG_FILE!
echo [CMD] gradlew buildPlugin --quiet >> !LOG_FILE!
call gradlew.bat buildPlugin --quiet >> !LOG_FILE! 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Build failed
    echo [ERROR] Build failed >> !LOG_FILE!
    echo [INFO] See !LOG_FILE! for details
    pause
    exit /b 1
)

echo [SUCCESS] Build OK
echo [SUCCESS] Build OK >> !LOG_FILE!

REM Check output
if exist "build\distributions\*.zip" (
    echo.
    echo [SUCCESS] Plugin package:
    echo [SUCCESS] Plugin package: >> !LOG_FILE!
    dir /b build\distributions\*.zip
    dir /b build\distributions\*.zip >> !LOG_FILE!
) else (
    echo [ERROR] Plugin package not found
    echo [ERROR] Plugin package not found >> !LOG_FILE!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo [INFO] Log saved to: !LOG_FILE!
echo.
echo Install steps:
echo 1. Open PyCharm
echo 2. Settings -^> Plugins -^> Install from disk
echo 3. Select build\distributions\*.zip
echo 4. Restart PyCharm
echo.

pause
