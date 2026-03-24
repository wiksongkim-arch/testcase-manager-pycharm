package com.testcase.manager.git

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import com.testcase.manager.ui.TestCaseTableModel
import com.testcase.manager.yaml.YamlParser
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

/**
 * Diff 查看器类
 *
 * 实现双栏对比视图，用于显示测试用例文件的 Git 版本差异。
 * 支持对比当前工作区版本与 HEAD 版本，或对比任意两个版本。
 *
 * @property project 当前项目实例
 * @property file 当前对比的虚拟文件
 */
class DiffViewer(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {

    private val logger = Logger.getInstance(DiffViewer::class.java)
    private val gitIntegration = GitIntegration.getInstance(project)
    private val yamlParser = YamlParser()

    private val component: JComponent
    private val leftTable: JBTable
    private val rightTable: JBTable
    private val leftModel: TestCaseTableModel
    private val rightModel: TestCaseTableModel

    private var leftTitle = "HEAD (当前版本)"
    private var rightTitle = "工作区"
    private var leftContent: String? = null
    private var rightContent: String? = null

    companion object {
        val EDITOR_NAME = Key.create<String>("TESTCASE_DIFF_VIEWER")
    }

    init {
        // 创建表格模型
        leftModel = TestCaseTableModel()
        rightModel = TestCaseTableModel()

        // 创建表格组件
        leftTable = createDiffTable(leftModel)
        rightTable = createDiffTable(rightModel)

        // 创建主界面
        component = createComponent()

        // 加载对比数据
        loadDiffData()
    }

    /**
     * 创建对比表格组件
     *
     * @param model 表格模型
     * @return 配置好的 JBTable 实例
     */
    private fun createDiffTable(model: TestCaseTableModel): JBTable {
        return JBTable(model).apply {
            autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
            rowHeight = 30
            preferredScrollableViewportSize = Dimension(400, 600)

            // 设置表头
            tableHeader.apply {
                preferredSize = Dimension(preferredSize.width, 35)
                reorderingAllowed = false
                resizingAllowed = true
            }

            // 设置选择模式
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            setCellSelectionEnabled(true)

            // 设置网格线
            showHorizontalLines = true
            showVerticalLines = true
            gridColor = Color(200, 200, 200)

            // 禁用编辑
            isEnabled = false
        }
    }

    /**
     * 创建主界面组件
     *
     * @return 包含双栏对比视图的 JComponent
     */
    private fun createComponent(): JComponent {
        return JPanel(BorderLayout()).apply {
            // 添加工具栏
            add(createToolBar(), BorderLayout.NORTH)

            // 添加对比视图（分割面板）
            add(createDiffPanel(), BorderLayout.CENTER)

            // 添加状态栏
            add(createStatusBar(), BorderLayout.SOUTH)
        }
    }

    /**
     * 创建工具栏
     *
     * @return 包含对比操作按钮的工具栏
     */
    private fun createToolBar(): JComponent {
        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = JBUI.Borders.empty(5)

            // 对比模式选择
            add(javax.swing.JLabel("对比模式: "))

            add(javax.swing.JComboBox<String>(arrayOf(
                "工作区 vs HEAD",
                "工作区 vs 上一版本",
                "HEAD vs 上一版本"
            )).apply {
                addActionListener { e ->
                    val selected = (e.source as javax.swing.JComboBox<*>).selectedIndex
                    when (selected) {
                        0 -> loadDiffData()
                        1 -> loadDiffWithPrevious()
                        2 -> loadHeadVsPrevious()
                    }
                }
            })

            add(javax.swing.Box.createHorizontalStrut(20))

            // 接受左侧按钮
            add(javax.swing.JButton("接受左侧").apply {
                addActionListener { acceptLeft() }
            })

            // 接受右侧按钮
            add(javax.swing.JButton("接受右侧").apply {
                addActionListener { acceptRight() }
            })

            add(javax.swing.Box.createHorizontalStrut(10))

            // 刷新按钮
            add(javax.swing.JButton("刷新").apply {
                addActionListener { refresh() }
            })
        }
    }

    /**
     * 创建对比面板（双栏视图）
     *
     * @return 包含左右两个表格的分割面板
     */
    private fun createDiffPanel(): JComponent {
        return JBSplitter(false, 0.5f).apply {
            // 左侧面板
            firstComponent = createSidePanel(leftTable, leftTitle)

            // 右侧面板
            secondComponent = createSidePanel(rightTable, rightTitle)

            // 设置分隔条宽度
            dividerWidth = 3
        }
    }

    /**
     * 创建侧边面板
     *
     * @param table 表格组件
     * @param title 面板标题
     * @return 配置好的面板
     */
    private fun createSidePanel(table: JBTable, title: String): JComponent {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(title)

            // 标题标签
            add(JPanel(BorderLayout()).apply {
                background = Color(240, 240, 240)
                border = JBUI.Borders.empty(5, 10)
                add(JBLabel(title).apply {
                    font = font.deriveFont(Font.BOLD)
                }, BorderLayout.WEST)
            }, BorderLayout.NORTH)

            // 表格（带滚动条）
            add(JBScrollPane(table), BorderLayout.CENTER)
        }
    }

    /**
     * 创建状态栏
     *
     * @return 状态栏组件
     */
    private fun createStatusBar(): JComponent {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 10)
            background = Color(245, 245, 245)

            add(JBLabel("就绪").apply {
                name = "diffStatusLabel"
            }, BorderLayout.WEST)
        }
    }

    /**
     * 加载对比数据（工作区 vs HEAD）
     */
    fun loadDiffData() {
        leftTitle = "HEAD (当前版本)"
        rightTitle = "工作区"

        // 获取 HEAD 版本内容
        leftContent = gitIntegration.getHeadVersionContent(file)

        // 获取工作区版本内容
        rightContent = try {
            String(file.contentsToByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Failed to read file content", e)
            null
        }

        // 加载到表格
        loadContentToTable(leftContent, leftModel)
        loadContentToTable(rightContent, rightModel)

        // 高亮差异
        highlightDifferences()

        // 更新状态栏
        updateStatusBar()

        // 更新标题
        updatePanelTitles()
    }

    /**
     * 加载与上一版本的对比
     */
    private fun loadDiffWithPrevious() {
        leftTitle = "上一版本"
        rightTitle = "工作区"

        // 获取上一版本
        val history = gitIntegration.getFileHistory(file, 2)
        if (history.size >= 2) {
            leftContent = gitIntegration.getVersionContent(file, history[1].hash)
        } else {
            leftContent = null
        }

        // 获取工作区版本
        rightContent = try {
            String(file.contentsToByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }

        loadContentToTable(leftContent, leftModel)
        loadContentToTable(rightContent, rightModel)
        highlightDifferences()
        updateStatusBar()
        updatePanelTitles()
    }

    /**
     * 加载 HEAD vs 上一版本的对比
     */
    private fun loadHeadVsPrevious() {
        leftTitle = "上一版本"
        rightTitle = "HEAD"

        // 获取上一版本
        val history = gitIntegration.getFileHistory(file, 2)
        if (history.size >= 2) {
            leftContent = gitIntegration.getVersionContent(file, history[1].hash)
        } else {
            leftContent = null
        }

        // 获取 HEAD 版本
        rightContent = gitIntegration.getHeadVersionContent(file)

        loadContentToTable(leftContent, leftModel)
        loadContentToTable(rightContent, rightModel)
        highlightDifferences()
        updateStatusBar()
        updatePanelTitles()
    }

    /**
     * 加载内容到表格模型
     *
     * @param content YAML 内容字符串
     * @param model 目标表格模型
     */
    private fun loadContentToTable(content: String?, model: TestCaseTableModel) {
        model.clearData()

        if (content == null) {
            return
        }

        try {
            val testCaseModel = yamlParser.parse(content)
            val (_, rows) = testCaseModel.toExcelData()

            rows.forEach { row ->
                model.addRow(row.toTypedArray())
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse YAML content: ${e.message}")
            // 如果解析失败，显示原始内容
            model.addRow(arrayOf("解析错误", e.message ?: "Unknown error", "", "", "", ""))
        }
    }

    /**
     * 高亮显示差异行
     */
    private fun highlightDifferences() {
        // 创建自定义渲染器来高亮差异
        val diffRenderer = DiffCellRenderer()

        for (col in 0 until leftModel.columnCount) {
            leftTable.columnModel.getColumn(col).cellRenderer = diffRenderer
            rightTable.columnModel.getColumn(col).cellRenderer = diffRenderer
        }

        // 触发重绘以应用高亮
        leftTable.repaint()
        rightTable.repaint()
    }

    /**
     * 检查指定行是否有差异
     *
     * @param row 行索引
     * @return 如果有差异返回 true
     */
    fun hasRowDifference(row: Int): Boolean {
        if (row >= leftModel.rowCount || row >= rightModel.rowCount) {
            return true // 行数不同也算差异
        }

        for (col in 0 until leftModel.columnCount) {
            val leftValue = leftModel.getValueAt(row, col)?.toString() ?: ""
            val rightValue = rightModel.getValueAt(row, col)?.toString() ?: ""
            if (leftValue != rightValue) {
                return true
            }
        }
        return false
    }

    /**
     * 检查指定单元格是否有差异
     *
     * @param row 行索引
     * @param col 列索引
     * @return 如果有差异返回 true
     */
    fun hasCellDifference(row: Int, col: Int): Boolean {
        if (row >= leftModel.rowCount || row >= rightModel.rowCount) {
            return true
        }
        if (col >= leftModel.columnCount || col >= rightModel.columnCount) {
            return true
        }

        val leftValue = leftModel.getValueAt(row, col)?.toString() ?: ""
        val rightValue = rightModel.getValueAt(row, col)?.toString() ?: ""
        return leftValue != rightValue
    }

    /**
     * 接受左侧版本
     */
    private fun acceptLeft() {
        // 将左侧内容写入文件
        leftContent?.let { content ->
            saveContentToFile(content)
            updateStatusBar("已接受左侧版本并保存")
        }
    }

    /**
     * 接受右侧版本
     */
    private fun acceptRight() {
        // 右侧就是当前工作区，无需操作
        updateStatusBar("右侧已是当前工作区版本")
    }

    /**
     * 刷新对比视图
     */
    fun refresh() {
        loadDiffData()
    }

    /**
     * 保存内容到文件
     *
     * @param content 要保存的内容
     */
    private fun saveContentToFile(content: String) {
        try {
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction {
                file.setBinaryContent(content.toByteArray(Charsets.UTF_8))
            }
            updateStatusBar("文件已保存")
        } catch (e: Exception) {
            logger.error("Failed to save file", e)
            updateStatusBar("保存失败: ${e.message}")
        }
    }

    /**
     * 更新状态栏
     *
     * @param message 状态消息
     */
    private fun updateStatusBar(message: String = "就绪") {
        val statusBar = (component as JPanel).getComponent(2) as JPanel
        val label = statusBar.getComponent(0) as JBLabel

        // 计算差异统计
        val diffCount = countDifferences()
        label.text = "$message | 发现 $diffCount 处差异"
    }

    /**
     * 计算差异数量
     *
     * @return 差异单元格数量
     */
    private fun countDifferences(): Int {
        var count = 0
        val maxRows = maxOf(leftModel.rowCount, rightModel.rowCount)

        for (row in 0 until maxRows) {
            for (col in 0 until leftModel.columnCount) {
                if (hasCellDifference(row, col)) {
                    count++
                }
            }
        }

        return count
    }

    /**
     * 更新面板标题
     */
    private fun updatePanelTitles() {
        val diffPanel = (component as JPanel).getComponent(1) as JBSplitter

        (diffPanel.firstComponent as JPanel).getComponent(0).let { header ->
            (header as JPanel).getComponent(0).let { label ->
                (label as JBLabel).text = leftTitle
            }
        }

        (diffPanel.secondComponent as JPanel).getComponent(0).let { header ->
            (header as JPanel).getComponent(0).let { label ->
                (label as JBLabel).text = rightTitle
            }
        }
    }

    // ==================== FileEditor 接口实现 ====================

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = leftTable

    override fun getName(): String = "Diff Viewer"

    override fun setState(state: FileEditorState) {
        // 恢复编辑器状态
    }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {
        // 添加属性变更监听器
    }

    override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {
        // 移除属性变更监听器
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        Disposer.dispose(this)
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? = null

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        // 存储用户数据
    }

    /**
     * Diff 单元格渲染器
     * 用于高亮显示有差异的单元格
     */
    inner class DiffCellRenderer : javax.swing.table.DefaultTableCellRenderer() {

        private val diffBackground = Color(255, 235, 235)  // 浅红色背景
        private val normalBackground = Color.WHITE
        private val addedBackground = Color(235, 255, 235)  // 浅绿色背景
        private val removedBackground = Color(255, 220, 220) // 深红色背景

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            )

            // 确定这是左侧还是右侧表格
            val isLeftTable = table == leftTable

            // 检查是否有差异
            val hasDiff = if (isLeftTable) {
                // 左侧表格：检查右侧对应位置是否存在且不同
                if (row >= rightModel.rowCount) {
                    true // 右侧没有这一行，表示删除
                } else {
                    hasCellDifference(row, column)
                }
            } else {
                // 右侧表格：检查左侧对应位置是否存在且不同
                if (row >= leftModel.rowCount) {
                    true // 左侧没有这一行，表示新增
                } else {
                    hasCellDifference(row, column)
                }
            }

            // 设置背景色
            background = when {
                isSelected -> table?.selectionBackground ?: normalBackground
                hasDiff -> {
                    if (isLeftTable && row >= rightModel.rowCount) {
                        removedBackground
                    } else if (!isLeftTable && row >= leftModel.rowCount) {
                        addedBackground
                    } else {
                        diffBackground
                    }
                }
                else -> normalBackground
            }

            // 如果有差异，添加标记
            if (hasDiff && !isSelected) {
                border = BorderFactory.createMatteBorder(0, 3, 0, 0,
                    if (isLeftTable) Color.RED else Color.GREEN)
            }

            return component
        }
    }
}

/**
 * Diff 请求包装类
 * 用于通过 IntelliJ 内置 Diff 工具显示对比
 */
class DiffRequestHelper(private val project: Project) {

    private val gitIntegration = GitIntegration.getInstance(project)
    private val diffContentFactory = DiffContentFactory.getInstance()

    /**
     * 显示文件与 HEAD 版本的对比
     *
     * @param file 要对比的文件
     */
    fun showDiffWithHead(file: VirtualFile) {
        val headContent = gitIntegration.getHeadVersionContent(file) ?: return
        val currentContent = String(file.contentsToByteArray(), Charsets.UTF_8)

        showDiff(
            headContent,
            currentContent,
            "HEAD",
            "工作区",
            file.name
        )
    }

    /**
     * 显示两个内容的对比
     *
     * @param leftContent 左侧内容
     * @param rightContent 右侧内容
     * @param leftTitle 左侧标题
     * @param rightTitle 右侧标题
     * @param title 对比窗口标题
     */
    fun showDiff(
        leftContent: String,
        rightContent: String,
        leftTitle: String,
        rightTitle: String,
        title: String
    ) {
        val leftDiffContent: DiffContent = diffContentFactory.create(leftContent)
        val rightDiffContent: DiffContent = diffContentFactory.create(rightContent)

        val request = SimpleDiffRequest(
            title,
            leftDiffContent,
            rightDiffContent,
            leftTitle,
            rightTitle
        )

        DiffManager.getInstance().showDiff(project, request)
    }
}
