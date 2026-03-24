package com.testcase.manager.sync

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.ui.TestCaseTableModel
import com.testcase.manager.yaml.YamlParser

/**
 * BidirectionalSyncManager 单元测试
 *
 * 测试 YAML 和 Excel 之间的双向同步
 */
class BidirectionalSyncManagerTest : BasePlatformTestCase() {

    private lateinit var syncManager: BidirectionalSyncManager
    private lateinit var tableModel: TestCaseTableModel
    private lateinit var parser: YamlParser

    override fun setUp() {
        super.setUp()
        tableModel = TestCaseTableModel()
        syncManager = BidirectionalSyncManager(project, tableModel)
        parser = YamlParser()
    }

    fun testSyncFromYamlToExcel() {
        val yamlContent = """
            test_cases:
              - id: TC001
                name: 登录测试
                priority: P0
                status: PUBLISHED
                steps:
                  - 打开页面
                expected: 成功
                tags: []
        """.trimIndent()

        val model = parser.parse(yamlContent)
        syncManager.syncFromYamlToExcel(model)

        assertEquals(1, tableModel.rowCount)
        assertEquals("TC001", tableModel.getValueAt(0, 0))
        assertEquals("登录测试", tableModel.getValueAt(0, 1))
    }

    fun testSyncFromExcelToYaml() {
        val testCase = TestCase(
            id = "TC001",
            name = "测试用例",
            priority = Priority.P1,
            status = Status.DRAFT,
            steps = listOf("步骤1", "步骤2"),
            expected = "预期结果",
            tags = listOf("tag1")
        )
        tableModel.addRow(testCase)

        val yaml = syncManager.syncFromExcelToYaml()

        assertTrue(yaml.contains("TC001"))
        assertTrue(yaml.contains("测试用例"))
        assertTrue(yaml.contains("P1"))
    }

    fun testSyncEmptyData() {
        val model = parser.parse("test_cases: []")
        syncManager.syncFromYamlToExcel(model)

        assertEquals(0, tableModel.rowCount)
    }

    fun testSyncMultipleRows() {
        val yamlContent = """
            test_cases:
              - id: TC001
                name: 测试1
                priority: P0
                status: DRAFT
                steps: []
                expected: 结果1
                tags: []
              - id: TC002
                name: 测试2
                priority: P1
                status: PUBLISHED
                steps: []
                expected: 结果2
                tags: []
        """.trimIndent()

        val model = parser.parse(yamlContent)
        syncManager.syncFromYamlToExcel(model)

        assertEquals(2, tableModel.rowCount)
        assertEquals("TC001", tableModel.getValueAt(0, 0))
        assertEquals("TC002", tableModel.getValueAt(1, 0))
    }
}
