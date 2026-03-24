@echo off
chcp 65001 > nul
REM 本地部署测试脚本 (Windows)
REM 使用方法: .\local-test.bat

echo =================================
echo TestCase Manager 本地部署测试
echo =================================
echo.

REM 检查 Java
echo [1/6] 检查 Java 环境...
java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo [X] Java 未安装，请先安装 Java 17
    exit /b 1
)
for /f "tokens=3" %%g in ('java -version 2>&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo [OK] Java 版本: %JAVA_VERSION%

REM 检查 Gradle
echo.
echo [2/6] 检查 Gradle...
if exist "gradlew.bat" (
    echo [OK] Gradle Wrapper 存在
) else (
    echo [X] Gradle Wrapper 不存在
    exit /b 1
)

REM 清理构建
echo.
echo [3/6] 清理旧构建...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo [X] 清理失败
    exit /b 1
)
echo [OK] 清理成功

REM 编译项目
echo.
echo [4/6] 编译项目...
call gradlew.bat compileKotlin
if %errorlevel% neq 0 (
    echo [X] 编译失败，请检查错误信息
    exit /b 1
)
echo [OK] 编译成功

REM 运行单元测试
echo.
echo [5/6] 运行单元测试...
call gradlew.bat test
if %errorlevel% neq 0 (
    echo [!] 部分测试失败，请检查测试报告
) else (
    echo [OK] 单元测试通过
)

REM 构建插件
echo.
echo [6/6] 构建插件...
call gradlew.bat buildPlugin
if %errorlevel% neq 0 (
    echo [X] 插件构建失败
    exit /b 1
)
echo [OK] 插件构建成功

echo.
echo 插件位置: build\distributions\
dir /b build\distributions\*.zip 2> nul

echo.
echo =================================
echo 部署测试完成!
echo =================================
echo.
echo 下一步:
echo 1. 在 PyCharm 中安装插件:
echo    Settings -^> Plugins -^> Install from disk -^> 选择 build\distributions\*.zip
echo.
echo 2. 重启 PyCharm
echo.
echo 3. 打开任意 .yaml 文件测试功能
echo.

pause
