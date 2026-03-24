package com.testcase.manager.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase

/**
 * TestCaseTableModel 单元测试
 *
 * 测试表格模型的增删改查功能
 */
class TestCaseTableModelTest : BasePlatformTestCase() {

    private lateinit var tableModel: TestCaseTableModel

    override fun setUp() {
        super.setUp()
        tableModel = TestCaseTableModel()
    }

    fun testInitialState() {
        assertEquals(0, tableModel.rowCount)
        assertEquals(6, tableModel.columnCount)
    }

    fun testAddRow() {
        val testCase = createTestCase("TC001", "测试用例1")
        tableModel.addRow(testCase)

        assertEquals(1, tableModel.rowCount)
        assertEquals("TC001", tableModel.getValueAt(0, 0))
        assertEquals("测试用例1", tableModel.getValueAt(0, 1))
    }

    fun testInsertRow() {
        val testCase1 = createTestCase("TC001", "测试用例1")
        val testCase2 = createTestCase("TC002", "测试用例2")

        tableModel.addRow(testCase1)
        tableModel.insertRow(0, testCase2)

        assertEquals(2, tableModel.rowCount)
        assertEquals("TC002", tableModel.getValueAt(0, 0))
        assertEquals("TC001", tableModel.getValueAt(1, 0))
    }

    fun testRemoveRow() {
        val testCase1 = createTestCase("TC001", "测试用例1")
        val testCase2 = createTestCase("TC002", "测试用例2")

        tableModel.addRow(testCase1)
        tableModel.addRow(testCase2)
        tableModel.removeRow(0)

        assertEquals(1, tableModel.rowCount)
        assertEquals("TC002", tableModel.getValueAt(0, 0))
    }

    fun testMoveRow() {
        val testCase1 = createTestCase("TC001", "测试用例1")
        val testCase2 = createTestCase("TC002", "测试用例2")

        tableModel.addRow(testCase1)
        tableModel.addRow(testCase2)
        tableModel.moveRow(0, 1)

        assertEquals("TC002", tableModel.getValueAt(0, 0))
        assertEquals("TC001", tableModel.getValueAt(1, 0))
    }

    fun testUpdateCell() {
        val testCase = createTestCase("TC001", "测试用例1")
        tableModel.addRow(testCase)

        tableModel.setValueAt("新名称", 0, 1)

        assertEquals("新名称", tableModel.getValueAt(0, 1))
    }

    fun testGetColumnName() {
        assertEquals("ID", tableModel.getColumnName(0))
        assertEquals("用例名称", tableModel.getColumnName(1))
        assertEquals("优先级", tableModel.getColumnName(2))
        assertEquals("状态", tableModel.getColumnName(3))
        assertEquals("测试步骤", tableModel.getColumnName(4))
        assertEquals("预期结果", tableModel.getColumnName(5))
    }

    fun testClear() {
        val testCase = createTestCase("TC001", "测试用例1")
        tableModel.addRow(testCase)
        tableModel.clear()

        assertEquals(0, tableModel.rowCount)
    }

    fun testGetAllTestCases() {
        val testCase1 = createTestCase("TC001", "测试用例1")
        val testCase2 = createTestCase("TC002", "测试用例2")

        tableModel.addRow(testCase1)
        tableModel.addRow(testCase2)

        val allCases = tableModel.getAllTestCases()
        assertEquals(2, allCases.size)
        assertEquals("TC001", allCases[0].id)
        assertEquals("TC002", allCases[1].id)
    }

    private fun createTestCase(id: String, name: String): TestCase {
        return TestCase(
            id = id,
            name = name,
            priority = Priority.P1,
            status = Status.DRAFT,
            steps = listOf("步骤1"),
            expected = "预期结果",
            tags = emptyList()
        )
    }
}
