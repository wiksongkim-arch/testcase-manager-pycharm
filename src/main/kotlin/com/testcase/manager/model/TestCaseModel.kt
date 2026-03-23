package com.testcase.manager.model

/**
 * 测试用例优先级枚举
 */
enum class Priority(val value: String, val level: Int) {
    P0("P0", 0),
    P1("P1", 1),
    P2("P2", 2),
    P3("P3", 3);

    companion object {
        fun fromValue(value: String): Priority {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: P2
        }
    }
}

/**
 * 测试用例状态枚举
 */
enum class Status(val value: String) {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    ARCHIVED("已归档"),
    DISABLED("已禁用");

    companion object {
        fun fromValue(value: String): Status {
            return entries.find { 
                it.value == value || it.name.equals(value, ignoreCase = true) 
            } ?: DRAFT
        }
    }
}

/**
 * 单个测试用例数据模型
 * 
 * @property id 测试用例唯一标识
 * @property name 测试用例名称
 * @property priority 优先级
 * @property status 状态
 * @property steps 测试步骤列表
 * @property expected 预期结果
 * @property tags 标签列表（可选）
 * @property description 描述（可选）
 * @property preconditions 前置条件（可选）
 * @property author 作者（可选）
 * @property createdAt 创建时间（可选）
 * @property updatedAt 更新时间（可选）
 */
data class TestCase(
    var id: String = "",
    var name: String = "",
    var priority: Priority = Priority.P2,
    var status: Status = Status.DRAFT,
    var steps: List<String> = emptyList(),
    var expected: String = "",
    var tags: List<String> = emptyList(),
    var description: String = "",
    var preconditions: String = "",
    var author: String = "",
    var createdAt: String? = null,
    var updatedAt: String? = null
) {
    /**
     * 获取步骤的字符串表示（用于 Excel 显示）
     */
    fun getStepsAsString(): String {
        return steps.mapIndexed { index, step ->
            "${index + 1}. $step"
        }.joinToString("\n")
    }

    /**
     * 从字符串解析步骤（用于 Excel 编辑后转换）
     */
    fun setStepsFromString(stepsString: String) {
        steps = stepsString.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                // 移除开头的数字序号（如 "1. " 或 "1) "）
                line.replace(Regex("^\\d+[.)]\\s*"), "")
            }
    }

    /**
     * 获取标签的字符串表示
     */
    fun getTagsAsString(): String {
        return tags.joinToString(", ")
    }

    /**
     * 从字符串解析标签
     */
    fun setTagsFromString(tagsString: String) {
        tags = tagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

/**
 * 测试用例集合模型
 * 对应整个 YAML 文件的内容
 * 
 * @property testCases 测试用例列表
 * @property metadata 元数据（可选）
 */
data class TestCaseModel(
    var testCases: MutableList<TestCase> = mutableListOf(),
    var metadata: TestCaseMetadata = TestCaseMetadata()
) {
    /**
     * 添加测试用例
     */
    fun addTestCase(testCase: TestCase) {
        testCases.add(testCase)
    }

    /**
     * 移除测试用例
     */
    fun removeTestCase(id: String): Boolean {
        return testCases.removeIf { it.id == id }
    }

    /**
     * 根据 ID 查找测试用例
     */
    fun findById(id: String): TestCase? {
        return testCases.find { it.id == id }
    }

    /**
     * 获取测试用例数量
     */
    fun size(): Int = testCases.size

    /**
     * 检查是否为空
     */
    fun isEmpty(): Boolean = testCases.isEmpty()

    /**
     * 转换为二维列表（用于 Excel 表格展示）
     * 返回表头和数据行
     */
    fun toExcelData(): Pair<List<String>, List<List<Any>>> {
        val headers = listOf(
            "ID",
            "用例名称",
            "优先级",
            "状态",
            "测试步骤",
            "预期结果",
            "标签",
            "描述",
            "前置条件",
            "作者"
        )

        val rows = testCases.map { tc ->
            listOf(
                tc.id,
                tc.name,
                tc.priority.value,
                tc.status.value,
                tc.getStepsAsString(),
                tc.expected,
                tc.getTagsAsString(),
                tc.description,
                tc.preconditions,
                tc.author
            )
        }

        return headers to rows
    }

    companion object {
        /**
         * 从 Excel 数据创建 TestCaseModel
         * 
         * @param headers 表头列表
         * @param rows 数据行列表
         */
        fun fromExcelData(headers: List<String>, rows: List<List<Any>>): TestCaseModel {
            val model = TestCaseModel()
            
            // 创建表头到索引的映射
            val headerMap = headers.mapIndexed { index, header ->
                header.trim() to index
            }.toMap()

            rows.forEach { row ->
                val testCase = TestCase()
                
                headerMap["ID"]?.let { index ->
                    if (index < row.size) testCase.id = row[index].toString()
                }
                headerMap["用例名称"]?.let { index ->
                    if (index < row.size) testCase.name = row[index].toString()
                }
                headerMap["优先级"]?.let { index ->
                    if (index < row.size) {
                        testCase.priority = Priority.fromValue(row[index].toString())
                    }
                }
                headerMap["状态"]?.let { index ->
                    if (index < row.size) {
                        testCase.status = Status.fromValue(row[index].toString())
                    }
                }
                headerMap["测试步骤"]?.let { index ->
                    if (index < row.size) {
                        testCase.setStepsFromString(row[index].toString())
                    }
                }
                headerMap["预期结果"]?.let { index ->
                    if (index < row.size) testCase.expected = row[index].toString()
                }
                headerMap["标签"]?.let { index ->
                    if (index < row.size) {
                        testCase.setTagsFromString(row[index].toString())
                    }
                }
                headerMap["描述"]?.let { index ->
                    if (index < row.size) testCase.description = row[index].toString()
                }
                headerMap["前置条件"]?.let { index ->
                    if (index < row.size) testCase.preconditions = row[index].toString()
                }
                headerMap["作者"]?.let { index ->
                    if (index < row.size) testCase.author = row[index].toString()
                }

                model.addTestCase(testCase)
            }

            return model
        }
    }
}

/**
 * 测试用例元数据
 * 
 * @property version 版本号
 * @property project 项目名称
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
data class TestCaseMetadata(
    var version: String = "1.0",
    var project: String = "",
    var createdAt: String? = null,
    var updatedAt: String? = null
)
