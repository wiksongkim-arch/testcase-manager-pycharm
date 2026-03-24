package com.testcase.manager.performance

import com.intellij.ui.table.JBTable
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.ListSelectionModel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.table.TableModel

/**
 * 虚拟滚动表格组件
 * 用于高效显示大量数据
 */
class VirtualScrollTable(model: TableModel) : JBTable(model) {

    // 可见行范围
    private var visibleRowStart = 0
    private var visibleRowEnd = 0

    // 缓冲行数（在可见区域外额外渲染的行数）
    var bufferRows = 10

    // 行高（固定高度以实现虚拟滚动）
    var fixedRowHeight = 30

    // 估计的总行数
    var estimatedRowCount: Int = 0
        set(value) {
            field = value
            updatePreferredSize()
        }

    // 数据加载回调
    var onLoadData: ((startRow: Int, endRow: Int) -> Unit)? = null

    init {
        // 设置固定行高
        rowHeight = fixedRowHeight

        // 禁用自动调整大小以提高性能
        autoResizeMode = AUTO_RESIZE_OFF

        // 设置选择模式
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        setCellSelectionEnabled(true)

        // 添加滚动监听
        setupScrollListener()
    }

    /**
     * 设置滚动监听
     */
    private fun setupScrollListener() {
        // 使用 viewport 监听滚动事件
        addPropertyChangeListener("ancestor") { evt ->
            if (evt.newValue is JScrollPane) {
                val scrollPane = evt.newValue as JScrollPane
                val viewport = scrollPane.viewport

                viewport.addChangeListener { e ->
                    updateVisibleRange()
                }
            }
        }
    }

    /**
     * 更新可见范围
     */
    private fun updateVisibleRange() {
        val viewport = parent as? JViewport ?: return

        val viewRect = viewport.viewRect
        val startRow = (viewRect.y / fixedRowHeight).coerceAtLeast(0)
        val endRow = ((viewRect.y + viewRect.height) / fixedRowHeight + 1)
            .coerceAtMost(estimatedRowCount - 1)

        // 添加缓冲
        val bufferedStart = (startRow - bufferRows).coerceAtLeast(0)
        val bufferedEnd = (endRow + bufferRows).coerceAtMost(estimatedRowCount - 1)

        if (bufferedStart != visibleRowStart || bufferedEnd != visibleRowEnd) {
            visibleRowStart = bufferedStart
            visibleRowEnd = bufferedEnd

            // 触发数据加载
            onLoadData?.invoke(visibleRowStart, visibleRowEnd)

            // 重绘
            repaint()
        }
    }

    /**
     * 更新首选大小
     */
    private fun updatePreferredSize() {
        val height = estimatedRowCount * fixedRowHeight
        val width = columnModel.totalColumnWidth
        preferredSize = Dimension(width, height)
        revalidate()
    }

    /**
     * 获取可见行范围
     */
    fun getVisibleRange(): Pair<Int, Int> {
        return visibleRowStart to visibleRowEnd
    }

    /**
     * 滚动到指定行
     */
    fun scrollToRow(row: Int) {
        val y = row * fixedRowHeight
        scrollRectToVisible(Rectangle(0, y, width, fixedRowHeight))
    }

    /**
     * 优化绘制性能
     */
    override fun paintComponent(g: Graphics) {
        // 只绘制可见区域
        val clipBounds = g.clipBounds
        if (clipBounds != null) {
            // 计算需要绘制的行范围
            val startRow = (clipBounds.y / fixedRowHeight).coerceAtLeast(0)
            val endRow = ((clipBounds.y + clipBounds.height) / fixedRowHeight + 1)
                .coerceAtMost(model.rowCount - 1)

            // 设置裁剪区域
            g.clipRect(clipBounds.x, startRow * fixedRowHeight, clipBounds.width, (endRow - startRow + 1) * fixedRowHeight)
        }

        super.paintComponent(g)
    }

    /**
     * 获取单元格矩形区域
     */
    override fun getCellRect(row: Int, column: Int, includeSpacing: Boolean): Rectangle {
        val rect = super.getCellRect(row, column, includeSpacing)
        // 使用固定行高
        rect.y = row * fixedRowHeight
        rect.height = fixedRowHeight
        return rect
    }

    /**
     * 获取行高
     */
    override fun getRowHeight(row: Int): Int {
        return fixedRowHeight
    }

    /**
     * 获取总行高
     */
    override fun getRowHeight(): Int {
        return fixedRowHeight
    }

    /**
     * 获取行数
     */
    override fun getRowCount(): Int {
        return model.rowCount
    }

    /**
     * 获取行位置
     */
    fun getRowY(row: Int): Int {
        return row * fixedRowHeight
    }

    /**
     * 从 Y 坐标获取行号
     */
    fun getRowAtY(y: Int): Int {
        return y / fixedRowHeight
    }

    companion object {
        /**
         * 创建带虚拟滚动的表格
         */
        fun create(model: TableModel, estimatedRows: Int, configure: (VirtualScrollTable.() -> Unit)? = null): VirtualScrollTable {
            return VirtualScrollTable(model).apply {
                estimatedRowCount = estimatedRows
                configure?.invoke(this)
            }
        }
    }
}

/**
 * 虚拟表格模型接口
 */
interface VirtualTableModel : TableModel {
    /**
     * 获取估计的总行数
     */
    fun getEstimatedRowCount(): Int

    /**
     * 加载指定范围的数据
     */
    fun loadData(startRow: Int, endRow: Int)

    /**
     * 数据是否已加载
     */
    fun isRowLoaded(row: Int): Boolean
}
