package com.testcase.manager.yaml

import com.intellij.openapi.diagnostic.Logger
import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseMetadata
import com.testcase.manager.model.TestCaseModel
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * YAML 序列化器
 *
 * 负责将 TestCaseModel 序列化为 YAML 字符串。
 * 支持多种输出格式：标准、紧凑、Pytest 格式。
 */
class YamlSerializer {

    /** 日志记录器 */
    private val logger = Logger.getInstance(YamlSerializer::class.java)

    /** YAML 序列化器实例 */
    private val yaml: Yaml

    init {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
            lineBreak = DumperOptions.LineBreak.UNIX
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
            width = 120
        }

        val representer = object : Representer() {
            init {
                // 添加自定义类型表示
                addClassTag(TestCaseModel::class.java, Tag.MAP)
            }
        }

        yaml = Yaml(representer, options)
    }

    /**
     * 将 TestCaseModel 序列化为 YAML 字符串
     *
     * @param model 测试用例模型
     * @param includeMetadata 是否包含元数据
     * @return YAML 字符串
     */
    fun serialize(model: TestCaseModel, includeMetadata: Boolean = true): String {
        return try {
            val data = mutableMapOf<String, Any>()

            // 添加元数据
            if (includeMetadata) {
                data["metadata"] = serializeMetadata(model.metadata)
            }

            // 添加测试用例列表
            data["test_cases"] = model.testCases.map { serializeTestCase(it) }

            yaml.dump(data)
        } catch (e: Exception) {
            logger.error("Failed to serialize model", e)
            throw YamlSerializeException("Failed to serialize: ${e.message}", e)
        }
    }

    /**
     * 序列化元数据
     *
     * @param metadata 元数据对象
     * @return 序列化后的 Map
     */
    private fun serializeMetadata(metadata: TestCaseMetadata): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        if (metadata.version.isNotBlank()) {
            map["version"] = metadata.version
        }
        if (metadata.project.isNotBlank()) {
            map["project"] = metadata.project
        }
        metadata.createdAt?.let { map["created_at"] = it }

        // 更新时间始终设置为当前时间
        map["updated_at"] = getCurrentTimestamp()

        return map
    }

    /**
     * 序列化单个测试用例为 YAML 字符串
     *
     * @param testCase 测试用例对象
     * @return YAML 字符串
     */
    fun serializeSingleTestCase(testCase: TestCase): String {
        return try {
            val data = serializeTestCase(testCase)
            yaml.dump(data)
        } catch (e: Exception) {
            logger.error("Failed to serialize single test case", e)
            throw YamlSerializeException("Failed to serialize test case: ${e.message}", e)
        }
    }

    /**
     * 序列化单个测试用例
     *
     * @param testCase 测试用例对象
     * @return 序列化后的 Map
     */
    private fun serializeTestCase(testCase: TestCase): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        // 必填字段
        map["id"] = testCase.id
        map["name"] = testCase.name

        // 枚举字段
        map["priority"] = testCase.priority.value
        map["status"] = testCase.status.value

        // 步骤列表
        if (testCase.steps.isNotEmpty()) {
            map["steps"] = testCase.steps
        }

        // 预期结果
        if (testCase.expected.isNotBlank()) {
            map["expected"] = testCase.expected
        }

        // 标签
        if (testCase.tags.isNotEmpty()) {
            map["tags"] = testCase.tags
        }

        // 可选字段
        if (testCase.description.isNotBlank()) {
            map["description"] = testCase.description
        }
        if (testCase.preconditions.isNotBlank()) {
            map["preconditions"] = testCase.preconditions
        }
        if (testCase.author.isNotBlank()) {
            map["author"] = testCase.author
        }
        testCase.createdAt?.let { map["created_at"] = it }
        testCase.updatedAt?.let { map["updated_at"] = it }

        return map
    }

    /**
     * 序列化为简化格式（不含空字段）
     *
     * @param model 测试用例模型
     * @return YAML 字符串
     */
    fun serializeCompact(model: TestCaseModel): String {
        return try {
            val data = mapOf(
                "test_cases" to model.testCases.map { tc ->
                    mapOf(
                        "id" to tc.id,
                        "name" to tc.name,
                        "priority" to tc.priority.value,
                        "status" to tc.status.value,
                        "steps" to tc.steps
                    ).filterValues { it !is String || it.isNotBlank() }
                }
            )

            yaml.dump(data)
        } catch (e: Exception) {
            logger.error("Failed to serialize compact model", e)
            throw YamlSerializeException("Failed to serialize: ${e.message}", e)
        }
    }

    /**
     * 序列化为标准 pytest 测试数据格式
     *
     * @param model 测试用例模型
     * @return YAML 字符串
     */
    fun serializeForPytest(model: TestCaseModel): String {
        return try {
            val sb = StringBuilder()

            // 添加文件头注释
            sb.appendLine("# Test Cases for pytest")
            sb.appendLine("# Generated at: ${getCurrentTimestamp()}")
            if (model.metadata.project.isNotBlank()) {
                sb.appendLine("# Project: ${model.metadata.project}")
            }
            sb.appendLine()

            // 序列化测试用例
            val data = mapOf(
                "test_cases" to model.testCases.map { serializeTestCase(it) }
            )
            sb.append(yaml.dump(data))

            sb.toString()
        } catch (e: Exception) {
            logger.error("Failed to serialize for pytest", e)
            throw YamlSerializeException("Failed to serialize: ${e.message}", e)
        }
    }

    /**
     * 更新现有 YAML 内容
     *
     * 保留原有格式和注释（简化实现，仅保留文件头注释）。
     *
     * @param existingContent 现有 YAML 内容
     * @param model 新的测试用例模型
     * @return 更新后的 YAML 字符串
     */
    fun update(existingContent: String, model: TestCaseModel): String {
        return try {
            // 尝试保留文件头注释
            val headerComments = extractHeaderComments(existingContent)

            val sb = StringBuilder()
            if (headerComments.isNotBlank()) {
                sb.appendLine(headerComments)
                sb.appendLine()
            }

            // 添加元数据
            val metadata = model.metadata.copy(
                updatedAt = getCurrentTimestamp()
            )
            val data = mutableMapOf<String, Any>()
            data["metadata"] = serializeMetadata(metadata)
            data["test_cases"] = model.testCases.map { serializeTestCase(it) }

            sb.append(yaml.dump(data))

            sb.toString()
        } catch (e: Exception) {
            logger.error("Failed to update YAML content", e)
            throw YamlSerializeException("Failed to update: ${e.message}", e)
        }
    }

    /**
     * 提取文件头注释
     *
     * @param content YAML 内容
     * @return 文件头注释字符串
     */
    private fun extractHeaderComments(content: String): String {
        val lines = content.lines()
        val comments = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                comments.add(line)
            } else if (trimmed.isBlank()) {
                continue
            } else {
                break
            }
        }

        return comments.joinToString("\n")
    }

    companion object {
        /** ISO 日期时间格式化器 */
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /**
         * 获取当前时间字符串
         *
         * @return ISO 格式的当前时间字符串
         */
        fun getCurrentTimestamp(): String {
            return LocalDateTime.now().format(DATE_FORMATTER)
        }

        /**
         * 生成示例 YAML 内容
         *
         * @return 示例 YAML 字符串
         */
        fun generateExample(): String {
            val model = TestCaseModel().apply {
                metadata = TestCaseMetadata(
                    version = "1.0",
                    project = "Example Project",
                    createdAt = getCurrentTimestamp()
                )

                addTestCase(
                    TestCase(
                        id = "TC001",
                        name = "登录成功",
                        priority = Priority.P0,
                        status = Status.PUBLISHED,
                        steps = listOf(
                            "打开登录页面",
                            "输入用户名和密码",
                            "点击登录按钮"
                        ),
                        expected = "登录成功，跳转到首页",
                        tags = listOf("登录", "正向场景"),
                        author = "QA Team"
                    )
                )

                addTestCase(
                    TestCase(
                        id = "TC002",
                        name = "登录失败-密码错误",
                        priority = Priority.P1,
                        status = Status.PUBLISHED,
                        steps = listOf(
                            "打开登录页面",
                            "输入用户名和错误密码",
                            "点击登录按钮"
                        ),
                        expected = "提示密码错误，停留在登录页",
                        tags = listOf("登录", "异常场景"),
                        author = "QA Team"
                    )
                )
            }

            return YamlSerializer().serializeForPytest(model)
        }
    }
}

/**
 * 序列化结果
 *
 * @property content 序列化后的内容
 * @property success 是否成功
 * @property errors 错误信息列表
 */
data class SerializationResult(
    val content: String,
    val success: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * YAML 序列化异常
 *
 * @param message 错误信息
 * @param cause 原始异常
 */
class YamlSerializeException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
