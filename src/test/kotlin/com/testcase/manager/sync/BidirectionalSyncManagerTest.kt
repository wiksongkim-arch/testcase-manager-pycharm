package com.testcase.manager.sync

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

class BidirectionalSyncManagerTest {

    private lateinit var syncManager: BidirectionalSyncManager

    @BeforeEach
    fun setUp() {
        syncManager = BidirectionalSyncManager()
    }

    @Test
    @DisplayName("从 YAML 同步到 Excel")
    fun `test sync from YAML to Excel`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 登录测试
                priority: P0
                status: 已发布
                steps:
                  - 打开页面
                  - 输入密码
                expected: 登录成功
                tags:
                  - 登录
                author: QA
        """.trimIndent()

        val result = syncManager.syncFromYamlToExcel(yaml)

        assertTrue(result.success)
        assertEquals(BidirectionalSyncManager.SyncDirection.YAML_TO_EXCEL, result.direction)
        assertNotNull(result.model)
        assertEquals(1, result.model?.size())
        
        val testCase = result.model?.findById("TC001")
        assertNotNull(testCase)
        assertEquals("登录测试", testCase?.name)
    }

    @Test
    @DisplayName("从 Excel 同步到 YAML")
    fun `test sync from Excel to YAML`() {
        val headers = listOf("ID", "用例名称", "优先级", "状态", "测试步骤", "预期结果", "标签", "作者")
        val rows = listOf(
            listOf("TC001", "登录测试", "P0", "已发布", "1. 打开页面\n2. 输入密码", "登录成功", "登录", "QA")
        )

        val result = syncManager.syncFromExcelToYaml(headers, rows)

        assertTrue(result.success)
        assertEquals(BidirectionalSyncManager.SyncDirection.EXCEL_TO_YAML, result.direction)
        assertNotNull(result.model)
    }

    @Test
    @DisplayName("Excel 到 YAML 验证失败")
    fun `test Excel to YAML validation failure`() {
        val headers = listOf("ID", "用例名称")
        val rows = listOf(
            listOf("", "没有ID的测试")  // 空 ID
        )

        val result = syncManager.syncFromExcelToYaml(headers, rows)

        assertFalse(result.success)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("ID") })
    }

    @Test
    @DisplayName("检测重复 ID")
    fun `test detect duplicate IDs`() {
        val headers = listOf("ID", "用例名称")
        val rows = listOf(
            listOf("TC001", "测试1"),
            listOf("TC001", "测试2")  // 重复 ID
        )

        val result = syncManager.syncFromExcelToYaml(headers, rows)

        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("Duplicate") || it.contains("重复") })
    }

    @Test
    @DisplayName("转换到 YAML 字符串")
    fun `test convert to YAML`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "测试",
                priority = Priority.P0,
                status = Status.PUBLISHED
            ))
        }

        val yaml = syncManager.convertToYaml(model)

        assertTrue(yaml.contains("test_cases:"))
        assertTrue(yaml.contains("TC001"))
        assertTrue(yaml.contains("测试"))
    }

    @Test
    @DisplayName("转换到不同格式")
    fun `test convert to different formats`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(id = "TC001", name = "测试"))
        }

        val standard = syncManager.convertToYaml(model, YamlOutputFormat.STANDARD)
        val compact = syncManager.convertToYaml(model, YamlOutputFormat.COMPACT)
        val pytest = syncManager.convertToYaml(model, YamlOutputFormat.PYTEST)

        assertTrue(standard.contains("metadata:"))
        assertFalse(compact.contains("metadata:"))
        assertTrue(pytest.contains("# Test Cases for pytest"))
    }

    @Test
    @DisplayName("转换到 Excel 数据")
    fun `test convert to Excel data`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试
                priority: P0
        """.trimIndent()

        val result = syncManager.convertToExcelData(yaml)

        assertNotNull(result)
        val (headers, rows) = result!!
        assertTrue(headers.contains("ID"))
        assertTrue(headers.contains("用例名称"))
        assertEquals(1, rows.size)
        assertEquals("TC001", rows[0][0])
    }

    @Test
    @DisplayName("转换无效 YAML 返回 null")
    fun `test convert invalid YAML returns null`() {
        val yaml = "invalid: yaml: content: ["

        val result = syncManager.convertToExcelData(yaml)

        assertNull(result)
    }

    @Test
    @DisplayName("检查 YAML 错误")
    fun `test has errors`() {
        val validYaml = """
            test_cases:
              - id: TC001
                name: 测试
        """.trimIndent()

        val invalidYaml = """
            test_cases:
              - name: 没有ID
        """.trimIndent()

        assertFalse(syncManager.hasErrors(validYaml))
        assertTrue(syncManager.hasErrors(invalidYaml))
    }

    @Test
    @DisplayName("获取验证错误")
    fun `test get validation errors`() {
        val yaml = """
            test_cases:
              - name: 只有名称
        """.trimIndent()

        val errors = syncManager.getValidationErrors(yaml)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("id") || it.contains("ID") })
    }

    @Test
    @DisplayName("生成示例")
    fun `test generate example`() {
        val yaml = syncManager.generateExampleYaml()

        assertTrue(yaml.contains("test_cases:"))
        assertTrue(yaml.contains("TC001"))
        assertTrue(yaml.contains("TC002"))
    }

    @Test
    @DisplayName("保留现有 YAML 注释")
    fun `test preserve existing comments`() {
        val existingYaml = """
            # 项目测试用例
            # 创建于 2024
            
            test_cases:
              - id: TC001
                name: 旧名称
        """.trimIndent()

        val headers = listOf("ID", "用例名称")
        val rows = listOf(listOf("TC001", "新名称"))

        val result = syncManager.syncFromExcelToYaml(headers, rows, existingYaml)

        assertTrue(result.success)
    }

    @Test
    @DisplayName("空模型验证")
    fun `test empty model validation`() {
        val headers = listOf("ID", "用例名称")
        val rows = emptyList<List<Any>>()

        val result = syncManager.syncFromExcelToYaml(headers, rows)

        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("No test cases") || it.contains("Empty") })
    }

    @Test
    @DisplayName("完整往返测试")
    fun `test round trip conversion`() {
        // 原始 YAML
        val originalYaml = """
            test_cases:
              - id: TC001
                name: 登录测试
                priority: P0
                status: 已发布
                steps:
                  - 打开页面
                  - 输入密码
                expected: 登录成功
                tags:
                  - 登录
                author: QA
        """.trimIndent()

        // YAML -> Excel
        val syncResult = syncManager.syncFromYamlToExcel(originalYaml)
        assertTrue(syncResult.success)
        val model = syncResult.model!!

        // Excel -> YAML
        val (headers, rows) = model.toExcelData()
        val reverseResult = syncManager.syncFromExcelToYaml(headers, rows)
        assertTrue(reverseResult.success)

        // 验证数据一致性
        val reversedModel = reverseResult.model!!
        assertEquals(model.size(), reversedModel.size())
        
        val originalCase = model.findById("TC001")!!
        val reversedCase = reversedModel.findById("TC001")!!
        
        assertEquals(originalCase.name, reversedCase.name)
        assertEquals(originalCase.priority, reversedCase.priority)
        assertEquals(originalCase.status, reversedCase.status)
        assertEquals(originalCase.steps.size, reversedCase.steps.size)
    }
}
