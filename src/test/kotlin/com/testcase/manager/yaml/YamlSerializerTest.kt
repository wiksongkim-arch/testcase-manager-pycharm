package com.testcase.manager.yaml

import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseMetadata
import com.testcase.manager.model.TestCaseModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

class YamlSerializerTest {

    private lateinit var serializer: YamlSerializer

    @BeforeEach
    fun setUp() {
        serializer = YamlSerializer()
    }

    @Test
    @DisplayName("序列化基本测试用例")
    fun `test serialize basic test case`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "登录测试",
                priority = Priority.P0,
                status = Status.PUBLISHED
            ))
        }

        val yaml = serializer.serialize(model)

        assertTrue(yaml.contains("test_cases:"))
        assertTrue(yaml.contains("id: TC001"))
        assertTrue(yaml.contains("name: 登录测试"))
        assertTrue(yaml.contains("priority: P0"))
        assertTrue(yaml.contains("status: 已发布"))
    }

    @Test
    @DisplayName("序列化包含所有字段的测试用例")
    fun `test serialize full test case`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "完整测试",
                priority = Priority.P1,
                status = Status.DRAFT,
                steps = listOf("步骤1", "步骤2", "步骤3"),
                expected = "预期结果",
                tags = listOf("标签1", "标签2"),
                description = "描述",
                preconditions = "前置条件",
                author = "作者"
            ))
        }

        val yaml = serializer.serialize(model)

        assertTrue(yaml.contains("steps:"))
        assertTrue(yaml.contains("- 步骤1"))
        assertTrue(yaml.contains("expected: 预期结果"))
        assertTrue(yaml.contains("tags:"))
        assertTrue(yaml.contains("- 标签1"))
        assertTrue(yaml.contains("description: 描述"))
        assertTrue(yaml.contains("preconditions: 前置条件"))
        assertTrue(yaml.contains("author: 作者"))
    }

    @Test
    @DisplayName("序列化多个测试用例")
    fun `test serialize multiple test cases`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(id = "TC001", name = "测试1"))
            addTestCase(TestCase(id = "TC002", name = "测试2"))
            addTestCase(TestCase(id = "TC003", name = "测试3"))
        }

        val yaml = serializer.serialize(model)

        assertTrue(yaml.contains("id: TC001"))
        assertTrue(yaml.contains("id: TC002"))
        assertTrue(yaml.contains("id: TC003"))
    }

    @Test
    @DisplayName("序列化包含元数据")
    fun `test serialize with metadata`() {
        val model = TestCaseModel().apply {
            metadata = TestCaseMetadata(
                version = "1.0",
                project = "测试项目"
            )
            addTestCase(TestCase(id = "TC001", name = "测试"))
        }

        val yaml = serializer.serialize(model)

        assertTrue(yaml.contains("metadata:"))
        assertTrue(yaml.contains("version: \"1.0\""))
        assertTrue(yaml.contains("project: 测试项目"))
    }

    @Test
    @DisplayName("序列化紧凑格式")
    fun `test serialize compact format`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "测试",
                priority = Priority.P0,
                steps = listOf("步骤1")
            ))
        }

        val yaml = serializer.serializeCompact(model)

        assertTrue(yaml.contains("test_cases:"))
        assertTrue(yaml.contains("id: TC001"))
        // 紧凑格式不应包含空字段
        assertFalse(yaml.contains("expected:"))
    }

    @Test
    @DisplayName("序列化 Pytest 格式")
    fun `test serialize pytest format`() {
        val model = TestCaseModel().apply {
            metadata = TestCaseMetadata(project = "Example")
            addTestCase(TestCase(id = "TC001", name = "测试"))
        }

        val yaml = serializer.serializeForPytest(model)

        assertTrue(yaml.contains("# Test Cases for pytest"))
        assertTrue(yaml.contains("# Project: Example"))
        assertTrue(yaml.contains("test_cases:"))
    }

    @Test
    @DisplayName("更新现有 YAML 内容")
    fun `test update existing content`() {
        val existing = """
            # 这是注释
            # 应该被保留
            
            metadata:
              version: "1.0"
            test_cases:
              - id: TC001
                name: 旧名称
        """.trimIndent()

        val model = TestCaseModel().apply {
            addTestCase(TestCase(id = "TC001", name = "新名称"))
        }

        val yaml = serializer.update(existing, model)

        assertTrue(yaml.contains("# 这是注释"))
        assertTrue(yaml.contains("name: 新名称"))
    }

    @Test
    @DisplayName("生成示例 YAML")
    fun `test generate example`() {
        val yaml = YamlSerializer.generateExample()

        assertTrue(yaml.contains("# Test Cases for pytest"))
        assertTrue(yaml.contains("test_cases:"))
        assertTrue(yaml.contains("TC001"))
        assertTrue(yaml.contains("TC002"))
        assertTrue(yaml.contains("登录成功"))
    }

    @Test
    @DisplayName("序列化空模型")
    fun `test serialize empty model`() {
        val model = TestCaseModel()

        val yaml = serializer.serialize(model)

        assertTrue(yaml.contains("test_cases: []"))
    }

    @Test
    @DisplayName("序列化不包含元数据")
    fun `test serialize without metadata`() {
        val model = TestCaseModel().apply {
            metadata = TestCaseMetadata(version = "1.0")
            addTestCase(TestCase(id = "TC001", name = "测试"))
        }

        val yaml = serializer.serialize(model, includeMetadata = false)

        assertFalse(yaml.contains("metadata:"))
        assertTrue(yaml.contains("test_cases:"))
    }

    @Test
    @DisplayName("序列化保留空列表")
    fun `test serialize empty lists`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "测试",
                steps = emptyList(),
                tags = emptyList()
            ))
        }

        val yaml = serializer.serialize(model)

        // 空列表不应被序列化
        assertFalse(yaml.contains("steps: []"))
        assertFalse(yaml.contains("tags: []"))
    }

    @Test
    @DisplayName("序列化包含特殊字符")
    fun `test serialize special characters`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "测试：包含特殊字符",
                expected = "结果包含: 冒号和"引号""
            ))
        }

        val yaml = serializer.serialize(model)

        assertTrue(yaml.contains("测试：包含特殊字符"))
    }
}
