package com.testcase.manager.formula

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.ListSelectionModel

/**
 * 公式编辑器对话框
 */
class FormulaEditorDialog(
    private val initialFormula: String = "",
    private val cellReference: String = "",
    private val context: FormulaContext
) : DialogWrapper(true) {

    private lateinit var formulaField: JBTextField
    private lateinit var resultLabel: JLabel
    private lateinit var suggestionList: JList<String>
    private lateinit var functionDescription: JTextArea
    private lateinit var helpTable: JBTable

    init {
        title = "公式编辑器 - $cellReference"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.preferredSize = Dimension(700, 500)

        // 公式输入区域
        panel.add(createFormulaPanel(), BorderLayout.NORTH)

        // 中间分割面板
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = createSuggestionPanel()
        splitPane.rightComponent = createHelpPanel()
        splitPane.dividerLocation = 300
        panel.add(splitPane, BorderLayout.CENTER)

        // 结果显示
        panel.add(createResultPanel(), BorderLayout.SOUTH)

        return panel
    }

    /**
     * 创建公式输入面板
     */
    private fun createFormulaPanel(): JComponent {
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = BorderFactory.createTitledBorder("公式")

        // 公式输入框
        formulaField = JBTextField(initialFormula).apply {
            font = font.deriveFont(14f)
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateFormula()
                override fun removeUpdate(e: DocumentEvent?) = updateFormula()
                override fun changedUpdate(e: DocumentEvent?) = updateFormula()
            })
        }

        panel.add(JLabel("="), BorderLayout.WEST)
        panel.add(formulaField, BorderLayout.CENTER)

        // 常用按钮
        val buttonPanel = JPanel()
        buttonPanel.add(createFunctionButton("SUM"))
        buttonPanel.add(createFunctionButton("COUNT"))
        buttonPanel.add(createFunctionButton("IF"))
        buttonPanel.add(createFunctionButton("CONCAT"))
        buttonPanel.add(createFunctionButton("AVERAGE"))
        buttonPanel.add(createFunctionButton("MAX"))
        buttonPanel.add(createFunctionButton("MIN"))
        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    /**
     * 创建函数按钮
     */
    private fun createFunctionButton(functionName: String): JButton {
        return JButton(functionName).apply {
            addActionListener {
                insertFunction(functionName)
            }
        }
    }

    /**
     * 创建建议面板
     */
    private fun createSuggestionPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("函数建议")

        suggestionList = JList()
        suggestionList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        suggestionList.addListSelectionListener {
            val selected = suggestionList.selectedValue
            if (selected != null) {
                updateFunctionDescription(selected)
            }
        }
        suggestionList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = suggestionList.selectedValue
                    if (selected != null) {
                        insertFunction(selected.substringBefore(" "))
                    }
                }
            }
        })

        panel.add(JBScrollPane(suggestionList), BorderLayout.CENTER)

        return panel
    }

    /**
     * 创建帮助面板
     */
    private fun createHelpPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("函数帮助")

        functionDescription = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = font.deriveFont(12f)
        }
        panel.add(JBScrollPane(functionDescription), BorderLayout.NORTH)

        // 函数列表表格
        val engine = FormulaEngine()
        val functions = engine.getAllFunctions()
        val data = functions.map {
            arrayOf(
                it.name,
                it.description,
                it.syntax
            )
        }.toTypedArray()

        val model = object : DefaultTableModel(data, arrayOf("函数", "描述", "语法")) {
            override fun isCellEditable(row: Int, column: Int) = false
        }

        helpTable = JBTable(model).apply {
            autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
            columnModel.getColumn(0).preferredWidth = 80
            columnModel.getColumn(1).preferredWidth = 200
            columnModel.getColumn(2).preferredWidth = 250
            selectionMode = ListSelectionModel.SINGLE_SELECTION

            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    val row = rowAtPoint(e.point)
                    if (row >= 0) {
                        val functionName = getValueAt(row, 0) as String
                        if (e.clickCount == 2) {
                            insertFunction(functionName)
                        } else {
                            updateFunctionDescription(functionName)
                        }
                    }
                }
            })
        }

        panel.add(JBScrollPane(helpTable), BorderLayout.CENTER)

        return panel
    }

    /**
     * 创建结果面板
     */
    private fun createResultPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("计算结果")

        resultLabel = JLabel("准备就绪")
        panel.add(resultLabel, BorderLayout.CENTER)

        return panel
    }

    /**
     * 更新公式
     */
    private fun updateFormula() {
        val formula = formulaField.text

        // 更新建议列表
        updateSuggestions(formula)

        // 计算结果
        if (formula.isNotEmpty()) {
            val engine = FormulaEngine()
            val result = engine.evaluate("=$formula", context)
            resultLabel.text = when (result) {
                is FormulaValue.Error -> "错误: ${result.message}"
                else -> "结果: ${result.toText()}"
            }
        } else {
            resultLabel.text = "准备就绪"
        }
    }

    /**
     * 更新建议列表
     */
    private fun updateSuggestions(formula: String) {
        val engine = FormulaEngine()

        // 获取当前输入的单词
        val lastWord = formula.substringAfterLast(",").substringAfterLast("(").substringAfterLast(" ").trim()

        if (lastWord.length >= 1) {
            val suggestions = engine.getFunctionSuggestions(lastWord)
            val listData = suggestions.map { "${it.name} - ${it.description}" }.toTypedArray()
            suggestionList.setListData(listData)
        } else {
            suggestionList.setListData(emptyArray())
        }
    }

    /**
     * 更新函数描述
     */
    private fun updateFunctionDescription(functionName: String) {
        val engine = FormulaEngine()
        val function = engine.getAllFunctions().find { it.name == functionName }
        if (function != null) {
            val description = buildString {
                appendLine("函数: ${function.name}")
                appendLine()
                appendLine("描述: ${function.description}")
                appendLine()
                appendLine("语法: ${function.syntax}")
                appendLine()
                appendLine("示例:")
                function.examples.forEach { example ->
                    appendLine("  $example")
                }
            }
            functionDescription.text = description
        }
    }

    /**
     * 插入函数
     */
    private fun insertFunction(functionName: String) {
        val engine = FormulaEngine()
        val function = engine.getAllFunctions().find { it.name == functionName }

        val insertText = if (function != null) {
            // 从语法中提取参数部分
            val syntax = function.syntax
            val paramPart = syntax.substringAfter("(", "").substringBefore(")", "")
            if (paramPart.isNotEmpty()) {
                "$functionName($paramPart)"
            } else {
                "$functionName()"
            }
        } else {
            "$functionName()"
        }

        val currentText = formulaField.text
        val caretPosition = formulaField.caretPosition

        // 在当前光标位置插入
        val newText = currentText.substring(0, caretPosition) + insertText + currentText.substring(caretPosition)
        formulaField.text = newText

        // 将光标放在括号内
        val newCaretPos = caretPosition + functionName.length + 1
        formulaField.caretPosition = newCaretPos

        formulaField.requestFocus()
    }

    /**
     * 获取编辑后的公式
     */
    fun getFormula(): String {
        return formulaField.text
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return formulaField
    }
}
