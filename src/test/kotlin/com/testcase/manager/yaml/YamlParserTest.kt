package com.testcase.manager.yaml

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

class YamlParserTest {

    private lateinit var parser: YamlParser

    @BeforeEach
    fun setUp() {
        parser = YamlParser()
    }

    @Test
    @DisplayName("解析基本 YAML 内容")
    fun `test parse basic YAML`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 登录成功
                priority: P0
                status: 已发布
                steps:
                  - 打开登录页面
                  - 输入用户名密码
                expected: 登录成功
        """.trimIndent()

        val model = parser.parse(yaml)

        assertEquals(1, model.size())
        val testCase = model.findById("TC001")
        assertNotNull(testCase)
        assertEquals("登录成功", testCase?.name)
        assertEquals(Priority.P0, testCase?.priority)
        assertEquals(Status.PUBLISHED, testCase?.status)
        assertEquals(2, testCase?.steps?.size)
    }

    @Test
    @DisplayName("解析多个测试用例")
    fun `test parse multiple test cases`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试1
                priority: P0
              - id: TC002
                name: 测试2
                priority: P1
              - id: TC003
                name: 测试3
                priority: P2
        """.trimIndent()

        val model = parser.parse(yaml)

        assertEquals(3, model.size())
        assertNotNull(model.findById("TC001"))
        assertNotNull(model.findById("TC002"))
        assertNotNull(model.findById("TC003"))
    }

    @Test
    @DisplayName("解析包含元数据的 YAML")
    fun `test parse with metadata`() {
        val yaml = """
            metadata:
              version: "1.0"
              project: 测试项目
              created_at: "2024-01-01T00:00:00"
            test_cases:
              - id: TC001
                name: 测试用例
        """.trimIndent()

        val model = parser.parse(yaml)

        assertEquals("1.0", model.metadata.version)
        assertEquals("测试项目", model.metadata.project)
        assertEquals("2024-01-01T00:00:00", model.metadata.createdAt)
    }

    @Test
    @DisplayName("解析所有字段")
    fun `test parse all fields`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 完整测试用例
                priority: P0
                status: 已发布
                steps:
                  - 步骤1
                  - 步骤2
                expected: 预期结果
                tags:
                  - 标签1
                  - 标签2
                description: 描述信息
                preconditions: 前置条件
                author: 作者名
        """.trimIndent()

        val model = parser.parse(yaml)
        val testCase = model.findById("TC001")

        assertNotNull(testCase)
        assertEquals("完整测试用例", testCase?.name)
        assertEquals(Priority.P0, testCase?.priority)
        assertEquals(Status.PUBLISHED, testCase?.status)
        assertEquals(2, testCase?.steps?.size)
        assertEquals("预期结果", testCase?.expected)
        assertEquals(2, testCase?.tags?.size)
        assertEquals("描述信息", testCase?.description)
        assertEquals("前置条件", testCase?.preconditions)
        assertEquals("作者名", testCase?.author)
    }

    @Test
    @DisplayName("解析英文状态值")
    fun `test parse English status values`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试1
                status: DRAFT
              - id: TC002
                name: 测试2
                status: PUBLISHED
        """.trimIndent()

        val model = parser.parse(yaml)

        assertEquals(Status.DRAFT, model.findById("TC001")?.status)
        assertEquals(Status.PUBLISHED, model.findById("TC002")?.status)
    }

    @Test
    @DisplayName("解析数字优先级")
    fun `test parse numeric priority`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试1
                priority: 0
              - id: TC002
                name: 测试2
                priority: 1
        """.trimIndent()

        val model = parser.parse(yaml)

        assertEquals(Priority.P0, model.findById("TC001")?.priority)
        assertEquals(Priority.P1, model.findById("TC002")?.priority)
    }

    @Test
    @DisplayName("解析空 YAML")
    fun `test parse empty YAML`() {
        val yaml = ""

        val model = parser.parse(yaml)

        assertTrue(model.isEmpty())
    }

    @Test
    @DisplayName("验证有效 YAML")
    fun `test validate valid YAML`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试用例
                priority: P0
        """.trimIndent()

        val result = parser.validate(yaml)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    @DisplayName("验证缺少 test_cases 字段")
    fun `test validate missing test_cases`() {
        val yaml = """
            metadata:
              version: "1.0"
        """.trimIndent()

        val result = parser.validate(yaml)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("test_cases") })
    }

    @Test
    @DisplayName("验证缺少必填字段")
    fun `test validate missing required fields`() {
        val yaml = """
            test_cases:
              - name: 只有名称
              - id: TC002
        """.trimIndent()

        val result = parser.validate(yaml)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("id") })
        assertTrue(result.errors.any { it.contains("name") })
    }

    @Test
    @DisplayName("验证无效的优先级")
    fun `test validate invalid priority`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试
                priority: INVALID
        """.trimIndent()

        val result = parser.validate(yaml)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("priority") })
    }

    @Test
    @DisplayName("解析字符串形式的 steps")
    fun `test parse string steps`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试
                steps: 单个步骤
        """.trimIndent()

        val model = parser.parse(yaml)
        val testCase = model.findById("TC001")

        assertEquals(1, testCase?.steps?.size)
        assertEquals("单个步骤", testCase?.steps?.get(0))
    }

    @Test
    @DisplayName("解析逗号分隔的标签")
    fun `test parse comma separated tags`() {
        val yaml = """
            test_cases:
              - id: TC001
                name: 测试
                tags: "tag1, tag2, tag3"
        """.trimIndent()

        val model = parser.parse(yaml)
        val testCase = model.findById("TC001")

        assertEquals(3, testCase?.tags?.size)
        assertEquals("tag1", testCase?.tags?.get(0))
        assertEquals("tag2", testCase?.tags?.get(1))
    }
}
