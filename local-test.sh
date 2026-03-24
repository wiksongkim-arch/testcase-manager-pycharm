#!/bin/bash
# 本地部署测试脚本
# 使用方法: ./local-test.sh

echo "================================"
echo "TestCase Manager 本地部署测试"
echo "================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Java
echo "[1/6] 检查 Java 环境..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo -e "${GREEN}✓${NC} Java 版本: $JAVA_VERSION"
else
    echo -e "${RED}✗${NC} Java 未安装，请先安装 Java 17"
    exit 1
fi

# 检查 Gradle
echo ""
echo "[2/6] 检查 Gradle..."
if [ -f "./gradlew" ]; then
    echo -e "${GREEN}✓${NC} Gradle Wrapper 存在"
else
    echo -e "${RED}✗${NC} Gradle Wrapper 不存在"
    exit 1
fi

# 清理构建
echo ""
echo "[3/6] 清理旧构建..."
./gradlew clean
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 清理成功"
else
    echo -e "${RED}✗${NC} 清理失败"
    exit 1
fi

# 编译项目
echo ""
echo "[4/6] 编译项目..."
./gradlew compileKotlin
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 编译成功"
else
    echo -e "${RED}✗${NC} 编译失败，请检查错误信息"
    exit 1
fi

# 运行单元测试
echo ""
echo "[5/6] 运行单元测试..."
./gradlew test
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 单元测试通过"
else
    echo -e "${YELLOW}⚠${NC} 部分测试失败，请检查测试报告"
fi

# 构建插件
echo ""
echo "[6/6] 构建插件..."
./gradlew buildPlugin
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 插件构建成功"
    echo ""
    echo "插件位置: build/distributions/"
    ls -lh build/distributions/*.zip 2>/dev/null || echo "未找到插件包"
else
    echo -e "${RED}✗${NC} 插件构建失败"
    exit 1
fi

echo ""
echo "================================"
echo "部署测试完成!"
echo "================================"
echo ""
echo "下一步:"
echo "1. 在 PyCharm 中安装插件:"
echo "   Settings → Plugins → Install from disk → 选择 build/distributions/*.zip"
echo ""
echo "2. 重启 PyCharm"
echo ""
echo "3. 打开任意 .yaml 文件测试功能"
echo ""
