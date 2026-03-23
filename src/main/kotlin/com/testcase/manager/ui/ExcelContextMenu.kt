package com.testcase.manager.ui

import com.intellij.openapi.ui.Messages
import com.intellij.ui.PopupMenuListenerAdapter
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.JTable
import javax.swing.event.PopupMenuEvent

/**
 * Excel 表格右键菜单
 * 提供行操作、单元格操作等功能
 */
class ExcelContextMenu(private val table: JTable, private val model: TestCaseTableModel) {

    private val popupMenu = JPopupMenu()

    init {
        initMenu()
    }

    /**
     * 初始化菜单项
     */
    private fun initMenu() {
        // ===== 行操作 =====
        popupMenu.add(createMenuItem("在上方插入行") {
            insertRowAbove()
        })

        popupMenu.add(createMenuItem("在下方插入行") {
            insertRowBelow()
        })

        popupMenu.add(createMenuItem("复制行") {
            copyRow()
        })

        popupMenu.add(createMenuItem("删除行") {
            deleteRow()
        })

        popupMenu.add(JSeparator())

        // ===== 单元格操作 =====
        popupMenu.add(createMenuItem("复制 (Ctrl+C)") {
            copyCell()
        })

        popupMenu.add(createMenuItem("粘贴 (Ctrl+V)") {
            pasteCell()
        })

        popupMenu.add(createMenuItem("剪切 (Ctrl+X)") {
            cutCell()
        })

        popupMenu.add(JSeparator())

        // ===== 其他操作 =====
        popupMenu.add(createMenuItem("清空单元格") {
            clearCell()
        })

        popupMenu.add(createMenuItem("选择整行") {
            selectRow()
        })

        // 添加菜单监听器，在显示前更新状态
        popupMenu.addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                updateMenuState()
            }
        })
    }

    /**
     * 创建菜单项
     */
    private fun createMenuItem(text: String, action: () -> Unit): JMenuItem {
        return JMenuItem(text).apply {
            addActionListener { action() }
        }
    }

    /**
     * 更新菜单项状态
     */
    private fun updateMenuState() {
        val hasSelection = table.selectedRow >= 0
        val hasCellSelection = table.selectedRow >= 0 && table.selectedColumn >= 0

        // 根据选择状态启用/禁用菜单项
        for (i in 0 until popupMenu.componentCount) {
            val component = popupMenu.getComponent(i)
            if (component is JMenuItem) {
                when {
                    component.text.contains("行") -> component.isEnabled = hasSelection
                    component.text.contains("单元格") && !component.text.contains("清空") -> {
                        component.isEnabled = hasCellSelection
                    }
                }
            }
        }
    }

    /**
     * 在选中行上方插入新行
     */
    private fun insertRowAbove() {
        val row = table.selectedRow
        if (row >= 0) {
            model.insertRowAt(row, createEmptyRow())
            table.setRowSelectionInterval(row, row)
        }
    }

    /**
     * 在选中行下方插入新行
     */
    private fun insertRowBelow() {
        val row = table.selectedRow
        if (row >= 0) {
            model.insertRowAt(row + 1, createEmptyRow())
            table.setRowSelectionInterval(row + 1, row + 1)
        }
    }

    /**
     * 复制选中行
     */
    private fun copyRow() {
        val row = table.selectedRow
        if (row >= 0) {
            model.copyRow(row)
            table.setRowSelectionInterval(row + 1, row + 1)
        }
    }

    /**
     * 删除选中行
     */
    private fun deleteRow() {
        val row = table.selectedRow
        if (row >= 0) {
            val result = Messages.showYesNoDialog(
                "确定要删除选中的行吗？",
                "确认删除",
                Messages.getQuestionIcon()
            )
            if (result == Messages.YES) {
                model.removeRow(row)
            }
        }
    }

    /**
     * 复制单元格内容
     */
    private fun copyCell() {
        val row = table.selectedRow
        val col = table.selectedColumn
        if (row >= 0 && col >= 0) {
            val value = model.getValueAt(row, col)?.toString() ?: ""
            val selection = StringSelection(value)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        }
    }

    /**
     * 粘贴到单元格
     */
    private fun pasteCell() {
        val row = table.selectedRow
        val col = table.selectedColumn
        if (row >= 0 && col >= 0) {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val data = clipboard.getData(DataFlavor.stringFlavor) as? String
                if (data != null) {
                    model.setValueAt(data, row, col)
                }
            } catch (e: Exception) {
                // 粘贴失败，忽略
            }
        }
    }

    /**
     * 剪切单元格内容
     */
    private fun cutCell() {
        val row = table.selectedRow
        val col = table.selectedColumn
        if (row >= 0 && col >= 0) {
            val value = model.getValueAt(row, col)?.toString() ?: ""
            val selection = StringSelection(value)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            model.setValueAt("", row, col)
        }
    }

    /**
     * 清空单元格
     */
    private fun clearCell() {
        val row = table.selectedRow
        val col = table.selectedColumn
        if (row >= 0 && col >= 0) {
            model.setValueAt("", row, col)
        }
    }

    /**
     * 选择整行
     */
    private fun selectRow() {
        val row = table.selectedRow
        if (row >= 0) {
            table.setRowSelectionInterval(row, row)
        }
    }

    /**
     * 创建空行数据
     */
    private fun createEmptyRow(): Array<Any?> {
        return arrayOf("", "", "P1", "草稿", "", "")
    }

    /**
     * 获取弹出菜单
     */
    fun getPopupMenu(): JPopupMenu = popupMenu
}
