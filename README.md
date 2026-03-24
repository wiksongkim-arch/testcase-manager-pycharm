# TestCase Manager for PyCharm

<p align="center">
  <b>为 PyCharm 提供 Excel 风格的测试用例编辑体验</b><br>
  <b>Excel-style Test Case Editor for PyCharm</b>
</p>

<p align="center">
  <a href="#中文文档">中文</a> | <a href="#english-documentation">English</a>
</p>

---

<a name="中文文档"></a>
## 📖 中文文档

### 🎯 项目简介

TestCase Manager 是一个 PyCharm 插件，为测试用例 YAML 文件提供 Excel 风格的编辑界面。支持双向同步、Git 集成、公式计算等高级功能。

### ✨ 核心功能

| 功能 | 描述 |
|------|------|
| 📊 **Excel 编辑器** | 直观的表格界面编辑测试用例 |
| 📝 **YAML 双向同步** | 表格与 YAML 文件实时同步 |
| 📁 **Git 集成** | 状态检测、Diff 对比、冲突解决 |
| 🔍 **筛选排序** | 按优先级、状态、文本筛选和排序 |
| 🎨 **单元格格式化** | 颜色、字体、边框、条件格式 |
| 🧮 **公式支持** | SUM、COUNT、IF、CONCAT 等函数 |
| ⚡ **性能优化** | 虚拟滚动支持大数据量 |

### 🚀 快速开始

#### 环境要求
- PyCharm 2023.3.5 或更高版本
- Java 17（脚本会自动检测并安装）

#### 安装步骤（推荐 - 自动安装依赖）

**智能安装脚本（自动安装缺失依赖）**

```bash
# Linux/Mac - 自动检测并安装 Java
./local-test-auto.sh

# Windows - 自动检测并安装 Java
local-test-auto.bat
```

脚本会自动：
- 检测 Java 环境，未安装则自动安装
- 检测 Gradle Wrapper，不存在则自动生成
- 创建默认的 gradle.properties
- 执行完整的构建流程
- 生成详细的日志和报告

#### 手动安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/wiksongkim-arch/testcase-manager-pycharm.git
cd testcase-manager-pycharm

# 2. 确保已安装 Java 17
java -version

# 3. 运行构建脚本
./local-test.sh          # Linux/Mac
# 或
local-test.bat           # Windows

# 4. 在 PyCharm 中安装插件
# Settings → Plugins → Install from disk → 选择 build/distributions/*.zip
```
# 或
local-test.bat           # Windows

# 3. 在 PyCharm 中安装插件
# Settings → Plugins → Install from disk → 选择 build/distributions/*.zip
```

**方式二：直接安装**（待发布到 JetBrains 插件市场）

```
Settings → Plugins → Marketplace → 搜索 "TestCase Manager"
```

#### 使用方法

1. **打开 YAML 文件**：双击任意 `.yaml` 或 `.yml` 文件
2. **编辑测试用例**：在表格中直接编辑单元格
3. **下拉选择**：优先级和状态列支持下拉选择
4. **保存文件**：按 `Ctrl+S` 自动同步到 YAML
5. **Git 操作**：右键菜单提供 Git 对比、历史查看等功能

### 📋 YAML 格式

插件支持标准的 pytest YAML 测试数据格式：

```yaml
test_cases:
  - id: TC001
    name: 登录成功测试
    priority: P0
    status: PUBLISHED
    steps:
      - 打开登录页面
      - 输入用户名密码
      - 点击登录按钮
    expected: 登录成功，跳转到首页
    tags:
      - login
      - smoke
```

**字段映射**：

| Excel 列 | YAML 字段 | 类型 | 说明 |
|---------|----------|------|------|
| ID | id | String | 唯一标识符 |
| 用例名称 | name | String | 测试用例名称 |
| 优先级 | priority | Enum | P0/P1/P2/P3 |
| 状态 | status | Enum | 草稿/已发布/已归档/已禁用 |
| 测试步骤 | steps | List | 步骤列表 |
| 预期结果 | expected | String | 预期结果 |
| 标签 | tags | List | 标签列表 |

### 🛠️ 开发架构

```
testcase-manager-pycharm/
├── src/main/kotlin/com/testcase/manager/
│   ├── TestCaseManagerPlugin.kt      # 插件入口
│   ├── formula/                      # 公式引擎
│   ├── git/                          # Git 集成
│   ├── model/                        # 数据模型
│   ├── performance/                  # 性能优化
│   ├── sync/                         # 同步管理
│   ├── ui/                           # 用户界面
│   └── yaml/                         # YAML 处理
└── src/test/kotlin/                  # 单元测试
```

### 🧪 测试

```bash
# 运行单元测试
./gradlew test

# 运行插件（开发模式）
./gradlew runIde
```

详细测试清单见 [FUNCTION_TEST.md](FUNCTION_TEST.md)

### 📚 文档

- [设计文档](docs/testcase-manager-pycharm-design.md)
- [功能测试清单](FUNCTION_TEST.md)
- [代码审查清单](BUG_CHECKLIST.md)

### 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 📄 许可证

MIT License

---

<a name="english-documentation"></a>
## 📖 English Documentation

### 🎯 Introduction

TestCase Manager is a PyCharm plugin that provides an Excel-style editing interface for test case YAML files. It supports bidirectional synchronization, Git integration, formula calculation, and other advanced features.

### ✨ Features

| Feature | Description |
|---------|-------------|
| 📊 **Excel Editor** | Intuitive table interface for editing test cases |
| 📝 **YAML Sync** | Real-time synchronization between table and YAML |
| 📁 **Git Integration** | Status detection, Diff comparison, conflict resolution |
| 🔍 **Filter & Sort** | Filter and sort by priority, status, text |
| 🎨 **Cell Formatting** | Colors, fonts, borders, conditional formatting |
| 🧮 **Formula Support** | SUM, COUNT, IF, CONCAT functions |
| ⚡ **Performance** | Virtual scrolling for large datasets |

### 🚀 Quick Start

#### Requirements
- PyCharm 2023.3.5 or higher
- Java 17
- Git (optional, for Git integration features)

#### Installation

**Option 1: Build from Source**

```bash
# 1. Clone the project
git clone https://github.com/wiksongkim-arch/testcase-manager-pycharm.git
cd testcase-manager-pycharm

# 2. Run test script
./local-test.sh          # Linux/Mac
# or
local-test.bat           # Windows

# 3. Install plugin in PyCharm
# Settings → Plugins → Install from disk → Select build/distributions/*.zip
```

**Option 2: Install from Marketplace** (Coming soon)

```
Settings → Plugins → Marketplace → Search "TestCase Manager"
```

#### Usage

1. **Open YAML file**: Double-click any `.yaml` or `.yml` file
2. **Edit test cases**: Edit cells directly in the table
3. **Dropdown selection**: Priority and status columns support dropdown
4. **Save file**: Press `Ctrl+S` to sync to YAML automatically
5. **Git operations**: Right-click menu provides Git compare, history, etc.

### 📋 YAML Format

The plugin supports standard pytest YAML test data format:

```yaml
test_cases:
  - id: TC001
    name: Login Success Test
    priority: P0
    status: PUBLISHED
    steps:
      - Open login page
      - Enter username and password
      - Click login button
    expected: Login successful, redirect to home page
    tags:
      - login
      - smoke
```

**Field Mapping**:

| Excel Column | YAML Field | Type | Description |
|-------------|-----------|------|-------------|
| ID | id | String | Unique identifier |
| Name | name | String | Test case name |
| Priority | priority | Enum | P0/P1/P2/P3 |
| Status | status | Enum | DRAFT/PUBLISHED/ARCHIVED/DISABLED |
| Steps | steps | List | Step list |
| Expected | expected | String | Expected result |
| Tags | tags | List | Tag list |

### 🛠️ Architecture

```
testcase-manager-pycharm/
├── src/main/kotlin/com/testcase/manager/
│   ├── TestCaseManagerPlugin.kt      # Plugin entry
│   ├── formula/                      # Formula engine
│   ├── git/                          # Git integration
│   ├── model/                        # Data models
│   ├── performance/                  # Performance optimization
│   ├── sync/                         # Sync management
│   ├── ui/                           # User interface
│   └── yaml/                         # YAML processing
└── src/test/kotlin/                  # Unit tests
```

### 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run plugin (development mode)
./gradlew runIde
```

See [FUNCTION_TEST.md](FUNCTION_TEST.md) for detailed test checklist.

### 📚 Documentation

- [Design Document](docs/testcase-manager-pycharm-design.md)
- [Function Test Checklist](FUNCTION_TEST.md)
- [Code Review Checklist](BUG_CHECKLIST.md)

### 🤝 Contributing

Issues and Pull Requests are welcome!

### 📄 License

MIT License

---

## 📊 Project Statistics

| Metric | Value |
|--------|-------|
| **Code Files** | 60+ Kotlin files |
| **Test Files** | 12+ test classes |
| **Total Lines** | ~15,000 lines |
| **Git Commits** | 15+ commits |
| **Phases** | 4 completed |

## 🏗️ Development Phases

| Phase | Content | Status |
|-------|---------|--------|
| Phase 1 | Basic Framework | ✅ Complete |
| Phase 2 | Excel Editor + YAML | ✅ Complete |
| Phase 3 | Git Integration | ✅ Complete |
| Phase 4 | Advanced Features | ✅ Complete |

## 📞 Contact

- **GitHub**: https://github.com/wiksongkim-arch/testcase-manager-pycharm
- **Issues**: [GitHub Issues](https://github.com/wiksongkim-arch/testcase-manager-pycharm/issues)

---

<p align="center">
  Made with ❤️ for Test Engineers
</p>
