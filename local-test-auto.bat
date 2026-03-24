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

REM 检查并安装 Java
echo [STEP 1/10] 检查 Java 环境...
echo [STEP 1/10] 检查 Java 环境... >> %LOG_FILE%

java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Java 未安装，尝试自动安装...
    echo [WARNING] Java 未安装，尝试自动安装... >> %LOG_FILE%
    
    REM 检查是否有 winget
    where winget > nul 2>&1
    if %errorlevel% equ 0 (
        echo [INFO] 使用 winget 安装 Java...
        winget install EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements
        if %errorlevel% equ 0 (
            echo [SUCCESS] Java 安装成功
            echo [SUCCESS] Java 安装成功 >> %LOG_FILE%
            REM 刷新环境变量
            call refreshenv
        ) else (
            echo [ERROR] Java 自动安装失败
            echo [ERROR] Java 自动安装失败 >> %LOG_FILE%
            echo 请手动安装 Java 17: https://adoptium.net/
            pause
            exit /b 1
        )
    ) else (
        echo [ERROR] 无法自动安装 Java（未找到 winget）
        echo [ERROR] 无法自动安装 Java（未找到 winget） >> %LOG_FILE%
        echo 请手动安装 Java 17: https://adoptium.net/
        pause
        exit /b 1
    )
)

echo [SUCCESS] Java 已安装
echo [SUCCESS] Java 已安装 >> %LOG_FILE%
java -version 2>> %LOG_FILE%

REM 检查 Gradle Wrapper
echo.
echo [STEP 2/10] 检查 Gradle Wrapper...
echo [STEP 2/10] 检查 Gradle Wrapper... >> %LOG_FILE%

if not exist "gradlew.bat" (
    echo [WARNING] Gradle Wrapper 不存在，尝试生成...
    echo [WARNING] Gradle Wrapper 不存在，尝试生成... >> %LOG_FILE%
    
    REM 检查是否有 gradle
    where gradle > nul 2>&1
    if %errorlevel% equ 0 (
        echo [INFO] 使用系统 gradle 生成 wrapper...
        gradle wrapper --gradle-version 8.5 2>> %LOG_FILE%
    ) else (
        echo [ERROR] Gradle 未安装，无法生成 wrapper
        echo [ERROR] Gradle 未安装，无法生成 wrapper >> %LOG_FILE%
        echo 请安装 Gradle: https://gradle.org/install/
        pause
        exit /b 1
    )
)

if exist "gradlew.bat" (
    echo [SUCCESS] Gradle Wrapper 存在
    echo [SUCCESS] Gradle Wrapper 存在 >> %LOG_FILE%
    call gradlew.bat --version 2>> %LOG_FILE%
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

set ALL_FILES_EXIST=true

if not exist "src\main\kotlin\com\testcase\manager\TestCaseManagerPlugin.kt" (
    echo [ERROR] TestCaseManagerPlugin.kt 不存在
    set ALL_FILES_EXIST=false
)

if not exist "src\main\kotlin\com\testcase\manager\ui\ExcelEditor.kt" (
    echo [ERROR] ExcelEditor.kt 不存在
    set ALL_FILES_EXIST=false
)

if not exist "src\main\resources\META-INF\plugin.xml" (
    echo [ERROR] plugin.xml 不存在
    set ALL_FILES_EXIST=false
)

if not exist "build.gradle.kts" (
    echo [ERROR] build.gradle.kts 不存在
    set ALL_FILES_EXIST=false
)

if "%ALL_FILES_EXIST%"=="false" (
    echo [ERROR] 缺少必要文件，停止构建
    pause
    exit /b 1
)

echo [SUCCESS] 项目结构检查通过
echo [SUCCESS] 项目结构检查通过 >> %LOG_FILE%

REM 检查并创建 gradle.properties
echo.
echo [STEP 4/10] 检查 Gradle 配置...
echo [STEP 4/10] 检查 Gradle 配置... >> %LOG_FILE%

if not exist "gradle.properties" (
    echo [WARNING] gradle.properties 不存在，创建默认配置...
    echo [WARNING] gradle.properties 不存在，创建默认配置... >> %LOG_FILE%
    (
        echo kotlin.stdlib.default.dependency=false
        echo kotlin.incremental.useClasspathSnapshot=false
        echo org.gradle.jvmargs=-Xmx2048m
        echo org.gradle.parallel=true
        echo org.gradle.caching=true
    ) > gradle.properties
    echo [SUCCESS] 已创建 gradle.properties
    echo [SUCCESS] 已创建 gradle.properties >> %LOG_FILE%
)

REM 清理旧构建
echo.
echo [STEP 5/10] 清理旧构建...
echo [STEP 5/10] 清理旧构建... >> %LOG_FILE%

call gradlew.bat clean 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [WARNING] 清理过程中有警告
    echo [WARNING] 清理过程中有警告 >> %LOG_FILE%
) else (
    echo [SUCCESS] 清理成功
    echo [SUCCESS] 清理成功 >> %LOG_FILE%
)

REM 检查依赖
echo.
echo [STEP 6/10] 检查依赖...
echo [STEP 6/10] 检查依赖... >> %LOG_FILE%

call gradlew.bat dependencies --configuration compileClasspath 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [WARNING] 依赖检查有警告，尝试刷新...
    echo [WARNING] 依赖检查有警告，尝试刷新... >> %LOG_FILE%
    call gradlew.bat --refresh-dependencies 2>> %LOG_FILE%
) else (
    echo [SUCCESS] 依赖检查完成
    echo [SUCCESS] 依赖检查完成 >> %LOG_FILE%
)

REM 编译 Kotlin
echo.
echo [STEP 7/10] 编译 Kotlin 代码...
echo [STEP 7/10] 编译 Kotlin 代码... >> %LOG_FILE%

call gradlew.bat compileKotlin --stacktrace 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [ERROR] Kotlin 编译失败
    echo [ERROR] Kotlin 编译失败 >> %LOG_FILE%
    echo 查看详细错误: %ERR_LOG_FILE%
    
    REM 提取错误信息
    echo === 编译错误摘要 === >> %ERR_LOG_FILE%
    findstr /B "e:" %LOG_FILE% >> %ERR_LOG_FILE%
    
    echo.
    echo 可能的解决方案:
    echo 1. 检查 Kotlin 语法错误
    echo 2. 运行 gradlew --refresh-dependencies 刷新依赖
    echo 3. 删除 %USERPROFILE%\.gradle\caches 后重试
    
    pause
    exit /b 1
)

echo [SUCCESS] Kotlin 编译成功
echo [SUCCESS] Kotlin 编译成功 >> %LOG_FILE%

REM 运行单元测试
echo.
echo [STEP 8/10] 运行单元测试...
echo [STEP 8/10] 运行单元测试... >> %LOG_FILE%

call gradlew.bat test --stacktrace 2>> %LOG_FILE%
if %errorlevel% neq 0 (
    echo [WARNING] 部分测试失败
    echo [WARNING] 部分测试失败 >> %LOG_FILE%
    echo 测试报告: build\reports\tests\test\index.html
) else (
    echo [SUCCESS] 单元测试通过
    echo [SUCCESS] 单元测试通过 >> %LOG_FILE%
)

REM 构建插件
echo.
echo [STEP 9/10] 构建插件...
echo [STEP 9/10] 构建插件... >> %LOG_FILE%

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
echo [STEP 10/10] 检查构建产物...
echo [STEP 10/10] 检查构建产物... >> %LOG_FILE%

if exist "build\distributions" (
    echo [SUCCESS] 构建目录存在
    echo [SUCCESS] 构建目录存在 >> %LOG_FILE%
    
    echo 构建产物:
    echo 构建产物: >> %LOG_FILE%
    dir /b build\distributions\*.zip 2>> %LOG_FILE%
    dir /b build\distributions\*.zip
    
    for /f "delims=" %%i in ('dir /b /od build\distributions\*.zip') do set PLUGIN_ZIP=%%i
) else (
    echo [ERROR] 构建目录不存在
    echo [ERROR] 构建目录不存在 >> %LOG_FILE%
    pause
    exit /b 1
)

REM 生成构建报告
echo.
echo 生成构建报告...
echo 生成构建报告... >> %LOG_FILE%

(
echo # TestCase Manager 构建报告
echo.
echo ## 构建信息
echo - **构建时间**: %date% %time%
echo - **构建状态**: 成功
echo - **Java 版本**: 
java -version 2>&1
echo.
echo ## 构建产物
dir build\distributions\*.zip /b
echo.
echo ## 日志文件
echo - 完整日志: %LOG_FILE%
echo - 错误日志: %ERR_LOG_FILE%
echo.
echo ## 安装步骤
echo 1. 打开 PyCharm
echo 2. Settings → Plugins → Install from disk
echo 3. 选择 build\distributions\%PLUGIN_ZIP%
echo 4. 重启 PyCharm
echo 5. 打开任意 .yaml 文件测试
echo.
echo ## 提示
echo - 如果遇到问题，查看日志文件: %LOG_FILE%
echo - 错误信息: %ERR_LOG_FILE%
) > %REPORT_FILE%

echo [SUCCESS] 构建报告已生成: %REPORT_FILE%
echo [SUCCESS] 构建报告已生成: %REPORT_FILE% >> %LOG_FILE%

REM 总结
echo.
echo ========================================
echo 构建成功!
echo ========================================
echo.
echo 日志文件: %LOG_FILE%
echo 错误日志: %ERR_LOG_FILE%
echo 构建报告: %REPORT_FILE%
echo.
echo 安装步骤:
echo 1. 打开 PyCharm
echo 2. Settings → Plugins → Install from disk
echo 3. 选择 build\distributions\%PLUGIN_ZIP%
echo 4. 重启 PyCharm
echo 5. 打开任意 .yaml 文件测试
echo.

pause
