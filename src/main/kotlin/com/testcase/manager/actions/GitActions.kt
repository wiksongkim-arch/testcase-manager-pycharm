package com.testcase.manager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.testcase.manager.git.DiffRequestHelper
import com.testcase.manager.git.DiffViewer
import com.testcase.manager.git.GitIntegration

/**
 * Git 对比动作
 *
 * 打开 Diff 查看器，显示当前文件与 Git HEAD 版本的对比。
 * 可以在编辑器右键菜单或工具栏中使用。
 */
class GitCompareAction : AnAction(
    "Git 对比",
    "显示当前文件与 Git HEAD 版本的对比",
    com.intellij.icons.AllIcons.Actions.Diff
) {

    private val logger = Logger.getInstance(GitCompareAction::class.java)

    /**
     * 动作执行逻辑
     *
     * @param e 动作事件
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!GitIntegration.getInstance(project).isGitEnabled()) {
            Messages.showWarningDialog(
                project,
                "当前项目未启用 Git 版本控制",
                "Git 不可用"
            )
            return
        }

        if (!GitIntegration.getInstance(project).isFileUnderGit(file)) {
            Messages.showWarningDialog(
                project,
                "文件 '${file.name}' 不在 Git 版本控制下",
                "Git 对比"
            )
            return
        }

        try {
            // 使用 IntelliJ 内置 Diff 工具
            val diffHelper = DiffRequestHelper(project)
            diffHelper.showDiffWithHead(file)
        } catch (ex: Exception) {
            logger.error("Failed to open diff viewer", ex)
            Messages.showErrorDialog(
                project,
                "打开对比视图失败: ${ex.message}",
                "错误"
            )
        }
    }

    /**
     * 更新动作的可用状态
     *
     * @param e 动作事件
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // 默认禁用
        e.presentation.isEnabled = false

        if (project == null || file == null) {
            return
        }

        // 检查是否是 YAML 文件
        if (!file.name.endsWith(".yaml") && !file.name.endsWith(".yml")) {
            return
        }

        // 检查 Git 是否启用
        val gitIntegration = GitIntegration.getInstance(project)
        if (!gitIntegration.isGitEnabled()) {
            return
        }

        // 检查文件是否在 Git 控制下
        if (!gitIntegration.isFileUnderGit(file)) {
            return
        }

        e.presentation.isEnabled = true
    }
}

/**
 * Git 状态刷新动作
 *
 * 刷新文件的 Git 状态显示
 */
class GitRefreshStatusAction : AnAction(
    "刷新 Git 状态",
    "刷新当前文件的 Git 状态",
    com.intellij.icons.AllIcons.Actions.Refresh
) {

    private val logger = Logger.getInstance(GitRefreshStatusAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val gitIntegration = GitIntegration.getInstance(project)
        gitIntegration.refreshFileStatus(file)

        logger.info("Refreshed Git status for file: ${file.path}")
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        e.presentation.isEnabled = project != null &&
                file != null &&
                GitIntegration.getInstance(project).isGitEnabled()
    }
}

/**
 * Git 历史查看动作
 *
 * 显示文件的 Git 提交历史
 */
class GitHistoryAction : AnAction(
    "Git 历史",
    "查看文件的 Git 提交历史",
    com.intellij.icons.AllIcons.Vcs.History
) {

    private val logger = Logger.getInstance(GitHistoryAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val gitIntegration = GitIntegration.getInstance(project)
        val history = gitIntegration.getFileHistory(file, 10)

        if (history.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "文件 '${file.name}' 没有提交历史",
                "Git 历史"
            )
            return
        }

        // 构建历史信息字符串
        val sb = StringBuilder()
        sb.append("文件: ${file.name}\n")
        sb.append("最近 ${history.size} 条提交:\n\n")

        history.forEachIndexed { index, commit ->
            sb.append("${index + 1}. ${commit.hash.substring(0, 7)} - ${commit.subject}\n")
            sb.append("   作者: ${commit.authorName} (${commit.authorEmail})\n")
            sb.append("   日期: ${commit.date}\n\n")
        }

        Messages.showInfoMessage(
            project,
            sb.toString(),
            "Git 提交历史"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        e.presentation.isEnabled = project != null &&
                file != null &&
                GitIntegration.getInstance(project).isFileUnderGit(file)
    }
}

/**
 * Git 分支信息显示动作
 *
 * 显示当前文件所在分支的信息
 */
class GitBranchInfoAction : AnAction(
    "Git 分支信息",
    "显示当前分支信息",
    com.intellij.icons.AllIcons.Vcs.BranchNode
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val gitIntegration = GitIntegration.getInstance(project)
        val branch = gitIntegration.getCurrentBranch(file)
        val status = gitIntegration.getFileStatus(file)

        val message = buildString {
            appendLine("文件: ${file.name}")
            appendLine()
            appendLine("当前分支: ${branch ?: "未知"}")
            appendLine("文件状态: ${getStatusDisplayName(status)}")
            appendLine()
            appendLine("状态说明:")
            appendLine(getStatusDescription(status))
        }

        Messages.showInfoMessage(
            project,
            message,
            "Git 分支信息"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        e.presentation.isEnabled = project != null &&
                file != null &&
                GitIntegration.getInstance(project).isGitEnabled()
    }

    private fun getStatusDisplayName(status: com.testcase.manager.git.GitFileStatus): String {
        return when (status) {
            com.testcase.manager.git.GitFileStatus.UNCHANGED -> "已提交"
            com.testcase.manager.git.GitFileStatus.MODIFIED -> "已修改"
            com.testcase.manager.git.GitFileStatus.ADDED -> "已暂存"
            com.testcase.manager.git.GitFileStatus.DELETED -> "已删除"
            com.testcase.manager.git.GitFileStatus.UNTRACKED -> "未跟踪"
            com.testcase.manager.git.GitFileStatus.IGNORED -> "已忽略"
            com.testcase.manager.git.GitFileStatus.CONFLICT -> "冲突"
            com.testcase.manager.git.GitFileStatus.MERGED -> "已合并"
            com.testcase.manager.git.GitFileStatus.UNKNOWN -> "未知"
            com.testcase.manager.git.GitFileStatus.NOT_TRACKED -> "未跟踪"
        }
    }

    private fun getStatusDescription(status: com.testcase.manager.git.GitFileStatus): String {
        return when (status) {
            com.testcase.manager.git.GitFileStatus.UNCHANGED -> "文件与 HEAD 版本一致，没有未提交的更改。"
            com.testcase.manager.git.GitFileStatus.MODIFIED -> "文件在工作区有修改，但尚未添加到暂存区。\n使用 'git add' 添加到暂存区。"
            com.testcase.manager.git.GitFileStatus.ADDED -> "文件已添加到暂存区，等待提交。\n使用 'git commit' 提交更改。"
            com.testcase.manager.git.GitFileStatus.DELETED -> "文件已被删除。"
            com.testcase.manager.git.GitFileStatus.UNTRACKED -> "文件未被 Git 跟踪，是新文件。\n使用 'git add' 开始跟踪。"
            com.testcase.manager.git.GitFileStatus.IGNORED -> "文件被 .gitignore 忽略。"
            com.testcase.manager.git.GitFileStatus.CONFLICT -> "文件有合并冲突，需要手动解决。"
            com.testcase.manager.git.GitFileStatus.MERGED -> "文件已成功合并。"
            com.testcase.manager.git.GitFileStatus.UNKNOWN -> "文件状态未知。"
            com.testcase.manager.git.GitFileStatus.NOT_TRACKED -> "文件不在 Git 版本控制中。"
        }
    }
}
