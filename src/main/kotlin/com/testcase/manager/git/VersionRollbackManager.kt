package com.testcase.manager.git

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import com.testcase.manager.yaml.YamlParser
import com.testcase.manager.yaml.YamlSerializer

/**
 * 版本回滚管理器
 *
 * 提供文件版本回滚功能，支持回滚到指定提交版本，
 * 并处理回滚后的冲突检测。
 *
 * @property project IntelliJ 项目实例
 */
class VersionRollbackManager(private val project: Project) {

    private val git: Git = Git.getInstance()

    /**
     * 回滚结果数据类
     *
     * @property success 是否成功
     * @property message 结果消息
     * @property hasConflicts 是否有冲突
     * @property backupCreated 是否创建了备份
     * @property backupPath 备份文件路径
     */
    data class RollbackResult(
        val success: Boolean,
        val message: String,
        val hasConflicts: Boolean = false,
        val backupCreated: Boolean = false,
        val backupPath: String? = null
    )

    /**
     * 回滚选项数据类
     *
     * @property createBackup 是否创建备份
     * @property backupSuffix 备份文件后缀
     * @property confirmOverwrite 是否确认覆盖
     * @property stashChanges 是否暂存当前更改
     */
    data class RollbackOptions(
        val createBackup: Boolean = true,
        val backupSuffix: String = ".backup",
        val confirmOverwrite: Boolean = true,
        val stashChanges: Boolean = false
    )

    /**
     * 回滚到指定提交
     *
     * @param file 要回滚的文件
     * @param commitHash 目标提交哈希
     * @param options 回滚选项
     * @return 回滚结果
     */
    fun rollbackToCommit(
        file: VirtualFile,
        commitHash: String,
        options: RollbackOptions = RollbackOptions()
    ): RollbackResult {
        val repository = getGitRepository(file) ?: return RollbackResult(
            success = false,
            message = "文件不在 Git 仓库中"
        )

        // 检查文件是否有未保存的更改
        if (hasUnsavedChanges(file)) {
            return RollbackResult(
                success = false,
                message = "文件有未保存的更改，请先保存"
            )
        }

        // 创建备份
        var backupPath: String? = null
        if (options.createBackup) {
            backupPath = createBackup(file, options.backupSuffix)
            if (backupPath == null) {
                return RollbackResult(
                    success = false,
                    message = "创建备份失败"
                )
            }
        }

        // 获取目标版本内容
        val targetContent = getFileContentAtCommit(repository, file, commitHash)
            ?: return RollbackResult(
                success = false,
                message = "无法获取版本 $commitHash 的内容",
                backupCreated = options.createBackup,
                backupPath = backupPath
            )

        // 验证内容格式
        if (!validateYamlContent(targetContent)) {
            return RollbackResult(
                success = false,
                message = "目标版本的 YAML 格式无效",
                backupCreated = options.createBackup,
                backupPath = backupPath
            )
        }

        // 执行回滚
        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                file.setBinaryContent(targetContent.toByteArray())
            }

            RollbackResult(
                success = true,
                message = "已成功回滚到版本 ${commitHash.take(7)}",
                backupCreated = options.createBackup,
                backupPath = backupPath
            )
        } catch (e: Exception) {
            RollbackResult(
                success = false,
                message = "回滚失败: ${e.message}",
                backupCreated = options.createBackup,
                backupPath = backupPath
            )
        }
    }

    /**
     * 使用 Git checkout 回滚文件
     *
     * @param file 要回滚的文件
     * @param commitHash 目标提交哈希
     * @param options 回滚选项
     * @return 回滚结果
     */
    fun checkoutFileVersion(
        file: VirtualFile,
        commitHash: String,
        options: RollbackOptions = RollbackOptions()
    ): RollbackResult {
        val repository = getGitRepository(file) ?: return RollbackResult(
            success = false,
            message = "文件不在 Git 仓库中"
        )

        // 创建备份
        var backupPath: String? = null
        if (options.createBackup) {
            backupPath = createBackup(file, options.backupSuffix)
        }

        // 执行 git checkout
        val handler = GitLineHandler(project, repository.root, GitCommand.CHECKOUT)
        handler.addParameters(commitHash, "--", getRelativePath(repository, file))

        val result = git.runCommand(handler)

        return if (result.success()) {
            // 刷新文件
            file.refresh(false, false)

            RollbackResult(
                success = true,
                message = "已成功检出版本 ${commitHash.take(7)}",
                backupCreated = options.createBackup,
                backupPath = backupPath
            )
        } else {
            RollbackResult(
                success = false,
                message = "检出失败: ${result.errorOutputAsJoinedString}",
                backupCreated = options.createBackup,
                backupPath = backupPath
            )
        }
    }

    /**
     * 回滚到上一个版本
     *
     * @param file 要回滚的文件
     * @param options 回滚选项
     * @return 回滚结果
     */
    fun rollbackToPrevious(
        file: VirtualFile,
        options: RollbackOptions = RollbackOptions()
    ): RollbackResult {
        val repository = getGitRepository(file) ?: return RollbackResult(
            success = false,
            message = "文件不在 Git 仓库中"
        )

        // 获取上一个提交的哈希
        val previousCommit = getPreviousCommit(repository, file)
            ?: return RollbackResult(
                success = false,
                message = "没有找到上一个版本"
            )

        return rollbackToCommit(file, previousCommit, options)
    }

    /**
     * 撤销回滚
     *
     * 从备份文件恢复
     *
     * @param file 原始文件
     * @param backupPath 备份文件路径
     * @return 是否成功
     */
    fun undoRollback(file: VirtualFile, backupPath: String): Boolean {
        return try {
            val backupFile = java.io.File(backupPath)
            if (!backupFile.exists()) {
                return false
            }

            WriteCommandAction.runWriteCommandAction(project) {
                file.setBinaryContent(backupFile.readBytes())
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 清理备份文件
     *
     * @param backupPath 备份文件路径
     * @return 是否成功
     */
    fun cleanupBackup(backupPath: String): Boolean {
        return try {
            java.io.File(backupPath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 Git 仓库
     */
    private fun getGitRepository(file: VirtualFile): GitRepository? {
        return GitUtil.getRepositoryManager(project).getRepositoryForFile(file)
    }

    /**
     * 获取文件相对路径
     */
    private fun getRelativePath(repository: GitRepository, file: VirtualFile): String {
        return file.path.removePrefix(repository.root.path).removePrefix("/")
    }

    /**
     * 检查是否有未保存的更改
     */
    private fun hasUnsavedChanges(file: VirtualFile): Boolean {
        // 检查文档缓存
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
            .getDocument(file)
        return document?.isModified ?: false
    }

    /**
     * 创建备份
     */
    private fun createBackup(file: VirtualFile, suffix: String): String? {
        return try {
            val backupPath = "${file.path}$suffix.${System.currentTimeMillis()}"
            val backupFile = java.io.File(backupPath)
            backupFile.writeBytes(file.contentsToByteArray())
            backupPath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取指定提交的文件内容
     */
    private fun getFileContentAtCommit(
        repository: GitRepository,
        file: VirtualFile,
        commitHash: String
    ): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.SHOW)
        handler.addParameters("$commitHash:${getRelativePath(repository, file)}")

        val result = git.runCommand(handler)
        return if (result.success()) {
            result.output.joinToString("\n")
        } else {
            null
        }
    }

    /**
     * 验证 YAML 内容
     */
    private fun validateYamlContent(content: String): Boolean {
        return try {
            YamlParser().parse(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取上一个提交
     */
    private fun getPreviousCommit(repository: GitRepository, file: VirtualFile): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.LOG)
        handler.addParameters(
            "--pretty=format:%H",
            "-n", "2",
            "--skip", "1",
            "--",
            file.path
        )

        val result = git.runCommand(handler)
        return if (result.success() && result.output.size >= 2) {
            result.output[1]  // 第二个提交（第一个是 HEAD）
        } else {
            null
        }
    }

    /**
     * 检查回滚后是否会产生冲突
     *
     * @param file 要检查的文件
     * @param commitHash 目标提交哈希
     * @return 冲突检查结果
     */
    fun checkRollbackConflicts(
        file: VirtualFile,
        commitHash: String
    ): ConflictCheckResult {
        val repository = getGitRepository(file) ?: return ConflictCheckResult(
            canRollback = false,
            reason = "文件不在 Git 仓库中"
        )

        // 获取当前版本
        val currentContent = file.readText()
        val currentModel = try {
            YamlParser().parse(currentContent)
        } catch (e: Exception) {
            return ConflictCheckResult(
                canRollback = false,
                reason = "当前文件 YAML 格式无效"
            )
        }

        // 获取目标版本
        val targetContent = getFileContentAtCommit(repository, file, commitHash)
            ?: return ConflictCheckResult(
                canRollback = false,
                reason = "无法获取目标版本内容"
            )

        val targetModel = try {
            YamlParser().parse(targetContent)
        } catch (e: Exception) {
            return ConflictCheckResult(
                canRollback = false,
                reason = "目标版本 YAML 格式无效"
            )
        }

        // 获取共同祖先
        val baseCommit = findCommonAncestor(repository, file, commitHash)
        val baseModel = baseCommit?.let {
            getFileContentAtCommit(repository, file, it)?.let { content ->
                try {
                    YamlParser().parse(content)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // 执行三方合并检查
        val resolver = ConflictResolver(project)
        val mergeResult = resolver.threeWayMerge(
            baseModel,
            currentModel,
            targetModel,
            ConflictResolver.MergeStrategy.AUTO
        )

        return if (mergeResult.success) {
            ConflictCheckResult(
                canRollback = true,
                conflicts = emptyList(),
                mergeResult = mergeResult
            )
        } else {
            ConflictCheckResult(
                canRollback = false,
                reason = "回滚将产生 ${mergeResult.conflicts.size} 个冲突",
                conflicts = mergeResult.conflicts,
                mergeResult = mergeResult
            )
        }
    }

    /**
     * 查找共同祖先
     */
    private fun findCommonAncestor(
        repository: GitRepository,
        file: VirtualFile,
        commitHash: String
    ): String? {
        val handler = GitLineHandler(project, repository.root, GitCommand.MERGE_BASE)
        handler.addParameters("HEAD", commitHash)

        val result = git.runCommand(handler)
        return if (result.success() && result.output.isNotEmpty()) {
            result.output[0]
        } else {
            null
        }
    }

    /**
     * 冲突检查结果数据类
     */
    data class ConflictCheckResult(
        val canRollback: Boolean,
        val reason: String? = null,
        val conflicts: List<ConflictResolver.ConflictInfo> = emptyList(),
        val mergeResult: ConflictResolver.MergeResult? = null
    )

    /**
     * 显示回滚确认对话框
     *
     * @param file 要回滚的文件
     * @param commitHash 目标提交哈希
     * @param commitMessage 提交消息（可选）
     * @return 用户选择的结果
     */
    fun showRollbackConfirmationDialog(
        file: VirtualFile,
        commitHash: String,
        commitMessage: String? = null
    ): RollbackConfirmationResult {
        val checkResult = checkRollbackConflicts(file, commitHash)

        if (!checkResult.canRollback) {
            val message = buildString {
                appendLine("无法回滚到版本 ${commitHash.take(7)}")
                appendLine()
                appendLine("原因: ${checkResult.reason}")

                if (checkResult.conflicts.isNotEmpty()) {
                    appendLine()
                    appendLine("冲突详情:")
                    checkResult.conflicts.forEach { conflict ->
                        appendLine("  - ${conflict.testCaseId}: ${conflict.conflictType}")
                    }
                }
            }

            val result = Messages.showYesNoCancelDialog(
                project,
                message,
                "回滚确认",
                "强制回滚",
                "取消",
                "查看冲突",
                Messages.getWarningIcon()
            )

            return when (result) {
                Messages.YES -> RollbackConfirmationResult.FORCE_ROLLBACK
                Messages.NO -> RollbackConfirmationResult.CANCEL
                else -> RollbackConfirmationResult.SHOW_CONFLICTS
            }
        }

        val message = buildString {
            appendLine("确定要回滚文件 '${file.name}' 到以下版本吗？")
            appendLine()
            appendLine("提交: ${commitHash.take(7)}")
            if (commitMessage != null) {
                appendLine("消息: $commitMessage")
            }
            appendLine()
            appendLine("警告: 此操作将覆盖当前文件的未保存更改。")
        }

        val result = Messages.showYesNoDialog(
            project,
            message,
            "确认回滚",
            Messages.getQuestionIcon()
        )

        return if (result == Messages.YES) {
            RollbackConfirmationResult.PROCEED
        } else {
            RollbackConfirmationResult.CANCEL
        }
    }

    /**
     * 回滚确认结果枚举
     */
    enum class RollbackConfirmationResult {
        /** 继续回滚 */
        PROCEED,
        /** 强制回滚（忽略冲突） */
        FORCE_ROLLBACK,
        /** 取消 */
        CANCEL,
        /** 显示冲突详情 */
        SHOW_CONFLICTS
    }

    /**
     * 批量回滚多个文件
     *
     * @param files 要回滚的文件列表
     * @param commitHash 目标提交哈希
     * @param options 回滚选项
     * @return 批量回滚结果
     */
    fun batchRollback(
        files: List<VirtualFile>,
        commitHash: String,
        options: RollbackOptions = RollbackOptions()
    ): BatchRollbackResult {
        val results = mutableMapOf<VirtualFile, RollbackResult>()
        val successful = mutableListOf<VirtualFile>()
        val failed = mutableListOf<Pair<VirtualFile, String>>()

        files.forEach { file ->
            val result = rollbackToCommit(file, commitHash, options)
            results[file] = result

            if (result.success) {
                successful.add(file)
            } else {
                failed.add(file to result.message)
            }
        }

        return BatchRollbackResult(
            totalFiles = files.size,
            successfulFiles = successful.size,
            failedFiles = failed.size,
            results = results,
            successful = successful,
            failed = failed
        )
    }

    /**
     * 批量回滚结果数据类
     */
    data class BatchRollbackResult(
        val totalFiles: Int,
        val successfulFiles: Int,
        val failedFiles: Int,
        val results: Map<VirtualFile, RollbackResult>,
        val successful: List<VirtualFile>,
        val failed: List<Pair<VirtualFile, String>>
    ) {
        fun isCompleteSuccess(): Boolean = failedFiles == 0
        fun isCompleteFailure(): Boolean = successfulFiles == 0
        fun isPartialSuccess(): Boolean = successfulFiles > 0 && failedFiles > 0
    }
}
