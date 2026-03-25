@echo off
REM TestCase Manager 构建脚本
REM 使用方法: .\build.bat

echo ========================================
echo TestCase Manager 构建脚本
echo ========================================
echo.

REM 检查 Java
echo [INFO] 检查 Java 环境...

java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java 未安装
    echo 请安装 Java 17: https://adoptium.net/
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
)

echo [INFO] Java 版本: !JAVA_VERSION!

REM 提取主版本号
for /f "tokens=1 delims=." %%v in ("!JAVA_VERSION!") do set JAVA_MAJOR=%%v
if "!JAVA_MAJOR!"=="1" (
    for /f "tokens=2 delims=." %%v in ("!JAVA_VERSION!") do set JAVA_MAJOR=%%v
)

if !JAVA_MAJOR! LSS 17 (
    echo [ERROR] Java 版本过低: !JAVA_VERSION!
    echo 需要 Java 17 或更高版本
    echo 请安装 Java 17: https://adoptium.net/
    pause
    exit /b 1
)

echo [SUCCESS] Java 版本符合要求

REM 检查 Gradle Wrapper
echo [INFO] 检查 Gradle Wrapper...

if not exist "gradlew.bat" (
    echo [ERROR] Gradle Wrapper 不存在
    echo 请运行: gradle wrapper
    pause
    exit /b 1
)

echo [SUCCESS] Gradle Wrapper 存在

REM 清理旧构建
echo [INFO] 清理旧构建...
call gradlew.bat clean --quiet 2> nul || echo [WARNING] 清理警告

REM 编译
echo [INFO] 编译项目...
call gradlew.bat compileKotlin --quiet
if %errorlevel% neq 0 (
    echo [ERROR] 编译失败
    pause
    exit /b 1
)

echo [SUCCESS] 编译成功

REM 构建插件
echo [INFO] 构建插件...
call gradlew.bat buildPlugin --quiet
if %errorlevel% neq 0 (
    echo [ERROR] 构建失败
    pause
    exit /b 1
)

echo [SUCCESS] 构建成功

REM 检查构建产物
if exist "build\distributions\*.zip" (
    echo [SUCCESS] 插件包:
    dir /b build\distributions\*.zip
) else (
    echo [ERROR] 未找到插件包
    pause
    exit /b 1
)

echo.
echo ========================================
echo 构建完成!
echo ========================================
echo.
echo 安装步骤:
echo 1. 打开 PyCharm
echo 2. Settings → Plugins → Install from disk
echo 3. 选择 build\distributions\*.zip
echo 4. 重启 PyCharm
echo.

pause
