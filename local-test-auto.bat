@echo off
chcp 65001 > nul
REM 智能本地部署测试脚本（自动安装缺失依赖）- Windows版本
REM 使用方法: .\local-test-auto.bat

setlocal EnableDelayedExpansion

REM 生成时间戳
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set TIMESTAMP=%mydate%-%mytime%

REM 日志文件
set LOG_FILE=build-test-%TIMESTAMP%.log
set ERR_LOG_FILE=build-test-%TIMESTAMP%.error.log
set REPORT_FILE=build-report-%TIMESTAMP%.md

REM 初始化日志
echo TestCase Manager Auto Build - %date% %time% > %LOG_FILE%
echo Error Log - %date% %time% > %ERR_LOG_FILE%

echo ========================================
echo TestCase Manager 智能构建（自动安装）
echo ========================================
echo.
echo 日志文件: %LOG_FILE%
echo 错误日志: %ERR_LOG_FILE%
echo.

REM 函数：获取 Java 主版本号
:GET_JAVA_MAJOR
setlocal
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set ver=%%g
    set ver=!ver:"=!
)
for /f "tokens=1 delims=." %%v in ("!ver!") do set major=%%v
if "!major!"=="1" (
    for /f "tokens=2 delims=." %%v in ("!ver!") do set major=%%v
)
endlocal & set RESULT=%major%
goto :EOF

REM 检查 Java
echo [STEP 1/10] 检查 Java 环境...
echo [STEP 1/10] 检查 Java 环境... >> %LOG_FILE%

java -version > nul 2>&1
if %errorlevel% equ 0 (
    call :GET_JAVA_MAJOR
    set JAVA_MAJOR=%RESULT%
    echo [INFO] 检测到 Java 主版本号: !JAVA_MAJOR!
    echo [INFO] 检测到 Java 主版本号: !JAVA_MAJOR! >> %LOG_FILE%
    
    if !JAVA_MAJOR! LSS 17 (
        echo [WARNING] Java 版本过低，需要 Java 17+
        echo [WARNING] Java 版本过低，需要 Java 17+ >> %LOG_FILE%
        goto :INSTALL_JAVA
    ) else (
        echo [SUCCESS] Java 版本符合要求
        echo [SUCCESS] Java 版本符合要求 >> %LOG_FILE%
        java -version >> %LOG_FILE% 2>&1
        goto :JAVA_DONE
    )
) else (
    echo [WARNING] Java 未安装
    echo [WARNING] Java 未安装 >> %LOG_FILE%
    goto :INSTALL_JAVA
)

:INSTALL_JAVA
echo.
echo [INFO] 正在安装 Java 17...
echo [INFO] 正在安装 Java 17... >> %LOG_FILE%

REM 检查 winget
where winget > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 winget，无法自动安装
    echo [ERROR] 未找到 winget，无法自动安装 >> %LOG_FILE%
    echo 请手动安装 Java 17: https://adoptium.net/
    pause
    exit /b 1
)

REM 安装 Java 17
echo [INFO] 使用 winget 安装 Eclipse Temurin JDK 17...
winget install EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements
if %errorlevel% neq 0 (
    echo [ERROR] Java 17 安装失败
    echo [ERROR] Java 17 安装失败 >> %LOG_FILE%
    pause
    exit /b 1
)

echo [SUCCESS] Java 17 安装成功
echo [SUCCESS] Java 17 安装成功 >> %LOG_FILE%

REM 查找 Java 17 安装路径并设置环境变量
echo [INFO] 配置 Java 环境变量...
echo [INFO] 配置 Java 环境变量... >> %LOG_FILE%

REM 常见安装路径
set JAVA_HOME=
if exist "C:\Program Files\Eclipse Adoptium\jdk-17" (
    set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17
) else if exist "C:\Program Files\Java\jdk-17" (
    set JAVA_HOME=C:\Program Files\Java\jdk-17
) else if exist "C:\Program Files (x86)\Eclipse Adoptium\jdk-17" (
    set JAVA_HOME=C:\Program Files (x86)\Eclipse Adoptium\jdk-17
)

if not defined JAVA_HOME (
    REM 尝试从注册表查找
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\Eclipse Adoptium\JDK\17" /v Path 2^>nul') do set JAVA_HOME=%%b
)

if defined JAVA_HOME (
    echo [INFO] 找到 Java 17: !JAVA_HOME!
    echo [INFO] 找到 Java 17: !JAVA_HOME! >> %LOG_FILE%
    
    REM 设置环境变量（当前会话）
    set "JAVA_HOME=!JAVA_HOME!"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
    
    REM 验证
    "!JAVA_HOME!\bin\java.exe" -version >> %LOG_FILE% 2>&1
) else (
    echo [WARNING] 无法自动找到 Java 17 路径，尝试使用 refreshenv
    echo [WARNING] 无法自动找到 Java 17 路径，尝试使用 refreshenv >> %LOG_FILE%
    call refreshenv
)

REM 最终验证
call :GET_JAVA_MAJOR
if !RESULT! LSS 17 (
    echo [ERROR] Java 17 安装后仍无法使用
    echo [ERROR] Java 17 安装后仍无法使用 >> %LOG_FILE%
    echo 请手动安装 Java 17 并设置 JAVA_HOME 环境变量
    pause
    exit /b 1
)

:JAVA_DONE

REM 检查 Gradle Wrapper
echo.
echo [STEP 2/10] 检查 Gradle Wrapper...
echo [STEP 2/10] 检查 Gradle Wrapper... >> %LOG_FILE%

if not exist "gradlew.bat" (
    echo [WARNING] Gradle Wrapper 不存在，尝试生成...
    echo [WARNING] Gradle Wrapper 不存在，尝试生成... >> %LOG_FILE%
    
    where gradle > nul 2>&1
    if %errorlevel% equ 0 (
        echo [INFO] 使用系统 gradle 生成 wrapper...
        gradle wrapper --gradle-version 8.5 2>> %LOG_FILE%
    ) else (
        echo [ERROR] Gradle 未安装
        echo [ERROR] Gradle 未安装 >> %LOG_FILE%
        pause
        exit /b 1
    )
)

if exist "gradlew.bat" (
    echo [SUCCESS] Gradle Wrapper 存在
    echo [SUCCESS] Gradle Wrapper 存在 >> %LOG_FILE%
) else (
    echo [ERROR] Gradle Wrapper 生成失败
    echo [ERROR] Gradle Wrapper 生成失败 >> %LOG_FILE%
    pause
    exit /b 1
)

REM 检查项目结构
echo.
echo [STEP 3/10] 检查项目结构...
echo [STEP 3/10] 检查项目结构... >> %LOG_FILE%

set ALL_EXIST=true
for %%f in (
    "src\main\kotlin\com\testcase\manager\TestCaseManagerPlugin.kt"
    "src\main\kotlin\com\testcase\manager\ui\ExcelEditor.kt"
    "src\main\resources\META-INF\plugin.xml"
    "build.gradle.kts"
) do (
    if exist %%f (
        echo [SUCCESS] ✓ %%f
        echo [SUCCESS] ✓ %%f >> %LOG_FILE%
    ) else (
        echo [ERROR] ✗ %%f 不存在
        echo [ERROR] ✗ %%f 不存在 >> %LOG_FILE%
        set ALL_EXIST=false
    )
)

if "%ALL_EXIST%"=="false" (
    echo [ERROR] 缺少必要文件
    echo [ERROR] 缺少必要文件 >> %LOG_FILE%
    pause
    exit /b 1
)

REM 清理旧构建
echo.
echo [STEP 4/10] 清理旧构建...
echo [STEP 4/10] 清理旧构建... >> %LOG_FILE%

call gradlew.bat clean 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [WARNING] 清理警告（可能无旧构建）
    echo [WARNING] 清理警告（可能无旧构建） >> %LOG_FILE%
) else (
    echo [SUCCESS] 清理成功
    echo [SUCCESS] 清理成功 >> %LOG_FILE%
)

REM 编译 Kotlin
echo.
echo [STEP 5/10] 编译 Kotlin 代码...
echo [STEP 5/10] 编译 Kotlin 代码... >> %LOG_FILE%

call gradlew.bat compileKotlin --stacktrace 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [ERROR] Kotlin 编译失败
    echo [ERROR] Kotlin 编译失败 >> %LOG_FILE%
    echo === 编译错误摘要 === >> %ERR_LOG_FILE%
    findstr /B "e:" %LOG_FILE% >> %ERR_LOG_FILE%
    pause
    exit /b 1
)

echo [SUCCESS] Kotlin 编译成功
echo [SUCCESS] Kotlin 编译成功 >> %LOG_FILE%

REM 构建插件
echo.
echo [STEP 6/10] 构建插件...
echo [STEP 6/10] 构建插件... >> %LOG_FILE%

call gradlew.bat buildPlugin --stacktrace 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [ERROR] 插件构建失败
    echo [ERROR] 插件构建失败 >> %LOG_FILE%
    pause
    exit /b 1
)

echo [SUCCESS] 插件构建成功
echo [SUCCESS] 插件构建成功 >> %LOG_FILE%

REM 检查构建产物
echo.
echo [STEP 7/10] 检查构建产物...
echo [STEP 7/10] 检查构建产物... >> %LOG_FILE%

if exist "build\distributions\*.zip" (
    echo [SUCCESS] 构建产物:
    echo [SUCCESS] 构建产物: >> %LOG_FILE%
    dir /b build\distributions\*.zip 2>> %LOG_FILE%
    dir /b build\distributions\*.zip
) else (
    echo [ERROR] 未找到构建产物
    echo [ERROR] 未找到构建产物 >> %LOG_FILE%
    pause
    exit /b 1
)

REM 生成报告
echo.
echo 生成构建报告...
echo 生成构建报告... >> %LOG_FILE%

(
echo # TestCase Manager 构建报告
echo.
echo ## 构建信息
echo - 时间: %date% %time%
echo - 状态: 成功
echo.
echo ## 构建产物
dir build\distributions\*.zip /b
echo.
echo ## 日志
echo - 完整日志: %LOG_FILE%
echo - 错误日志: %ERR_LOG_FILE%
) > %REPORT_FILE%

echo [SUCCESS] 报告已生成: %REPORT_FILE%

REM 总结
echo.
echo ========================================
echo 构建成功!
echo ========================================
echo.
echo 日志: %LOG_FILE%
echo 报告: %REPORT_FILE%
echo.
pause
