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
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import com.testcase.manager.yaml.YamlParser
import com.testcase.manager.yaml.YamlSerializer
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Excel 风格编辑器（简化版）
 * 为测试用例 YAML 文件提供表格编辑界面
 */
class ExcelEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {

    private val component: JComponent
    private val table: JBTable
    private val tableModel: TestCaseTableModel
    private val yamlParser = YamlParser()
    private val yamlSerializer = YamlSerializer()

    companion object {
        val EDITOR_NAME = Key.create<String>("TESTCASE_EXCEL_EDITOR")
    }

    init {
        // 创建表格模型
        tableModel = TestCaseTableModel()

        // 创建表格
        table = JBTable(tableModel).apply {
            setShowGrid(true)
            gridColor = java.awt.Color.LIGHT_GRAY
            autoResizeMode = JBTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            rowHeight = 28

            // 设置列宽
            columnModel.getColumn(0).preferredWidth = 80
            columnModel.getColumn(1).preferredWidth = 200
            columnModel.getColumn(2).preferredWidth = 80
            columnModel.getColumn(3).preferredWidth = 80
            columnModel.getColumn(4).preferredWidth = 200
            columnModel.getColumn(5).preferredWidth = 200
        }

        // 创建主面板
        component = JPanel(BorderLayout()).apply {
            add(JBScrollPane(table), BorderLayout.CENTER)
        }

        // 加载 YAML 数据
        loadYamlData()
    }

    /**
     * 加载 YAML 数据
     */
    private fun loadYamlData() {
        try {
            val model = yamlParser.parse(file)
            tableModel.clear()
            model.testCases.forEach { testCase ->
                tableModel.addRow(testCase)
            }
        } catch (e: Exception) {
            // 解析失败时创建空模型
            tableModel.clear()
        }
    }

    /**
     * 保存数据到 YAML
     */
    fun saveToYaml() {
        try {
            val testCases = tableModel.getAllTestCases()
            val model = TestCaseModel()
            testCases.forEach { model.addTestCase(it) }
            val yaml = yamlSerializer.serialize(model)

            // 写入文件
            com.intellij.openapi.vfs.VfsUtil.saveText(file, yaml)
        } catch (e: Exception) {
            com.intellij.openapi.diagnostic.Logger.getInstance(ExcelEditor::class.java)
                .error("Failed to save YAML", e)
        }
    }

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = table

    override fun getName(): String = "TestCase Excel Editor"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        Disposer.dispose(this)
    }
}
