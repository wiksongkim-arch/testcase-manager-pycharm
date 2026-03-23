package com.testcase.manager.ui

import org.junit.Test
import javax.swing.JTable
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ExcelCellEditor 单元测试
 */
class ExcelCellEditorTest {

    @Test
    fun `test getCellEditorValue returns current value`() {
        val editor = ExcelCellEditor(CellEditorType.TEXT)
        // 初始值为 null
        assertEquals(null, editor.cellEditorValue)
    }

    @Test
    fun `test factory creates correct editor types`() {
        assertEquals(CellEditorType.TEXT, ExcelCellEditorFactory.getEditorType(0)) // ID
        assertEquals(CellEditorType.TEXT, ExcelCellEditorFactory.getEditorType(1)) // 名称
        assertEquals(CellEditorType.PRIORITY, ExcelCellEditorFactory.getEditorType(2)) // 优先级
        assertEquals(CellEditorType.STATUS, ExcelCellEditorFactory.getEditorType(3)) // 状态
        assertEquals(CellEditorType.TEXT, ExcelCellEditorFactory.getEditorType(4)) // 步骤
        assertEquals(CellEditorType.TEXT, ExcelCellEditorFactory.getEditorType(5)) // 预期结果
    }

    @Test
    fun `test factory creates editor for each column`() {
        for (col in 0..5) {
            val editor = ExcelCellEditorFactory.createEditor(col)
            assertTrue(editor is ExcelCellEditor)
        }
    }
}

/**
 * TestCaseTableModel 单元测试
 */
class TestCaseTableModelTest {

    @Test
    fun `test model has correct column count`() {
        val model = TestCaseTableModel()
        assertEquals(6, model.columnCount)
    }

    @Test
    fun `test model column names`() {
        val model = TestCaseTableModel()
        assertEquals("ID", model.getColumnName(0))
        assertEquals("用例名称", model.getColumnName(1))
        assertEquals("优先级", model.getColumnName(2))
        assertEquals("状态", model.getColumnName(3))
        assertEquals("测试步骤", model.getColumnName(4))
        assertEquals("预期结果", model.getColumnName(5))
    }

    @Test
    fun `test all cells are editable`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试", "P0", "草稿", "", ""))

        for (col in 0 until model.columnCount) {
            assertTrue(model.isCellEditable(0, col))
        }
    }

    @Test
    fun `test insert row at valid position`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试1", "P0", "草稿", "", ""))
        model.addRow(arrayOf("TC002", "测试2", "P1", "已发布", "", ""))

        model.insertRowAt(1, arrayOf("TC003", "插入", "P2", "草稿", "", ""))

        assertEquals(3, model.rowCount)
        assertEquals("TC001", model.getValueAt(0, 0))
        assertEquals("TC003", model.getValueAt(1, 0))
        assertEquals("TC002", model.getValueAt(2, 0))
    }

    @Test
    fun `test insert row at invalid position does nothing`() {
        val model = TestCaseTableModel()
        model.insertRowAt(-1, arrayOf("TC001", "测试", "P0", "草稿", "", ""))
        assertEquals(0, model.rowCount)

        model.insertRowAt(100, arrayOf("TC001", "测试", "P0", "草稿", "", ""))
        assertEquals(0, model.rowCount)
    }

    @Test
    fun `test copy row`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试", "P0", "草稿", "", ""))

        val result = model.copyRow(0)

        assertTrue(result)
        assertEquals(2, model.rowCount)
        assertEquals("TC001", model.getValueAt(0, 0))
        assertEquals("TC001", model.getValueAt(1, 0))
    }

    @Test
    fun `test copy row with invalid index returns false`() {
        val model = TestCaseTableModel()
        assertFalse(model.copyRow(-1))
        assertFalse(model.copyRow(0))
        assertFalse(model.copyRow(100))
    }

    @Test
    fun `test move row down`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试1", "P0", "草稿", "", ""))
        model.addRow(arrayOf("TC002", "测试2", "P1", "已发布", "", ""))
        model.addRow(arrayOf("TC003", "测试3", "P2", "已归档", "", ""))

        val result = model.moveRow(0, 2)

        assertTrue(result)
        assertEquals("TC002", model.getValueAt(0, 0))
        assertEquals("TC001", model.getValueAt(1, 0))
        assertEquals("TC003", model.getValueAt(2, 0))
    }

    @Test
    fun `test move row up`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试1", "P0", "草稿", "", ""))
        model.addRow(arrayOf("TC002", "测试2", "P1", "已发布", "", ""))
        model.addRow(arrayOf("TC003", "测试3", "P2", "已归档", "", ""))

        val result = model.moveRow(2, 0)

        assertTrue(result)
        assertEquals("TC003", model.getValueAt(0, 0))
        assertEquals("TC001", model.getValueAt(1, 0))
        assertEquals("TC002", model.getValueAt(2, 0))
    }

    @Test
    fun `test move row with invalid indices returns false`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试", "P0", "草稿", "", ""))

        assertFalse(model.moveRow(-1, 0))
        assertFalse(model.moveRow(0, -1))
        assertFalse(model.moveRow(0, 100))
        assertFalse(model.moveRow(0, 0)) // 相同位置
    }

    @Test
    fun `test get row data`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试", "P0", "草稿", "", ""))

        val rowData = model.getRowData(0)

        assertEquals(6, rowData.size)
        assertEquals("TC001", rowData[0])
        assertEquals("测试", rowData[1])
        assertEquals("P0", rowData[2])
    }

    @Test
    fun `test set row data`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试", "P0", "草稿", "", ""))

        model.setRowData(0, arrayOf("TC002", "新测试", "P1", "已发布", "步骤", "预期"))

        assertEquals("TC002", model.getValueAt(0, 0))
        assertEquals("新测试", model.getValueAt(0, 1))
        assertEquals("P1", model.getValueAt(0, 2))
    }

    @Test
    fun `test clear data`() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "测试", "P0", "草稿", "", ""))
        model.addRow(arrayOf("TC002", "测试2", "P1", "已发布", "", ""))

        model.clearData()

        assertEquals(0, model.rowCount)
    }

    @Test
    fun `test validate data for ID column`() {
        val model = TestCaseTableModel()

        assertTrue(model.validateData(0, 0, "TC001"))
        assertTrue(model.validateData(0, 0, "test-case-123"))
        assertTrue(model.validateData(0, 0, "TEST_123"))
        assertFalse(model.validateData(0, 0, "")) // 空值
        assertFalse(model.validateData(0, 0, "TC 001")) // 包含空格
        assertFalse(model.validateData(0, 0, "TC@001")) // 特殊字符
    }

    @Test
    fun `test validate data for name column`() {
        val model = TestCaseTableModel()

        assertTrue(model.validateData(0, 1, "测试用例"))
        assertTrue(model.validateData(0, 1, "Test Case"))
        assertFalse(model.validateData(0, 1, "")) // 空值
    }
}

/**
 * ExcelTableTransferHandler 单元测试
 */
class ExcelTableTransferHandlerTest {

    @Test
    fun `test transfer handler creation`() {
        val model = TestCaseTableModel()
        val handler = ExcelTableTransferHandler(model)

        assertTrue(handler.sourceActions(null) == TransferHandler.COPY_OR_MOVE)
    }
}
