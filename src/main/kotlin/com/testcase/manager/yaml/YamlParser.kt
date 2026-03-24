package com.testcase.manager.yaml

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.testcase.manager.model.Priority
import com.testcase.manager.model.Status
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseMetadata
import com.testcase.manager.model.TestCaseModel
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.representer.Representer
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * YAML 解析器
 *
 * 负责将 YAML 文件内容解析为 TestCaseModel 对象。
 * 支持从 VirtualFile 或字符串解析，提供验证功能。
 */
class YamlParser {

    /** 日志记录器 */
    private val logger = Logger.getInstance(YamlParser::class.java)

    /** YAML 解析器实例 */
    private val yaml: Yaml

    init {
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
        }

        val constructor = Constructor(Map::class.java)
        yaml = Yaml(constructor, Representer(), options)
    }

    /**
     * 从 VirtualFile 解析 YAML 文件
     *
     * @param file 虚拟文件对象
     * @return 解析后的 TestCaseModel
     * @throws YamlParseException 解析失败时抛出
     */
    fun parse(file: VirtualFile): TestCaseModel {
        return try {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            parse(content)
        } catch (e: Exception) {
            logger.error("Failed to read file: ${file.path}", e)
            throw YamlParseException("Failed to read file: ${e.message}", e)
        }
    }

    /**
     * 从字符串解析 YAML 内容
     *
     * @param content YAML 字符串内容
     * @return 解析后的 TestCaseModel
     * @throws YamlParseException 解析失败时抛出
     */
    fun parse(content: String): TestCaseModel {
        return try {
            val data = yaml.load<Map<String, Any>>(StringReader(content))
            parseFromMap(data ?: emptyMap())
        } catch (e: YAMLException) {
            logger.error("YAML syntax error", e)
            throw YamlParseException("Failed to parse YAML: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error parsing YAML", e)
            throw YamlParseException("Failed to parse YAML: ${e.message}", e)
        }
    }

    /**
     * 从 Map 解析 TestCaseModel
     *
     * @param data YAML 解析后的 Map 数据
     * @return 解析后的 TestCaseModel
     */
    private fun parseFromMap(data: Map<String, Any?>): TestCaseModel {
        val model = TestCaseModel()

        // 解析元数据
        try {
            (data["metadata"] as? Map<*, *>)?.let { metadataMap ->
                model.metadata = parseMetadata(metadataMap)
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse metadata: ${e.message}")
        }

        // 解析测试用例列表
        (data["test_cases"] as? List<*>)?.let { cases ->
            cases.forEachIndexed { index, caseData ->
                if (caseData is Map<*, *>) {
                    try {
                        val testCase = parseTestCase(caseData)
                        model.addTestCase(testCase)
                    } catch (e: Exception) {
                        // 记录错误但继续解析其他用例
                        logger.warn(
                            "Failed to parse test case at index $index: ${e.message}"
                        )
                    }
                } else {
                    logger.warn("Test case at index $index is not a valid map")
                }
            }
        }

        return model
    }

    /**
     * 解析元数据
     *
     * @param metadataMap 元数据 Map
     * @return 解析后的 TestCaseMetadata
     */
    private fun parseMetadata(metadataMap: Map<*, *>): TestCaseMetadata {
        return try {
            TestCaseMetadata(
                version = metadataMap["version"]?.toString() ?: "1.0",
                project = metadataMap["project"]?.toString() ?: "",
                createdAt = metadataMap["created_at"]?.toString(),
                updatedAt = metadataMap["updated_at"]?.toString()
            )
        } catch (e: Exception) {
            logger.warn("Error parsing metadata: ${e.message}")
            TestCaseMetadata()
        }
    }

    /**
     * 解析单个测试用例
     *
     * @param caseData 测试用例 Map 数据
     * @return 解析后的 TestCase
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseTestCase(caseData: Map<*, *>): TestCase {
        val caseMap = caseData as Map<String, Any?>

        return try {
            TestCase(
                id = caseMap["id"]?.toString() ?: "",
                name = caseMap["name"]?.toString() ?: "",
                priority = parsePriority(caseMap["priority"]),
                status = parseStatus(caseMap["status"]),
                steps = parseSteps(caseMap["steps"]),
                expected = caseMap["expected"]?.toString() ?: "",
                tags = parseTags(caseMap["tags"]),
                description = caseMap["description"]?.toString() ?: "",
                preconditions = caseMap["preconditions"]?.toString()
                    ?: caseMap["precondition"]?.toString() ?: "",
                author = caseMap["author"]?.toString() ?: "",
                createdAt = caseMap["created_at"]?.toString(),
                updatedAt = caseMap["updated_at"]?.toString()
            )
        } catch (e: Exception) {
            logger.error("Error parsing test case: ${e.message}")
            throw e
        }
    }

    /**
     * 解析优先级
     *
     * @param value 优先级值（字符串或数字）
     * @return 解析后的 Priority 枚举
     */
    private fun parsePriority(value: Any?): Priority {
        return when (value) {
            is String -> Priority.fromValue(value)
            is Int -> when (value) {
                0 -> Priority.P0
                1 -> Priority.P1
                2 -> Priority.P2
                3 -> Priority.P3
                else -> {
                    logger.warn("Invalid priority value: $value, using default P2")
                    Priority.P2
                }
            }
            else -> {
                if (value != null) {
                    logger.warn("Unknown priority type: ${value::class.simpleName}")
                }
                Priority.P2
            }
        }
    }

    /**
     * 解析状态
     *
     * @param value 状态值
     * @return 解析后的 Status 枚举
     */
    private fun parseStatus(value: Any?): Status {
        return when (value) {
            is String -> Status.fromValue(value)
            else -> {
                if (value != null) {
                    logger.warn("Unknown status type: ${value::class.simpleName}")
                }
                Status.DRAFT
            }
        }
    }

    /**
     * 解析测试步骤
     *
     * @param value 步骤数据（列表或字符串）
     * @return 步骤字符串列表
     */
    private fun parseSteps(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> listOf(value)
            else -> emptyList()
        }
    }

    /**
     * 解析标签
     *
     * @param value 标签数据（列表或逗号分隔字符串）
     * @return 标签字符串列表
     */
    private fun parseTags(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    /**
     * 验证 YAML 格式是否正确
     *
     * @param content YAML 字符串
     * @return 验证结果对象
     */
    fun validate(content: String): ValidationResult {
        return try {
            val data = yaml.load<Map<String, Any>>(StringReader(content))
            validateStructure(data ?: emptyMap())
        } catch (e: YAMLException) {
            logger.error("YAML validation failed: ${e.message}")
            ValidationResult(false, listOf("YAML syntax error: ${e.message}"))
        } catch (e: Exception) {
            logger.error("Validation error: ${e.message}")
            ValidationResult(false, listOf("Validation error: ${e.message}"))
        }
    }

    /**
     * 验证数据结构
     *
     * @param data 解析后的 YAML 数据
     * @return 验证结果
     */
    private fun validateStructure(data: Map<String, Any?>): ValidationResult {
        val errors = mutableListOf<String>()

        // 检查 test_cases 字段
        if (!data.containsKey("test_cases")) {
            errors.add("Missing required field: 'test_cases'")
        } else {
            val testCases = data["test_cases"]
            if (testCases !is List<*>) {
                errors.add("'test_cases' must be a list")
            } else {
                // 验证每个测试用例
                testCases.forEachIndexed { index, case ->
                    if (case !is Map<*, *>) {
                        errors.add("Test case at index $index must be a map")
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        validateTestCase(
                            case as Map<String, Any?>,
                            index,
                            errors
                        )
                    }
                }
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * 验证单个测试用例
     *
     * @param case 测试用例数据
     * @param index 测试用例索引
     * @param errors 错误列表（会追加到此列表）
     */
    private fun validateTestCase(
        case: Map<String, Any?>,
        index: Int,
        errors: MutableList<String>
    ) {
        // 检查必填字段
        if (case["id"] == null || case["id"].toString().isBlank()) {
            errors.add("Test case at index $index: 'id' is required")
        }
        if (case["name"] == null || case["name"].toString().isBlank()) {
            errors.add("Test case at index $index: 'name' is required")
        }

        // 验证 priority 值
        case["priority"]?.let { priority ->
            val priorityStr = priority.toString()
            val validPriorities = listOf("P0", "P1", "P2", "P3")
            if (!validPriorities.contains(priorityStr)) {
                errors.add(
                    "Test case at index $index: invalid priority '$priorityStr'"
                )
            }
        }

        // 验证 status 值
        case["status"]?.let { status ->
            val validStatuses = listOf(
                "草稿", "已发布", "已归档", "已禁用",
                "DRAFT", "PUBLISHED", "ARCHIVED", "DISABLED"
            )
            if (!validStatuses.contains(status.toString())) {
                errors.add(
                    "Test case at index $index: invalid status '${status}'"
                )
            }
        }
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
    }
}

/**
 * 验证结果
 *
 * @property isValid 是否验证通过
 * @property errors 错误信息列表
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * YAML 解析异常
 *
 * @param message 错误信息
 * @param cause 原始异常
 */
class YamlParseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
