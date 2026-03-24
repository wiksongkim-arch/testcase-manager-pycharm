package com.testcase.manager.performance

import javax.swing.table.AbstractTableModel

/**
 * 虚拟表格数据模型
 * 支持大数据集的异步加载
 */
abstract class VirtualTableModel : AbstractTableModel(), VirtualTableModel {

    // 估计的总行数
    protected var estimatedRows = 0

    // 已加载的数据缓存
    protected val dataCache = mutableMapOf<Int, Array<Any?>>()

    // 数据加载回调
    var onDataNeeded: ((startRow: Int, endRow: Int) -> Unit)? = null

    // 加载状态回调
    var onLoadingStateChanged: ((isLoading: Boolean) -> Unit)? = null

    // 是否正在加载
    var isLoading = false
        private set

    override fun getEstimatedRowCount(): Int = estimatedRows

    override fun getRowCount(): Int = dataCache.size

    /**
     * 设置估计的总行数
     */
    fun setEstimatedRowCount(count: Int) {
        val oldCount = estimatedRows
        estimatedRows = count
        if (count > oldCount) {
            fireTableRowsInserted(oldCount, count - 1)
        } else if (count < oldCount) {
            fireTableRowsDeleted(count, oldCount - 1)
        }
    }

    /**
     * 加载指定范围的数据
     */
    override fun loadData(startRow: Int, endRow: Int) {
        // 检查是否已有缓存
        val missingRows = (startRow..endRow).filter { !dataCache.containsKey(it) }

        if (missingRows.isNotEmpty()) {
            isLoading = true
            onLoadingStateChanged?.invoke(true)

            // 触发数据加载
            onDataNeeded?.invoke(missingRows.first(), missingRows.last())
        }
    }

    /**
     * 数据是否已加载
     */
    override fun isRowLoaded(row: Int): Boolean {
        return dataCache.containsKey(row)
    }

    /**
     * 添加数据到缓存
     */
    fun addData(rowIndex: Int, rowData: Array<Any?>) {
        dataCache[rowIndex] = rowData
        val sortedKeys = dataCache.keys.sorted()
        val actualIndex = sortedKeys.indexOf(rowIndex)
        fireTableRowsInserted(actualIndex, actualIndex)
    }

    /**
     * 批量添加数据
     */
    fun addDataBatch(startRow: Int, rows: List<Array<Any?>>) {
        if (rows.isEmpty()) return

        rows.forEachIndexed { index, rowData ->
            dataCache[startRow + index] = rowData
        }

        val sortedKeys = dataCache.keys.sorted()
        val firstIndex = sortedKeys.indexOf(startRow)
        val lastIndex = sortedKeys.indexOf(startRow + rows.size - 1)

        fireTableRowsInserted(firstIndex, lastIndex)

        isLoading = false
        onLoadingStateChanged?.invoke(false)
    }

    /**
     * 更新数据
     */
    fun updateData(rowIndex: Int, rowData: Array<Any?>) {
        dataCache[rowIndex] = rowData
        val sortedKeys = dataCache.keys.sorted()
        val actualIndex = sortedKeys.indexOf(rowIndex)
        fireTableRowsUpdated(actualIndex, actualIndex)
    }

    /**
     * 获取缓存的数据
     */
    fun getCachedData(rowIndex: Int): Array<Any?>? {
        return dataCache[rowIndex]
    }

    /**
     * 获取所有缓存的行索引
     */
    fun getCachedRowIndices(): List<Int> {
        return dataCache.keys.sorted()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        val oldSize = dataCache.size
        dataCache.clear()
        if (oldSize > 0) {
            fireTableRowsDeleted(0, oldSize - 1)
        }
    }

    /**
     * 获取实际显示的行索引对应的原始行索引
     */
    fun getOriginalRowIndex(displayRow: Int): Int {
        return dataCache.keys.sorted().getOrNull(displayRow) ?: displayRow
    }

    /**
     * 获取显示行索引对应的原始行索引
     */
    fun getDisplayRowIndex(originalRow: Int): Int {
        return dataCache.keys.sorted().indexOf(originalRow)
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val sortedKeys = dataCache.keys.sorted()
        val originalRow = sortedKeys.getOrNull(rowIndex) ?: return null
        return dataCache[originalRow]?.getOrNull(columnIndex)
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        val sortedKeys = dataCache.keys.sorted()
        val originalRow = sortedKeys.getOrNull(rowIndex) ?: return
        val rowData = dataCache[originalRow] ?: return
        if (columnIndex < rowData.size) {
            rowData[columnIndex] = aValue
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }

    /**
     * 预加载数据
     */
    fun preloadData(centerRow: Int, range: Int) {
        val startRow = (centerRow - range).coerceAtLeast(0)
        val endRow = (centerRow + range).coerceAtMost(estimatedRows - 1)
        loadData(startRow, endRow)
    }

    /**
     * 获取已加载的行数
     */
    fun getLoadedRowCount(): Int = dataCache.size

    /**
     * 获取缓存命中率统计
     */
    fun getCacheStats(): CacheStats {
        val totalRequests = dataCache.size
        val cachedRequests = dataCache.count { it.value.isNotEmpty() }
        return CacheStats(totalRequests, cachedRequests)
    }

    /**
     * 缓存统计
     */
    data class CacheStats(
        val totalRequests: Int,
        val cachedRequests: Int
    ) {
        val hitRate: Double
            get() = if (totalRequests > 0) cachedRequests.toDouble() / totalRequests else 0.0
    }
}
