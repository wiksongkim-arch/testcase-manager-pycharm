package com.testcase.manager.model

/**
 * 测试用例优先级枚举
 *
 * 定义测试用例的优先级等级，从 P0（最高）到 P3（最低）。
 *
 * @property value 优先级的字符串表示
 * @property level 优先级的数字等级（0-3）
 */
enum class Priority(val value: String, val level: Int) {
    /** 最高优先级 - 阻塞性问题 */
    P0("P0", 0),

    /** 高优先级 - 重要功能 */
    P1("P1", 1),

    /** 中优先级 - 一般功能 */
    P2("P2", 2),

    /** 低优先级 - 次要功能 */
    P3("P3", 3);

    companion object {
        /**
         * 从字符串值获取优先级枚举
         *
         * @param value 优先级字符串（如 "P0", "P1"）
         * @return 对应的优先级枚举，如果未找到则返回 P2
         */
        fun fromValue(value: String): Priority {
            return values().find {
                it.value.equals(value, ignoreCase = true)
            } ?: P2
        }
    }
}

/**
 * 测试用例状态枚举
 *
 * 定义测试用例在其生命周期中的各种状态。
 *
 * @property value 状态的中文显示名称
 */
enum class Status(val value: String) {
    /** 草稿状态 - 正在编辑中 */
    DRAFT("草稿"),

    /** 已发布 - 可用于测试执行 */
    PUBLISHED("已发布"),

    /** 已归档 - 不再使用但保留记录 */
    ARCHIVED("已归档"),

    /** 已禁用 - 暂时不可用 */
    DISABLED("已禁用");

    companion object {
        /**
         * 从字符串值获取状态枚举
         *
         * @param value 状态字符串（中文名称或英文名称）
         * @return 对应的状态枚举，如果未找到则返回 DRAFT
         */
        fun fromValue(value: String): Status {
            return values().find {
                it.value == value || it.name.equals(value, ignoreCase = true)
            } ?: DRAFT
        }
    }
}

/**
 * 单个测试用例数据模型
 *
 * 表示一个完整的测试用例，包含所有相关属性和方法。
 *
 * @property id 测试用例唯一标识符
 * @property name 测试用例名称/标题
 * @property priority 优先级（默认为 P2）
 * @property status 状态（默认为草稿）
 * @property steps 测试步骤列表
 * @property expected 预期结果描述
 * @property tags 标签列表，用于分类和筛选
 * @property description 详细描述（可选）
 * @property preconditions 前置条件（可选）
 * @property author 作者名称（可选）
 * @property createdAt 创建时间 ISO 格式字符串（可选）
 * @property updatedAt 更新时间 ISO 格式字符串（可选）
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
     *
     * 将步骤列表格式化为带序号的字符串，每行一个步骤。
     *
     * @return 格式化的步骤字符串
     */
    fun getStepsAsString(): String {
        return steps.mapIndexed { index, step ->
            "${index + 1}. $step"
        }.joinToString("\n")
    }

    /**
     * 从字符串解析步骤（用于 Excel 编辑后转换）
     *
     * 解析带序号的步骤字符串，移除序号前缀并提取步骤内容。
     *
     * @param stepsString 步骤字符串（每行一个步骤，可带序号前缀）
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
     *
     * @return 逗号分隔的标签字符串
     */
    fun getTagsAsString(): String {
        return tags.joinToString(", ")
    }

    /**
     * 从字符串解析标签
     *
     * 解析逗号分隔的标签字符串，去除空白并过滤空值。
     *
     * @param tagsString 逗号分隔的标签字符串
     */
    fun setTagsFromString(tagsString: String) {
        tags = tagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

/**
 * 测试用例集合模型
 *
 * 对应整个 YAML 文件的内容，包含多个测试用例和元数据。
 *
 * @property testCases 测试用例列表
 * @property metadata 文件元数据
 */
data class TestCaseModel(
    var testCases: MutableList<TestCase> = mutableListOf(),
    var metadata: TestCaseMetadata = TestCaseMetadata()
) {
    /**
     * 添加测试用例到集合
     *
     * @param testCase 要添加的测试用例
     */
    fun addTestCase(testCase: TestCase) {
        testCases.add(testCase)
    }

    /**
     * 根据 ID 移除测试用例
     *
     * @param id 要移除的测试用例 ID
     * @return 是否成功移除
     */
    fun removeTestCase(id: String): Boolean {
        return testCases.removeIf { it.id == id }
    }

    /**
     * 根据 ID 查找测试用例
     *
     * @param id 测试用例 ID
     * @return 找到的测试用例，未找到返回 null
     */
    fun findById(id: String): TestCase? {
        return testCases.find { it.id == id }
    }

    /**
     * 获取测试用例数量
     *
     * @return 测试用例总数
     */
    fun size(): Int = testCases.size

    /**
     * 检查是否为空（无测试用例）
     *
     * @return 是否为空
     */
    fun isEmpty(): Boolean = testCases.isEmpty()

    /**
     * 转换为二维列表（用于 Excel 表格展示）
     *
     * @return Pair(表头列表, 数据行列表)
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
         * 解析 Excel 表格数据，根据表头映射创建测试用例对象。
         *
         * @param headers 表头列表
         * @param rows 数据行列表
         * @return 解析后的 TestCaseModel 实例
         */
        fun fromExcelData(
            headers: List<String>,
            rows: List<List<Any>>
        ): TestCaseModel {
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
                    if (index < row.size) {
                        testCase.expected = row[index].toString()
                    }
                }
                headerMap["标签"]?.let { index ->
                    if (index < row.size) {
                        testCase.setTagsFromString(row[index].toString())
                    }
                }
                headerMap["描述"]?.let { index ->
                    if (index < row.size) {
                        testCase.description = row[index].toString()
                    }
                }
                headerMap["前置条件"]?.let { index ->
                    if (index < row.size) {
                        testCase.preconditions = row[index].toString()
                    }
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
 * 存储测试用例文件的基本信息和版本控制数据。
 *
 * @property version 文件格式版本号
 * @property project 项目名称
 * @property createdAt 创建时间 ISO 格式字符串
 * @property updatedAt 最后更新时间 ISO 格式字符串
 */
data class TestCaseMetadata(
    var version: String = "1.0",
    var project: String = "",
    var createdAt: String? = null,
    var updatedAt: String? = null
)
