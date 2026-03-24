package com.testcase.manager.ui

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.testcase.manager.git.GitIntegration
import com.testcase.manager.git.GitStatusIndicator
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke

/**
 * Excel 风格编辑器
 * 为测试用例 YAML 文件提供表格编辑界面
 * 
 * 功能特性：
 * - 单元格编辑（文本、数字、下拉选择）
 * - 行列操作（增删、拖拽、复制粘贴）
 * - 右键菜单
 */
class ExcelEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {

    private val component: JComponent
    private val table: JBTable
    private val tableModel: TestCaseTableModel
    private val transferHandler: ExcelTableTransferHandler
    private val contextMenu: ExcelContextMenu
    private val gitStatusIndicator: GitStatusIndicator
    private val gitIntegration: GitIntegration

    companion object {
        val EDITOR_NAME = Key.create<String>("TESTCASE_EXCEL_EDITOR")
    }

    init {
        // 初始化 Git 集成
        gitIntegration = GitIntegration.getInstance(project)

        // 创建表格模型
        tableModel = TestCaseTableModel()

        // 创建表格组件
        table = createTable()

        // 创建拖拽处理器
        transferHandler = ExcelTableTransferHandler(tableModel)
        table.transferHandler = transferHandler

        // 创建右键菜单
        contextMenu = ExcelContextMenu(table, tableModel)

        // 创建 Git 状态指示器
        gitStatusIndicator = GitStatusIndicator(project, file)

        // 创建主界面
        component = createComponent()

        // 加载文件数据
        loadFileData()

        // 刷新 Git 状态
        refreshGitStatus()
    }

    /**
     * 创建表格组件
     */
    private fun createTable(): JBTable {
        return JBTable(tableModel).apply {
            // 设置表格属性
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            rowHeight = 30
            preferredScrollableViewportSize = Dimension(800, 600)

            // 设置表头
            tableHeader.apply {
                preferredSize = Dimension(preferredSize.width, 35)
                reorderingAllowed = true
                resizingAllowed = true
            }

            // 设置选择模式
            setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            setCellSelectionEnabled(true)

            // 设置网格线
            showHorizontalLines = true
            showVerticalLines = true
            gridColor = java.awt.Color(200, 200, 200)

            // 启用拖拽
            dragEnabled = true
            dropMode = javax.swing.DropMode.INSERT_ROWS

            // 设置自定义单元格编辑器
            setupCellEditors()

            // 设置右键菜单
            setupContextMenu()

            // 设置键盘快捷键
            setupKeyboardShortcuts()
        }
    }

    /**
     * 设置单元格编辑器
     */
    private fun JTable.setupCellEditors() {
        for (col in 0 until tableModel.columnCount) {
            val editor = ExcelCellEditorFactory.createEditor(col)
            columnModel.getColumn(col).cellEditor = editor
        }
    }

    /**
     * 设置右键菜单
     */
    private fun JTable.setupContextMenu() {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e)
                }
            }

            private fun showContextMenu(e: MouseEvent) {
                // 如果点击位置没有选中行，则选中该行
                val row = rowAtPoint(e.point)
                val col = columnAtPoint(e.point)

                if (row >= 0) {
                    if (!isRowSelected(row)) {
                        setRowSelectionInterval(row, row)
                    }
                    if (col >= 0) {
                        setColumnSelectionInterval(col, col)
                    }
                }

                contextMenu.getPopupMenu().show(e.component, e.x, e.y)
            }
        })
    }

    /**
     * 设置键盘快捷键
     */
    private fun JTable.setupKeyboardShortcuts() {
        // Ctrl+C 复制
        getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK),
            "copy"
        )
        actionMap.put("copy", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                transferHandler.copyToClipboard(this@setupKeyboardShortcuts)
            }
        })

        // Ctrl+V 粘贴
        getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK),
            "paste"
        )
        actionMap.put("paste", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val row = selectedRow
                val col = selectedColumn
                if (row >= 0 && col >= 0) {
                    transferHandler.pasteFromClipboard(this@setupKeyboardShortcuts, row, col)
                }
            }
        })

        // Delete 删除选中行
        getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            "deleteRow"
        )
        actionMap.put("deleteRow", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                removeSelectedRow()
            }
        })

        // Ctrl+N 添加新行
        getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK),
            "addRow"
        )
        actionMap.put("addRow", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                addRow()
            }
        })
    }

    /**
     * 创建主界面组件
     */
    private fun createComponent(): JComponent {
        return JPanel(BorderLayout()).apply {
            // 添加 Git 状态指示器
            add(gitStatusIndicator, BorderLayout.NORTH)

            // 添加工具栏
            add(createToolBar(), BorderLayout.CENTER)

            // 创建内容面板（包含表格）
            val contentPanel = JPanel(BorderLayout())
            contentPanel.add(JBScrollPane(table), BorderLayout.CENTER)
            contentPanel.add(createStatusBar(), BorderLayout.SOUTH)

            add(contentPanel, BorderLayout.SOUTH)
        }
    }

    /**
     * 创建工具栏
     */
    private fun createToolBar(): JComponent {
        return javax.swing.JToolBar().apply {
            isFloatable = false

            // 添加行按钮
            add(javax.swing.JButton("添加行 (Ctrl+N)").apply {
                addActionListener { addRow() }
            })

            add(javax.swing.JButton("删除行 (Del)").apply {
                addActionListener { removeSelectedRow() }
            })

            add(javax.swing.JToolBar.Separator())

            // 插入行按钮
            add(javax.swing.JButton("上方插入").apply {
                addActionListener { insertRowAbove() }
            })

            add(javax.swing.JButton("下方插入").apply {
                addActionListener { insertRowBelow() }
            })

            add(javax.swing.JToolBar.Separator())

            // 复制行按钮
            add(javax.swing.JButton("复制行").apply {
                addActionListener { copySelectedRow() }
            })

            add(javax.swing.JToolBar.Separator())

            // 保存按钮
            add(javax.swing.JButton("保存").apply {
                addActionListener { saveFile() }
            })

            add(javax.swing.JToolBar.Separator())

            // 刷新按钮
            add(javax.swing.JButton("刷新").apply {
                addActionListener { loadFileData() }
            })

            add(javax.swing.JToolBar.Separator())

            // Git 对比按钮
            add(javax.swing.JButton("Git 对比").apply {
                addActionListener { showGitDiff() }
            })

            add(javax.swing.JToolBar.Separator())

            // Git 状态刷新按钮
            add(javax.swing.JButton("刷新 Git 状态").apply {
                addActionListener { refreshGitStatus() }
            })
        }
    }

    /**
     * 创建状态栏
     */
    private fun createStatusBar(): JComponent {
        return javax.swing.JLabel("就绪 - 共 0 行").apply {
            border = javax.swing.border.EmptyBorder(5, 10, 5, 10)
        }
    }

    /**
     * 添加新行
     */
    private fun addRow() {
        tableModel.addRow(arrayOf("", "", "P1", "草稿", "", ""))
        val newRow = tableModel.rowCount - 1
        table.setRowSelectionInterval(newRow, newRow)
        table.scrollRectToVisible(table.getCellRect(newRow, 0, true))
        updateStatusBar()
    }

    /**
     * 在选中行上方插入新行
     */
    private fun insertRowAbove() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            tableModel.insertRowAt(selectedRow, arrayOf("", "", "P1", "草稿", "", ""))
            table.setRowSelectionInterval(selectedRow, selectedRow)
            updateStatusBar()
        }
    }

    /**
     * 在选中行下方插入新行
     */
    private fun insertRowBelow() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            tableModel.insertRowAt(selectedRow + 1, arrayOf("", "", "P1", "草稿", "", ""))
            table.setRowSelectionInterval(selectedRow + 1, selectedRow + 1)
            updateStatusBar()
        }
    }

    /**
     * 复制选中行
     */
    private fun copySelectedRow() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            tableModel.copyRow(selectedRow)
            table.setRowSelectionInterval(selectedRow + 1, selectedRow + 1)
            updateStatusBar()
        }
    }

    /**
     * 删除选中行
     */
    private fun removeSelectedRow() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow)
            updateStatusBar()
        }
    }

    /**
     * 更新状态栏
     */
    private fun updateStatusBar() {
        val statusBar = (component as JPanel).getComponent(2) as javax.swing.JLabel
        statusBar.text = "就绪 - 共 ${tableModel.rowCount} 行"
    }

    /**
     * 显示 Git Diff 对比视图
     */
    private fun showGitDiff() {
        if (!gitIntegration.isGitEnabled()) {
            javax.swing.JOptionPane.showMessageDialog(
                component,
                "当前项目未启用 Git 版本控制",
                "Git 不可用",
                javax.swing.JOptionPane.WARNING_MESSAGE
            )
            return
        }

        if (!gitIntegration.isFileUnderGit(file)) {
            javax.swing.JOptionPane.showMessageDialog(
                component,
                "文件 '${file.name}' 不在 Git 版本控制下",
                "Git 对比",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        try {
            // 使用 IntelliJ 内置 Diff 工具
            val diffHelper = com.testcase.manager.git.DiffRequestHelper(project)
            diffHelper.showDiffWithHead(file)
        } catch (e: Exception) {
            javax.swing.JOptionPane.showMessageDialog(
                component,
                "打开对比视图失败: ${e.message}",
                "错误",
                javax.swing.JOptionPane.ERROR_MESSAGE
            )
        }
    }

    /**
     * 刷新 Git 状态
     */
    private fun refreshGitStatus() {
        gitStatusIndicator.updateStatus()
    }

    /**
     * 加载文件数据
     * 目前显示空白表格，后续实现 YAML 解析
     */
    private fun loadFileData() {
        // 清空现有数据
        tableModel.clearData()

        // 添加示例数据（用于展示）
        tableModel.addRow(arrayOf("TC001", "登录成功", "P0", "已发布", "1. 打开登录页\n2. 输入用户名密码\n3. 点击登录", "登录成功，跳转首页"))
        tableModel.addRow(arrayOf("TC002", "登录失败-密码错误", "P1", "已发布", "1. 打开登录页\n2. 输入错误密码\n3. 点击登录", "提示密码错误"))
        tableModel.addRow(arrayOf("TC003", "忘记密码", "P2", "草稿", "1. 点击忘记密码\n2. 输入邮箱\n3. 点击发送", "收到重置邮件"))

        updateStatusBar()
    }

    /**
     * 保存文件
     * 目前仅打印日志，后续实现 YAML 序列化
     */
    private fun saveFile() {
        // TODO: 实现 YAML 序列化
        javax.swing.JOptionPane.showMessageDialog(
            component,
            "保存功能将在后续实现",
            "提示",
            javax.swing.JOptionPane.INFORMATION_MESSAGE
        )
    }

    // ==================== FileEditor 接口实现 ====================

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = table

    override fun getName(): String = "Excel Editor"

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
}
