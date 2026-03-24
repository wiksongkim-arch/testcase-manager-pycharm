package com.testcase.manager.ui.style

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import java.awt.Color

/**
 * 条件格式规则
 * 基于单元格值自动应用样式
 */
sealed class ConditionalFormatRule {

    /**
     * 评估单元格值并返回匹配的样式
     * @param value 单元格值
     * @param row 行索引
     * @param column 列索引
     * @return 应用的样式，如果不匹配返回 null
     */
    abstract fun evaluate(value: Any?, row: Int, column: Int): CellStyle?

    /**
     * 文本包含规则
     */
    data class TextContains(
        val searchText: String,
        val style: CellStyle,
        val ignoreCase: Boolean = true
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val text = value?.toString() ?: return null
            val compareText = if (ignoreCase) text.lowercase() else text
            val compareSearch = if (ignoreCase) searchText.lowercase() else searchText
            return if (compareText.contains(compareSearch)) style else null
        }
    }

    /**
     * 文本等于规则
     */
    data class TextEquals(
        val matchText: String,
        val style: CellStyle,
        val ignoreCase: Boolean = true
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val text = value?.toString() ?: return null
            return if (text.equals(matchText, ignoreCase)) style else null
        }
    }

    /**
     * 数值大于规则
     */
    data class NumberGreaterThan(
        val threshold: Double,
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val number = value?.toString()?.toDoubleOrNull() ?: return null
            return if (number > threshold) style else null
        }
    }

    /**
     * 数值小于规则
     */
    data class NumberLessThan(
        val threshold: Double,
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val number = value?.toString()?.toDoubleOrNull() ?: return null
            return if (number < threshold) style else null
        }
    }

    /**
     * 数值介于规则
     */
    data class NumberBetween(
        val min: Double,
        val max: Double,
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val number = value?.toString()?.toDoubleOrNull() ?: return null
            return if (number in min..max) style else null
        }
    }

    /**
     * 优先级规则
     */
    data class PriorityRule(
        val priority: Priority,
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val text = value?.toString() ?: return null
            return if (Priority.fromValue(text) == priority) style else null
        }
    }

    /**
     * 状态规则
     */
    data class StatusRule(
        val status: Status,
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val text = value?.toString() ?: return null
            return if (Status.fromValue(text) == status) style else null
        }
    }

    /**
     * 空值规则
     */
    data class EmptyRule(
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            val text = value?.toString() ?: ""
            return if (text.isBlank()) style else null
        }
    }

    /**
     * 自定义评估规则
     */
    data class CustomRule(
        val evaluator: (value: Any?, row: Int, column: Int) -> Boolean,
        val style: CellStyle
    ) : ConditionalFormatRule() {
        override fun evaluate(value: Any?, row: Int, column: Int): CellStyle? {
            return if (evaluator(value, row, column)) style else null
        }
    }
}

/**
 * 条件格式管理器
 * 管理应用于表格的条件格式规则
 */
class ConditionalFormatManager {

    // 列索引到规则列表的映射
    private val columnRules: MutableMap<Int, MutableList<ConditionalFormatRule>> = mutableMapOf()

    // 全局规则（应用于所有列）
    private val globalRules: MutableList<ConditionalFormatRule> = mutableListOf()

    // 是否启用条件格式
    var enabled: Boolean = true

    /**
     * 为指定列添加规则
     */
    fun addRule(columnIndex: Int, rule: ConditionalFormatRule) {
        columnRules.getOrPut(columnIndex) { mutableListOf() }.add(rule)
    }

    /**
     * 添加全局规则
     */
    fun addGlobalRule(rule: ConditionalFormatRule) {
        globalRules.add(rule)
    }

    /**
     * 移除指定列的所有规则
     */
    fun clearRules(columnIndex: Int) {
        columnRules.remove(columnIndex)
    }

    /**
     * 移除所有规则
     */
    fun clearAllRules() {
        columnRules.clear()
        globalRules.clear()
    }

    /**
     * 获取指定列的规则
     */
    fun getRules(columnIndex: Int): List<ConditionalFormatRule> {
        return columnRules[columnIndex] ?: emptyList()
    }

    /**
     * 评估单元格并返回应用的样式
     * 如果有多个规则匹配，后面的规则会覆盖前面的
     */
    fun evaluateCell(value: Any?, row: Int, column: Int): CellStyle? {
        if (!enabled) return null

        var resultStyle: CellStyle? = null

        // 先应用全局规则
        for (rule in globalRules) {
            rule.evaluate(value, row, column)?.let {
                resultStyle = resultStyle?.merge(it) ?: it
            }
        }

        // 再应用列特定规则
        columnRules[column]?.forEach { rule ->
            rule.evaluate(value, row, column)?.let {
                resultStyle = resultStyle?.merge(it) ?: it
            }
        }

        return resultStyle
    }

    /**
     * 设置默认的测试用例条件格式
     */
    fun setupDefaultTestCaseFormats() {
        clearAllRules()

        // 优先级列格式化（列索引 2）
        addRule(2, ConditionalFormatRule.PriorityRule(
            Priority.P0,
            CellStyle(
                backgroundColor = Color(255, 180, 180),
                fontStyle = FontStyle.BOLD,
                foregroundColor = Color(180, 0, 0)
            )
        ))
        addRule(2, ConditionalFormatRule.PriorityRule(
            Priority.P1,
            CellStyle(
                backgroundColor = Color(255, 200, 150),
                fontStyle = FontStyle.BOLD
            )
        ))
        addRule(2, ConditionalFormatRule.PriorityRule(
            Priority.P2,
            CellStyle(backgroundColor = Color(255, 255, 180))
        ))
        addRule(2, ConditionalFormatRule.PriorityRule(
            Priority.P3,
            CellStyle(backgroundColor = Color(200, 255, 200))
        ))

        // 状态列格式化（列索引 3）
        addRule(3, ConditionalFormatRule.StatusRule(
            Status.PUBLISHED,
            CellStyle(
                backgroundColor = Color(180, 255, 180),
                foregroundColor = Color(0, 100, 0)
            )
        ))
        addRule(3, ConditionalFormatRule.StatusRule(
            Status.DRAFT,
            CellStyle(backgroundColor = Color(255, 255, 200))
        ))
        addRule(3, ConditionalFormatRule.StatusRule(
            Status.ARCHIVED,
            CellStyle(
                backgroundColor = Color(230, 230, 230),
                foregroundColor = Color(100, 100, 100)
            )
        ))
        addRule(3, ConditionalFormatRule.StatusRule(
            Status.DISABLED,
            CellStyle(
                backgroundColor = Color(255, 200, 200),
                foregroundColor = Color(150, 0, 0)
            )
        ))

        // ID 列空值检查（列索引 0）
        addRule(0, ConditionalFormatRule.EmptyRule(
            CellStyle(
                backgroundColor = Color(255, 220, 220),
                borderStyle = BorderStyle.SOLID,
                borderColor = Color.RED
            )
        ))

        // 用例名称列空值检查（列索引 1）
        addRule(1, ConditionalFormatRule.EmptyRule(
            CellStyle(
                backgroundColor = Color(255, 220, 220),
                borderStyle = BorderStyle.SOLID,
                borderColor = Color.RED
            )
        ))
    }

    /**
     * 创建数据条样式（用于数值可视化）
     */
    fun createDataBarStyle(
        value: Double,
        min: Double,
        max: Double,
        color: Color = Color(100, 150, 255)
    ): CellStyle {
        val ratio = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        val intensity = (ratio * 200 + 55).toInt()
        return CellStyle(
            backgroundColor = Color(
                color.red * intensity / 255,
                color.green * intensity / 255,
                color.blue * intensity / 255
            )
        )
    }

    /**
     * 创建色阶样式
     */
    fun createColorScaleStyle(
        value: Double,
        min: Double,
        max: Double,
        minColor: Color = Color(255, 200, 200),
        maxColor: Color = Color(200, 255, 200)
    ): CellStyle {
        val ratio = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        return CellStyle(
            backgroundColor = Color(
                (minColor.red * (1 - ratio) + maxColor.red * ratio).toInt(),
                (minColor.green * (1 - ratio) + maxColor.green * ratio).toInt(),
                (minColor.blue * (1 - ratio) + maxColor.blue * ratio).toInt()
            )
        )
    }
}
