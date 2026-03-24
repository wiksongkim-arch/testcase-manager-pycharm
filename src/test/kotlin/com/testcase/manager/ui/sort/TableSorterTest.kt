package com.testcase.manager.ui.sort

import com.testcase.manager.ui.TestCaseTableModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TableSorter 单元测试
 */
class TableSorterTest {

    @Test
    fun testSortBySingleColumn() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC003", "Test C", "P2", "已发布", "", ""))
        model.addRow(arrayOf("TC001", "Test A", "P0", "草稿", "", ""))
        model.addRow(arrayOf("TC002", "Test B", "P1", "已发布", "", ""))

        val sorter = TableSorter(model)
        sorter.toggleSort(0) // 按 ID 列排序

        assertEquals(3, sorter.getRowCount())
        // 升序排序后，第一行应该是 TC001
        assertEquals(1, sorter.getSourceRowIndex(0))
    }

    @Test
    fun testSortDirectionToggle() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC002", "Test B", "P1", "已发布", "", ""))
        model.addRow(arrayOf("TC001", "Test A", "P0", "草稿", "", ""))

        val sorter = TableSorter(model)

        // 第一次点击 - 升序
        sorter.toggleSort(0)
        assertEquals(SortDirection.ASCENDING, sorter.getSortDirection(0))

        // 第二次点击 - 降序
        sorter.toggleSort(0)
        assertEquals(SortDirection.DESCENDING, sorter.getSortDirection(0))

        // 第三次点击 - 无排序
        sorter.toggleSort(0)
        assertEquals(SortDirection.NONE, sorter.getSortDirection(0))
    }

    @Test
    fun testMultiColumnSort() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test B", "P1", "已发布", "", ""))
        model.addRow(arrayOf("TC001", "Test A", "P0", "草稿", "", ""))
        model.addRow(arrayOf("TC002", "Test C", "P2", "已发布", "", ""))

        val sorter = TableSorter(model)

        // 先按 ID 排序，再按名称排序
        sorter.toggleSort(0, false) // 不按 Shift，单排序
        sorter.toggleSort(1, true)  // 按 Shift，多排序

        val rules = sorter.getSortRules()
        assertEquals(2, rules.size)
        assertEquals(0, rules[0].columnIndex)
        assertEquals(1, rules[1].columnIndex)
    }

    @Test
    fun testClearSort() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC002", "Test B", "P1", "已发布", "", ""))
        model.addRow(arrayOf("TC001", "Test A", "P0", "草稿", "", ""))

        val sorter = TableSorter(model)
        sorter.toggleSort(0)
        assertTrue(sorter.isSorted())

        sorter.clearSort()
        assertEquals(SortDirection.NONE, sorter.getSortDirection(0))
        assertEquals(0, sorter.getSourceRowIndex(0))
    }

    @Test
    fun testSortDescription() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test A", "P0", "草稿", "", ""))

        val sorter = TableSorter(model)
        assertEquals("未排序", sorter.getSortDescription())

        sorter.toggleSort(0)
        assertTrue(sorter.getSortDescription().contains("ID"))
        assertTrue(sorter.getSortDescription().contains("▲"))
    }

    @Test
    fun testGetPrimarySortColumn() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test A", "P0", "草稿", "", ""))

        val sorter = TableSorter(model)
        assertNull(sorter.getPrimarySortColumn())

        sorter.toggleSort(2)
        assertEquals(2, sorter.getPrimarySortColumn())
    }
}
