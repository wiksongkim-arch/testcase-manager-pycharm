package com.testcase.manager.ui.style

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.JTable
import javax.swing.border.Border
import javax.swing.table.DefaultTableCellRenderer

/**
 * 自定义单元格渲染器
 * 支持样式、条件格式、边框等
 */
class StyledCellRenderer(
    private val styleManager: CellStyleManager = CellStyleManager()
) : DefaultTableCellRenderer() {

    private var currentStyle: CellStyle? = null
    private var isSelected: Boolean = false
    private var hasFocus: Boolean = false

    init {
        // 设置默认透明背景
        isOpaque = true
    }

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        // 调用父类方法获取基础组件
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        this.isSelected = isSelected
        this.hasFocus = hasFocus
        this.currentStyle = null

        // 获取单元格样式
        if (table != null) {
            currentStyle = styleManager.getCellStyle(table, row, column)
            applyStyle(currentStyle, table, isSelected)
        }

        // 设置文本值
        text = value?.toString() ?: ""

        // 设置工具提示（如果文本被截断）
        toolTipText = if (text.length > 50) text else null

        return this
    }

    /**
     * 应用样式到渲染器
     */
    private fun applyStyle(style: CellStyle?, table: JTable, isSelected: Boolean) {
        if (style == null) {
            // 使用默认样式
            if (isSelected) {
                background = table.selectionBackground
                foreground = table.selectionForeground
            } else {
                background = table.background
                foreground = table.foreground
            }
            font = table.font
            horizontalAlignment = LEFT
            return
        }

        // 应用背景色
        background = when {
            isSelected -> table.selectionBackground
            style.backgroundColor != null -> style.backgroundColor
            else -> table.background
        }

        // 应用前景色
        foreground = when {
            isSelected -> table.selectionForeground
            style.foregroundColor != null -> style.foregroundColor
            else -> table.foreground
        }

        // 应用字体
        font = style.createFont(table.font)

        // 应用对齐方式
        horizontalAlignment = when (style.horizontalAlignment) {
            Alignment.LEFT -> LEFT
            Alignment.CENTER -> CENTER
            Alignment.RIGHT -> RIGHT
            else -> LEFT
        }

        // 应用边框
        border = createBorder(style)
    }

    /**
     * 根据样式创建边框
     */
    private fun createBorder(style: CellStyle?): Border {
        if (style == null || style.borderStyle == BorderStyle.NONE) {
            return BorderFactory.createEmptyBorder(2, 4, 2, 4)
        }

        val borderColor = style.borderColor ?: JBColor.GRAY
        val padding = JBUI.insets(2, 4)

        return when (style.borderStyle) {
            BorderStyle.SOLID -> BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
            )
            BorderStyle.DASHED -> BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(borderColor),
                BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
            )
            BorderStyle.DOTTED -> BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(borderColor, 2f, 2f),
                BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
            )
            BorderStyle.DOUBLE -> BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 0, 0, borderColor),
                    BorderFactory.createMatteBorder(0, 0, 1, 1, borderColor)
                ),
                BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
            )
            else -> BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D

        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        // 绘制背景
        if (background != null) {
            g2d.color = background
            g2d.fillRect(0, 0, width, height)
        }

        // 如果有条件格式的特殊效果，在这里绘制
        currentStyle?.let { style ->
            paintSpecialEffects(g2d, style)
        }

        // 调用父类绘制文本
        super.paintComponent(g)
    }

    /**
     * 绘制特殊效果（如数据条、图标集等）
     */
    private fun paintSpecialEffects(g2d: Graphics2D, style: CellStyle) {
        // 可以在这里添加数据条、图标集等视觉效果
        // 目前作为占位符，后续可以扩展
    }
}

/**
 * 表头渲染器
 * 支持排序指示器
 */
class StyledHeaderRenderer(
    private val sorter: com.testcase.manager.ui.sort.TableSorter? = null
) : DefaultTableCellRenderer() {

    init {
        horizontalAlignment = CENTER
        font = font.deriveFont(Font.BOLD)
    }

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        // 添加排序指示器
        val sortDirection = sorter?.getSortDirection(column) ?: com.testcase.manager.ui.sort.SortDirection.NONE
        val sortSymbol = sortDirection.getSymbol()

        text = if (sortSymbol.isNotEmpty()) {
            "<html><b>$value</b> <span style='color:blue'>$sortSymbol</span></html>"
        } else {
            value?.toString() ?: ""
        }

        // 设置表头样式
        background = JBColor(Color(240, 240, 240), Color(60, 60, 60))
        foreground = JBColor(Color.BLACK, Color.WHITE)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, JBColor.GRAY),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        )

        return this
    }
}
