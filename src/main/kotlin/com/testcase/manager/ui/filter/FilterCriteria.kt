package com.testcase.manager.ui.filter

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status

/**
 * 筛选条件数据类
 * 支持按优先级、状态、文本搜索筛选
 */
data class FilterCriteria(
    val priorities: Set<Priority> = emptySet(),
    val statuses: Set<Status> = emptySet(),
    val searchText: String = "",
    val searchColumns: Set<Int> = emptySet() // 空表示搜索所有列
) {
    /**
     * 检查是否设置了任何筛选条件
     */
    fun isActive(): Boolean {
        return priorities.isNotEmpty() ||
               statuses.isNotEmpty() ||
               searchText.isNotBlank()
    }

    /**
     * 清空所有筛选条件
     */
    fun clear(): FilterCriteria {
        return FilterCriteria()
    }

    /**
     * 切换优先级筛选
     */
    fun togglePriority(priority: Priority): FilterCriteria {
        val newPriorities = priorities.toMutableSet()
        if (priority in newPriorities) {
            newPriorities.remove(priority)
        } else {
            newPriorities.add(priority)
        }
        return copy(priorities = newPriorities)
    }

    /**
     * 切换状态筛选
     */
    fun toggleStatus(status: Status): FilterCriteria {
        val newStatuses = statuses.toMutableSet()
        if (status in newStatuses) {
            newStatuses.remove(status)
        } else {
            newStatuses.add(status)
        }
        return copy(statuses = newStatuses)
    }

    /**
     * 设置搜索文本
     */
    fun withSearchText(text: String): FilterCriteria {
        return copy(searchText = text)
    }

    /**
     * 设置搜索列
     */
    fun withSearchColumns(columns: Set<Int>): FilterCriteria {
        return copy(searchColumns = columns)
    }

    companion object {
        /**
         * 从优先级列表创建筛选条件
         */
        fun byPriorities(vararg priorities: Priority): FilterCriteria {
            return FilterCriteria(priorities = priorities.toSet())
        }

        /**
         * 从状态列表创建筛选条件
         */
        fun byStatuses(vararg statuses: Status): FilterCriteria {
            return FilterCriteria(statuses = statuses.toSet())
        }

        /**
         * 从搜索文本创建筛选条件
         */
        fun bySearch(text: String): FilterCriteria {
            return FilterCriteria(searchText = text)
        }
    }
}
