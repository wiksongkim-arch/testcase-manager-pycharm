package com.testcase.manager.ui

import com.testcase.manager.ui.filter.TableFilter
import com.testcase.manager.ui.sort.TableSorter
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableModel

/**
 * 支持筛选和排序的表格模型包装器
 * 将筛选和排序功能透明地应用到基础表格模型
 */
class FilterSortTableModel(
    private val sourceModel: TestCaseTableModel
) : AbstractTableModel(), TableModelListener {

    val filter: TableFilter = TableFilter(sourceModel)
    val sorter: TableSorter = TableSorter(sourceModel)

    // 缓存的视图到模型的索引映射
    private var viewToModel: IntArray = IntArray(0)
    private var modelToView: IntArray = IntArray(0)

    init {
        // 监听源模型变化
        sourceModel.addTableModelListener(this)

        // 监听筛选变化
        filter.addFilterListener(object : TableFilter.FilterListener {
            override fun onFilterChanged() {
                rebuildIndexMapping()
                fireTableDataChanged()
            }
        })

        // 监听排序变化
        sorter.addSortListener(object : TableSorter.SortListener {
            override fun onSortChanged() {
                rebuildIndexMapping()
                fireTableDataChanged()
            }
        })

        // 初始化索引映射
        rebuildIndexMapping()
    }

    override fun tableChanged(e: TableModelEvent?) {
        // 源模型变化时重新构建索引
        rebuildIndexMapping()
        fireTableDataChanged()
    }

    /**
     * 重建视图到模型的索引映射
     * 先应用筛选，再应用排序
     */
    private fun rebuildIndexMapping() {
        // 获取筛选后的行索引
        val filteredIndices = filter.getFilteredIndices()

        // 创建临时模型用于排序
        val tempModel = object : TableModel {
            override fun getRowCount(): Int = filteredIndices.size
            override fun getColumnCount(): Int = sourceModel.columnCount
            override fun getColumnName(columnIndex: Int): String = sourceModel.getColumnName(columnIndex)
            override fun getColumnClass(columnIndex: Int): Class<*> = sourceModel.getColumnClass(columnIndex)
            override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
                return sourceModel.isCellEditable(filteredIndices[rowIndex], columnIndex)
            }
            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
                return sourceModel.getValueAt(filteredIndices[rowIndex], columnIndex)
            }
            override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
                sourceModel.setValueAt(aValue, filteredIndices[rowIndex], columnIndex)
            }
            override fun addTableModelListener(l: TableModelListener?) {}
            override fun removeTableModelListener(l: TableModelListener?) {}
        }

        // 创建新的排序器对筛选后的数据进行排序
        val filteredSorter = TableSorter(tempModel)
        filteredSorter.setSortRules(sorter.getSortRules())

        // 构建最终的视图到模型映射
        val sortedCount = filteredSorter.getRowCount()
        viewToModel = IntArray(sortedCount) { viewRow ->
            val filteredRow = filteredSorter.getSourceRowIndex(viewRow)
            filteredIndices[filteredRow]
        }

        // 构建模型到视图的映射
        modelToView = IntArray(sourceModel.rowCount) { -1 }
        for (viewRow in viewToModel.indices) {
            modelToView[viewToModel[viewRow]] = viewRow
        }
    }

    /**
     * 将视图行索引转换为源模型行索引
     */
    fun convertRowIndexToModel(viewRowIndex: Int): Int {
        return if (viewRowIndex in viewToModel.indices) {
            viewToModel[viewRowIndex]
        } else {
            -1
        }
    }

    /**
     * 将源模型行索引转换为视图行索引
     */
    fun convertRowIndexToView(modelRowIndex: Int): Int {
        return if (modelRowIndex in modelToView.indices) {
            modelToView[modelRowIndex]
        } else {
            -1
        }
    }

    // ==================== TableModel 接口实现 ====================

    override fun getRowCount(): Int = viewToModel.size

    override fun getColumnCount(): Int = sourceModel.columnCount

    override fun getColumnName(column: Int): String = sourceModel.getColumnName(column)

    override fun getColumnClass(columnIndex: Int): Class<*> = sourceModel.getColumnClass(columnIndex)

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        val modelRow = convertRowIndexToModel(rowIndex)
        return if (modelRow >= 0) {
            sourceModel.isCellEditable(modelRow, columnIndex)
        } else {
            false
        }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val modelRow = convertRowIndexToModel(rowIndex)
        return if (modelRow >= 0) {
            sourceModel.getValueAt(modelRow, columnIndex)
        } else {
            null
        }
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        val modelRow = convertRowIndexToModel(rowIndex)
        if (modelRow >= 0) {
            sourceModel.setValueAt(aValue, modelRow, columnIndex)
        }
    }

    // ==================== 代理方法 ====================

    /**
     * 添加行（添加到源模型）
     */
    fun addRow(rowData: Array<Any?>) {
        sourceModel.addRow(rowData)
    }

    /**
     * 在指定位置插入行
     */
    fun insertRowAt(row: Int, rowData: Array<Any?>) {
        val modelRow = convertRowIndexToModel(row)
        if (modelRow >= 0) {
            sourceModel.insertRowAt(modelRow, rowData)
        } else {
            sourceModel.addRow(rowData)
        }
    }

    /**
     * 删除行
     */
    fun removeRow(row: Int) {
        val modelRow = convertRowIndexToModel(row)
        if (modelRow >= 0) {
            sourceModel.removeRow(modelRow)
        }
    }

    /**
     * 复制行
     */
    fun copyRow(row: Int): Boolean {
        val modelRow = convertRowIndexToModel(row)
        return if (modelRow >= 0) {
            sourceModel.copyRow(modelRow)
        } else {
            false
        }
    }

    /**
     * 清空所有数据
     */
    fun clearData() {
        sourceModel.clearData()
    }

    /**
     * 获取源模型
     */
    fun getSourceModel(): TestCaseTableModel = sourceModel

    /**
     * 获取筛选统计信息
     */
    fun getFilterStats() = filter.getFilterStats()

    /**
     * 获取排序描述
     */
    fun getSortDescription() = sorter.getSortDescription()
}
