package com.testcase.manager.git

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsHistoryProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitLocalBranch
import git4idea.GitRevisionNumber
import git4idea.GitUtil
import git4idea.commands.*
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Git 集成类
 *
 * 集成 PyCharm 内置的 Git4Idea 插件，提供 Git 状态检测和文件对比功能。
 * 支持检测文件的修改、新增、删除状态，以及获取文件的历史版本。
 *
 * @property project 当前项目实例
 */
class GitIntegration(private val project: Project) {

    private val logger = Logger.getInstance(GitIntegration::class.java)

    /** Git 仓库管理器 */
    private val repositoryManager: GitRepositoryManager
        get() = GitRepositoryManager.getInstance(project)

    /** 变更列表管理器 */
    private val changeListManager: ChangeListManager
        get() = ChangeListManager.getInstance(project)

    /** 文件状态管理器 */
    private val fileStatusManager: FileStatusManager
        get() = FileStatusManager.getInstance(project)

    /**
     * 检查项目是否使用 Git 版本控制
     *
     * @return 如果项目使用 Git 则返回 true
     */
    fun isGitEnabled(): Boolean {
        return repositoryManager.repositories.isNotEmpty()
    }

    /**
     * 获取指定文件所在的 Git 仓库
     *
     * @param file 虚拟文件
     * @return GitRepository 实例，如果文件不在任何仓库中则返回 null
     */
    fun getRepositoryForFile(file: VirtualFile): GitRepository? {
        return repositoryManager.getRepositoryForFile(file)
    }

    /**
     * 获取文件在当前工作区的 Git 状态
     *
     * @param file 虚拟文件
     * @return GitFileStatus 枚举值表示文件状态
     */
    fun getFileStatus(file: VirtualFile): GitFileStatus {
        if (!isGitEnabled()) {
            return GitFileStatus.NOT_TRACKED
        }

        val repository = getRepositoryForFile(file) ?: return GitFileStatus.NOT_TRACKED
        val filePath = VcsUtil.getFilePath(file)

        // 检查文件是否在 Git 仓库中
        if (!isFileUnderGit(file)) {
            return GitFileStatus.NOT_TRACKED
        }

        // 获取 IntelliJ 的文件状态
        val status = fileStatusManager.getStatus(file)

        return when {
            status == FileStatus.NOT_CHANGED -> GitFileStatus.UNCHANGED
            status == FileStatus.MODIFIED -> GitFileStatus.MODIFIED
            status == FileStatus.ADDED -> GitFileStatus.ADDED
            status == FileStatus.DELETED -> GitFileStatus.DELETED
            status == FileStatus.UNKNOWN -> GitFileStatus.UNTRACKED
            status == FileStatus.IGNORED -> GitFileStatus.IGNORED
            status == FileStatus.MERGED -> GitFileStatus.MERGED
            status == FileStatus.CONFLICTING -> GitFileStatus.CONFLICT
            else -> GitFileStatus.UNKNOWN
        }
    }

    /**
     * 检查文件是否在 Git 版本控制下
     *
     * @param file 虚拟文件
     * @return 如果文件在 Git 控制下则返回 true
     */
    fun isFileUnderGit(file: VirtualFile): Boolean {
        val repository = getRepositoryForFile(file) ?: return false
        val filePath = VcsUtil.getFilePath(file)
        return GitUtil.isFileUnderGit(repository, filePath)
    }

    /**
     * 获取当前分支名称
     *
     * @param file 用于确定仓库的文件
     * @return 分支名称，如果不在 Git 仓库中则返回 null
     */
    fun getCurrentBranch(file: VirtualFile): String? {
        val repository = getRepositoryForFile(file) ?: return null
        val branch = repository.currentBranch ?: return null
        return branch.name
    }

    /**
     * 获取文件在 HEAD 版本的内容
     *
     * @param file 虚拟文件
     * @return HEAD 版本的文件内容字符串，如果获取失败则返回 null
     */
    fun getHeadVersionContent(file: VirtualFile): String? {
        val repository = getRepositoryForFile(file) ?: return null
        val filePath = VcsUtil.getFilePath(file)

        return try {
            val result = GitLineHandler(project, repository.root, GitCommand.SHOW)
            result.addParameters("HEAD:${getRelativePath(repository, file)}")

            val output = ByteArrayOutputStream()
            val error = ByteArrayOutputStream()

            result.setStdoutStream(output)
            result.setStderrStream(error)

            val exitCode = Git.getInstance().runCommand(result).exitCode

            if (exitCode == 0) {
                output.toString(Charsets.UTF_8.name())
            } else {
                logger.warn("Failed to get HEAD version: ${error.toString(Charsets.UTF_8.name())}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting HEAD version", e)
            null
        }
    }

    /**
     * 获取文件在指定提交版本的内容
     *
     * @param file 虚拟文件
     * @param commitHash 提交哈希值
     * @return 指定版本的文件内容字符串，如果获取失败则返回 null
     */
    fun getVersionContent(file: VirtualFile, commitHash: String): String? {
        val repository = getRepositoryForFile(file) ?: return null

        return try {
            val result = GitLineHandler(project, repository.root, GitCommand.SHOW)
            result.addParameters("$commitHash:${getRelativePath(repository, file)}")

            val output = ByteArrayOutputStream()
            val error = ByteArrayOutputStream()

            result.setStdoutStream(output)
            result.setStderrStream(error)

            val exitCode = Git.getInstance().runCommand(result).exitCode

            if (exitCode == 0) {
                output.toString(Charsets.UTF_8.name())
            } else {
                logger.warn("Failed to get version $commitHash: ${error.toString(Charsets.UTF_8.name())}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting version $commitHash", e)
            null
        }
    }

    /**
     * 获取文件的变更信息
     *
     * @param file 虚拟文件
     * @return Change 对象，如果文件没有变更则返回 null
     */
    fun getChange(file: VirtualFile): Change? {
        return changeListManager.getChange(file)
    }

    /**
     * 获取文件是否被修改
     *
     * @param file 虚拟文件
     * @return 如果文件在工作区有修改则返回 true
     */
    fun isModified(file: VirtualFile): Boolean {
        return getFileStatus(file) == GitFileStatus.MODIFIED
    }

    /**
     * 获取文件是否是新增未提交
     *
     * @param file 虚拟文件
     * @return 如果文件是新增且未提交则返回 true
     */
    fun isAdded(file: VirtualFile): Boolean {
        return getFileStatus(file) == GitFileStatus.ADDED
    }

    /**
     * 获取文件是否被删除
     *
     * @param file 虚拟文件
     * @return 如果文件被删除则返回 true
     */
    fun isDeleted(file: VirtualFile): Boolean {
        return getFileStatus(file) == GitFileStatus.DELETED
    }

    /**
     * 获取文件是否有冲突
     *
     * @param file 虚拟文件
     * @return 如果文件有合并冲突则返回 true
     */
    fun hasConflict(file: VirtualFile): Boolean {
        return getFileStatus(file) == GitFileStatus.CONFLICT
    }

    /**
     * 获取文件的 Git 状态颜色
     *
     * @param file 虚拟文件
     * @return 对应状态的颜色
     */
    fun getStatusColor(file: VirtualFile): java.awt.Color {
        return when (getFileStatus(file)) {
            GitFileStatus.UNCHANGED -> java.awt.Color(100, 100, 100)
            GitFileStatus.MODIFIED -> java.awt.Color(0, 100, 200)    // 蓝色
            GitFileStatus.ADDED -> java.awt.Color(0, 150, 0)         // 绿色
            GitFileStatus.DELETED -> java.awt.Color(200, 50, 50)     // 红色
            GitFileStatus.UNTRACKED -> java.awt.Color(150, 150, 150) // 灰色
            GitFileStatus.IGNORED -> java.awt.Color(180, 180, 180)   // 浅灰
            GitFileStatus.CONFLICT -> java.awt.Color(255, 100, 0)    // 橙色
            GitFileStatus.MERGED -> java.awt.Color(150, 0, 200)      // 紫色
            GitFileStatus.UNKNOWN -> java.awt.Color(100, 100, 100)
            GitFileStatus.NOT_TRACKED -> java.awt.Color(100, 100, 100)
        }
    }

    /**
     * 获取文件状态指示器的显示文本
     *
     * @param file 虚拟文件
     * @return 状态指示文本（如 "M", "A", "D" 等）
     */
    fun getStatusIndicator(file: VirtualFile): String {
        return when (getFileStatus(file)) {
            GitFileStatus.UNCHANGED -> ""
            GitFileStatus.MODIFIED -> "M"
            GitFileStatus.ADDED -> "A"
            GitFileStatus.DELETED -> "D"
            GitFileStatus.UNTRACKED -> "?"
            GitFileStatus.IGNORED -> "I"
            GitFileStatus.CONFLICT -> "C"
            GitFileStatus.MERGED -> "M"
            GitFileStatus.UNKNOWN -> ""
            GitFileStatus.NOT_TRACKED -> ""
        }
    }

    /**
     * 获取文件的最近提交历史
     *
     * @param file 虚拟文件
     * @param limit 返回的最大提交数
     * @return 提交历史列表
     */
    fun getFileHistory(file: VirtualFile, limit: Int = 10): List<GitCommitInfo> {
        val repository = getRepositoryForFile(file) ?: return emptyList()
        val filePath = VcsUtil.getFilePath(file)

        return try {
            val result = GitLineHandler(project, repository.root, GitCommand.LOG)
            result.addParameters(
                "--max-count=$limit",
                "--pretty=format:%H|%an|%ae|%ad|%s",
                "--date=iso",
                "--",
                filePath.path
            )

            val output = ByteArrayOutputStream()
            result.setStdoutStream(output)

            val exitCode = Git.getInstance().runCommand(result).exitCode

            if (exitCode == 0) {
                parseGitLog(output.toString(Charsets.UTF_8.name()))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error getting file history", e)
            emptyList()
        }
    }

    /**
     * 刷新文件状态
     *
     * @param file 虚拟文件
     */
    fun refreshFileStatus(file: VirtualFile) {
        fileStatusManager.fileStatusChanged(file)
    }

    /**
     * 刷新所有文件状态
     */
    fun refreshAllStatuses() {
        fileStatusManager.refreshFileStatusFromDocument()
    }

    /**
     * 获取文件相对于仓库根目录的路径
     *
     * @param repository Git 仓库
     * @param file 虚拟文件
     * @return 相对路径字符串
     */
    private fun getRelativePath(repository: GitRepository, file: VirtualFile): String {
        val rootPath = repository.root.path
        val filePath = file.path
        return if (filePath.startsWith(rootPath)) {
            filePath.substring(rootPath.length + 1).replace(File.separatorChar, '/')
        } else {
            filePath.replace(File.separatorChar, '/')
        }
    }

    /**
     * 解析 Git log 输出
     *
     * @param logOutput Git log 命令输出
     * @return 提交信息列表
     */
    private fun parseGitLog(logOutput: String): List<GitCommitInfo> {
        return logOutput.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|", limit = 5)
                if (parts.size >= 5) {
                    GitCommitInfo(
                        hash = parts[0],
                        authorName = parts[1],
                        authorEmail = parts[2],
                        date = parts[3],
                        subject = parts[4]
                    )
                } else {
                    null
                }
            }
    }

    companion object {
        /**
         * 获取项目级别的 GitIntegration 实例
         *
         * @param project 项目实例
         * @return GitIntegration 实例
         */
        fun getInstance(project: Project): GitIntegration {
            return GitIntegration(project)
        }
    }
}

/**
 * Git 文件状态枚举
 */
enum class GitFileStatus {
    UNCHANGED,      // 未变更
    MODIFIED,       // 已修改
    ADDED,          // 已添加（暂存区）
    DELETED,        // 已删除
    UNTRACKED,      // 未跟踪
    IGNORED,        // 被忽略
    CONFLICT,       // 冲突
    MERGED,         // 已合并
    UNKNOWN,        // 未知
    NOT_TRACKED     // 不在版本控制中
}

/**
 * Git 提交信息数据类
 *
 * @property hash 提交哈希
 * @property authorName 作者名称
 * @property authorEmail 作者邮箱
 * @property date 提交日期
 * @property subject 提交主题
 */
data class GitCommitInfo(
    val hash: String,
    val authorName: String,
    val authorEmail: String,
    val date: String,
    val subject: String
)
