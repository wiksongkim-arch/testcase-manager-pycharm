package com.testcase.manager.ui.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.testcase.manager.ui.style.*
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

/**
 * 格式工具栏组件
 * 提供单元格格式化功能（颜色、字体、边框等）
 */
class FormattingToolbar(
    private val onApplyStyle: (CellStyle) -> Unit = {},
    private val onClearStyle: () -> Unit = {},
    private val onToggleConditionalFormat: (Boolean) -> Unit = {}
) : JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)) {

    // 颜色选择按钮
    private val bgColorButton: JButton
    private val fgColorButton: JButton
    private val borderColorButton: JButton

    // 字体样式按钮
    private val boldButton: JToggleButton
    private val italicButton: JToggleButton

    // 对齐方式按钮组
    private val alignLeftButton: JToggleButton
    private val alignCenterButton: JToggleButton
    private val alignRightButton: JToggleButton

    // 边框样式下拉框
    private val borderStyleCombo: ComboBox<BorderStyleItem>

    // 条件格式开关
    private val conditionalFormatCheckBox: JCheckBox

    // 当前选中的颜色
    private var selectedBgColor: Color? = null
    private var selectedFgColor: Color? = null
    private var selectedBorderColor: Color? = null

    init {
        border = JBUI.Borders.empty(4, 8)

        // 背景颜色按钮
        bgColorButton = createColorButton("背景色", Color.WHITE) { color ->
            selectedBgColor = color
            applyCurrentStyle()
        }

        // 前景颜色按钮
        fgColorButton = createColorButton("文字色", Color.BLACK) { color ->
            selectedFgColor = color
            applyCurrentStyle()
        }

        // 边框颜色按钮
        borderColorButton = createColorButton("边框色", Color.GRAY) { color ->
            selectedBorderColor = color
            applyCurrentStyle()
        }

        // 字体样式按钮
        boldButton = JToggleButton("B").apply {
            font = font.deriveFont(Font.BOLD)
            preferredSize = Dimension(32, 28)
            toolTipText = "粗体"
            addActionListener { applyCurrentStyle() }
        }

        italicButton = JToggleButton("I").apply {
            font = font.deriveFont(Font.ITALIC)
            preferredSize = Dimension(32, 28)
            toolTipText = "斜体"
            addActionListener { applyCurrentStyle() }
        }

        // 对齐按钮组
        val alignGroup = ButtonGroup()
        alignLeftButton = JToggleButton(AllIcons.Actions.MoveLeft).apply {
            preferredSize = Dimension(32, 28)
            toolTipText = "左对齐"
            addActionListener { applyCurrentStyle() }
        }
        alignCenterButton = JToggleButton(AllIcons.Actions.MoveToCenter).apply {
            preferredSize = Dimension(32, 28)
            toolTipText = "居中对齐"
            addActionListener { applyCurrentStyle() }
        }
        alignRightButton = JToggleButton(AllIcons.Actions.MoveRight).apply {
            preferredSize = Dimension(32, 28)
            toolTipText = "右对齐"
            addActionListener { applyCurrentStyle() }
        }
        alignGroup.add(alignLeftButton)
        alignGroup.add(alignCenterButton)
        alignGroup.add(alignRightButton)
        alignLeftButton.isSelected = true

        // 边框样式下拉框
        borderStyleCombo = ComboBox<BorderStyleItem>().apply {
            addItem(BorderStyleItem(BorderStyle.NONE, "无边框"))
            addItem(BorderStyleItem(BorderStyle.SOLID, "实线"))
            addItem(BorderStyleItem(BorderStyle.DASHED, "虚线"))
            addItem(BorderStyleItem(BorderStyle.DOTTED, "点线"))
            addItem(BorderStyleItem(BorderStyle.DOUBLE, "双线"))
            addActionListener { applyCurrentStyle() }
        }

        // 条件格式开关
        conditionalFormatCheckBox = JCheckBox("条件格式").apply {
            isSelected = true
            toolTipText = "根据单元格内容自动应用格式"
            addActionListener {
                onToggleConditionalFormat(isSelected)
            }
        }

        // 清除格式按钮
        val clearButton = JButton("清除格式", AllIcons.Actions.GC).apply {
            addActionListener { onClearStyle() }
        }

        // 组装工具栏
        add(JLabel(AllIcons.Actions.Edit))
        add(Box.createHorizontalStrut(8))

        add(JLabel("背景:"))
        add(bgColorButton)
        add(Box.createHorizontalStrut(4))

        add(JLabel("文字:"))
        add(fgColorButton)
        add(Box.createHorizontalStrut(8))

        add(JSeparator(SwingConstants.VERTICAL).apply {
            preferredSize = Dimension(2, 24)
        })
        add(Box.createHorizontalStrut(8))

        add(boldButton)
        add(italicButton)
        add(Box.createHorizontalStrut(8))

        add(alignLeftButton)
        add(alignCenterButton)
        add(alignRightButton)
        add(Box.createHorizontalStrut(8))

        add(JSeparator(SwingConstants.VERTICAL).apply {
            preferredSize = Dimension(2, 24)
        })
        add(Box.createHorizontalStrut(8))

        add(JLabel("边框:"))
        add(borderStyleCombo)
        add(borderColorButton)
        add(Box.createHorizontalStrut(8))

        add(JSeparator(SwingConstants.VERTICAL).apply {
            preferredSize = Dimension(2, 24)
        })
        add(Box.createHorizontalStrut(8))

        add(conditionalFormatCheckBox)
        add(Box.createHorizontalStrut(16))
        add(clearButton)
    }

    /**
     * 创建颜色选择按钮
     */
    private fun createColorButton(
        label: String,
        defaultColor: Color,
        onColorSelected: (Color) -> Unit
    ): JButton {
        return JButton(label).apply {
            preferredSize = Dimension(60, 28)
            background = defaultColor
            isOpaque = true
            contentAreaFilled = true
            border = BorderFactory.createLineBorder(JBColor.GRAY)

            addActionListener {
                val color = JColorChooser.showDialog(
                    this@FormattingToolbar,
                    "选择颜色",
                    background
                )
                if (color != null) {
                    background = color
                    onColorSelected(color)
                }
            }
        }
    }

    /**
     * 应用当前样式设置
     */
    private fun applyCurrentStyle() {
        val fontStyle = when {
            boldButton.isSelected && italicButton.isSelected -> FontStyle.BOLD_ITALIC
            boldButton.isSelected -> FontStyle.BOLD
            italicButton.isSelected -> FontStyle.ITALIC
            else -> FontStyle.NORMAL
        }

        val horizontalAlignment = when {
            alignCenterButton.isSelected -> Alignment.CENTER
            alignRightButton.isSelected -> Alignment.RIGHT
            else -> Alignment.LEFT
        }

        val borderStyle = (borderStyleCombo.selectedItem as? BorderStyleItem)?.style ?: BorderStyle.NONE

        val style = CellStyle(
            backgroundColor = selectedBgColor,
            foregroundColor = selectedFgColor,
            fontStyle = fontStyle,
            horizontalAlignment = horizontalAlignment,
            borderStyle = borderStyle,
            borderColor = selectedBorderColor
        )

        onApplyStyle(style)
    }

    /**
     * 更新工具栏状态以匹配当前单元格样式
     */
    fun updateFromStyle(style: CellStyle?) {
        if (style == null) {
            boldButton.isSelected = false
            italicButton.isSelected = false
            alignLeftButton.isSelected = true
            borderStyleCombo.selectedIndex = 0
            return
        }

        // 更新字体样式按钮
        boldButton.isSelected = style.fontStyle == FontStyle.BOLD || style.fontStyle == FontStyle.BOLD_ITALIC
        italicButton.isSelected = style.fontStyle == FontStyle.ITALIC || style.fontStyle == FontStyle.BOLD_ITALIC

        // 更新对齐按钮
        when (style.horizontalAlignment) {
            Alignment.LEFT -> alignLeftButton.isSelected = true
            Alignment.CENTER -> alignCenterButton.isSelected = true
            Alignment.RIGHT -> alignRightButton.isSelected = true
            else -> alignLeftButton.isSelected = true
        }

        // 更新边框样式
        for (i in 0 until borderStyleCombo.itemCount) {
            if (borderStyleCombo.getItemAt(i)?.style == style.borderStyle) {
                borderStyleCombo.selectedIndex = i
                break
            }
        }

        // 更新颜色按钮
        style.backgroundColor?.let {
            selectedBgColor = it
            bgColorButton.background = it
        }
        style.foregroundColor?.let {
            selectedFgColor = it
            fgColorButton.background = it
        }
        style.borderColor?.let {
            selectedBorderColor = it
            borderColorButton.background = it
        }
    }

    /**
     * 边框样式选项
     */
    private data class BorderStyleItem(
        val style: BorderStyle,
        val displayName: String
    ) {
        override fun toString(): String = displayName
    }
}
