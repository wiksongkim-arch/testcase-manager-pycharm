package com.testcase.manager.ui.filter

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import javax.swing.table.TableModel

/**
 * 表格筛选器
 * 处理行数据的筛选逻辑
 */
class TableFilter(private val sourceModel: TableModel) {

    private var filterCriteria: FilterCriteria = FilterCriteria()
    private var filteredIndices: List<Int> = emptyList()
    private var listeners: MutableList<FilterListener> = mutableListOf()

    // 列索引映射（根据 TestCaseTableModel 的列定义）
    companion object {
        const val COL_ID = 0
        const val COL_NAME = 1
        const val COL_PRIORITY = 2
        const val COL_STATUS = 3
        const val COL_STEPS = 4
        const val COL_EXPECTED = 5
    }

    /**
     * 筛选监听器接口
     */
    interface FilterListener {
        fun onFilterChanged()
    }

    /**
     * 添加筛选变化监听器
     */
    fun addFilterListener(listener: FilterListener) {
        listeners.add(listener)
    }

    /**
     * 移除筛选变化监听器
     */
    fun removeFilterListener(listener: FilterListener) {
        listeners.remove(listener)
    }

    /**
     * 通知所有监听器筛选已变化
     */
    private fun notifyFilterChanged() {
        listeners.forEach { it.onFilterChanged() }
    }

    /**
     * 获取当前筛选条件
     */
    fun getCriteria(): FilterCriteria = filterCriteria

    /**
     * 设置筛选条件并应用筛选
     */
    fun setCriteria(criteria: FilterCriteria) {
        this.filterCriteria = criteria
        applyFilter()
        notifyFilterChanged()
    }

    /**
     * 应用筛选逻辑
     */
    private fun applyFilter() {
        if (!filterCriteria.isActive()) {
            // 无筛选条件，显示所有行
            filteredIndices = (0 until sourceModel.rowCount).toList()
            return
        }

        filteredIndices = (0 until sourceModel.rowCount).filter { rowIndex ->
            matchesCriteria(rowIndex)
        }
    }

    /**
     * 检查指定行是否匹配筛选条件
     */
    private fun matchesCriteria(rowIndex: Int): Boolean {
        // 检查优先级筛选
        if (filterCriteria.priorities.isNotEmpty()) {
            val priorityValue = sourceModel.getValueAt(rowIndex, COL_PRIORITY)?.toString() ?: ""
            val priority = Priority.fromValue(priorityValue)
            if (priority !in filterCriteria.priorities) {
                return false
            }
        }

        // 检查状态筛选
        if (filterCriteria.statuses.isNotEmpty()) {
            val statusValue = sourceModel.getValueAt(rowIndex, COL_STATUS)?.toString() ?: ""
            val status = Status.fromValue(statusValue)
            if (status !in filterCriteria.statuses) {
                return false
            }
        }

        // 检查文本搜索
        if (filterCriteria.searchText.isNotBlank()) {
            if (!matchesSearch(rowIndex)) {
                return false
            }
        }

        return true
    }

    /**
     * 检查指定行是否匹配搜索文本
     */
    private fun matchesSearch(rowIndex: Int): Boolean {
        val searchText = filterCriteria.searchText.lowercase()
        val columnsToSearch = if (filterCriteria.searchColumns.isEmpty()) {
            // 搜索所有列
            (0 until sourceModel.columnCount).toList()
        } else {
            filterCriteria.searchColumns.toList()
        }

        return columnsToSearch.any { colIndex ->
            val cellValue = sourceModel.getValueAt(rowIndex, colIndex)?.toString() ?: ""
            cellValue.lowercase().contains(searchText)
        }
    }

    /**
     * 获取筛选后的行数
     */
    fun getRowCount(): Int = filteredIndices.size

    /**
     * 将筛选后的行索引映射到源模型行索引
     */
    fun getSourceRowIndex(filteredRowIndex: Int): Int {
        return if (filteredRowIndex in filteredIndices.indices) {
            filteredIndices[filteredRowIndex]
        } else {
            -1
        }
    }

    /**
     * 将源模型行索引映射到筛选后的行索引
     */
    fun getFilteredRowIndex(sourceRowIndex: Int): Int {
        return filteredIndices.indexOf(sourceRowIndex)
    }

    /**
     * 获取所有筛选后的源行索引
     */
    fun getFilteredIndices(): List<Int> = filteredIndices.toList()

    /**
     * 清空筛选条件
     */
    fun clearFilter() {
        setCriteria(FilterCriteria())
    }

    /**
     * 刷新筛选（当源数据变化时调用）
     */
    fun refresh() {
        applyFilter()
        notifyFilterChanged()
    }

    /**
     * 获取筛选结果统计信息
     */
    fun getFilterStats(): FilterStats {
        val totalRows = sourceModel.rowCount
        val filteredRows = filteredIndices.size
        return FilterStats(
            totalRows = totalRows,
            filteredRows = filteredRows,
            hiddenRows = totalRows - filteredRows,
            isFiltered = filterCriteria.isActive()
        )
    }
}

/**
 * 筛选统计信息
 */
data class FilterStats(
    val totalRows: Int,
    val filteredRows: Int,
    val hiddenRows: Int,
    val isFiltered: Boolean
)
