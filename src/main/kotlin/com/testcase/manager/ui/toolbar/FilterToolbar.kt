package com.testcase.manager.ui.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.ui.filter.FilterCriteria
import com.testcase.manager.ui.filter.TableFilter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * 筛选工具栏组件
 * 提供优先级、状态、文本搜索筛选功能
 */
class FilterToolbar(
    private val filter: TableFilter,
    private val onFilterChanged: (FilterStats) -> Unit = {}
) : JPanel(BorderLayout()) {

    private val searchField: JBTextField
    private val priorityCombo: ComboBox<PriorityFilterItem>
    private val statusCombo: ComboBox<StatusFilterItem>
    private val clearButton: JButton
    private val statsLabel: JLabel

    // 当前筛选条件
    private var currentCriteria: FilterCriteria = FilterCriteria()

    init {
        border = JBUI.Borders.empty(4, 8)

        // 创建搜索框
        searchField = JBTextField().apply {
            preferredSize = Dimension(150, 28)
            toolTipText = "搜索文本（按回车确认）"
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER) {
                        applySearchFilter()
                    }
                }
            })
        }

        // 创建优先级筛选下拉框
        priorityCombo = ComboBox<PriorityFilterItem>().apply {
            addItem(PriorityFilterItem(null, "所有优先级"))
            Priority.entries.forEach { priority ->
                addItem(PriorityFilterItem(priority, priority.value))
            }
            addActionListener { applyPriorityFilter() }
        }

        // 创建状态筛选下拉框
        statusCombo = ComboBox<StatusFilterItem>().apply {
            addItem(StatusFilterItem(null, "所有状态"))
            Status.entries.forEach { status ->
                addItem(StatusFilterItem(status, status.value))
            }
            addActionListener { applyStatusFilter() }
        }

        // 创建清除按钮
        clearButton = JButton("清除筛选", AllIcons.Actions.GC).apply {
            addActionListener { clearFilters() }
        }

        // 创建统计标签
        statsLabel = JLabel("显示: 0 / 0 行").apply {
            border = JBUI.Borders.emptyLeft(16)
        }

        // 组装工具栏
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JLabel(AllIcons.Actions.Search))
            add(searchField)
            add(Box.createHorizontalStrut(16))
            add(JLabel("优先级:"))
            add(priorityCombo)
            add(Box.createHorizontalStrut(8))
            add(JLabel("状态:"))
            add(statusCombo)
            add(Box.createHorizontalStrut(16))
            add(clearButton)
        }

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            add(statsLabel)
        }

        add(leftPanel, BorderLayout.WEST)
        add(rightPanel, BorderLayout.EAST)

        // 添加筛选监听器
        filter.addFilterListener(object : TableFilter.FilterListener {
            override fun onFilterChanged() {
                updateStats()
            }
        })
    }

    /**
     * 应用搜索筛选
     */
    private fun applySearchFilter() {
        val searchText = searchField.text ?: ""
        currentCriteria = currentCriteria.withSearchText(searchText)
        applyFilter()
    }

    /**
     * 应用优先级筛选
     */
    private fun applyPriorityFilter() {
        val selected = priorityCombo.selectedItem as? PriorityFilterItem
        currentCriteria = if (selected?.priority != null) {
            FilterCriteria.byPriorities(selected.priority)
        } else {
            currentCriteria.copy(priorities = emptySet())
        }
        applyFilter()
    }

    /**
     * 应用状态筛选
     */
    private fun applyStatusFilter() {
        val selected = statusCombo.selectedItem as? StatusFilterItem
        currentCriteria = if (selected?.status != null) {
            FilterCriteria.byStatuses(selected.status)
        } else {
            currentCriteria.copy(statuses = emptySet())
        }
        applyFilter()
    }

    /**
     * 应用筛选条件
     */
    private fun applyFilter() {
        // 合并所有筛选条件
        val searchText = searchField.text ?: ""
        val priorityItem = priorityCombo.selectedItem as? PriorityFilterItem
        val statusItem = statusCombo.selectedItem as? StatusFilterItem

        val criteria = FilterCriteria(
            priorities = priorityItem?.priority?.let { setOf(it) } ?: emptySet(),
            statuses = statusItem?.status?.let { setOf(it) } ?: emptySet(),
            searchText = searchText
        )

        filter.setCriteria(criteria)
        updateStats()
    }

    /**
     * 清除所有筛选
     */
    private fun clearFilters() {
        searchField.text = ""
        priorityCombo.selectedIndex = 0
        statusCombo.selectedIndex = 0
        currentCriteria = FilterCriteria()
        filter.clearFilter()
        updateStats()
    }

    /**
     * 更新统计信息
     */
    private fun updateStats() {
        val stats = filter.getFilterStats()
        statsLabel.text = "显示: ${stats.filteredRows} / ${stats.totalRows} 行"
        onFilterChanged(stats)
    }

    /**
     * 设置筛选条件
     */
    fun setFilterCriteria(criteria: FilterCriteria) {
        currentCriteria = criteria
        searchField.text = criteria.searchText

        // 设置优先级下拉框
        if (criteria.priorities.isNotEmpty()) {
            val priority = criteria.priorities.first()
            for (i in 0 until priorityCombo.itemCount) {
                if (priorityCombo.getItemAt(i)?.priority == priority) {
                    priorityCombo.selectedIndex = i
                    break
                }
            }
        } else {
            priorityCombo.selectedIndex = 0
        }

        // 设置状态下拉框
        if (criteria.statuses.isNotEmpty()) {
            val status = criteria.statuses.first()
            for (i in 0 until statusCombo.itemCount) {
                if (statusCombo.getItemAt(i)?.status == status) {
                    statusCombo.selectedIndex = i
                    break
                }
            }
        } else {
            statusCombo.selectedIndex = 0
        }

        filter.setCriteria(criteria)
    }

    /**
     * 获取当前筛选条件
     */
    fun getFilterCriteria(): FilterCriteria = currentCriteria

    /**
     * 优先级筛选项
     */
    private data class PriorityFilterItem(
        val priority: Priority?,
        val displayName: String
    ) {
        override fun toString(): String = displayName
    }

    /**
     * 状态筛选项
     */
    private data class StatusFilterItem(
        val status: Status?,
        val displayName: String
    ) {
        override fun toString(): String = displayName
    }
}

/**
 * 筛选统计信息
 */
data class FilterStats(
    val totalRows: Int,
    val filteredRows: Int,
    val isFiltered: Boolean
)
