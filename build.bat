@echo off
chcp 65001 > nul 2>&1
REM TestCase Manager 构建脚本 - Windows

setlocal EnableDelayedExpansion

echo ========================================
echo TestCase Manager Build Script
echo ========================================
echo.

REM 检查 Java
echo [INFO] Checking Java...

java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    echo Please install Java 17: https://adoptium.net/
    pause
    exit /b 1
)

REM 获取 Java 版本
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%g"
    set "JAVA_VERSION=!JAVA_VERSION:"=!"
)

echo [INFO] Java version: !JAVA_VERSION!

REM 提取主版本号
for /f "tokens=1 delims=." %%v in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%v"
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2 delims=." %%v in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%v"
)

REM 检查版本
if !JAVA_MAJOR! LSS 17 (
    echo [ERROR] Java version too old: !JAVA_VERSION!
    echo Java 17 or higher is required
    echo Please install Java 17: https://adoptium.net/
    pause
    exit /b 1
)

echo [SUCCESS] Java version OK

REM 检查 Gradle Wrapper
echo [INFO] Checking Gradle Wrapper...

if not exist "gradlew.bat" (
    echo [ERROR] Gradle Wrapper not found
    echo Please run: gradle wrapper
    pause
    exit /b 1
)

echo [SUCCESS] Gradle Wrapper found

REM 清理
echo [INFO] Cleaning...
call gradlew.bat clean --quiet 2> nul
if %errorlevel% neq 0 (
    echo [WARNING] Clean warning
)

REM 编译
echo [INFO] Compiling...
call gradlew.bat compileKotlin --quiet
if %errorlevel% neq 0 (
    echo [ERROR] Compile failed
    pause
    exit /b 1
)

echo [SUCCESS] Compile OK

REM 构建
echo [INFO] Building plugin...
call gradlew.bat buildPlugin --quiet
if %errorlevel% neq 0 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

echo [SUCCESS] Build OK

REM 检查产物
if exist "build\distributions\*.zip" (
    echo.
    echo [SUCCESS] Plugin package:
    dir /b build\distributions\*.zip
) else (
    echo [ERROR] Plugin package not found
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo Install steps:
echo 1. Open PyCharm
echo 2. Settings -^> Plugins -^> Install from disk
echo 3. Select build\distributions\*.zip
echo 4. Restart PyCharm
echo.

pause
