package com.testcase.manager.sync

import com.intellij.openapi.vfs.VirtualFile
import com.testcase.manager.model.TestCaseModel
import com.testcase.manager.yaml.YamlParseException
import com.testcase.manager.yaml.YamlParser
import com.testcase.manager.yaml.YamlSerializer
import java.io.IOException

/**
 * 双向同步管理器
 * 负责 YAML 和 Excel 数据之间的双向同步
 */
class BidirectionalSyncManager {

    private val parser = YamlParser()
    private val serializer = YamlSerializer()

    /**
     * 同步方向
     */
    enum class SyncDirection {
        YAML_TO_EXCEL,  // YAML -> Excel
        EXCEL_TO_YAML   // Excel -> YAML
    }

    /**
     * 同步结果
     */
    data class SyncResult(
        val success: Boolean,
        val direction: SyncDirection,
        val message: String,
        val model: TestCaseModel? = null,
        val errors: List<String> = emptyList()
    )

    /**
     * 从 YAML 文件同步到 Excel 数据
     * 
     * @param file YAML 文件
     * @return 同步结果，包含 Excel 格式的数据
     */
    fun syncFromYamlToExcel(file: VirtualFile): SyncResult {
        return try {
            val model = parser.parse(file)
            val (headers, rows) = model.toExcelData()
            
            SyncResult(
                success = true,
                direction = SyncDirection.YAML_TO_EXCEL,
                message = "Successfully loaded ${model.size()} test cases",
                model = model
            )
        } catch (e: YamlParseException) {
            SyncResult(
                success = false,
                direction = SyncDirection.YAML_TO_EXCEL,
                message = "Failed to parse YAML: ${e.message}",
                errors = listOf(e.message ?: "Unknown error")
            )
        } catch (e: IOException) {
            SyncResult(
                success = false,
                direction = SyncDirection.YAML_TO_EXCEL,
                message = "Failed to read file: ${e.message}",
                errors = listOf(e.message ?: "IO error")
            )
        }
    }

    /**
     * 从 YAML 字符串同步到 Excel 数据
     * 
     * @param content YAML 内容
     * @return 同步结果
     */
    fun syncFromYamlToExcel(content: String): SyncResult {
        return try {
            val model = parser.parse(content)
            
            SyncResult(
                success = true,
                direction = SyncDirection.YAML_TO_EXCEL,
                message = "Successfully parsed ${model.size()} test cases",
                model = model
            )
        } catch (e: YamlParseException) {
            SyncResult(
                success = false,
                direction = SyncDirection.YAML_TO_EXCEL,
                message = "Failed to parse YAML: ${e.message}",
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    /**
     * 从 Excel 数据同步到 YAML
     * 
     * @param headers Excel 表头
     * @param rows Excel 数据行
     * @param existingContent 现有 YAML 内容（用于保留注释）
     * @return 同步结果，包含 YAML 字符串
     */
    fun syncFromExcelToYaml(
        headers: List<String>,
        rows: List<List<Any>>,
        existingContent: String? = null
    ): SyncResult {
        return try {
            val model = TestCaseModel.fromExcelData(headers, rows)
            
            // 验证数据
            val validationErrors = validateModel(model)
            if (validationErrors.isNotEmpty()) {
                return SyncResult(
                    success = false,
                    direction = SyncDirection.EXCEL_TO_YAML,
                    message = "Validation failed",
                    model = model,
                    errors = validationErrors
                )
            }

            // 序列化为 YAML
            val yamlContent = if (existingContent != null) {
                serializer.update(existingContent, model)
            } else {
                serializer.serialize(model)
            }

            SyncResult(
                success = true,
                direction = SyncDirection.EXCEL_TO_YAML,
                message = "Successfully converted to YAML",
                model = model
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                direction = SyncDirection.EXCEL_TO_YAML,
                message = "Failed to convert: ${e.message}",
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }

    /**
     * 验证模型数据
     */
    private fun validateModel(model: TestCaseModel): List<String> {
        val errors = mutableListOf<String>()

        if (model.isEmpty()) {
            errors.add("No test cases found")
            return errors
        }

        val ids = mutableSetOf<String>()
        model.testCases.forEachIndexed { index, testCase ->
            // 检查必填字段
            if (testCase.id.isBlank()) {
                errors.add("Row ${index + 1}: ID is required")
            } else {
                // 检查 ID 唯一性
                if (ids.contains(testCase.id)) {
                    errors.add("Row ${index + 1}: Duplicate ID '${testCase.id}'")
                }
                ids.add(testCase.id)
            }

            if (testCase.name.isBlank()) {
                errors.add("Row ${index + 1}: Name is required")
            }
        }

        return errors
    }

    /**
     * 将模型转换为 YAML 字符串
     * 
     * @param model 测试用例模型
     * @param format 输出格式
     * @return YAML 字符串
     */
    fun convertToYaml(
        model: TestCaseModel,
        format: YamlOutputFormat = YamlOutputFormat.STANDARD
    ): String {
        return when (format) {
            YamlOutputFormat.STANDARD -> serializer.serialize(model)
            YamlOutputFormat.COMPACT -> serializer.serializeCompact(model)
            YamlOutputFormat.PYTEST -> serializer.serializeForPytest(model)
        }
    }

    /**
     * 将 YAML 转换为 Excel 数据
     * 
     * @param yamlContent YAML 内容
     * @return Pair(表头, 数据行)
     */
    fun convertToExcelData(yamlContent: String): Pair<List<String>, List<List<Any>>>? {
        return try {
            val model = parser.parse(yamlContent)
            model.toExcelData()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查 YAML 是否有语法错误
     * 
     * @param content YAML 内容
     * @return 是否有错误
     */
    fun hasErrors(content: String): Boolean {
        val result = parser.validate(content)
        return !result.isValid
    }

    /**
     * 获取 YAML 验证错误
     * 
     * @param content YAML 内容
     * @return 错误列表
     */
    fun getValidationErrors(content: String): List<String> {
        val result = parser.validate(content)
        return result.errors
    }

    /**
     * 生成示例 YAML
     */
    fun generateExampleYaml(): String {
        return YamlSerializer.generateExample()
    }
}

/**
 * YAML 输出格式
 */
enum class YamlOutputFormat {
    STANDARD,   // 标准格式
    COMPACT,    // 紧凑格式
    PYTEST      // Pytest 测试数据格式
}
