package com.testcase.manager.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TestCaseModelTest {

    @Test
    fun `test TestCase default values`() {
        val testCase = TestCase()
        
        assertEquals("", testCase.id)
        assertEquals("", testCase.name)
        assertEquals(Priority.P2, testCase.priority)
        assertEquals(Status.DRAFT, testCase.status)
        assertTrue(testCase.steps.isEmpty())
        assertEquals("", testCase.expected)
    }

    @Test
    fun `test TestCase with values`() {
        val testCase = TestCase(
            id = "TC001",
            name = "登录测试",
            priority = Priority.P0,
            status = Status.PUBLISHED,
            steps = listOf("步骤1", "步骤2"),
            expected = "预期结果"
        )
        
        assertEquals("TC001", testCase.id)
        assertEquals("登录测试", testCase.name)
        assertEquals(Priority.P0, testCase.priority)
        assertEquals(Status.PUBLISHED, testCase.status)
        assertEquals(2, testCase.steps.size)
        assertEquals("预期结果", testCase.expected)
    }

    @Test
    fun `test getStepsAsString`() {
        val testCase = TestCase(
            steps = listOf("打开页面", "输入用户名", "点击登录")
        )
        
        val result = testCase.getStepsAsString()
        
        assertEquals("1. 打开页面\n2. 输入用户名\n3. 点击登录", result)
    }

    @Test
    fun `test setStepsFromString`() {
        val testCase = TestCase()
        val stepsString = "1. 打开页面\n2. 输入用户名\n3. 点击登录"
        
        testCase.setStepsFromString(stepsString)
        
        assertEquals(3, testCase.steps.size)
        assertEquals("打开页面", testCase.steps[0])
        assertEquals("输入用户名", testCase.steps[1])
        assertEquals("点击登录", testCase.steps[2])
    }

    @Test
    fun `test setStepsFromString without numbers`() {
        val testCase = TestCase()
        val stepsString = "打开页面\n输入用户名\n点击登录"
        
        testCase.setStepsFromString(stepsString)
        
        assertEquals(3, testCase.steps.size)
        assertEquals("打开页面", testCase.steps[0])
    }

    @Test
    fun `test getTagsAsString`() {
        val testCase = TestCase(
            tags = listOf("登录", "正向场景", "P0")
        )
        
        val result = testCase.getTagsAsString()
        
        assertEquals("登录, 正向场景, P0", result)
    }

    @Test
    fun `test setTagsFromString`() {
        val testCase = TestCase()
        val tagsString = "登录, 正向场景, P0"
        
        testCase.setTagsFromString(tagsString)
        
        assertEquals(3, testCase.tags.size)
        assertEquals("登录", testCase.tags[0])
        assertEquals("正向场景", testCase.tags[1])
        assertEquals("P0", testCase.tags[2])
    }

    @Test
    fun `test TestCaseModel operations`() {
        val model = TestCaseModel()
        
        assertTrue(model.isEmpty())
        assertEquals(0, model.size())
        
        val tc1 = TestCase(id = "TC001", name = "测试1")
        val tc2 = TestCase(id = "TC002", name = "测试2")
        
        model.addTestCase(tc1)
        model.addTestCase(tc2)
        
        assertEquals(2, model.size())
        assertFalse(model.isEmpty())
        
        val found = model.findById("TC001")
        assertNotNull(found)
        assertEquals("测试1", found?.name)
        
        val removed = model.removeTestCase("TC001")
        assertTrue(removed)
        assertEquals(1, model.size())
        
        val notFound = model.findById("TC001")
        assertNull(notFound)
    }

    @Test
    fun `test toExcelData`() {
        val model = TestCaseModel().apply {
            addTestCase(TestCase(
                id = "TC001",
                name = "登录测试",
                priority = Priority.P0,
                status = Status.PUBLISHED,
                steps = listOf("步骤1", "步骤2"),
                expected = "预期结果",
                tags = listOf("登录"),
                author = "QA"
            ))
        }
        
        val (headers, rows) = model.toExcelData()
        
        assertEquals(10, headers.size)
        assertEquals("ID", headers[0])
        assertEquals("用例名称", headers[1])
        assertEquals("优先级", headers[2])
        
        assertEquals(1, rows.size)
        assertEquals("TC001", rows[0][0])
        assertEquals("登录测试", rows[0][1])
        assertEquals("P0", rows[0][2])
    }

    @Test
    fun `test fromExcelData`() {
        val headers = listOf("ID", "用例名称", "优先级", "状态", "测试步骤", "预期结果", "标签", "作者")
        val rows = listOf(
            listOf("TC001", "登录测试", "P0", "已发布", "1. 步骤1\n2. 步骤2", "预期结果", "登录, P0", "QA")
        )
        
        val model = TestCaseModel.fromExcelData(headers, rows)
        
        assertEquals(1, model.size())
        
        val testCase = model.findById("TC001")
        assertNotNull(testCase)
        assertEquals("登录测试", testCase?.name)
        assertEquals(Priority.P0, testCase?.priority)
        assertEquals(Status.PUBLISHED, testCase?.status)
        assertEquals(2, testCase?.steps?.size)
        assertEquals(2, testCase?.tags?.size)
    }

    @Test
    fun `test Priority fromValue`() {
        assertEquals(Priority.P0, Priority.fromValue("P0"))
        assertEquals(Priority.P1, Priority.fromValue("p1"))
        assertEquals(Priority.P2, Priority.fromValue("P2"))
        assertEquals(Priority.P3, Priority.fromValue("p3"))
        assertEquals(Priority.P2, Priority.fromValue("invalid"))
    }

    @Test
    fun `test Status fromValue`() {
        assertEquals(Status.DRAFT, Status.fromValue("草稿"))
        assertEquals(Status.PUBLISHED, Status.fromValue("已发布"))
        assertEquals(Status.ARCHIVED, Status.fromValue("已归档"))
        assertEquals(Status.DISABLED, Status.fromValue("已禁用"))
        assertEquals(Status.PUBLISHED, Status.fromValue("PUBLISHED"))
        assertEquals(Status.DRAFT, Status.fromValue("published"))
        assertEquals(Status.DRAFT, Status.fromValue("invalid"))
    }
}
