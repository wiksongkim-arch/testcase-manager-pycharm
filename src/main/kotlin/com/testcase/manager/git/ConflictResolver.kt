package com.testcase.manager.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import com.testcase.manager.yaml.YamlParser
import com.testcase.manager.yaml.YamlSerializer
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.io.BufferedReader
import java.io.StringReader

/**
 * 冲突解决器
 *
 * 处理 Git 合并冲突，提供三方合并功能。
 * 支持自动合并和手动冲突解决。
 *
 * @property project IntelliJ 项目实例
 */
class ConflictResolver(private val project: Project) {

    /**
     * 冲突类型枚举
     */
    enum class ConflictType {
        /** 无冲突 */
        NONE,
        /** 内容冲突 - 同一行被修改 */
        CONTENT,
        /** 删除冲突 - 一方删除，一方修改 */
        DELETE_MODIFY,
        /** 添加冲突 - 双方添加相同ID */
        ADD_ADD,
        /** 重命名冲突 */
        RENAME
    }

    /**
     * 冲突信息数据类
     *
     * @property testCaseId 冲突的测试用例ID
     * @property conflictType 冲突类型
     * @property baseVersion 基础版本（共同祖先）
     * @property localVersion 本地版本（当前分支）
     * @property remoteVersion 远程版本（合并分支）
     * @property conflictFields 冲突的字段列表
     * @property isResolvable 是否可自动解决
     */
    data class ConflictInfo(
        val testCaseId: String,
        val conflictType: ConflictType,
        val baseVersion: TestCase?,
        val localVersion: TestCase?,
        val remoteVersion: TestCase?,
        val conflictFields: List<String>,
        val isResolvable: Boolean = false
    )

    /**
     * 合并结果数据类
     *
     * @property success 是否成功合并
     * @property conflicts 冲突列表（如果失败）
     * @property mergedModel 合并后的模型（如果成功）
     * @property mergeLog 合并日志
     */
    data class MergeResult(
        val success: Boolean,
        val conflicts: List<ConflictInfo> = emptyList(),
        val mergedModel: TestCaseModel? = null,
        val mergeLog: List<String> = emptyList()
    )

    /**
     * 解析冲突标记
     *
     * 解析包含 Git 冲突标记的 YAML 内容，提取三个版本。
     *
     * @param content 包含冲突标记的内容
     * @return Triple(基础版本, 本地版本, 远程版本)
     */
    fun parseConflictMarkers(content: String): Triple<String?, String?, String?> {
        val baseLines = mutableListOf<String>()
        val localLines = mutableListOf<String>()
        val remoteLines = mutableListOf<String>()

        var currentSection: MutableList<String>? = null
        var inConflict = false

        BufferedReader(StringReader(content)).useLines { lines ->
            lines.forEach { line ->
                when {
                    line.startsWith("<<<<<<<") -> {
                        inConflict = true
                        currentSection = localLines
                    }
                    line.startsWith("|||||||") -> {
                        currentSection = baseLines
                    }
                    line.startsWith("=======") -> {
                        currentSection = remoteLines
                    }
                    line.startsWith(">>>>>>>") -> {
                        inConflict = false
                        currentSection = null
                    }
                    else -> {
                        if (inConflict) {
                            currentSection?.add(line)
                        } else {
                            // 非冲突区域，添加到所有版本
                            baseLines.add(line)
                            localLines.add(line)
                            remoteLines.add(line)
                        }
                    }
                }
            }
        }

        return Triple(
            if (baseLines.isNotEmpty()) baseLines.joinToString("\n") else null,
            if (localLines.isNotEmpty()) localLines.joinToString("\n") else null,
            if (remoteLines.isNotEmpty()) remoteLines.joinToString("\n") else null
        )
    }

    /**
     * 检测文件是否有冲突
     *
     * @param file 要检查的文件
     * @return 是否有冲突
     */
    fun hasConflicts(file: VirtualFile): Boolean {
        val content = String(file.contentsToByteArray())
        return content.contains("<<<<<<<") && content.contains(">>>>>>>")
    }

    /**
     * 获取文件的 Git 仓库
     *
     * @param file 文件
     * @return Git 仓库实例，如果不在仓库中则返回 null
     */
    fun getGitRepository(file: VirtualFile): GitRepository? {
        return GitUtil.getRepositoryManager(project).getRepositoryForFile(file)
    }

    /**
     * 执行三方合并
     *
     * @param baseModel 基础版本（共同祖先）
     * @param localModel 本地版本（当前分支）
     * @param remoteModel 远程版本（合并分支）
     * @param strategy 合并策略
     * @return 合并结果
     */
    fun threeWayMerge(
        baseModel: TestCaseModel?,
        localModel: TestCaseModel,
        remoteModel: TestCaseModel,
        strategy: MergeStrategy = MergeStrategy.AUTO
    ): MergeResult {
        val conflicts = mutableListOf<ConflictInfo>()
        val mergedModel = TestCaseModel()
        val mergeLog = mutableListOf<String>()

        // 收集所有测试用例ID
        val allIds = mutableSetOf<String>()
        baseModel?.testCases?.forEach { allIds.add(it.id) }
        localModel.testCases.forEach { allIds.add(it.id) }
        remoteModel.testCases.forEach { allIds.add(it.id) }

        allIds.forEach { id ->
            val baseTC = baseModel?.findById(id)
            val localTC = localModel.findById(id)
            val remoteTC = remoteModel.findById(id)

            when {
                // 情况1: 三方都存在
                baseTC != null && localTC != null && remoteTC != null -> {
                    val result = mergeTestCase(baseTC, localTC, remoteTC, strategy)
                    if (result.hasConflict) {
                        conflicts.add(result.conflictInfo!!)
                        mergeLog.add("冲突: 测试用例 $id 存在字段冲突")
                    } else {
                        mergedModel.addTestCase(result.testCase!!)
                        mergeLog.add("合并: 测试用例 $id 成功合并")
                    }
                }

                // 情况2: 基础不存在，本地和远程都存在（添加冲突）
                baseTC == null && localTC != null && remoteTC != null -> {
                    if (localTC == remoteTC) {
                        // 内容相同，无冲突
                        mergedModel.addTestCase(localTC)
                        mergeLog.add("合并: 测试用例 $id 双方添加且内容相同")
                    } else {
                        val conflict = ConflictInfo(
                            testCaseId = id,
                            conflictType = ConflictType.ADD_ADD,
                            baseVersion = null,
                            localVersion = localTC,
                            remoteVersion = remoteTC,
                            conflictFields = findDifferentFields(localTC, remoteTC),
                            isResolvable = false
                        )
                        conflicts.add(conflict)
                        mergeLog.add("冲突: 测试用例 $id 双方添加但内容不同")
                    }
                }

                // 情况3: 基础存在，本地不存在，远程存在（删除-修改冲突）
                baseTC != null && localTC == null && remoteTC != null -> {
                    if (baseTC == remoteTC) {
                        // 本地删除，远程未修改，接受删除
                        mergeLog.add("接受: 测试用例 $id 在本地删除，远程未修改")
                    } else {
                        val conflict = ConflictInfo(
                            testCaseId = id,
                            conflictType = ConflictType.DELETE_MODIFY,
                            baseVersion = baseTC,
                            localVersion = null,
                            remoteVersion = remoteTC,
                            conflictFields = listOf("deleted"),
                            isResolvable = false
                        )
                        conflicts.add(conflict)
                        mergeLog.add("冲突: 测试用例 $id 本地删除但远程修改")
                    }
                }

                // 情况4: 基础存在，本地存在，远程不存在（修改-删除冲突）
                baseTC != null && localTC != null && remoteTC == null -> {
                    if (baseTC == localTC) {
                        // 远程删除，本地未修改，接受删除
                        mergeLog.add("接受: 测试用例 $id 在远程删除，本地未修改")
                    } else {
                        val conflict = ConflictInfo(
                            testCaseId = id,
                            conflictType = ConflictType.DELETE_MODIFY,
                            baseVersion = baseTC,
                            localVersion = localTC,
                            remoteVersion = null,
                            conflictFields = listOf("deleted"),
                            isResolvable = false
                        )
                        conflicts.add(conflict)
                        mergeLog.add("冲突: 测试用例 $id 远程删除但本地修改")
                    }
                }

                // 情况5: 只有本地存在（新增）
                baseTC == null && localTC != null && remoteTC == null -> {
                    mergedModel.addTestCase(localTC)
                    mergeLog.add("添加: 测试用例 $id 来自本地")
                }

                // 情况6: 只有远程存在（新增）
                baseTC == null && localTC == null && remoteTC != null -> {
                    mergedModel.addTestCase(remoteTC)
                    mergeLog.add("添加: 测试用例 $id 来自远程")
                }

                // 情况7: 三方都不存在（已删除）
                baseTC != null && localTC == null && remoteTC == null -> {
                    mergeLog.add("删除: 测试用例 $id 已在双方删除")
                }
            }
        }

        // 复制元数据（使用本地版本）
        mergedModel.metadata = localModel.metadata

        return MergeResult(
            success = conflicts.isEmpty(),
            conflicts = conflicts,
            mergedModel = if (conflicts.isEmpty()) mergedModel else null,
            mergeLog = mergeLog
        )
    }

    /**
     * 合并单个测试用例
     *
     * @param base 基础版本
     * @param local 本地版本
     * @param remote 远程版本
     * @param strategy 合并策略
     * @return 合并结果
     */
    private fun mergeTestCase(
        base: TestCase,
        local: TestCase,
        remote: TestCase,
        strategy: MergeStrategy
    ): TestCaseMergeResult {
        val conflictFields = mutableListOf<String>()
        val merged = TestCase(id = base.id)

        // 合并各个字段
        merged.name = mergeField(base.name, local.name, remote.name, "name", strategy, conflictFields)
        merged.priority = mergeField(base.priority, local.priority, remote.priority, "priority", strategy, conflictFields)
        merged.status = mergeField(base.status, local.status, remote.status, "status", strategy, conflictFields)
        merged.expected = mergeField(base.expected, local.expected, remote.expected, "expected", strategy, conflictFields)
        merged.description = mergeField(base.description, local.description, remote.description, "description", strategy, conflictFields)
        merged.preconditions = mergeField(base.preconditions, local.preconditions, remote.preconditions, "preconditions", strategy, conflictFields)
        merged.author = mergeField(base.author, local.author, remote.author, "author", strategy, conflictFields)

        // 合并步骤列表
        merged.steps = mergeListField(base.steps, local.steps, remote.steps, "steps", strategy, conflictFields)
        merged.tags = mergeListField(base.tags, local.tags, remote.tags, "tags", strategy, conflictFields)

        return if (conflictFields.isEmpty()) {
            TestCaseMergeResult(false, null, merged)
        } else {
            TestCaseMergeResult(true, ConflictInfo(
                testCaseId = base.id,
                conflictType = ConflictType.CONTENT,
                baseVersion = base,
                localVersion = local,
                remoteVersion = remote,
                conflictFields = conflictFields,
                isResolvable = strategy == MergeStrategy.AUTO
            ), null)
        }
    }

    /**
     * 合并单个字段
     */
    private fun <T> mergeField(
        base: T,
        local: T,
        remote: T,
        fieldName: String,
        strategy: MergeStrategy,
        conflictFields: MutableList<String>
    ): T {
        return when {
            // 本地和远程相同
            local == remote -> local
            // 本地未修改，使用远程
            local == base -> remote
            // 远程未修改，使用本地
            remote == base -> local
            // 三方都不同，冲突
            else -> {
                conflictFields.add(fieldName)
                when (strategy) {
                    MergeStrategy.LOCAL -> local
                    MergeStrategy.REMOTE -> remote
                    else -> local // AUTO 时返回本地，但标记冲突
                }
            }
        }
    }

    /**
     * 合并列表字段
     */
    private fun <T> mergeListField(
        base: List<T>,
        local: List<T>,
        remote: List<T>,
        fieldName: String,
        strategy: MergeStrategy,
        conflictFields: MutableList<String>
    ): List<T> {
        return when {
            local == remote -> local
            local == base -> remote
            remote == base -> local
            else -> {
                conflictFields.add(fieldName)
                when (strategy) {
                    MergeStrategy.LOCAL -> local
                    MergeStrategy.REMOTE -> remote
                    else -> local
                }
            }
        }
    }

    /**
     * 查找两个测试用例的不同字段
     */
    private fun findDifferentFields(tc1: TestCase, tc2: TestCase): List<String> {
        val differences = mutableListOf<String>()

        if (tc1.name != tc2.name) differences.add("name")
        if (tc1.priority != tc2.priority) differences.add("priority")
        if (tc1.status != tc2.status) differences.add("status")
        if (tc1.steps != tc2.steps) differences.add("steps")
        if (tc1.expected != tc2.expected) differences.add("expected")
        if (tc1.tags != tc2.tags) differences.add("tags")
        if (tc1.description != tc2.description) differences.add("description")
        if (tc1.preconditions != tc2.preconditions) differences.add("preconditions")
        if (tc1.author != tc2.author) differences.add("author")

        return differences
    }

    /**
     * 测试用例合并结果
     */
    private data class TestCaseMergeResult(
        val hasConflict: Boolean,
        val conflictInfo: ConflictInfo?,
        val testCase: TestCase?
    )

    /**
     * 合并策略枚举
     */
    enum class MergeStrategy {
        /** 自动合并，标记冲突 */
        AUTO,
        /** 优先使用本地版本 */
        LOCAL,
        /** 优先使用远程版本 */
        REMOTE,
        /** 手动解决 */
        MANUAL
    }

    /**
     * 生成冲突标记内容
     *
     * 用于将冲突信息序列化为带 Git 冲突标记的 YAML。
     *
     * @param conflict 冲突信息
     * @return 带冲突标记的 YAML 内容
     */
    fun generateConflictMarkers(conflict: ConflictInfo): String {
        val sb = StringBuilder()

        sb.appendLine("<<<<<<< HEAD")
        if (conflict.localVersion != null) {
            sb.appendLine(YamlSerializer.serializeSingleTestCase(conflict.localVersion))
        } else {
            sb.appendLine("# 本地版本已删除")
        }

        if (conflict.baseVersion != null) {
            sb.appendLine("||||||| BASE")
            sb.appendLine(YamlSerializer.serializeSingleTestCase(conflict.baseVersion))
        }

        sb.appendLine("=======")
        if (conflict.remoteVersion != null) {
            sb.appendLine(YamlSerializer.serializeSingleTestCase(conflict.remoteVersion))
        } else {
            sb.appendLine("# 远程版本已删除")
        }
        sb.appendLine(">>>>>>> REMOTE")

        return sb.toString()
    }

    /**
     * 应用冲突解决
     *
     * 根据用户选择解决单个冲突。
     *
     * @param conflict 冲突信息
     * @param resolution 解决方式
     * @return 解决后的测试用例
     */
    fun resolveConflict(
        conflict: ConflictInfo,
        resolution: ConflictResolution
    ): TestCase {
        return when (resolution) {
            ConflictResolution.ACCEPT_LOCAL -> conflict.localVersion ?: TestCase(id = conflict.testCaseId)
            ConflictResolution.ACCEPT_REMOTE -> conflict.remoteVersion ?: TestCase(id = conflict.testCaseId)
            ConflictResolution.ACCEPT_BASE -> conflict.baseVersion ?: TestCase(id = conflict.testCaseId)
            ConflictResolution.MERGE_MANUAL -> {
                // 手动合并，返回一个合并后的版本（需要用户后续编辑）
                mergeManually(conflict)
            }
        }
    }

    /**
     * 手动合并
     */
    private fun mergeManually(conflict: ConflictInfo): TestCase {
        // 创建一个基础合并版本，标记需要手动处理的字段
        val merged = TestCase(id = conflict.testCaseId)

        // 对于非冲突字段，使用本地版本
        conflict.localVersion?.let { local ->
            merged.name = local.name
            merged.priority = local.priority
            merged.status = local.status
            merged.expected = local.expected
            merged.steps = local.steps
            merged.tags = local.tags
            merged.description = local.description
            merged.preconditions = local.preconditions
            merged.author = local.author
        }

        return merged
    }

    /**
     * 冲突解决方式枚举
     */
    enum class ConflictResolution {
        /** 接受本地版本 */
        ACCEPT_LOCAL,
        /** 接受远程版本 */
        ACCEPT_REMOTE,
        /** 接受基础版本 */
        ACCEPT_BASE,
        /** 手动合并 */
        MERGE_MANUAL
    }
}
