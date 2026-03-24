package com.testcase.manager.performance

import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.*

/**
 * 加载指示器组件
 */
class LoadingIndicator : JPanel(BorderLayout()) {

    private val progressBar: JProgressBar
    private val statusLabel: JLabel
    private val spinnerPanel: SpinnerPanel

    // 是否显示详细信息
    var showDetails = true

    // 进度值 (0-100)
    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            progressBar.value = field
            updateVisibility()
        }

    // 状态文本
    var status: String = ""
        set(value) {
            field = value
            statusLabel.text = value
            updateVisibility()
        }

    // 是否正在加载
    var isLoading: Boolean = false
        set(value) {
            field = value
            spinnerPanel.isSpinning = value
            updateVisibility()
        }

    init {
        isOpaque = false
        preferredSize = Dimension(300, 60)

        // 进度条
        progressBar = JProgressBar(0, 100).apply {
            isStringPainted = true
            string = "0%"
        }

        // 状态标签
        statusLabel = JLabel("准备就绪").apply {
            horizontalAlignment = SwingConstants.CENTER
        }

        // 旋转器面板
        spinnerPanel = SpinnerPanel()

        // 组装界面
        val centerPanel = JPanel(BorderLayout(5, 5)).apply {
            isOpaque = false
            add(spinnerPanel, BorderLayout.WEST)
            add(statusLabel, BorderLayout.CENTER)
        }

        add(centerPanel, BorderLayout.CENTER)
        add(progressBar, BorderLayout.SOUTH)

        updateVisibility()
    }

    /**
     * 更新可见性
     */
    private fun updateVisibility() {
        progressBar.isVisible = showDetails && progress > 0
        spinnerPanel.isVisible = isLoading
        statusLabel.isVisible = status.isNotEmpty()
    }

    /**
     * 显示加载中状态
     */
    fun showLoading(message: String = "加载中...") {
        isLoading = true
        status = message
        progress = 0
        isVisible = true
    }

    /**
     * 更新进度
     */
    fun updateProgress(loaded: Int, total: Int) {
        if (total > 0) {
            progress = (loaded * 100 / total)
            status = "已加载 $loaded / $total"
        }
    }

    /**
     * 显示完成状态
     */
    fun showComplete(message: String = "完成") {
        isLoading = false
        status = message
        progress = 100

        // 延迟隐藏
        Timer(1000) {
            isVisible = false
        }.apply {
            isRepeats = false
            start()
        }
    }

    /**
     * 显示错误状态
     */
    fun showError(message: String) {
        isLoading = false
        status = "错误: $message"
        progress = 0
    }

    /**
     * 隐藏指示器
     */
    fun hide() {
        isLoading = false
        isVisible = false
    }

    /**
     * 旋转动画面板
     */
    inner class SpinnerPanel : JPanel() {

        var isSpinning = false
            set(value) {
                field = value
                if (value) {
                    startAnimation()
                } else {
                    stopAnimation()
                }
            }

        private var rotationAngle = 0f
        private var timer: Timer? = null

        init {
            preferredSize = Dimension(30, 30)
            isOpaque = false
        }

        private fun startAnimation() {
            timer?.stop()
            timer = Timer(50) {
                rotationAngle += 15f
                if (rotationAngle >= 360f) {
                    rotationAngle = 0f
                }
                repaint()
            }.apply {
                start()
            }
        }

        private fun stopAnimation() {
            timer?.stop()
            timer = null
            rotationAngle = 0f
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val centerX = width / 2
            val centerY = height / 2
            val radius = (width.coerceAtMost(height) / 2 - 4).coerceAtLeast(5)

            // 保存当前变换
            val oldTransform = g2d.transform

            // 应用旋转
            g2d.rotate(Math.toRadians(rotationAngle.toDouble()), centerX.toDouble(), centerY.toDouble())

            // 绘制旋转的弧线
            g2d.color = JBColor.BLUE
            g2d.stroke = java.awt.BasicStroke(3f)
            g2d.drawArc(
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2,
                0,
                270
            )

            // 恢复变换
            g2d.transform = oldTransform

            g2d.dispose()
        }
    }

    companion object {
        /**
         * 创建加载指示器
         */
        fun create(configure: (LoadingIndicator.() -> Unit)? = null): LoadingIndicator {
            return LoadingIndicator().apply {
                configure?.invoke(this)
            }
        }
    }
}
