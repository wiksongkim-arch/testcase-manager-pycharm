package com.testcase.manager.ui

import javax.swing.table.DefaultTableModel

/**
 * 测试用例数据模型
 * 定义表格列结构和数据类型
 */
class TestCaseTableModel : DefaultTableModel() {

    // 列定义
    private val columnNames = arrayOf(
        "ID",
        "用例名称",
        "优先级",
        "状态",
        "测试步骤",
        "预期结果"
    )

    // 列数据类型
    private val columnClasses = arrayOf(
        String::class.java,   // ID
        String::class.java,   // 用例名称
        String::class.java,   // 优先级
        String::class.java,   // 状态
        String::class.java,   // 测试步骤
        String::class.java    // 预期结果
    )

    // 列是否可编辑
    private val columnEditable = booleanArrayOf(
        true,   // ID
        true,   // 用例名称
        true,   // 优先级
        true,   // 状态
        true,   // 测试步骤
        true    // 预期结果
    )

    init {
        // 设置列名
        setColumnIdentifiers(columnNames)
    }

    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getColumnClass(columnIndex: Int): Class<*> = columnClasses[columnIndex]

    override fun isCellEditable(row: Int, column: Int): Boolean = columnEditable[column]

    /**
     * 在指定位置插入新行
     */
    fun insertRowAt(row: Int, data: Array<Any?>) {
        if (row < 0 || row > rowCount) {
            return
        }
        insertRow(row, data)
    }

    /**
     * 复制指定行
     */
    fun copyRow(row: Int): Boolean {
        if (row < 0 || row >= rowCount) {
            return false
        }

        val rowData = Array(columnCount) { col -> getValueAt(row, col) }
        insertRowAt(row + 1, rowData)
        return true
    }

    /**
     * 移动行到新的位置
     */
    fun moveRow(from: Int, to: Int): Boolean {
        if (from < 0 || from >= rowCount || to < 0 || to >= rowCount || from == to) {
            return false
        }

        // 保存源行数据
        val rowData = Array(columnCount) { col -> getValueAt(from, col) }

        // 删除源行
        removeRow(from)

        // 计算插入位置（如果目标位置在源行之后，需要减1）
        val insertPos = if (to > from) to - 1 else to

        // 插入到新位置
        insertRowAt(insertPos, rowData)

        return true
    }

    /**
     * 获取行数据
     */
    fun getRowData(row: Int): Array<Any?> {
        return Array(columnCount) { col -> getValueAt(row, col) }
    }

    /**
     * 设置行数据
     */
    fun setRowData(row: Int, data: Array<Any?>) {
        if (row < 0 || row >= rowCount || data.size != columnCount) {
            return
        }
        for (col in 0 until columnCount) {
            setValueAt(data[col], row, col)
        }
    }

    /**
     * 清空所有数据
     */
    fun clearData() {
        rowCount = 0
    }

    /**
     * 验证数据
     */
    fun validateData(row: Int, col: Int, value: Any?): Boolean {
        return when (col) {
            0 -> { // ID 列
                val text = value?.toString() ?: ""
                text.isNotEmpty() && text.matches(Regex("^[A-Za-z0-9_-]+$"))
            }
            1 -> { // 用例名称
                val text = value?.toString() ?: ""
                text.isNotEmpty()
            }
            else -> true
        }
    }
}
