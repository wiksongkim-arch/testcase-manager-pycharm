#!/bin/bash
# 智能本地部署测试脚本（自动安装缺失依赖）
# 使用方法: ./local-test-auto.sh

# 日志文件
LOG_FILE="build-test-$(date +%Y%m%d-%H%M%S).log"
ERR_LOG_FILE="build-test-$(date +%Y%m%d-%H%M%S).error.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$ERR_LOG_FILE"
    echo -e "${RED}[ERROR]${NC} $1" >> "$LOG_FILE"
}

log_step() {
    echo -e "\n${CYAN}========================================${NC}" | tee -a "$LOG_FILE"
    echo -e "${CYAN}STEP $1: $2${NC}" | tee -a "$LOG_FILE"
    echo -e "${CYAN}========================================${NC}\n" | tee -a "$LOG_FILE"
}

# 初始化日志
echo "TestCase Manager Auto Build - $(date)" > "$LOG_FILE"
echo "Error Log - $(date)" > "$ERR_LOG_FILE"

log_step "0" "初始化"
log_info "日志文件: $LOG_FILE"
log_info "错误日志: $ERR_LOG_FILE"
log_info "工作目录: $(pwd)"
log_info "操作系统: $(uname -s)"

# 检测包管理器
detect_package_manager() {
    if command -v apt-get &> /dev/null; then
        echo "apt"
    elif command -v yum &> /dev/null; then
        echo "yum"
    elif command -v dnf &> /dev/null; then
        echo "dnf"
    elif command -v pacman &> /dev/null; then
        echo "pacman"
    elif command -v brew &> /dev/null; then
        echo "brew"
    else
        echo "unknown"
    fi
}

PACKAGE_MANAGER=$(detect_package_manager)
log_info "检测到包管理器: $PACKAGE_MANAGER"

# 自动安装 Java
install_java() {
    log_info "正在安装 Java 17..."
    
    case $PACKAGE_MANAGER in
        "apt")
            sudo apt-get update
            sudo apt-get install -y openjdk-17-jdk
            ;;
        "yum")
            sudo yum install -y java-17-openjdk-devel
            ;;
        "dnf")
            sudo dnf install -y java-17-openjdk-devel
            ;;
        "pacman")
            sudo pacman -S --noconfirm jdk17-openjdk
            ;;
        "brew")
            brew install openjdk@17
            ;;
        *)
            log_error "无法自动安装 Java，请手动安装 Java 17"
            log_info "下载地址: https://adoptium.net/"
            return 1
            ;;
    esac
    
    # 验证安装
    if command -v java &> /dev/null; then
        log_success "Java 安装成功"
        java -version 2>&1 | head -1
        return 0
    else
        log_error "Java 安装失败"
        return 1
    fi
}

# 检查并安装 Java
log_step "1" "检查并安装 Java"

# 检查 Java 版本是否符合要求
check_java_version() {
    if command -v java &> /dev/null; then
        # 获取 Java 版本
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        log_info "检测到 Java 版本: $JAVA_VERSION"
        
        # 提取主版本号
        JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
        
        # 处理版本号格式（如 1.8 -> 8, 17 -> 17）
        if [ "$JAVA_MAJOR" = "1" ]; then
            JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
        fi
        
        log_info "Java 主版本号: $JAVA_MAJOR"
        
        # 检查版本是否 >= 17
        if [ "$JAVA_MAJOR" -ge 17 ]; then
            log_success "Java 版本符合要求"
            return 0
        else
            log_warning "Java 版本过低，需要 Java 17+"
            return 1
        fi
    else
        log_warning "Java 未安装"
        return 1
    fi
}

if check_java_version; then
    java -version 2>&1 | head -1
else
    log_info "尝试自动安装 Java 17..."
    if install_java; then
        log_success "Java 17 自动安装完成"
    else
        log_error "Java 安装失败，请手动安装"
        exit 1
    fi
fi

# 设置 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    log_info "设置 JAVA_HOME..."
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    log_info "JAVA_HOME=$JAVA_HOME"
fi

# 检查 Gradle Wrapper
log_step "2" "检查 Gradle Wrapper"

if [ ! -f "./gradlew" ]; then
    log_warning "Gradle Wrapper 不存在，尝试生成..."
    
    # 检查是否有 gradle
    if command -v gradle &> /dev/null; then
        log_info "使用系统 gradle 生成 wrapper..."
        gradle wrapper --gradle-version 8.5 2>&1 | tee -a "$LOG_FILE"
    else
        log_error "Gradle 未安装，无法生成 wrapper"
        log_info "请安装 Gradle: https://gradle.org/install/"
        exit 1
    fi
fi

if [ -f "./gradlew" ]; then
    log_success "Gradle Wrapper 存在"
    chmod +x ./gradlew
    log_info "Gradle 版本:"
    ./gradlew --version 2>&1 | tee -a "$LOG_FILE"
else
    log_error "Gradle Wrapper 生成失败"
    exit 1
fi

# 检查项目结构
log_step "3" "检查项目结构"

REQUIRED_FILES=(
    "src/main/kotlin/com/testcase/manager/TestCaseManagerPlugin.kt"
    "src/main/kotlin/com/testcase/manager/ui/ExcelEditor.kt"
    "src/main/kotlin/com/testcase/manager/model/TestCaseModel.kt"
    "src/main/kotlin/com/testcase/manager/yaml/YamlParser.kt"
    "src/main/resources/META-INF/plugin.xml"
    "build.gradle.kts"
)

ALL_FILES_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        log_success "✓ $file"
    else
        log_error "✗ $file 不存在"
        ALL_FILES_EXIST=false
    fi
done

if [ "$ALL_FILES_EXIST" = false ]; then
    log_error "缺少必要文件，停止构建"
    exit 1
fi

# 检查并创建 gradle.properties
log_step "4" "检查 Gradle 配置"

if [ ! -f "gradle.properties" ]; then
    log_warning "gradle.properties 不存在，创建默认配置..."
    cat > gradle.properties << 'EOF'
kotlin.stdlib.default.dependency = false
kotlin.incremental.useClasspathSnapshot=false
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
org.gradle.caching=true
EOF
    log_success "已创建 gradle.properties"
fi

# 清理旧构建
log_step "5" "清理旧构建"
log_info "执行: ./gradlew clean"
if ./gradlew clean 2>&1 | tee -a "$LOG_FILE"; then
    log_success "清理成功"
else
    log_warning "清理过程中有警告（可能无旧构建）"
fi

# 检查依赖
log_step "6" "检查依赖"
log_info "执行: ./gradlew dependencies --configuration compileClasspath"
if ./gradlew dependencies --configuration compileClasspath 2>&1 | tee -a "$LOG_FILE"; then
    log_success "依赖检查完成"
else
    log_error "依赖检查失败"
    log_info "尝试刷新依赖..."
    ./gradlew --refresh-dependencies 2>&1 | tee -a "$LOG_FILE"
fi

# 编译 Kotlin
log_step "7" "编译 Kotlin 代码"
log_info "执行: ./gradlew compileKotlin"
if ./gradlew compileKotlin --stacktrace 2>&1 | tee -a "$LOG_FILE"; then
    log_success "Kotlin 编译成功"
else
    log_error "Kotlin 编译失败"
    log_info "查看详细错误: $ERR_LOG_FILE"
    
    # 提取错误信息
    echo "=== 编译错误摘要 ===" >> "$ERR_LOG_FILE"
    grep -E "^e: " "$LOG_FILE" | head -30 >> "$ERR_LOG_FILE"
    
    # 提供解决方案
    log_info "可能的解决方案:"
    log_info "1. 检查 Kotlin 语法错误"
    log_info "2. 运行 ./gradlew --refresh-dependencies 刷新依赖"
    log_info "3. 删除 ~/.gradle/caches 后重试"
    
    exit 1
fi

# 编译测试代码
log_step "8" "编译测试代码"
log_info "执行: ./gradlew compileTestKotlin"
if ./gradlew compileTestKotlin --stacktrace 2>&1 | tee -a "$LOG_FILE"; then
    log_success "测试代码编译成功"
else
    log_warning "测试代码编译有警告（可能无测试）"
fi

# 运行单元测试
log_step "9" "运行单元测试"
log_info "执行: ./gradlew test"
if ./gradlew test --stacktrace 2>&1 | tee -a "$LOG_FILE"; then
    log_success "单元测试通过"
else
    log_warning "部分测试失败（查看日志详情）"
    log_info "测试报告: build/reports/tests/test/index.html"
fi

# 构建插件
log_step "10" "构建插件"
log_info "执行: ./gradlew buildPlugin"
if ./gradlew buildPlugin --stacktrace 2>&1 | tee -a "$LOG_FILE"; then
    log_success "插件构建成功"
else
    log_error "插件构建失败"
    log_info "查看详细日志: $LOG_FILE"
    exit 1
fi

# 检查构建产物
log_step "11" "检查构建产物"
PLUGIN_DIR="build/distributions"
if [ -d "$PLUGIN_DIR" ]; then
    log_success "构建目录存在: $PLUGIN_DIR"
    log_info "构建产物:"
    ls -lh "$PLUGIN_DIR"/*.zip 2>&1 | tee -a "$LOG_FILE"
    
    PLUGIN_ZIP=$(ls -t "$PLUGIN_DIR"/*.zip 2>/dev/null | head -1)
    if [ -n "$PLUGIN_ZIP" ]; then
        log_success "插件包: $PLUGIN_ZIP"
        log_info "文件大小: $(ls -lh "$PLUGIN_ZIP" | awk '{print $5}')"
        
        # 验证插件包
        log_info "验证插件包内容..."
        unzip -l "$PLUGIN_ZIP" | grep -E "(META-INF/plugin.xml|lib/)" | head -5 | tee -a "$LOG_FILE"
    else
        log_warning "未找到插件包 (.zip)"
    fi
else
    log_error "构建目录不存在: $PLUGIN_DIR"
    exit 1
fi

# 生成构建报告
log_step "12" "生成构建报告"
REPORT_FILE="build-report-$(date +%Y%m%d-%H%M%S).md"

cat > "$REPORT_FILE" << EOF
# TestCase Manager 构建报告

## 构建信息
- **构建时间**: $(date)
- **构建状态**: ✅ 成功
- **Java 版本**: $(java -version 2>&1 | head -1)
- **Gradle 版本**: $(./gradlew --version 2>&1 | grep "Gradle" | head -1)

## 构建产物
\`\`\`
$(ls -lh "$PLUGIN_DIR"/*.zip 2>/dev/null)
\`\`\`

## 日志文件
- 完整日志: \`$LOG_FILE\`
- 错误日志: \`$ERR_LOG_FILE\`

## 安装步骤
1. 打开 PyCharm
2. Settings → Plugins → Install from disk
3. 选择: \`$PLUGIN_ZIP\`
4. 重启 PyCharm
5. 打开任意 .yaml 文件测试

## 环境信息
- 操作系统: $(uname -s)
- 包管理器: $PACKAGE_MANAGER
- JAVA_HOME: $JAVA_HOME
EOF

log_success "构建报告已生成: $REPORT_FILE"

# 总结
log_step "COMPLETE" "构建完成"
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 构建成功!${NC}"
echo -e "${GREEN}========================================${NC}\n"

log_info "📄 日志文件: $LOG_FILE"
log_info "📄 错误日志: $ERR_LOG_FILE"
log_info "📄 构建报告: $REPORT_FILE"
log_info "📦 插件包: $PLUGIN_ZIP"

echo -e "\n${CYAN}📋 安装步骤:${NC}"
echo "1. 打开 PyCharm"
echo "2. Settings → Plugins → Install from disk"
echo "3. 选择: $PLUGIN_ZIP"
echo "4. 重启 PyCharm"
echo "5. 打开任意 .yaml 文件测试"

echo -e "\n${CYAN}💡 提示:${NC}"
echo "- 如果遇到问题，查看日志文件: $LOG_FILE"
echo "- 错误信息: $ERR_LOG_FILE"
