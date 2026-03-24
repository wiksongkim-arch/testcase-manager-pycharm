package com.testcase.manager.git

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.testcase.manager.yaml.YamlFileType

/**
 * 查看历史版本动作
 *
 * 在右键菜单中显示，用于打开历史版本查看器。
 */
class ViewHistoryAction : AnAction(
    "查看历史版本",
    "查看此文件的 Git 提交历史",
    com.intellij.icons.AllIcons.Vcs.History
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!isYamlFile(file)) {
            Messages.showWarningDialog(
                project,
                "此操作仅支持 YAML 文件",
                "不支持的文件类型"
            )
            return
        }

        val viewer = HistoryVersionViewer(project, file)
        viewer.showHistoryDialog()
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isYamlFile(file)
    }

    private fun isYamlFile(file: VirtualFile?): Boolean {
        return file?.extension?.lowercase() in listOf("yaml", "yml")
    }
}

/**
 * 回滚到上一版本动作
 */
class RollbackToPreviousAction : AnAction(
    "回滚到上一版本",
    "将文件回滚到上一个 Git 提交版本",
    com.intellij.icons.AllIcons.Actions.Rollback
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!isYamlFile(file)) {
            Messages.showWarningDialog(
                project,
                "此操作仅支持 YAML 文件",
                "不支持的文件类型"
            )
            return
        }

        val manager = VersionRollbackManager(project)

        // 显示确认对话框
        val result = Messages.showYesNoDialog(
            project,
            "确定要将 '${file.name}' 回滚到上一个版本吗？\n\n此操作将覆盖当前文件的未保存更改。",
            "确认回滚",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            val rollbackResult = manager.rollbackToPrevious(file)

            if (rollbackResult.success) {
                Messages.showInfoMessage(
                    project,
                    rollbackResult.message +
                        (rollbackResult.backupPath?.let { "\n\n已创建备份: $it" } ?: ""),
                    "回滚成功"
                )
            } else {
                Messages.showErrorDialog(
                    project,
                    rollbackResult.message,
                    "回滚失败"
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isYamlFile(file)
    }

    private fun isYamlFile(file: VirtualFile?): Boolean {
        return file?.extension?.lowercase() in listOf("yaml", "yml")
    }
}

/**
 * 解决合并冲突动作
 */
class ResolveConflictsAction : AnAction(
    "解决合并冲突",
    "打开三方合并对话框解决冲突",
    com.intellij.icons.AllIcons.Vcs.Merge
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!isYamlFile(file)) {
            Messages.showWarningDialog(
                project,
                "此操作仅支持 YAML 文件",
                "不支持的文件类型"
            )
            return
        }

        val resolver = ConflictResolver(project)

        // 检查是否有冲突
        if (!resolver.hasConflicts(file)) {
            Messages.showInfoMessage(
                project,
                "此文件没有合并冲突标记",
                "无冲突"
            )
            return
        }

        // 解析冲突标记
        val content = String(file.contentsToByteArray())
        val (baseContent, localContent, remoteContent) = resolver.parseConflictMarkers(content)

        // 解析三个版本
        val parser = com.testcase.manager.yaml.YamlParser()
        val baseModel = baseContent?.let { try { parser.parse(it) } catch (_: Exception) { null } }
        val localModel = localContent?.let { try { parser.parse(it) } catch (_: Exception) { null } }
            ?: com.testcase.manager.model.TestCaseModel()
        val remoteModel = remoteContent?.let { try { parser.parse(it) } catch (_: Exception) { null } }
            ?: com.testcase.manager.model.TestCaseModel()

        // 执行三方合并
        val mergeResult = resolver.threeWayMerge(baseModel, localModel, remoteModel)

        if (mergeResult.success && mergeResult.mergedModel != null) {
            // 无冲突，自动合并
            val serialized = com.testcase.manager.yaml.YamlSerializer.serialize(mergeResult.mergedModel)
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                file.setBinaryContent(serialized.toByteArray())
            }
            Messages.showInfoMessage(
                project,
                "已成功自动合并所有更改",
                "合并成功"
            )
        } else if (mergeResult.conflicts.isNotEmpty()) {
            // 有冲突，显示三方合并对话框
            val dialog = ThreeWayMergeDialog(
                project,
                mergeResult.conflicts,
                baseModel,
                localModel,
                remoteModel
            )

            if (dialog.showAndGet()) {
                val result = dialog.getMergeResult()

                // 应用合并结果
                val serialized = com.testcase.manager.yaml.YamlSerializer.serialize(result.model)
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                    file.setBinaryContent(serialized.toByteArray())
                }

                Messages.showInfoMessage(
                    project,
                    "已成功解决 ${result.resolutions.size} 个冲突",
                    "冲突已解决"
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val hasConflicts = file?.let {
            isYamlFile(it) && hasConflictMarkers(it)
        } ?: false
        e.presentation.isEnabledAndVisible = hasConflicts
    }

    private fun isYamlFile(file: VirtualFile?): Boolean {
        return file?.extension?.lowercase() in listOf("yaml", "yml")
    }

    private fun hasConflictMarkers(file: VirtualFile): Boolean {
        return try {
            val content = String(file.contentsToByteArray())
            content.contains("<<<<<<<") && content.contains(">>>>>>>")
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 检查冲突动作
 *
 * 用于检查当前文件是否有合并冲突
 */
class CheckConflictsAction : AnAction(
    "检查合并冲突",
    "检查此文件是否有合并冲突",
    com.intellij.icons.AllIcons.Actions.CheckMulticaret
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!isYamlFile(file)) {
            Messages.showWarningDialog(
                project,
                "此操作仅支持 YAML 文件",
                "不支持的文件类型"
            )
            return
        }

        val resolver = ConflictResolver(project)
        val hasConflicts = resolver.hasConflicts(file)

        if (hasConflicts) {
            val result = Messages.showYesNoDialog(
                project,
                "此文件包含合并冲突标记。\n\n是否立即打开冲突解决对话框？",
                "发现冲突",
                Messages.getWarningIcon()
            )

            if (result == Messages.YES) {
                // 触发解决冲突动作
                e.presentation.putClientProperty("trigger_resolve", true)
            }
        } else {
            Messages.showInfoMessage(
                project,
                "此文件没有合并冲突",
                "检查完成"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isYamlFile(file)
    }

    private fun isYamlFile(file: VirtualFile?): Boolean {
        return file?.extension?.lowercase() in listOf("yaml", "yml")
    }
}

/**
 * 比较版本动作
 *
 * 用于比较当前文件与指定版本
 */
class CompareWithVersionAction : AnAction(
    "与版本比较...",
    "将当前文件与指定 Git 版本进行比较",
    com.intellij.icons.AllIcons.Actions.Diff
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!isYamlFile(file)) {
            Messages.showWarningDialog(
                project,
                "此操作仅支持 YAML 文件",
                "不支持的文件类型"
            )
            return
        }

        // 显示输入对话框，让用户输入提交哈希
        val commitHash = Messages.showInputDialog(
            project,
            "请输入要比较的 Git 提交哈希（短格式或完整格式）：",
            "选择版本",
            Messages.getQuestionIcon()
        ) ?: return

        if (commitHash.isBlank()) {
            Messages.showErrorDialog(
                project,
                "提交哈希不能为空",
                "输入错误"
            )
            return
        }

        // 打开历史版本查看器并比较
        val viewer = HistoryVersionViewer(project, file)
        val repository = git4idea.GitUtil.getRepositoryManager(project).getRepositoryForFile(file)

        if (repository == null) {
            Messages.showErrorDialog(
                project,
                "文件不在 Git 仓库中",
                "错误"
            )
            return
        }

        // 获取当前版本和目标版本
        val currentContent = String(file.contentsToByteArray())
        val targetContent = viewer.getVersionContent(repository, commitHash)

        if (targetContent == null) {
            Messages.showErrorDialog(
                project,
                "无法获取版本 $commitHash 的内容",
                "错误"
            )
            return
        }

        // 显示差异对话框
        val parser = com.testcase.manager.yaml.YamlParser()
        val currentModel = try {
            parser.parse(currentContent)
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "当前文件 YAML 格式无效",
                "错误"
            )
            return
        }

        val targetModel = try {
            parser.parse(targetContent)
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "目标版本 YAML 格式无效",
                "错误"
            )
            return
        }

        // 计算差异
        val diff = viewer.compareVersions(repository, commitHash, "HEAD")

        // 显示差异对话框
        val dialog = VersionDiffDialog(project, commitHash, "HEAD", diff)
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isYamlFile(file)
    }

    private fun isYamlFile(file: VirtualFile?): Boolean {
        return file?.extension?.lowercase() in listOf("yaml", "yml")
    }
}

/**
 * 版本差异对话框
 */
private class VersionDiffDialog(
    project: com.intellij.openapi.project.Project,
    private val oldCommit: String,
    private val newCommit: String,
    private val diff: HistoryVersionViewer.VersionDiff
) : com.intellij.openapi.ui.DialogWrapper(project) {

    init {
        title = "版本对比: ${oldCommit.take(7)} → ${newCommit.take(7)}"
        init()
        setOKButtonText("关闭")
    }

    override fun createCenterPanel(): javax.swing.JComponent {
        val panel = javax.swing.JPanel(java.awt.BorderLayout())
        panel.preferredSize = java.awt.Dimension(800, 600)

        val tabbedPane = javax.swing.JTabbedPane()

        // 摘要
        tabbedPane.addTab("摘要", createSummaryPanel())

        // 新增
        if (diff.added.isNotEmpty()) {
            tabbedPane.addTab("新增 (${diff.added.size})", createTestCasePanel(diff.added, java.awt.Color(200, 255, 200)))
        }

        // 修改
        if (diff.modified.isNotEmpty()) {
            tabbedPane.addTab("修改 (${diff.modified.size})", createModifiedPanel())
        }

        // 删除
        if (diff.deleted.isNotEmpty()) {
            tabbedPane.addTab("删除 (${diff.deleted.size})", createTestCasePanel(diff.deleted, java.awt.Color(255, 200, 200)))
        }

        panel.add(tabbedPane, java.awt.BorderLayout.CENTER)
        return panel
    }

    private fun createSummaryPanel(): javax.swing.JComponent {
        val panel = javax.swing.JPanel(java.awt.BorderLayout())
        panel.border = javax.swing.border.EmptyBorder(10, 10, 10, 10)

        val label = com.intellij.ui.components.JBLabel("""
            <html>
            <h2>变更摘要</h2>
            <ul>
            <li><font color='green'>新增: ${diff.added.size} 个测试用例</font></li>
            <li><font color='orange'>修改: ${diff.modified.size} 个测试用例</font></li>
            <li><font color='red'>删除: ${diff.deleted.size} 个测试用例</font></li>
            </ul>
            </html>
        """.trimIndent())

        panel.add(label, java.awt.BorderLayout.NORTH)
        return panel
    }

    private fun createTestCasePanel(
        testCases: List<com.testcase.manager.model.TestCase>,
        bgColor: java.awt.Color
    ): javax.swing.JComponent {
        val panel = javax.swing.JPanel(java.awt.BorderLayout())
        val list = javax.swing.JPanel()
        list.layout = javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS)
        list.background = bgColor

        testCases.forEach { tc ->
            val itemPanel = javax.swing.JPanel(java.awt.BorderLayout())
            itemPanel.background = bgColor
            itemPanel.border = javax.swing.border.EmptyBorder(5, 10, 5, 10)
            itemPanel.add(
                com.intellij.ui.components.JBLabel("<html><b>${tc.id}</b>: ${tc.name}</html>"),
                java.awt.BorderLayout.CENTER
            )
            list.add(itemPanel)
        }

        panel.add(com.intellij.ui.components.JBScrollPane(list), java.awt.BorderLayout.CENTER)
        return panel
    }

    private fun createModifiedPanel(): javax.swing.JComponent {
        val panel = javax.swing.JPanel(java.awt.BorderLayout())
        val list = javax.swing.JPanel()
        list.layout = javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS)

        diff.modified.forEach { change ->
            val itemPanel = javax.swing.JPanel(java.awt.BorderLayout())
            itemPanel.border = javax.swing.border.EmptyBorder(5, 10, 5, 10)

            val header = com.intellij.ui.components.JBLabel(
                "<html><b>${change.testCase.id}</b>: ${change.testCase.name}</html>"
            )
            itemPanel.add(header, java.awt.BorderLayout.NORTH)

            val fieldsPanel = javax.swing.JPanel()
            fieldsPanel.layout = javax.swing.BoxLayout(fieldsPanel, javax.swing.BoxLayout.Y_AXIS)
            change.changedFields.forEach { field ->
                val fieldLabel = com.intellij.ui.components.JBLabel(
                    "  $field: ${change.oldValues[field]} → ${change.newValues[field]}"
                )
                fieldLabel.foreground = java.awt.Color(0, 100, 200)
                fieldsPanel.add(fieldLabel)
            }
            itemPanel.add(fieldsPanel, java.awt.BorderLayout.CENTER)

            list.add(itemPanel)
            list.add(javax.swing.JSeparator())
        }

        panel.add(com.intellij.ui.components.JBScrollPane(list), java.awt.BorderLayout.CENTER)
        return panel
    }
}
