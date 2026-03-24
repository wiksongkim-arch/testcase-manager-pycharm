package com.testcase.manager.ui.style

import java.awt.Color
import java.awt.Font

/**
 * 单元格样式数据类
 * 支持颜色、字体、边框等格式设置
 */
data class CellStyle(
    // 背景颜色
    val backgroundColor: Color? = null,
    // 前景/文字颜色
    val foregroundColor: Color? = null,
    // 字体
    val font: Font? = null,
    // 字体样式（粗体、斜体等）
    val fontStyle: FontStyle = FontStyle.NORMAL,
    // 字体大小
    val fontSize: Int? = null,
    // 水平对齐方式
    val horizontalAlignment: Alignment = Alignment.LEFT,
    // 垂直对齐方式
    val verticalAlignment: Alignment = Alignment.CENTER,
    // 边框样式
    val borderStyle: BorderStyle = BorderStyle.NONE,
    // 边框颜色
    val borderColor: Color? = null,
    // 是否自动换行
    val wrapText: Boolean = false
) {
    /**
     * 合并另一个样式（非空值覆盖）
     */
    fun merge(other: CellStyle): CellStyle {
        return CellStyle(
            backgroundColor = other.backgroundColor ?: backgroundColor,
            foregroundColor = other.foregroundColor ?: foregroundColor,
            font = other.font ?: font,
            fontStyle = if (other.fontStyle != FontStyle.NORMAL) other.fontStyle else fontStyle,
            fontSize = other.fontSize ?: fontSize,
            horizontalAlignment = if (other.horizontalAlignment != Alignment.LEFT) other.horizontalAlignment else horizontalAlignment,
            verticalAlignment = if (other.verticalAlignment != Alignment.CENTER) other.verticalAlignment else verticalAlignment,
            borderStyle = if (other.borderStyle != BorderStyle.NONE) other.borderStyle else borderStyle,
            borderColor = other.borderColor ?: borderColor,
            wrapText = other.wrapText || wrapText
        )
    }

    /**
     * 创建应用了字体样式的 Font 对象
     */
    fun createFont(baseFont: Font): Font {
        val size = fontSize ?: baseFont.size
        val style = when (fontStyle) {
            FontStyle.NORMAL -> Font.PLAIN
            FontStyle.BOLD -> Font.BOLD
            FontStyle.ITALIC -> Font.ITALIC
            FontStyle.BOLD_ITALIC -> Font.BOLD or Font.ITALIC
        }
        return font ?: Font(baseFont.name, style, size)
    }

    companion object {
        // 预定义颜色
        val RED = Color(255, 200, 200)
        val GREEN = Color(200, 255, 200)
        val BLUE = Color(200, 200, 255)
        val YELLOW = Color(255, 255, 200)
        val ORANGE = Color(255, 220, 180)
        val GRAY = Color(230, 230, 230)
        val WHITE = Color.WHITE
        val BLACK = Color.BLACK

        // 预定义样式
        val DEFAULT = CellStyle()
        val HEADER = CellStyle(
            backgroundColor = Color(240, 240, 240),
            fontStyle = FontStyle.BOLD,
            horizontalAlignment = Alignment.CENTER,
            borderStyle = BorderStyle.SOLID
        )
        val HIGH_PRIORITY = CellStyle(
            backgroundColor = Color(255, 200, 200),
            fontStyle = FontStyle.BOLD
        )
        val MEDIUM_PRIORITY = CellStyle(
            backgroundColor = Color(255, 255, 200)
        )
        val LOW_PRIORITY = CellStyle(
            backgroundColor = Color(200, 255, 200)
        )
        val PUBLISHED = CellStyle(
            backgroundColor = Color(200, 255, 200)
        )
        val DRAFT = CellStyle(
            backgroundColor = Color(255, 255, 200)
        )
        val ARCHIVED = CellStyle(
            backgroundColor = Color(230, 230, 230),
            foregroundColor = Color.GRAY
        )
    }
}

/**
 * 字体样式枚举
 */
enum class FontStyle {
    NORMAL,
    BOLD,
    ITALIC,
    BOLD_ITALIC
}

/**
 * 对齐方式枚举
 */
enum class Alignment {
    LEFT,
    CENTER,
    RIGHT,
    TOP,
    BOTTOM
}

/**
 * 边框样式枚举
 */
enum class BorderStyle {
    NONE,
    SOLID,
    DASHED,
    DOTTED,
    DOUBLE
}
