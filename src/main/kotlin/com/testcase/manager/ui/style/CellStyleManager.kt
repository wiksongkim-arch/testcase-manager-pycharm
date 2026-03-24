package com.testcase.manager.ui.style

import com.testcase.manager.ui.filter.TableFilter
import com.testcase.manager.ui.sort.TableSorter
import javax.swing.JTable

/**
 * 单元格样式管理器
 * 管理单元格样式的获取和应用
 */
class CellStyleManager {

    // 条件格式管理器
    var conditionalFormatManager: ConditionalFormatManager? = null

    // 自定义样式映射（row -> column -> style）
    private val customStyles: MutableMap<Int, MutableMap<Int, CellStyle>> = mutableMapOf()

    // 列默认样式
    private val columnDefaultStyles: MutableMap<Int, CellStyle> = mutableMapOf()

    // 全局默认样式
    var globalDefaultStyle: CellStyle? = null

    // 筛选器（用于获取原始行索引）
    var filter: TableFilter? = null

    // 排序器（用于获取原始行索引）
    var sorter: TableSorter? = null

    /**
     * 获取单元格样式
     * 优先级：自定义样式 > 条件格式 > 列默认样式 > 全局默认样式
     */
    fun getCellStyle(table: JTable, row: Int, column: Int): CellStyle? {
        // 获取原始行索引（考虑筛选和排序）
        val sourceRow = getSourceRowIndex(row)

        // 获取单元格值
        val value = table.model.getValueAt(sourceRow, column)

        // 1. 检查自定义样式
        val customStyle = customStyles[sourceRow]?.get(column)
        if (customStyle != null) {
            return customStyle
        }

        // 2. 应用条件格式
        var style = conditionalFormatManager?.evaluateCell(value, sourceRow, column)

        // 3. 应用列默认样式
        columnDefaultStyles[column]?.let { columnStyle ->
            style = style?.merge(columnStyle) ?: columnStyle
        }

        // 4. 应用全局默认样式
        globalDefaultStyle?.let { globalStyle ->
            style = style?.merge(globalStyle) ?: globalStyle
        }

        return style
    }

    /**
     * 设置单元格自定义样式
     */
    fun setCellStyle(row: Int, column: Int, style: CellStyle?) {
        if (style == null) {
            customStyles[row]?.remove(column)
            if (customStyles[row]?.isEmpty() == true) {
                customStyles.remove(row)
            }
        } else {
            customStyles.getOrPut(row) { mutableMapOf() }[column] = style
        }
    }

    /**
     * 设置整行样式
     */
    fun setRowStyle(row: Int, style: CellStyle?, columnCount: Int) {
        for (col in 0 until columnCount) {
            setCellStyle(row, col, style)
        }
    }

    /**
     * 设置整列默认样式
     */
    fun setColumnDefaultStyle(column: Int, style: CellStyle?) {
        if (style == null) {
            columnDefaultStyles.remove(column)
        } else {
            columnDefaultStyles[column] = style
        }
    }

    /**
     * 清除所有自定义样式
     */
    fun clearCustomStyles() {
        customStyles.clear()
    }

    /**
     * 清除指定行的自定义样式
     */
    fun clearRowStyles(row: Int) {
        customStyles.remove(row)
    }

    /**
     * 清除指定列的自定义样式
     */
    fun clearColumnStyles(column: Int) {
        customStyles.values.forEach { it.remove(column) }
    }

    /**
     * 获取源行索引（考虑筛选和排序）
     */
    private fun getSourceRowIndex(filteredRow: Int): Int {
        var row = filteredRow

        // 先应用筛选映射
        filter?.let {
            row = it.getSourceRowIndex(row)
        }

        // 再应用排序映射
        sorter?.let {
            row = it.getSourceRowIndex(row)
        }

        return row
    }

    /**
     * 批量设置单元格样式（用于范围选择）
     */
    fun setCellStyleRange(
        startRow: Int,
        startCol: Int,
        endRow: Int,
        endCol: Int,
        style: CellStyle
    ) {
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                setCellStyle(row, col, style)
            }
        }
    }

    /**
     * 复制单元格样式
     */
    fun copyCellStyle(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val style = customStyles[fromRow]?.get(fromCol)
        setCellStyle(toRow, toCol, style)
    }

    /**
     * 获取已应用自定义样式的单元格列表
     */
    fun getStyledCells(): List<Triple<Int, Int, CellStyle>> {
        return customStyles.flatMap { (row, colMap) ->
            colMap.map { (col, style) ->
                Triple(row, col, style)
            }
        }
    }
}
