package com.testcase.manager.yaml

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel

/**
 * YamlParser 单元测试
 *
 * 测试 YAML 解析功能
 */
class YamlParserTest : BasePlatformTestCase() {

    private lateinit var parser: YamlParser

    override fun setUp() {
        super.setUp()
        parser = YamlParser()
    }

    fun testParseSimpleYaml() {
        val yamlContent = """
            test_cases:
              - id: TC001
                name: 登录测试
                priority: P0
                status: PUBLISHED
                steps:
                  - 打开登录页面
                  - 输入用户名密码
                expected: 登录成功
                tags:
                  - login
        """.trimIndent()

        val model = parser.parse(yamlContent)

        assertEquals(1, model.testCases.size)
        assertEquals("TC001", model.testCases[0].id)
        assertEquals("登录测试", model.testCases[0].name)
        assertEquals(Priority.P0, model.testCases[0].priority)
        assertEquals(Status.PUBLISHED, model.testCases[0].status)
    }

    fun testParseMultipleTestCases() {
        val yamlContent = """
            test_cases:
              - id: TC001
                name: 测试1
                priority: P0
                status: DRAFT
                steps: []
                expected: 结果1
              - id: TC002
                name: 测试2
                priority: P1
                status: PUBLISHED
                steps: []
                expected: 结果2
        """.trimIndent()

        val model = parser.parse(yamlContent)

        assertEquals(2, model.testCases.size)
        assertEquals("TC001", model.testCases[0].id)
        assertEquals("TC002", model.testCases[1].id)
    }

    fun testParseEmptyYaml() {
        val yamlContent = """
            test_cases: []
        """.trimIndent()

        val model = parser.parse(yamlContent)

        assertEquals(0, model.testCases.size)
    }

    fun testParseWithMetadata() {
        val yamlContent = """
            version: "1.0"
            project: 测试项目
            test_cases:
              - id: TC001
                name: 测试1
                priority: P0
                status: DRAFT
                steps: []
                expected: 结果
        """.trimIndent()

        val model = parser.parse(yamlContent)

        assertEquals(1, model.testCases.size)
        assertEquals("TC001", model.testCases[0].id)
    }

    fun testParseInvalidYaml() {
        val yamlContent = "invalid: yaml: content: ["

        try {
            parser.parse(yamlContent)
            fail("应该抛出异常")
        } catch (e: Exception) {
            // 预期行为
        }
    }

    fun testParseWithSpecialCharacters() {
        val yamlContent = """
            test_cases:
              - id: TC001
                name: "测试：特殊字符 <> & \""
                priority: P0
                status: DRAFT
                steps:
                  - "步骤1: 执行操作"
                expected: "结果：成功"
                tags: []
        """.trimIndent()

        val model = parser.parse(yamlContent)

        assertEquals(1, model.testCases.size)
        assertEquals("测试：特殊字符 <> & \"", model.testCases[0].name)
    }
}
