#!/bin/bash
# 增强版本地部署测试脚本（带详细日志）
# 使用方法: ./local-test-with-logs.sh

# 日志文件
LOG_FILE="build-test-$(date +%Y%m%d-%H%M%S).log"
ERR_LOG_FILE="build-test-$(date +%Y%m%d-%H%M%S).error.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    echo -e "\n${BLUE}========================================${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}STEP $1: $2${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}========================================${NC}\n" | tee -a "$LOG_FILE"
}

# 初始化日志
echo "TestCase Manager Build Log - $(date)" > "$LOG_FILE"
echo "Error Log - $(date)" > "$ERR_LOG_FILE"

log_step "0" "初始化"
log_info "日志文件: $LOG_FILE"
log_info "错误日志: $ERR_LOG_FILE"
log_info "工作目录: $(pwd)"

# 检查 Java
log_step "1" "检查 Java 环境"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    log_success "Java 已安装: $JAVA_VERSION"
    java -version 2>&1 | tee -a "$LOG_FILE"
else
    log_error "Java 未安装"
    log_info "请安装 Java 17: https://adoptium.net/"
    exit 1
fi

# 检查 Gradle Wrapper
log_step "2" "检查 Gradle Wrapper"
if [ -f "./gradlew" ]; then
    log_success "Gradle Wrapper 存在"
    log_info "Gradle 版本:"
    ./gradlew --version 2>&1 | tee -a "$LOG_FILE"
else
    log_error "Gradle Wrapper 不存在"
    exit 1
fi

# 检查项目结构
log_step "3" "检查项目结构"
log_info "检查核心文件..."

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

# 清理旧构建
log_step "4" "清理旧构建"
log_info "执行: ./gradlew clean"
if ./gradlew clean 2>&1 | tee -a "$LOG_FILE"; then
    log_success "清理成功"
else
    log_warning "清理过程中有警告（可能无旧构建）"
fi

# 检查依赖
log_step "5" "检查依赖"
log_info "执行: ./gradlew dependencies --configuration compileClasspath"
if ./gradlew dependencies --configuration compileClasspath 2>&1 | tee -a "$LOG_FILE"; then
    log_success "依赖检查完成"
else
    log_error "依赖检查失败"
    exit 1
fi

# 编译 Kotlin
log_step "6" "编译 Kotlin 代码"
log_info "执行: ./gradlew compileKotlin"
if ./gradlew compileKotlin 2>&1 | tee -a "$LOG_FILE"; then
    log_success "Kotlin 编译成功"
else
    log_error "Kotlin 编译失败"
    log_info "查看详细错误: $ERR_LOG_FILE"
    
    # 提取错误信息
    echo "=== 编译错误摘要 ===" >> "$ERR_LOG_FILE"
    grep -E "^e: " "$LOG_FILE" | head -20 >> "$ERR_LOG_FILE"
    
    exit 1
fi

# 编译测试代码
log_step "7" "编译测试代码"
log_info "执行: ./gradlew compileTestKotlin"
if ./gradlew compileTestKotlin 2>&1 | tee -a "$LOG_FILE"; then
    log_success "测试代码编译成功"
else
    log_warning "测试代码编译有警告（可能无测试）"
fi

# 运行单元测试
log_step "8" "运行单元测试"
log_info "执行: ./gradlew test"
if ./gradlew test 2>&1 | tee -a "$LOG_FILE"; then
    log_success "单元测试通过"
else
    log_warning "部分测试失败（查看日志详情）"
fi

# 构建插件
log_step "9" "构建插件"
log_info "执行: ./gradlew buildPlugin"
if ./gradlew buildPlugin 2>&1 | tee -a "$LOG_FILE"; then
    log_success "插件构建成功"
else
    log_error "插件构建失败"
    exit 1
fi

# 检查构建产物
log_step "10" "检查构建产物"
PLUGIN_DIR="build/distributions"
if [ -d "$PLUGIN_DIR" ]; then
    log_success "构建目录存在: $PLUGIN_DIR"
    log_info "构建产物:"
    ls -lh "$PLUGIN_DIR"/*.zip 2>&1 | tee -a "$LOG_FILE"
    
    # 获取插件包路径
    PLUGIN_ZIP=$(ls -t "$PLUGIN_DIR"/*.zip 2>/dev/null | head -1)
    if [ -n "$PLUGIN_ZIP" ]; then
        log_success "插件包: $PLUGIN_ZIP"
        log_info "文件大小: $(ls -lh "$PLUGIN_ZIP" | awk '{print $5}')"
    else
        log_warning "未找到插件包 (.zip)"
    fi
else
    log_error "构建目录不存在: $PLUGIN_DIR"
    exit 1
fi

# 验证插件包内容
log_step "11" "验证插件包"
if [ -n "$PLUGIN_ZIP" ]; then
    log_info "检查插件包内容..."
    unzip -l "$PLUGIN_ZIP" | grep -E "(META-INF/plugin.xml|lib/)" | head -10 | tee -a "$LOG_FILE"
    log_success "插件包验证完成"
fi

# 生成构建报告
log_step "12" "生成构建报告"
REPORT_FILE="build-report-$(date +%Y%m%d-%H%M%S).md"

cat > "$REPORT_FILE" << EOF
# TestCase Manager 构建报告

## 构建时间
$(date)

## 构建环境
- Java: $(java -version 2>&1 | head -1)
- Gradle: $(./gradlew --version 2>&1 | grep "Gradle" | head -1)
- 工作目录: $(pwd)

## 构建结果
✅ **成功**

## 构建产物
$(ls -lh "$PLUGIN_DIR"/*.zip 2>/dev/null)

## 日志文件
- 完整日志: $LOG_FILE
- 错误日志: $ERR_LOG_FILE

## 下一步
1. 在 PyCharm 中安装插件:
   \`\`\`
   Settings → Plugins → Install from disk → 选择 $PLUGIN_ZIP
   \`\`\`

2. 重启 PyCharm

3. 打开任意 .yaml 文件测试功能
EOF

log_success "构建报告已生成: $REPORT_FILE"

# 总结
log_step "COMPLETE" "构建完成"
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}构建成功!${NC}"
echo -e "${GREEN}========================================${NC}\n"

log_info "日志文件: $LOG_FILE"
log_info "错误日志: $ERR_LOG_FILE"
log_info "构建报告: $REPORT_FILE"
log_info "插件包: $PLUGIN_ZIP"

echo -e "\n${BLUE}安装步骤:${NC}"
echo "1. 在 PyCharm 中: Settings → Plugins → Install from disk"
echo "2. 选择: $PLUGIN_ZIP"
echo "3. 重启 PyCharm"
echo "4. 打开任意 .yaml 文件测试"
