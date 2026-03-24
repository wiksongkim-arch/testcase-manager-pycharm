package com.testcase.manager.ui.style

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import org.junit.Test
import java.awt.Color
import java.awt.Font
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ConditionalFormatManager 单元测试
 */
class ConditionalFormatManagerTest {

    @Test
    fun testPriorityRule() {
        val rule = ConditionalFormatRule.PriorityRule(
            Priority.P0,
            CellStyle(backgroundColor = Color.RED)
        )

        val style = rule.evaluate("P0", 0, 2)
        assertNotNull(style)
        assertEquals(Color.RED, style.backgroundColor)

        val noStyle = rule.evaluate("P1", 0, 2)
        assertNull(noStyle)
    }

    @Test
    fun testStatusRule() {
        val rule = ConditionalFormatRule.StatusRule(
            Status.PUBLISHED,
            CellStyle(backgroundColor = Color.GREEN)
        )

        val style = rule.evaluate("已发布", 0, 3)
        assertNotNull(style)
        assertEquals(Color.GREEN, style.backgroundColor)

        val noStyle = rule.evaluate("草稿", 0, 3)
        assertNull(noStyle)
    }

    @Test
    fun testTextContainsRule() {
        val rule = ConditionalFormatRule.TextContains(
            "error",
            CellStyle(backgroundColor = Color.YELLOW)
        )

        val style = rule.evaluate("This is an error message", 0, 0)
        assertNotNull(style)

        val noStyle = rule.evaluate("This is fine", 0, 0)
        assertNull(noStyle)
    }

    @Test
    fun testEmptyRule() {
        val rule = ConditionalFormatRule.EmptyRule(
            CellStyle(borderStyle = BorderStyle.SOLID, borderColor = Color.RED)
        )

        val style = rule.evaluate("", 0, 0)
        assertNotNull(style)

        val style2 = rule.evaluate("   ", 0, 0)
        assertNotNull(style2)

        val noStyle = rule.evaluate("not empty", 0, 0)
        assertNull(noStyle)
    }

    @Test
    fun testNumberGreaterThanRule() {
        val rule = ConditionalFormatRule.NumberGreaterThan(
            100.0,
            CellStyle(backgroundColor = Color.GREEN)
        )

        val style = rule.evaluate("150", 0, 0)
        assertNotNull(style)

        val noStyle = rule.evaluate("50", 0, 0)
        assertNull(noStyle)
    }

    @Test
    fun testManagerAddRule() {
        val manager = ConditionalFormatManager()
        val rule = ConditionalFormatRule.PriorityRule(
            Priority.P0,
            CellStyle(backgroundColor = Color.RED)
        )

        manager.addRule(2, rule)
        assertEquals(1, manager.getRules(2).size)
    }

    @Test
    fun testManagerEvaluateCell() {
        val manager = ConditionalFormatManager()
        manager.addRule(2, ConditionalFormatRule.PriorityRule(
            Priority.P0,
            CellStyle(backgroundColor = Color.RED)
        ))

        val style = manager.evaluateCell("P0", 0, 2)
        assertNotNull(style)
        assertEquals(Color.RED, style.backgroundColor)

        val noStyle = manager.evaluateCell("P1", 0, 2)
        assertNull(noStyle)
    }

    @Test
    fun testManagerDisabled() {
        val manager = ConditionalFormatManager()
        manager.enabled = false
        manager.addRule(2, ConditionalFormatRule.PriorityRule(
            Priority.P0,
            CellStyle(backgroundColor = Color.RED)
        ))

        val style = manager.evaluateCell("P0", 0, 2)
        assertNull(style)
    }

    @Test
    fun testSetupDefaultTestCaseFormats() {
        val manager = ConditionalFormatManager()
        manager.setupDefaultTestCaseFormats()

        // P0 优先级应该有红色背景
        val p0Style = manager.evaluateCell("P0", 0, 2)
        assertNotNull(p0Style)

        // 已发布状态应该有绿色背景
        val publishedStyle = manager.evaluateCell("已发布", 0, 3)
        assertNotNull(publishedStyle)

        // 空 ID 应该有边框样式
        val emptyStyle = manager.evaluateCell("", 0, 0)
        assertNotNull(emptyStyle)
    }

    @Test
    fun testCellStyleMerge() {
        val style1 = CellStyle(
            backgroundColor = Color.RED,
            fontStyle = FontStyle.BOLD
        )
        val style2 = CellStyle(
            foregroundColor = Color.BLUE,
            fontStyle = FontStyle.ITALIC
        )

        val merged = style1.merge(style2)
        assertEquals(Color.RED, merged.backgroundColor)
        assertEquals(Color.BLUE, merged.foregroundColor)
        assertEquals(FontStyle.ITALIC, merged.fontStyle)
    }

    @Test
    fun testCreateFont() {
        val baseFont = Font("Arial", Font.PLAIN, 12)
        val style = CellStyle(fontStyle = FontStyle.BOLD, fontSize = 14)

        val newFont = style.createFont(baseFont)
        assertEquals(Font.BOLD, newFont.style)
        assertEquals(14, newFont.size)
    }
}
