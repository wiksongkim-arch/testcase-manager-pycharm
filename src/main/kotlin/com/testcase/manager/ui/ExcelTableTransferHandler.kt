package com.testcase.manager.ui

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.TransferHandler

/**
 * Excel 表格拖拽和复制粘贴处理器
 * 支持行拖拽重排和单元格复制粘贴
 */
class ExcelTableTransferHandler(private val model: TestCaseTableModel) : TransferHandler() {

    // 拖拽的源行索引
    private var dragSourceRow = -1

    // 数据传输类型
    companion object {
        private val ROW_DATA_FLAVOR = DataFlavor(
            TestCaseTransferData::class.java,
            "TestCase Row Data"
        )
    }

    /**
     * 获取传输源动作
     */
    override fun getSourceActions(c: JComponent?): Int {
        return COPY_OR_MOVE
    }

    /**
     * 创建传输数据
     */
    override fun createTransferable(c: JComponent?): Transferable? {
        val table = c as? JTable ?: return null
        val row = table.selectedRow

        return if (row >= 0) {
            dragSourceRow = row
            val rowData = model.getRowData(row)
            TestCaseTransferData(rowData, row)
        } else {
            null
        }
    }

    /**
     * 导出完成后的处理
     */
    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        if (action == MOVE && dragSourceRow >= 0) {
            // 如果是移动操作，源行会在导入时处理
        }
        dragSourceRow = -1
    }

    /**
     * 检查是否可以导入
     */
    override fun canImport(support: TransferSupport?): Boolean {
        if (support == null) return false

        // 检查数据类型
        if (!support.isDataFlavorSupported(ROW_DATA_FLAVOR) &&
            !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false
        }

        // 检查目标位置
        val dropLocation = support.dropLocation as? JTable.DropLocation
        return dropLocation != null && dropLocation.row >= 0
    }

    /**
     * 导入数据
     */
    override fun importData(support: TransferSupport?): Boolean {
        if (support == null || !canImport(support)) return false

        val table = support.component as? JTable ?: return false
        val dropLocation = support.dropLocation as? JTable.DropLocation ?: return false
        val targetRow = dropLocation.row

        return when {
            support.isDataFlavorSupported(ROW_DATA_FLAVOR) -> {
                importRowData(support, targetRow)
            }
            support.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                importStringData(support, table, targetRow, dropLocation.column)
            }
            else -> false
        }
    }

    /**
     * 导入行数据（拖拽）
     */
    private fun importRowData(support: TransferSupport, targetRow: Int): Boolean {
        return try {
            val data = support.transferable.getTransferData(ROW_DATA_FLAVOR) as? TestCaseTransferData
            if (data != null) {
                val action = support.dropAction

                when (action) {
                    MOVE -> {
                        // 移动行
                        if (dragSourceRow >= 0 && dragSourceRow != targetRow) {
                            model.moveRow(dragSourceRow, targetRow)
                        }
                    }
                    COPY -> {
                        // 复制行
                        model.insertRowAt(targetRow, data.rowData)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 导入字符串数据（复制粘贴）
     */
    private fun importStringData(
        support: TransferSupport,
        table: JTable,
        row: Int,
        col: Int
    ): Boolean {
        return try {
            val data = support.transferable.getTransferData(DataFlavor.stringFlavor) as? String
            if (data != null && row >= 0 && col >= 0) {
                // 处理制表符分隔的多列数据
                val cells = data.split("\t")
                for ((index, cellValue) in cells.withIndex()) {
                    val targetCol = col + index
                    if (targetCol < model.columnCount) {
                        model.setValueAt(cellValue.trim(), row, targetCol)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否支持从剪贴板粘贴
     */
    fun canPasteFromClipboard(): Boolean {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
    }

    /**
     * 从剪贴板粘贴到指定位置
     */
    fun pasteFromClipboard(table: JTable, row: Int, col: Int): Boolean {
        return try {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            val data = clipboard.getData(DataFlavor.stringFlavor) as? String
            if (data != null) {
                // 处理多行多列数据（如从 Excel 复制）
                val rows = data.split("\n")
                for ((rowIndex, rowData) in rows.withIndex()) {
                    val targetRow = row + rowIndex
                    if (targetRow >= model.rowCount) {
                        model.addRow(arrayOf("", "", "P1", "草稿", "", ""))
                    }

                    val cells = rowData.split("\t")
                    for ((colIndex, cellValue) in cells.withIndex()) {
                        val targetCol = col + colIndex
                        if (targetCol < model.columnCount) {
                            model.setValueAt(cellValue.trim(), targetRow, targetCol)
                        }
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 复制选中区域到剪贴板
     */
    fun copyToClipboard(table: JTable): Boolean {
        val selectedRows = table.selectedRows
        val selectedCols = table.selectedColumns

        if (selectedRows.isEmpty() || selectedCols.isEmpty()) {
            return false
        }

        val sb = StringBuilder()
        for (row in selectedRows) {
            for ((colIndex, col) in selectedCols.withIndex()) {
                val value = model.getValueAt(row, col)?.toString() ?: ""
                sb.append(value)
                if (colIndex < selectedCols.size - 1) {
                    sb.append("\t")
                }
            }
            if (row < selectedRows.last()) {
                sb.append("\n")
            }
        }

        val selection = StringSelection(sb.toString())
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        return true
    }
}

/**
 * 测试用例传输数据类
 */
private data class TestCaseTransferData(
    val rowData: Array<Any?>,
    val sourceRow: Int
) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            ExcelTableTransferHandler::class.java.getDeclaredField("ROW_DATA_FLAVOR").let {
                it.isAccessible = true
                it.get(null) as DataFlavor
            }
        )
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return flavor?.humanPresentableName == "TestCase Row Data"
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestCaseTransferData) return false
        return sourceRow == other.sourceRow && rowData.contentEquals(other.rowData)
    }

    override fun hashCode(): Int {
        return 31 * sourceRow + rowData.contentHashCode()
    }
}
