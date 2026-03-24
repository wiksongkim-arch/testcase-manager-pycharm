package com.testcase.manager.ui.filter

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.ui.TestCaseTableModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TableFilter 单元测试
 */
class TableFilterTest {

    @Test
    fun testFilterByPriority() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test 1", "P0", "已发布", "", ""))
        model.addRow(arrayOf("TC002", "Test 2", "P1", "草稿", "", ""))
        model.addRow(arrayOf("TC003", "Test 3", "P2", "已发布", "", ""))

        val filter = TableFilter(model)
        filter.setCriteria(FilterCriteria.byPriorities(Priority.P0))

        assertEquals(1, filter.getRowCount())
        assertEquals(0, filter.getSourceRowIndex(0))
    }

    @Test
    fun testFilterByStatus() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test 1", "P0", "已发布", "", ""))
        model.addRow(arrayOf("TC002", "Test 2", "P1", "草稿", "", ""))
        model.addRow(arrayOf("TC003", "Test 3", "P2", "已发布", "", ""))

        val filter = TableFilter(model)
        filter.setCriteria(FilterCriteria.byStatuses(Status.PUBLISHED))

        assertEquals(2, filter.getRowCount())
    }

    @Test
    fun testFilterBySearchText() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Login Test", "P0", "已发布", "", ""))
        model.addRow(arrayOf("TC002", "Logout Test", "P1", "草稿", "", ""))
        model.addRow(arrayOf("TC003", "Search Test", "P2", "已发布", "", ""))

        val filter = TableFilter(model)
        filter.setCriteria(FilterCriteria.bySearch("Login"))

        assertEquals(1, filter.getRowCount())
        assertEquals("TC001", model.getValueAt(filter.getSourceRowIndex(0), 0))
    }

    @Test
    fun testClearFilter() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test 1", "P0", "已发布", "", ""))
        model.addRow(arrayOf("TC002", "Test 2", "P1", "草稿", "", ""))

        val filter = TableFilter(model)
        filter.setCriteria(FilterCriteria.byPriorities(Priority.P0))
        assertEquals(1, filter.getRowCount())

        filter.clearFilter()
        assertEquals(2, filter.getRowCount())
    }

    @Test
    fun testFilterStats() {
        val model = TestCaseTableModel()
        model.addRow(arrayOf("TC001", "Test 1", "P0", "已发布", "", ""))
        model.addRow(arrayOf("TC002", "Test 2", "P1", "草稿", "", ""))
        model.addRow(arrayOf("TC003", "Test 3", "P2", "已发布", "", ""))

        val filter = TableFilter(model)
        filter.setCriteria(FilterCriteria.byPriorities(Priority.P0))

        val stats = filter.getFilterStats()
        assertEquals(3, stats.totalRows)
        assertEquals(1, stats.filteredRows)
        assertEquals(2, stats.hiddenRows)
        assertTrue(stats.isFiltered)
    }

    @Test
    fun testIsActive() {
        val emptyCriteria = FilterCriteria()
        assertFalse(emptyCriteria.isActive())

        val priorityCriteria = FilterCriteria.byPriorities(Priority.P0)
        assertTrue(priorityCriteria.isActive())

        val statusCriteria = FilterCriteria.byStatuses(Status.PUBLISHED)
        assertTrue(statusCriteria.isActive())

        val searchCriteria = FilterCriteria.bySearch("test")
        assertTrue(searchCriteria.isActive())
    }
}
