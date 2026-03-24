package com.testcase.manager.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * Git 状态指示器组件
 *
 * 在编辑器中显示文件的 Git 状态，包括状态图标、分支信息和变更标记。
 * 支持实时更新和自定义样式。
 *
 * @property project 当前项目实例
 * @property file 关联的虚拟文件
 */
class GitStatusIndicator(
    private val project: Project,
    private val file: VirtualFile
) : JPanel() {

    private val gitIntegration = GitIntegration.getInstance(project)

    private val statusLabel: JLabel
    private val branchLabel: JLabel
    private val indicatorIcon: StatusIndicatorIcon

    init {
        layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5)
        border = EmptyBorder(5, 10, 5, 10)
        background = JBColor(Color(245, 245, 245), Color(60, 60, 60))

        // 创建状态图标
        indicatorIcon = StatusIndicatorIcon()
        add(indicatorIcon)

        // 创建状态标签
        statusLabel = JLabel("检测中...")
        statusLabel.font = statusLabel.font.deriveFont(java.awt.Font.BOLD)
        add(statusLabel)

        // 添加分隔
        add(JLabel(" | "))

        // 创建分支标签
        branchLabel = JLabel("")
        branchLabel.font = branchLabel.font.deriveFont(java.awt.Font.PLAIN)
        add(branchLabel)

        // 初始更新
        updateStatus()
    }

    /**
     * 更新 Git 状态显示
     */
    fun updateStatus() {
        if (!gitIntegration.isGitEnabled()) {
            statusLabel.text = "未使用 Git"
            statusLabel.foreground = Color.GRAY
            indicatorIcon.setStatus(GitFileStatus.NOT_TRACKED)
            branchLabel.text = ""
            return
        }

        if (!gitIntegration.isFileUnderGit(file)) {
            statusLabel.text = "未跟踪"
            statusLabel.foreground = Color.GRAY
            indicatorIcon.setStatus(GitFileStatus.UNTRACKED)
            branchLabel.text = ""
            return
        }

        val status = gitIntegration.getFileStatus(file)
        val branch = gitIntegration.getCurrentBranch(file)

        // 更新状态标签
        statusLabel.text = getStatusText(status)
        statusLabel.foreground = gitIntegration.getStatusColor(file)

        // 更新图标
        indicatorIcon.setStatus(status)

        // 更新分支标签
        branchLabel.text = branch?.let { "分支: $it" } ?: ""
    }

    /**
     * 获取状态显示文本
     *
     * @param status Git 文件状态
     * @return 状态文本
     */
    private fun getStatusText(status: GitFileStatus): String {
        return when (status) {
            GitFileStatus.UNCHANGED -> "已提交"
            GitFileStatus.MODIFIED -> "已修改"
            GitFileStatus.ADDED -> "已暂存"
            GitFileStatus.DELETED -> "已删除"
            GitFileStatus.UNTRACKED -> "未跟踪"
            GitFileStatus.IGNORED -> "已忽略"
            GitFileStatus.CONFLICT -> "冲突"
            GitFileStatus.MERGED -> "已合并"
            GitFileStatus.UNKNOWN -> "未知"
            GitFileStatus.NOT_TRACKED -> "未跟踪"
        }
    }

    /**
     * 状态指示图标组件
     */
    inner class StatusIndicatorIcon : JComponent() {

        private var currentStatus: GitFileStatus = GitFileStatus.UNKNOWN
        private val size = 16

        init {
            preferredSize = Dimension(size, size)
            minimumSize = Dimension(size, size)
            maximumSize = Dimension(size, size)
        }

        /**
         * 设置状态并重绘
         *
         * @param status Git 文件状态
         */
        fun setStatus(status: GitFileStatus) {
            currentStatus = status
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val color = when (currentStatus) {
                GitFileStatus.UNCHANGED -> Color(100, 100, 100)
                GitFileStatus.MODIFIED -> Color(0, 100, 200)
                GitFileStatus.ADDED -> Color(0, 150, 0)
                GitFileStatus.DELETED -> Color(200, 50, 50)
                GitFileStatus.UNTRACKED -> Color(150, 150, 150)
                GitFileStatus.IGNORED -> Color(180, 180, 180)
                GitFileStatus.CONFLICT -> Color(255, 100, 0)
                GitFileStatus.MERGED -> Color(150, 0, 200)
                GitFileStatus.UNKNOWN -> Color(100, 100, 100)
                GitFileStatus.NOT_TRACKED -> Color(100, 100, 100)
            }

            g2d.color = color

            // 根据状态绘制不同形状
            when (currentStatus) {
                GitFileStatus.UNCHANGED -> {
                    // 绘制实心圆
                    g2d.fillOval(2, 2, size - 4, size - 4)
                }
                GitFileStatus.MODIFIED -> {
                    // 绘制菱形
                    val x = intArrayOf(size / 2, size - 2, size / 2, 2)
                    val y = intArrayOf(2, size / 2, size - 2, size / 2)
                    g2d.fillPolygon(x, y, 4)
                }
                GitFileStatus.ADDED -> {
                    // 绘制加号
                    g2d.fillRect(size / 2 - 2, 3, 4, size - 6)
                    g2d.fillRect(3, size / 2 - 2, size - 6, 4)
                }
                GitFileStatus.DELETED -> {
                    // 绘制减号
                    g2d.fillRect(3, size / 2 - 2, size - 6, 4)
                }
                GitFileStatus.CONFLICT -> {
                    // 绘制叉号
                    g2d.drawLine(3, 3, size - 3, size - 3)
                    g2d.drawLine(size - 3, 3, 3, size - 3)
                    g2d.stroke = java.awt.BasicStroke(2f)
                    g2d.drawLine(3, 3, size - 3, size - 3)
                    g2d.drawLine(size - 3, 3, 3, size - 3)
                }
                else -> {
                    // 绘制空心圆
                    g2d.drawOval(2, 2, size - 4, size - 4)
                }
            }
        }
    }
}

/**
 * Git 状态徽章组件
 *
 * 用于在行号或单元格旁边显示小型状态徽章
 *
 * @property project 当前项目实例
 */
class GitStatusBadge(private val project: Project) {

    private val gitIntegration = GitIntegration.getInstance(project)

    /**
     * 创建状态徽章标签
     *
     * @param file 虚拟文件
     * @return 配置好的 JLabel
     */
    fun createBadge(file: VirtualFile): JLabel {
        val status = gitIntegration.getFileStatus(file)
        val indicator = gitIntegration.getStatusIndicator(file)
        val color = gitIntegration.getStatusColor(file)

        return JLabel(indicator).apply {
            foreground = color
            font = font.deriveFont(java.awt.Font.BOLD, 10f)
            toolTipText = getStatusTooltip(status)
        }
    }

    /**
     * 获取状态提示文本
     *
     * @param status Git 文件状态
     * @return 提示文本
     */
    private fun getStatusTooltip(status: GitFileStatus): String {
        return when (status) {
            GitFileStatus.UNCHANGED -> "文件未变更"
            GitFileStatus.MODIFIED -> "文件已修改但未提交"
            GitFileStatus.ADDED -> "文件已添加到暂存区"
            GitFileStatus.DELETED -> "文件已删除"
            GitFileStatus.UNTRACKED -> "文件未被 Git 跟踪"
            GitFileStatus.IGNORED -> "文件被 Git 忽略"
            GitFileStatus.CONFLICT -> "文件有合并冲突"
            GitFileStatus.MERGED -> "文件已合并"
            GitFileStatus.UNKNOWN -> "文件状态未知"
            GitFileStatus.NOT_TRACKED -> "文件不在 Git 版本控制中"
        }
    }
}

/**
 * Git 状态栏图标
 *
 * 提供标准化的 Git 状态图标
 */
object GitStatusIcons {

    /**
     * 获取状态图标
     *
     * @param status Git 文件状态
     * @return 图标实例
     */
    fun getIcon(status: GitFileStatus): Icon {
        return GitStatusIconImpl(status)
    }

    /**
     * Git 状态图标实现
     */
    private class GitStatusIconImpl(private val status: GitFileStatus) : Icon {

        override fun paintIcon(c: java.awt.Component?, g: Graphics, x: Int, y: Int) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val color = when (status) {
                GitFileStatus.UNCHANGED -> Color(100, 100, 100)
                GitFileStatus.MODIFIED -> Color(0, 100, 200)
                GitFileStatus.ADDED -> Color(0, 150, 0)
                GitFileStatus.DELETED -> Color(200, 50, 50)
                GitFileStatus.UNTRACKED -> Color(150, 150, 150)
                GitFileStatus.IGNORED -> Color(180, 180, 180)
                GitFileStatus.CONFLICT -> Color(255, 100, 0)
                GitFileStatus.MERGED -> Color(150, 0, 200)
                GitFileStatus.UNKNOWN -> Color(100, 100, 100)
                GitFileStatus.NOT_TRACKED -> Color(100, 100, 100)
            }

            g2d.color = color

            val size = iconWidth
            when (status) {
                GitFileStatus.MODIFIED -> {
                    // 绘制修改标记（菱形）
                    val xs = intArrayOf(x + size / 2, x + size - 2, x + size / 2, x + 2)
                    val ys = intArrayOf(y + 2, y + size / 2, y + size - 2, y + size / 2)
                    g2d.fillPolygon(xs, ys, 4)
                }
                GitFileStatus.ADDED -> {
                    // 绘制添加标记（加号）
                    g2d.fillRect(x + size / 2 - 1, y + 3, 2, size - 6)
                    g2d.fillRect(x + 3, y + size / 2 - 1, size - 6, 2)
                }
                GitFileStatus.DELETED -> {
                    // 绘制删除标记（减号）
                    g2d.fillRect(x + 3, y + size / 2 - 1, size - 6, 2)
                }
                else -> {
                    // 默认绘制圆点
                    g2d.fillOval(x + 3, y + 3, size - 6, size - 6)
                }
            }
        }

        override fun getIconWidth(): Int = 12
        override fun getIconHeight(): Int = 12
    }
}
