package com.testcase.manager.ui.sort

import javax.swing.table.TableModel

/**
 * 排序方向枚举
 */
enum class SortDirection {
    ASCENDING,
    DESCENDING,
    NONE;

    /**
     * 切换到下一个排序方向
     */
    fun next(): SortDirection {
        return when (this) {
            NONE -> ASCENDING
            ASCENDING -> DESCENDING
            DESCENDING -> NONE
        }
    }

    /**
     * 获取显示符号
     */
    fun getSymbol(): String {
        return when (this) {
            ASCENDING -> "▲"
            DESCENDING -> "▼"
            NONE -> ""
        }
    }
}

/**
 * 单列排序规则
 */
data class SortRule(
    val columnIndex: Int,
    val direction: SortDirection = SortDirection.ASCENDING
)

/**
 * 表格排序器
 * 支持多列排序
 */
class TableSorter(private val sourceModel: TableModel) {

    private var sortRules: List<SortRule> = emptyList()
    private var sortedIndices: List<Int> = emptyList()
    private var listeners: MutableList<SortListener> = mutableListOf()

    /**
     * 排序监听器接口
     */
    interface SortListener {
        fun onSortChanged()
    }

    /**
     * 添加排序变化监听器
     */
    fun addSortListener(listener: SortListener) {
        listeners.add(listener)
    }

    /**
     * 移除排序变化监听器
     */
    fun removeSortListener(listener: SortListener) {
        listeners.remove(listener)
    }

    /**
     * 通知所有监听器排序已变化
     */
    private fun notifySortChanged() {
        listeners.forEach { it.onSortChanged() }
    }

    /**
     * 获取当前排序规则
     */
    fun getSortRules(): List<SortRule> = sortRules.toList()

    /**
     * 获取指定列的排序方向
     */
    fun getSortDirection(columnIndex: Int): SortDirection {
        return sortRules.find { it.columnIndex == columnIndex }?.direction ?: SortDirection.NONE
    }

    /**
     * 切换指定列的排序
     * 支持多列排序（按住 Shift 键）
     */
    fun toggleSort(columnIndex: Int, multiSort: Boolean = false) {
        val existingRule = sortRules.find { it.columnIndex == columnIndex }

        if (existingRule != null) {
            val newDirection = existingRule.direction.next()
            if (newDirection == SortDirection.NONE) {
                // 移除该列的排序
                sortRules = sortRules.filter { it.columnIndex != columnIndex }
            } else {
                // 更新排序方向
                sortRules = sortRules.map {
                    if (it.columnIndex == columnIndex) it.copy(direction = newDirection)
                    else it
                }
            }
        } else {
            // 添加新的排序规则
            val newRule = SortRule(columnIndex, SortDirection.ASCENDING)
            sortRules = if (multiSort) {
                sortRules + newRule
            } else {
                listOf(newRule)
            }
        }

        applySort()
        notifySortChanged()
    }

    /**
     * 设置排序规则
     */
    fun setSortRules(rules: List<SortRule>) {
        this.sortRules = rules.filter { it.direction != SortDirection.NONE }
        applySort()
        notifySortChanged()
    }

    /**
     * 清除所有排序
     */
    fun clearSort() {
        sortRules = emptyList()
        sortedIndices = (0 until sourceModel.rowCount).toList()
        notifySortChanged()
    }

    /**
     * 应用排序逻辑
     */
    private fun applySort() {
        if (sortRules.isEmpty()) {
            sortedIndices = (0 until sourceModel.rowCount).toList()
            return
        }

        sortedIndices = (0 until sourceModel.rowCount).sortedWith { row1, row2 ->
            compareRows(row1, row2)
        }
    }

    /**
     * 比较两行数据
     */
    private fun compareRows(row1: Int, row2: Int): Int {
        for (rule in sortRules) {
            val comparison = compareCells(row1, row2, rule.columnIndex)
            if (comparison != 0) {
                return if (rule.direction == SortDirection.ASCENDING) comparison else -comparison
            }
        }
        return 0
    }

    /**
     * 比较两个单元格的值
     */
    @Suppress("UNCHECKED_CAST")
    private fun compareCells(row1: Int, row2: Int, columnIndex: Int): Int {
        val value1 = sourceModel.getValueAt(row1, columnIndex)
        val value2 = sourceModel.getValueAt(row2, columnIndex)

        // 处理 null 值
        if (value1 == null && value2 == null) return 0
        if (value1 == null) return -1
        if (value2 == null) return 1

        // 根据类型进行比较
        return when {
            value1 is Comparable<*> && value2 is Comparable<*> -> {
                try {
                    (value1 as Comparable<Any>).compareTo(value2)
                } catch (e: ClassCastException) {
                    value1.toString().compareTo(value2.toString())
                }
            }
            else -> value1.toString().compareTo(value2.toString())
        }
    }

    /**
     * 获取排序后的行数
     */
    fun getRowCount(): Int = sortedIndices.size

    /**
     * 将排序后的行索引映射到源模型行索引
     */
    fun getSourceRowIndex(sortedRowIndex: Int): Int {
        return if (sortedRowIndex in sortedIndices.indices) {
            sortedIndices[sortedRowIndex]
        } else {
            -1
        }
    }

    /**
     * 将源模型行索引映射到排序后的行索引
     */
    fun getSortedRowIndex(sourceRowIndex: Int): Int {
        return sortedIndices.indexOf(sourceRowIndex)
    }

    /**
     * 获取所有排序后的源行索引
     */
    fun getSortedIndices(): List<Int> = sortedIndices.toList()

    /**
     * 刷新排序（当源数据变化时调用）
     */
    fun refresh() {
        applySort()
        notifySortChanged()
    }

    /**
     * 是否已排序
     */
    fun isSorted(): Boolean = sortRules.isNotEmpty()

    /**
     * 获取主排序列（第一个排序规则）
     */
    fun getPrimarySortColumn(): Int? = sortRules.firstOrNull()?.columnIndex

    /**
     * 获取排序描述文本
     */
    fun getSortDescription(): String {
        if (sortRules.isEmpty()) return "未排序"

        return sortRules.mapIndexed { index, rule ->
            val columnName = sourceModel.getColumnName(rule.columnIndex)
            val orderNum = if (sortRules.size > 1) "${index + 1}. " else ""
            "$orderNum$columnName ${rule.direction.getSymbol()}"
        }.joinToString(", ")
    }
}
