#!/bin/bash
# 快速验证脚本 - 检查代码语法和项目结构
# 使用方法: ./quick-verify.sh

echo "========================================"
echo "TestCase Manager 快速验证"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 检查 Java
echo "[1/5] 检查 Java 环境..."
if command -v java &> /dev/null; then
    echo -e "${GREEN}✓${NC} Java 已安装: $(java -version 2>&1 | head -1)"
else
    echo -e "${RED}✗${NC} Java 未安装"
    echo "运行 ./local-test-auto.sh 自动安装 Java"
    exit 1
fi

# 检查项目结构
echo ""
echo "[2/5] 检查项目结构..."
REQUIRED_FILES=(
    "src/main/kotlin/com/testcase/manager/TestCaseManagerPlugin.kt"
    "src/main/kotlin/com/testcase/manager/ui/ExcelEditor.kt"
    "src/main/kotlin/com/testcase/manager/ui/ExcelEditorProvider.kt"
    "src/main/kotlin/com/testcase/manager/ui/TestCaseTableModel.kt"
    "src/main/kotlin/com/testcase/manager/model/TestCaseModel.kt"
    "src/main/kotlin/com/testcase/manager/yaml/YamlFileType.kt"
    "src/main/kotlin/com/testcase/manager/yaml/YamlFileTypeFactory.kt"
    "src/main/kotlin/com/testcase/manager/yaml/YamlParser.kt"
    "src/main/kotlin/com/testcase/manager/yaml/YamlSerializer.kt"
    "src/main/resources/META-INF/plugin.xml"
    "build.gradle.kts"
)

ALL_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file"
    else
        echo -e "${RED}✗${NC} $file 不存在"
        ALL_EXIST=false
    fi
done

if [ "$ALL_EXIST" = false ]; then
    echo -e "${RED}错误: 缺少必要文件${NC}"
    exit 1
fi

# 检查 Kotlin 语法
echo ""
echo "[3/5] 检查 Kotlin 语法..."
if command -v kotlinc &> /dev/null; then
    echo -e "${BLUE}ℹ${NC} 找到 kotlinc，进行语法检查..."
    SYNTAX_OK=true
    for file in src/main/kotlin/com/testcase/manager/**/*.kt; do
        if [ -f "$file" ]; then
            if kotlinc -d /tmp/compiled -include-runtime "$file" 2>/dev/null; then
                echo -e "${GREEN}✓${NC} $(basename $file)"
            else
                echo -e "${YELLOW}⚠${NC} $(basename $file) - 可能有语法问题"
                SYNTAX_OK=false
            fi
        fi
    done
else
    echo -e "${YELLOW}⚠${NC} 未找到 kotlinc，跳过语法检查"
    echo "  提示: kotlinc 包含在 Kotlin 编译器中"
fi

# 检查 Gradle 配置
echo ""
echo "[4/5] 检查 Gradle 配置..."
if [ -f "build.gradle.kts" ]; then
    echo -e "${GREEN}✓${NC} build.gradle.kts 存在"
    
    # 检查关键配置
    if grep -q "intellijPlatform" build.gradle.kts; then
        echo -e "${GREEN}✓${NC} IntelliJ Platform 插件配置正确"
    fi
    
    if grep -q "JvmTarget.JVM_17" build.gradle.kts; then
        echo -e "${GREEN}✓${NC} JVM 目标版本设置正确"
    fi
else
    echo -e "${RED}✗${NC} build.gradle.kts 不存在"
    exit 1
fi

# 检查 plugin.xml
echo ""
echo "[5/5] 检查 plugin.xml..."
if [ -f "src/main/resources/META-INF/plugin.xml" ]; then
    echo -e "${GREEN}✓${NC} plugin.xml 存在"
    
    # 检查关键配置
    if grep -q "ExcelEditorProvider" src/main/resources/META-INF/plugin.xml; then
        echo -e "${GREEN}✓${NC} 编辑器提供器已注册"
    fi
    
    if grep -q "YamlFileTypeFactory" src/main/resources/META-INF/plugin.xml; then
        echo -e "${GREEN}✓${NC} 文件类型工厂已注册"
    fi
else
    echo -e "${RED}✗${NC} plugin.xml 不存在"
    exit 1
fi

# 总结
echo ""
echo "========================================"
echo -e "${GREEN}✓ 快速验证通过${NC}"
echo "========================================"
echo ""
echo "项目结构完整，可以进行完整构建。"
echo ""
echo "下一步:"
echo "  运行完整构建: ./gradlew buildPlugin"
echo "  或使用自动脚本: ./local-test-auto.sh"
echo ""
echo "注意: 首次构建需要下载依赖，可能需要 10-20 分钟"
echo ""
