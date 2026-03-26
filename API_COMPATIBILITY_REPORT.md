# IntelliJ Platform 213 版本 API 兼容性检查报告

## 项目信息
- **目标版本**: 2021.3.3 (build 213)
- **检查时间**: 2026-03-26
- **检查范围**: 5 个核心文件

---

## 1. ExcelEditor.kt - FileEditor 接口 ✅ 兼容

### 使用的 API
| API | 类型 | 213 兼容性 | 说明 |
|-----|------|-----------|------|
| `FileEditor` | 接口 | ✅ 可用 | 核心接口，213 完全支持 |
| `FileEditorLocation` | 类 | ✅ 可用 | 标准 API |
| `FileEditorState` | 接口 | ✅ 可用 | 标准 API |
| `FileEditorStateLevel` | 枚举 | ✅ 可用 | 标准 API |
| `Project` | 类 | ✅ 可用 | 核心 API |
| `Disposer` | 类 | ✅ 可用 | 核心 API |
| `Key<T>` | 类 | ✅ 可用 | 核心 API |
| `VirtualFile` | 接口 | ✅ 可用 | 核心 API |
| `JBScrollPane` | 类 | ✅ 可用 | UI 组件 |
| `JBTable` | 类 | ✅ 可用 | UI 组件 |
| `VfsUtil.saveText()` | 方法 | ✅ 可用 | 静态工具方法 |
| `Logger.getInstance()` | 方法 | ✅ 可用 | 日志 API |

### 检查结果
**完全兼容**。所有使用的 API 在 213 版本中均可用。

---

## 2. ExcelEditorProvider.kt - FileEditorProvider ✅ 兼容

### 使用的 API
| API | 类型 | 213 兼容性 | 说明 |
|-----|------|-----------|------|
| `FileEditorProvider` | 接口 | ✅ 可用 | 核心接口 |
| `FileEditorPolicy` | 枚举 | ✅ 可用 | 标准 API |
| `FileEditor` | 接口 | ✅ 可用 | 标准 API |
| `VirtualFile.fileType` | 属性 | ✅ 可用 | 标准 API |
| `VirtualFile.extension` | 属性 | ✅ 可用 | 标准 API |

### 检查结果
**完全兼容**。所有使用的 API 在 213 版本中均可用。

---

## 3. YamlFileType.kt - FileType 接口 ✅ 兼容

### 使用的 API
| API | 类型 | 213 兼容性 | 说明 |
|-----|------|-----------|------|
| `FileType` | 接口 | ✅ 可用 | 核心接口 |
| `IconLoader.getIcon()` | 方法 | ✅ 可用 | 图标加载 API |
| `VirtualFile` | 接口 | ✅ 可用 | 核心 API |

### 检查结果
**完全兼容**。所有使用的 API 在 213 版本中均可用。

---

## 4. YamlFileTypeFactory.kt - FileTypeFactory ⚠️ 已弃用

### 使用的 API
| API | 类型 | 213 兼容性 | 说明 |
|-----|------|-----------|------|
| `FileTypeFactory` | 抽象类 | ⚠️ 已弃用 | 213 仍可用，但建议使用 `FileType` + `plugin.xml` 注册 |
| `FileTypeConsumer` | 接口 | ⚠️ 已弃用 | 213 仍可用 |

### 问题说明
在 213 版本中，`FileTypeFactory` 仍然可用，但已被标记为弃用。从 2019.2 版本开始，推荐使用 `FileType` 接口配合 `plugin.xml` 中的 `<fileType>` 扩展点来注册文件类型。

### 建议修改
无需修改，213 版本完全支持。但未来版本升级时需要注意。

---

## 5. YamlParser.kt - VirtualFile API ✅ 兼容

### 使用的 API
| API | 类型 | 213 兼容性 | 说明 |
|-----|------|-----------|------|
| `VirtualFile` | 接口 | ✅ 可用 | 核心 API |
| `VirtualFile.contentsToByteArray()` | 方法 | ✅ 可用 | 标准 API |
| `VirtualFile.path` | 属性 | ✅ 可用 | 标准 API |
| `Logger.getInstance()` | 方法 | ✅ 可用 | 日志 API |

### 检查结果
**完全兼容**。所有使用的 API 在 213 版本中均可用。

---

## 6. TestCaseManagerPlugin.kt - StartupActivity ⚠️ 需要修改

### 使用的 API
| API | 类型 | 213 兼容性 | 说明 |
|-----|------|-----------|------|
| `StartupActivity` | 接口 | ✅ 可用 | 213 支持，但建议使用 `ProjectActivity` |
| `Project` | 类 | ✅ 可用 | 核心 API |
| `Logger.getInstance()` | 方法 | ✅ 可用 | 日志 API |

### 问题说明
`StartupActivity` 接口在 213 版本中**完全可用**，但需要注意：

1. **当前代码**:
   ```kotlin
   class TestCaseManagerPlugin : StartupActivity {
       override fun runActivity(project: Project) { ... }
   }
   ```

2. **213 版本兼容性**:
   - `StartupActivity` 在 213 中完全支持
   - `runActivity(Project)` 方法签名正确
   - 但 `StartupActivity` 在 2023.1 之后被标记为弃用，建议使用 `ProjectActivity`

### plugin.xml 配置问题
当前 `plugin.xml` 使用：
```xml
<startupActivity implementation="com.testcase.manager.TestCaseManagerPlugin"/>
```

在 213 版本中，应该使用 `postStartupActivity`：
```xml
<postStartupActivity implementation="com.testcase.manager.TestCaseManagerPlugin"/>
```

### 建议修改
1. 将 `plugin.xml` 中的 `<startupActivity>` 改为 `<postStartupActivity>`
2. 或者保持现状，因为 213 版本也支持 `startupActivity`

---

## 7. plugin.xml 配置检查

需要检查 `plugin.xml` 中注册的配置是否正确：

### 必需配置
```xml
<!-- FileEditorProvider 注册 -->
<fileEditorProvider 
    implementation="com.testcase.manager.ui.ExcelEditorProvider"/>

<!-- FileTypeFactory 注册 (213 支持) -->
<fileTypeFactory 
    implementation="com.testcase.manager.yaml.YamlFileTypeFactory"/>

<!-- StartupActivity 注册 -->
<postStartupActivity 
    implementation="com.testcase.manager.TestCaseManagerPlugin"/>
```

---

## 总结

| 文件 | 兼容性 | 需要修改 | 备注 |
|------|--------|----------|------|
| ExcelEditor.kt | ✅ | 否 | 完全兼容 |
| ExcelEditorProvider.kt | ✅ | 否 | 完全兼容 |
| YamlFileType.kt | ✅ | 否 | 完全兼容 |
| YamlFileTypeFactory.kt | ⚠️ | 否 | 已弃用但 213 支持 |
| YamlParser.kt | ✅ | 否 | 完全兼容 |
| TestCaseManagerPlugin.kt | ⚠️ | 可能需要 | 需要验证 plugin.xml 配置 |

### 需要关注的事项

1. **YamlFileTypeFactory**: 虽然 213 支持，但已被弃用。未来升级到新版本时需要改用新的注册方式。

2. **TestCaseManagerPlugin**: 确保 `plugin.xml` 中使用 `<postStartupActivity>` 而不是 `<startupActivity>`。

3. **整体评估**: 代码与 213 版本基本兼容，主要问题都是弃用警告而非不兼容错误。

---

## 建议操作

1. ✅ 当前代码可以在 213 版本上编译运行
2. ⚠️ 建议检查 `src/main/resources/META-INF/plugin.xml` 配置
3. 📋 记录弃用 API，为未来版本升级做准备
