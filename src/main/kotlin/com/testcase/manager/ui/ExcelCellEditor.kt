package com.testcase.manager.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.AbstractCellEditor
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.TableCellEditor

/**
 * 单元格编辑器类型枚举
 */
enum class CellEditorType {
    TEXT,       // 纯文本
    NUMBER,     // 数字（带验证）
    PRIORITY,   // 优先级下拉选择
    STATUS      // 状态下拉选择
}

/**
 * Excel 风格单元格编辑器
 * 支持文本、数字、下拉选择等多种编辑类型
 */
class ExcelCellEditor(private val editorType: CellEditorType) : AbstractCellEditor(), TableCellEditor {

    private var editorComponent: JComponent? = null
    private var currentValue: Any? = null

    // 优先级选项
    private val priorityOptions = arrayOf("P0", "P1", "P2", "P3")

    // 状态选项
    private val statusOptions = arrayOf("草稿", "已发布", "已废弃", "已归档")

    override fun getCellEditorValue(): Any? = currentValue

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        currentValue = value

        editorComponent = when (editorType) {
            CellEditorType.TEXT -> createTextEditor(value)
            CellEditorType.NUMBER -> createNumberEditor(value)
            CellEditorType.PRIORITY -> createPriorityEditor(value)
            CellEditorType.STATUS -> createStatusEditor(value)
        }

        return editorComponent!!
    }

    /**
     * 创建文本编辑器
     */
    private fun createTextEditor(value: Any?): JBTextField {
        return JBTextField(value?.toString() ?: "").apply {
            // 选中所有文本，方便快速编辑
            selectAll()

            // 添加焦点丢失时的处理
            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    stopCellEditing()
                }
            })

            // 添加回车键确认编辑
            addActionListener {
                currentValue = text
                fireEditingStopped()
            }
        }
    }

    /**
     * 创建数字编辑器（带验证）
     */
    private fun createNumberEditor(value: Any?): JBTextField {
        return JBTextField(value?.toString() ?: "").apply {
            selectAll()

            addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    if (validateNumber(text)) {
                        stopCellEditing()
                    }
                }
            })

            addActionListener {
                if (validateNumber(text)) {
                    currentValue = text
                    fireEditingStopped()
                }
            }
        }
    }

    /**
     * 验证数字输入
     */
    private fun validateNumber(text: String): Boolean {
        return text.isEmpty() || text.matches(Regex("^-?\\d*\\.?\\d*$"))
    }

    /**
     * 创建优先级下拉编辑器
     */
    private fun createPriorityEditor(value: Any?): ComboBox<String> {
        return ComboBox(priorityOptions).apply {
            selectedItem = value?.toString() ?: "P1"

            addActionListener {
                currentValue = selectedItem
                fireEditingStopped()
            }
        }
    }

    /**
     * 创建状态下拉编辑器
     */
    private fun createStatusEditor(value: Any?): ComboBox<String> {
        return ComboBox(statusOptions).apply {
            selectedItem = value?.toString() ?: "草稿"

            addActionListener {
                currentValue = selectedItem
                fireEditingStopped()
            }
        }
    }

    /**
     * 停止编辑并保存值
     */
    override fun stopCellEditing(): Boolean {
        editorComponent?.let {
            when (it) {
                is JBTextField -> currentValue = it.text
                is ComboBox<*> -> currentValue = it.selectedItem
            }
        }
        return super.stopCellEditing()
    }
}

/**
 * 单元格编辑器工厂
 * 根据列索引创建对应的编辑器
 */
object ExcelCellEditorFactory {

    // 列索引定义
    const val COL_ID = 0
    const val COL_NAME = 1
    const val COL_PRIORITY = 2
    const val COL_STATUS = 3
    const val COL_STEPS = 4
    const val COL_EXPECTED = 5

    /**
     * 根据列索引获取编辑器类型
     */
    fun getEditorType(column: Int): CellEditorType {
        return when (column) {
            COL_PRIORITY -> CellEditorType.PRIORITY
            COL_STATUS -> CellEditorType.STATUS
            COL_ID -> CellEditorType.NUMBER
            else -> CellEditorType.TEXT
        }
    }

    /**
     * 创建对应列的编辑器
     */
    fun createEditor(column: Int): ExcelCellEditor {
        return ExcelCellEditor(getEditorType(column))
    }
}
