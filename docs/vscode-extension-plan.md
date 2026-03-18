# VS Code 插件实施计划 - 阶段 5

> **任务:** 为 TestCase Manager 构建 VS Code 插件，提供与 Web 平台一致的测试用例管理体验。

## 目标

创建一个 VS Code 插件，允许用户在 VS Code 中直接管理测试用例，无需切换到浏览器。插件将复用 Web 前端的核心组件，通过 Webview 实现类 Excel 的表格编辑体验。

---

## 任务清单

### 任务 5.1: 创建 VS Code 插件基础结构

**文件:**
- 创建: `apps/vscode-extension/package.json`
- 创建: `apps/vscode-extension/tsconfig.json`
- 创建: `apps/vscode-extension/.vscodeignore`
- 创建: `apps/vscode-extension/README.md`

#### 步骤 1: 创建 package.json

```json
{
  "name": "testcase-manager",
  "displayName": "TestCase Manager",
  "description": "测试用例管理工具 - 类 Excel 编辑体验 + Git 版本控制",
  "version": "0.1.0",
  "publisher": "testcase-manager",
  "engines": {
    "vscode": "^1.74.0"
  },
  "categories": [
    "Other",
    "Testing"
  ],
  "keywords": [
    "testcase",
    "testing",
    "excel",
    "git"
  ],
  "activationEvents": [
    "onCommand:testcase-manager.open"
  ],
  "main": "./dist/extension.js",
  "contributes": {
    "commands": [
      {
        "command": "testcase-manager.open",
        "title": "Open TestCase Manager",
        "category": "TestCase Manager"
      },
      {
        "command": "testcase-manager.refresh",
        "title": "Refresh Projects",
        "category": "TestCase Manager",
        "icon": "$(refresh)"
      },
      {
        "command": "testcase-manager.clone",
        "title": "Clone Repository",
        "category": "TestCase Manager"
      }
    ],
    "views": {
      "explorer": [
        {
          "id": "testcaseManagerProjects",
          "name": "TestCase Projects",
          "when": "testcase-manager.enabled"
        }
      ]
    },
    "menus": {
      "view/title": [
        {
          "command": "testcase-manager.refresh",
          "when": "view == testcaseManagerProjects",
          "group": "navigation"
        }
      ],
      "commandPalette": [
        {
          "command": "testcase-manager.open"
        },
        {
          "command": "testcase-manager.clone"
        }
      ]
    },
    "configuration": {
      "title": "TestCase Manager",
      "properties": {
        "testcaseManager.apiUrl": {
          "type": "string",
          "default": "http://localhost:3001",
          "description": "TestCase Manager API 服务地址"
        },
        "testcaseManager.defaultAuthor": {
          "type": "string",
          "default": "",
          "description": "默认提交作者信息 (格式: Name <email>)"
        }
      }
    }
  },
  "scripts": {
    "vscode:prepublish": "npm run build",
    "build": "tsc",
    "dev": "tsc --watch",
    "lint": "eslint src/",
    "package": "vsce package"
  },
  "dependencies": {
    "@testcase/shared": "0.1.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "@types/vscode": "^1.74.0",
    "@vscode/vsce": "^2.22.0",
    "typescript": "^5.3.0"
  }
}
```

#### 步骤 2: 创建 tsconfig.json

```json
{
  "compilerOptions": {
    "module": "commonjs",
    "target": "ES2020",
    "lib": ["ES2020"],
    "outDir": "dist",
    "rootDir": "src",
    "sourceMap": true,
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true
  },
  "exclude": ["node_modules", "dist"]
}
```

#### 步骤 3: 创建 .vscodeignore

```
src/
node_modules/
.gitignore
tsconfig.json
*.vsix
.vscode/
```

#### 步骤 4: 创建 README.md

```markdown
# TestCase Manager for VS Code

在 VS Code 中管理测试用例，提供类 Excel 的编辑体验和 Git 版本控制。

## 功能

- 类 Excel 的表格编辑
- Git 版本控制集成
- 冲突解决
- 项目管理

## 使用方法

1. 按 `Ctrl+Shift+P` 打开命令面板
2. 输入 "TestCase Manager: Open" 打开管理器

## 配置

- `testcaseManager.apiUrl`: API 服务地址 (默认: http://localhost:3001)
- `testcaseManager.defaultAuthor`: 默认 Git 提交作者
```

---

### 任务 5.2: 实现插件核心逻辑

**文件:**
- 创建: `apps/vscode-extension/src/extension.ts`
- 创建: `apps/vscode-extension/src/commands.ts`
- 创建: `apps/vscode-extension/src/webview/panel.ts`
- 创建: `apps/vscode-extension/src/webview/content.ts`
- 创建: `apps/vscode-extension/src/api/client.ts`

---

### 任务 5.3: 实现项目树视图

**文件:**
- 创建: `apps/vscode-extension/src/treeView/projectsProvider.ts`
- 创建: `apps/vscode-extension/src/treeView/projectItem.ts`

---

## 提交

```bash
git add .
git commit -m "feat(vscode-extension): implement VS Code extension with webview and tree view"
```
