package com.testcase.manager.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import com.testcase.manager.ui.TestCaseTableModel
import com.testcase.manager.yaml.YamlParser
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 历史版本查看器
 *
 * 显示文件的 Git 提交历史，允许用户查看和比较不同版本。
 *
 * @property project IntelliJ 项目实例
 * @property file 要查看历史版本的文件
 */
class HistoryVersionViewer(private val project: Project, private val file: VirtualFile) {

    private val git: Git = Git.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 提交信息数据类
     *
     * @property hash 提交哈希（短格式）
     * @property fullHash 完整提交哈希
     * @property author 作者名称
     * @property email 作者邮箱
     * @property date 提交日期
     * @property message 提交消息
     * @property parentHashes 父提交哈希列表
     */
    data class CommitInfo(
        val hash: String,
        val fullHash: String,
        val author: String,
        val email: String,
        val date: Date,
        val message: String,
        val parentHashes: List<String> = emptyList()
    ) {
        fun getFormattedDate(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        }

        fun getShortMessage(): String {
            return message.lines().firstOrNull()?.take(50) ?: ""
        }
    }

    /**
     * 版本差异数据类
     *
     * @property oldVersion 旧版本
     * @property newVersion 新版本
     * @property added 新增的测试用例
     * @property modified 修改的测试用例
     * @property deleted 删除的测试用例
     */
    data class VersionDiff(
        val oldVersion: TestCaseModel?,
        val newVersion: TestCaseModel,
        val added: List<TestCase> = emptyList(),
        val modified: List<TestCaseChange> = emptyList(),
        val deleted: List<TestCase> = emptyList()
    ) {
        data class TestCaseChange(
            val testCase: TestCase,
            val changedFields: List<String>,
            val oldValues: Map<String, String>,
            val newValues: Map<String, String>
        )
    }

    /**
     * 显示历史版本对话框
     */
    fun showHistoryDialog() {
        val repository = getGitRepository() ?: run {
            showError("文件不在 Git 仓库中")
            return
        }

        val commits = loadCommitHistory(repository)
        if (commits.isEmpty()) {
            showError("没有找到提交历史")
            return
        }

        val dialog = HistoryDialog(project, file, commits, repository)
        dialog.show()
    }

    /**
     * 获取 Git 仓库
     */
    private fun getGitRepository(): GitRepository? {
        return GitUtil.getRepositoryManager(project).getRepositoryForFile(file)
    }

    /**
     * 加载提交历史
     */
    private fun loadCommitHistory(repository: GitRepository): List<CommitInfo> {
        val handler = GitLineHandler(project, repository.root, GitCommand.LOG)
        handler.addParameters(
            "--pretty=format:%H|%h|%an|%ae|%at|%s|%P",
            "--follow",
            "--",
            file.path
        )

        val result = git.runCommand(handler)
        if (!result.success()) {
            return emptyList()
        }

        return result.output.mapNotNull { line ->
            parseCommitLine(line)
        }
    }

    /**
     * 解析提交行
     */
    private fun parseCommitLine(line: String): CommitInfo? {
        val parts = line.split("|", limit = 7)
        if (parts.size < 6) return null

        return try {
            CommitInfo(
                fullHash = parts[0],
                hash = parts[1],
                author = parts[2],
                email = parts[3],
                date = Date(parts[4].toLong() * 1000),
                message = parts.getOrElse(5) { "" },
                parentHashes = parts.getOrElse(6) { "" }.split(" ").filter { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取特定版本的文件内容
     */
    fun getVersionContent(repository: GitRepository, commitHash: String): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.SHOW)
        handler.addParameters("$commitHash:${getRelativePath(repository)}")

        val result = git.runCommand(handler)
        return if (result.success()) result.output.joinToString("\n") else null
    }

    /**
     * 获取文件相对路径
     */
    private fun getRelativePath(repository: GitRepository): String {
        return file.path.removePrefix(repository.root.path).removePrefix("/")
    }

    /**
     * 获取特定版本的测试用例模型
     */
    fun getVersionModel(repository: GitRepository, commitHash: String): TestCaseModel? {
        val content = getVersionContent(repository, commitHash) ?: return null
        return try {
            YamlParser().parse(content)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 比较两个版本
     */
    fun compareVersions(
        repository: GitRepository,
        oldCommit: String,
        newCommit: String
    ): VersionDiff {
        val oldModel = getVersionModel(repository, oldCommit)
        val newModel = getVersionModel(repository, newCommit) ?: return VersionDiff(null, TestCaseModel())

        val oldIds = oldModel?.testCases?.map { it.id }?.toSet() ?: emptySet()
        val newIds = newModel.testCases.map { it.id }.toSet()

        // 新增的测试用例
        val added = newModel.testCases.filter { it.id !in oldIds }

        // 删除的测试用例
        val deleted = oldModel?.testCases?.filter { it.id !in newIds } ?: emptyList()

        // 修改的测试用例
        val modified = mutableListOf<VersionDiff.TestCaseChange>()
        oldModel?.testCases?.forEach { oldTC ->
            val newTC = newModel.findById(oldTC.id)
            if (newTC != null) {
                val changes = compareTestCases(oldTC, newTC)
                if (changes.changedFields.isNotEmpty()) {
                    modified.add(changes)
                }
            }
        }

        return VersionDiff(oldModel, newModel, added, modified, deleted)
    }

    /**
     * 比较两个测试用例
     */
    private fun compareTestCases(oldTC: TestCase, newTC: TestCase): VersionDiff.TestCaseChange {
        val changedFields = mutableListOf<String>()
        val oldValues = mutableMapOf<String, String>()
        val newValues = mutableMapOf<String, String>()

        fun checkField(fieldName: String, oldValue: String, newValue: String) {
            if (oldValue != newValue) {
                changedFields.add(fieldName)
                oldValues[fieldName] = oldValue
                newValues[fieldName] = newValue
            }
        }

        checkField("name", oldTC.name, newTC.name)
        checkField("priority", oldTC.priority.value, newTC.priority.value)
        checkField("status", oldTC.status.value, newTC.status.value)
        checkField("expected", oldTC.expected, newTC.expected)
        checkField("description", oldTC.description, newTC.description)
        checkField("preconditions", oldTC.preconditions, newTC.preconditions)
        checkField("author", oldTC.author, newTC.author)
        checkField("steps", oldTC.getStepsAsString(), newTC.getStepsAsString())
        checkField("tags", oldTC.getTagsAsString(), newTC.getTagsAsString())

        return VersionDiff.TestCaseChange(newTC, changedFields, oldValues, newValues)
    }

    /**
     * 回滚到指定版本
     */
    fun rollbackToVersion(repository: GitRepository, commitHash: String): Boolean {
        val content = getVersionContent(repository, commitHash) ?: return false

        return try {
            // 写入文件内容
            file.setBinaryContent(content.toByteArray())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 显示错误消息
     */
    private fun showError(message: String) {
        JOptionPane.showMessageDialog(
            null,
            message,
            "错误",
            JOptionPane.ERROR_MESSAGE
        )
    }

    /**
     * 历史版本对话框
     */
    private inner class HistoryDialog(
        project: Project,
        private val file: VirtualFile,
        private val commits: List<CommitInfo>,
        private val repository: GitRepository
    ) : DialogWrapper(project) {

        private lateinit var commitTable: JBTable
        private lateinit var diffPanel: VersionDiffPanel
        private lateinit var previewPanel: VersionPreviewPanel

        init {
            title = "历史版本 - ${file.name}"
            init()
            setOKButtonText("关闭")
        }

        override fun createCenterPanel(): JComponent {
            val mainPanel = JPanel(BorderLayout())
            mainPanel.preferredSize = Dimension(1000, 700)

            // 分割面板：左侧提交列表，右侧详情
            val splitter = JBSplitter(false, 0.4f)
            splitter.firstComponent = createCommitListPanel()
            splitter.secondComponent = createDetailPanel()

            mainPanel.add(splitter, BorderLayout.CENTER)
            return mainPanel
        }

        /**
         * 创建提交列表面板
         */
        private fun createCommitListPanel(): JComponent {
            val panel = JPanel(BorderLayout())

            // 标题
            panel.add(JBLabel("提交历史 (${commits.size} 个提交)").apply {
                font = font.deriveFont(Font.BOLD)
                border = EmptyBorder(5, 5, 5, 5)
            }, BorderLayout.NORTH)

            // 提交表格
            val tableModel = CommitTableModel(commits)
            commitTable = JBTable(tableModel).apply {
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                rowHeight = 40

                // 设置列宽
                columnModel.getColumn(0).preferredWidth = 80  // Hash
                columnModel.getColumn(1).preferredWidth = 120 // Author
                columnModel.getColumn(2).preferredWidth = 100 // Date
                columnModel.getColumn(3).preferredWidth = 300 // Message

                // 自定义渲染
                columnModel.getColumn(2).cellRenderer = DateCellRenderer()

                selectionModel.addListSelectionListener { event ->
                    if (!event.valueIsAdjusting) {
                        selectedRow.takeIf { it >= 0 }?.let { showCommitDetails(it) }
                    }
                }
            }

            panel.add(JBScrollPane(commitTable), BorderLayout.CENTER)

            // 按钮面板
            val buttonPanel = JPanel()
            buttonPanel.add(JButton("查看差异").apply {
                addActionListener { showDiffDialog() }
            })
            buttonPanel.add(JButton("回滚到此版本").apply {
                addActionListener { rollbackSelectedVersion() }
            })
            buttonPanel.add(JButton("导出版本").apply {
                addActionListener { exportSelectedVersion() }
            })

            panel.add(buttonPanel, BorderLayout.SOUTH)
            return panel
        }

        /**
         * 创建详情面板
         */
        private fun createDetailPanel(): JComponent {
            val splitter = JBSplitter(true, 0.5f)

            // 版本预览
            previewPanel = VersionPreviewPanel()
            splitter.firstComponent = previewPanel

            // 差异视图
            diffPanel = VersionDiffPanel()
            splitter.secondComponent = diffPanel

            return splitter
        }

        /**
         * 显示提交详情
         */
        private fun showCommitDetails(row: Int) {
            val commit = commits[row]
            val model = getVersionModel(repository, commit.fullHash)

            previewPanel.setVersion(commit, model)

            // 如果有父提交，显示差异
            if (commit.parentHashes.isNotEmpty()) {
                val diff = compareVersions(repository, commit.parentHashes[0], commit.fullHash)
                diffPanel.setDiff(diff, commit)
            }
        }

        /**
         * 显示差异对话框
         */
        private fun showDiffDialog() {
            val row = commitTable.selectedRow
            if (row < 0 || row >= commits.size - 1) {
                JOptionPane.showMessageDialog(
                    contentPanel,
                    "请选择两个相邻的提交进行比较",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE
                )
                return
            }

            val newCommit = commits[row]
            val oldCommit = commits[row + 1]

            val diff = compareVersions(repository, oldCommit.fullHash, newCommit.fullHash)
            val dialog = DiffDialog(project, oldCommit, newCommit, diff)
            dialog.show()
        }

        /**
         * 回滚到选中版本
         */
        private fun rollbackSelectedVersion() {
            val row = commitTable.selectedRow
            if (row < 0) return

            val commit = commits[row]

            val result = JOptionPane.showConfirmDialog(
                contentPanel,
                "确定要回滚到版本 ${commit.hash} 吗？\n\n这将覆盖当前文件的未保存更改。",
                "确认回滚",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )

            if (result == JOptionPane.YES_OPTION) {
                if (rollbackToVersion(repository, commit.fullHash)) {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "已成功回滚到版本 ${commit.hash}",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "回滚失败",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }

        /**
         * 导出选中版本
         */
        private fun exportSelectedVersion() {
            val row = commitTable.selectedRow
            if (row < 0) return

            val commit = commits[row]
            val content = getVersionContent(repository, commit.fullHash) ?: return

            val fileChooser = JFileChooser()
            fileChooser.selectedFile = java.io.File("${file.nameWithoutExtension}_${commit.hash}.yaml")

            if (fileChooser.showSaveDialog(contentPanel) == JFileChooser.APPROVE_OPTION) {
                try {
                    fileChooser.selectedFile.writeText(content)
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "已导出到 ${fileChooser.selectedFile.absolutePath}",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "导出失败: ${e.message}",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    /**
     * 提交表格模型
     */
    private class CommitTableModel(private val commits: List<CommitInfo>) : DefaultTableModel() {

        private val columns = arrayOf("提交", "作者", "日期", "消息")

        override fun getColumnCount(): Int = columns.size
        override fun getRowCount(): Int = commits.size
        override fun getColumnName(column: Int): String = columns[column]

        override fun getValueAt(row: Int, column: Int): Any {
            val commit = commits[row]
            return when (column) {
                0 -> commit.hash
                1 -> commit.author
                2 -> commit.date
                3 -> commit.getShortMessage()
                else -> ""
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    /**
     * 日期单元格渲染器
     */
    private class DateCellRenderer : DefaultTableCellRenderer() {
        private val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val date = value as? Date
            return super.getTableCellRendererComponent(
                table,
                date?.let { format.format(it) } ?: "",
                isSelected,
                hasFocus,
                row,
                column
            )
        }
    }

    /**
     * 版本预览面板
     */
    private class VersionPreviewPanel : JPanel(BorderLayout()) {

        private val infoPanel = JPanel()
        private val tableModel = TestCaseTableModel()
        private val table = JBTable(tableModel)

        init {
            border = BorderFactory.createTitledBorder("版本预览")

            infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
            infoPanel.border = EmptyBorder(5, 5, 5, 5)

            add(infoPanel, BorderLayout.NORTH)
            add(JBScrollPane(table), BorderLayout.CENTER)
        }

        fun setVersion(commit: CommitInfo, model: TestCaseModel?) {
            infoPanel.removeAll()

            infoPanel.add(JBLabel("提交: ${commit.hash}").apply {
                font = font.deriveFont(Font.BOLD)
            })
            infoPanel.add(JBLabel("作者: ${commit.author} <${commit.email}>"))
            infoPanel.add(JBLabel("日期: ${commit.getFormattedDate()}"))
            infoPanel.add(JBLabel("消息: ${commit.message}"))
            infoPanel.add(JBLabel("测试用例数: ${model?.size() ?: 0}"))

            // 更新表格
            tableModel.clearData()
            model?.testCases?.forEach { tc ->
                tableModel.addRow(arrayOf(
                    tc.id,
                    tc.name,
                    tc.priority.value,
                    tc.status.value,
                    tc.getStepsAsString(),
                    tc.expected
                ))
            }

            infoPanel.revalidate()
            infoPanel.repaint()
        }
    }

    /**
     * 版本差异面板
     */
    private class VersionDiffPanel : JPanel(BorderLayout()) {

        private val summaryLabel = JBLabel()
        private val detailsArea = JTextArea()

        init {
            border = BorderFactory.createTitledBorder("变更摘要")
            layout = BorderLayout()

            summaryLabel.border = EmptyBorder(5, 5, 5, 5)
            add(summaryLabel, BorderLayout.NORTH)

            detailsArea.isEditable = false
            detailsArea.font = Font("Monospaced", Font.PLAIN, 12)
            add(JBScrollPane(detailsArea), BorderLayout.CENTER)
        }

        fun setDiff(diff: VersionDiff, commit: CommitInfo) {
            val addedCount = diff.added.size
            val modifiedCount = diff.modified.size
            val deletedCount = diff.deleted.size

            summaryLabel.text = buildString {
                append("<html>")
                append("新增: <font color='green'>+$addedCount</font> ")
                append("修改: <font color='orange'>~$modifiedCount</font> ")
                append("删除: <font color='red'>-$deletedCount</font>")
                append("</html>")
            }

            val details = buildString {
                if (diff.added.isNotEmpty()) {
                    appendLine("=== 新增测试用例 ===")
                    diff.added.forEach { appendLine("+ ${it.id}: ${it.name}") }
                    appendLine()
                }

                if (diff.modified.isNotEmpty()) {
                    appendLine("=== 修改的测试用例 ===")
                    diff.modified.forEach { change ->
                        appendLine("~ ${change.testCase.id}: ${change.testCase.name}")
                        appendLine("  变更字段: ${change.changedFields.joinToString(", ")}")
                    }
                    appendLine()
                }

                if (diff.deleted.isNotEmpty()) {
                    appendLine("=== 删除的测试用例 ===")
                    diff.deleted.forEach { appendLine("- ${it.id}: ${it.name}") }
                }
            }

            detailsArea.text = details
        }
    }

    /**
     * 差异对话框
     */
    private class DiffDialog(
        project: Project,
        private val oldCommit: CommitInfo,
        private val newCommit: CommitInfo,
        private val diff: VersionDiff
    ) : DialogWrapper(project) {

        init {
            title = "版本对比: ${oldCommit.hash} → ${newCommit.hash}"
            init()
            setOKButtonText("关闭")
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout())
            panel.preferredSize = Dimension(800, 600)

            // 标题
            val headerPanel = JPanel(BorderLayout())
            headerPanel.border = EmptyBorder(10, 10, 10, 10)
            headerPanel.add(JBLabel("""
                <html>
                <b>旧版本:</b> ${oldCommit.hash} by ${oldCommit.author} at ${oldCommit.getFormattedDate()}<br>
                <b>新版本:</b> ${newCommit.hash} by ${newCommit.author} at ${newCommit.getFormattedDate()}
                </html>
            """.trimIndent()), BorderLayout.CENTER)
            panel.add(headerPanel, BorderLayout.NORTH)

            // 差异内容
            val tabbedPane = JTabbedPane()

            // 新增
            if (diff.added.isNotEmpty()) {
                tabbedPane.addTab("新增 (${diff.added.size})", createTestCaseListPanel(diff.added, Color(200, 255, 200)))
            }

            // 修改
            if (diff.modified.isNotEmpty()) {
                tabbedPane.addTab("修改 (${diff.modified.size})", createModifiedPanel(diff.modified))
            }

            // 删除
            if (diff.deleted.isNotEmpty()) {
                tabbedPane.addTab("删除 (${diff.deleted.size})", createTestCaseListPanel(diff.deleted, Color(255, 200, 200)))
            }

            panel.add(tabbedPane, BorderLayout.CENTER)
            return panel
        }

        private fun createTestCaseListPanel(testCases: List<TestCase>, bgColor: Color): JComponent {
            val panel = JPanel(BorderLayout())
            val list = JPanel()
            list.layout = BoxLayout(list, BoxLayout.Y_AXIS)
            list.background = bgColor

            testCases.forEach { tc ->
                val itemPanel = JPanel(BorderLayout())
                itemPanel.background = bgColor
                itemPanel.border = EmptyBorder(5, 10, 5, 10)
                itemPanel.add(JBLabel("<html><b>${tc.id}</b>: ${tc.name}</html>"), BorderLayout.CENTER)
                list.add(itemPanel)
            }

            panel.add(JBScrollPane(list), BorderLayout.CENTER)
            return panel
        }

        private fun createModifiedPanel(changes: List<VersionDiff.TestCaseChange>): JComponent {
            val panel = JPanel(BorderLayout())
            val list = JPanel()
            list.layout = BoxLayout(list, BoxLayout.Y_AXIS)

            changes.forEach { change ->
                val itemPanel = JPanel(BorderLayout())
                itemPanel.border = EmptyBorder(5, 10, 5, 10)

                val header = JBLabel("<html><b>${change.testCase.id}</b>: ${change.testCase.name}</html>")
                itemPanel.add(header, BorderLayout.NORTH)

                val fieldsPanel = JPanel()
                fieldsPanel.layout = BoxLayout(fieldsPanel, BoxLayout.Y_AXIS)
                change.changedFields.forEach { field ->
                    val fieldLabel = JBLabel("  $field: ${change.oldValues[field]} → ${change.newValues[field]}")
                    fieldLabel.foreground = Color(0, 100, 200)
                    fieldsPanel.add(fieldLabel)
                }
                itemPanel.add(fieldsPanel, BorderLayout.CENTER)

                list.add(itemPanel)
                list.add(JSeparator())
            }

            panel.add(JBScrollPane(list), BorderLayout.CENTER)
            return panel
        }
    }
}
