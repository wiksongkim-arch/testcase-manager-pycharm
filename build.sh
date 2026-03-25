#!/bin/bash
# TestCase Manager 构建脚本
# 使用方法: ./build.sh

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "========================================"
echo "TestCase Manager 构建脚本"
echo "========================================"
echo ""

# 检查 Java
log_info "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    log_error "Java 未安装"
    echo "请安装 Java 17: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
log_info "Java 版本: $JAVA_VERSION"

# 提取主版本号
JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
if [ "$JAVA_MAJOR" = "1" ]; then
    JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f2)
fi

if [ "$JAVA_MAJOR" -lt 17 ]; then
    log_error "Java 版本过低: $JAVA_VERSION"
    log_info "需要 Java 17 或更高版本"
    echo "请安装 Java 17: https://adoptium.net/"
    exit 1
fi

log_success "Java 版本符合要求"

# 检查 Gradle Wrapper
log_info "检查 Gradle Wrapper..."
if [ ! -f "./gradlew" ]; then
    log_error "Gradle Wrapper 不存在"
    echo "请运行: gradle wrapper"
    exit 1
fi

log_success "Gradle Wrapper 存在"

# 清理旧构建
log_info "清理旧构建..."
./gradlew clean --quiet || true

# 编译
log_info "编译项目..."
if ./gradlew compileKotlin --quiet; then
    log_success "编译成功"
else
    log_error "编译失败"
    echo "请检查错误信息 above"
    exit 1
fi

# 构建插件
log_info "构建插件..."
if ./gradlew buildPlugin --quiet; then
    log_success "构建成功"
else
    log_error "构建失败"
    exit 1
fi

# 检查构建产物
if [ -f "build/distributions"/*.zip ]; then
    PLUGIN_FILE=$(ls -t build/distributions/*.zip | head -1)
    log_success "插件包: $PLUGIN_FILE"
    ls -lh "$PLUGIN_FILE"
else
    log_error "未找到插件包"
    exit 1
fi

echo ""
echo "========================================"
echo "构建完成!"
echo "========================================"
echo ""
echo "安装步骤:"
echo "1. 打开 PyCharm"
echo "2. Settings → Plugins → Install from disk"
echo "3. 选择: $PLUGIN_FILE"
echo "4. 重启 PyCharm"
echo ""
