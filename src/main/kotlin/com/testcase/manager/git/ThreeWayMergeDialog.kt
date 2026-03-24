package com.testcase.manager.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.testcase.manager.model.TestCase
import com.testcase.manager.model.TestCaseModel
import com.testcase.manager.ui.TestCaseTableModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.table.DefaultTableCellRenderer

/**
 * 三方合并对话框
 *
 * 提供可视化的三方合并界面，显示基础版本、本地版本和远程版本，
 * 允许用户查看冲突并选择解决方案。
 *
 * @property project IntelliJ 项目实例
 * @property conflicts 冲突列表
 * @property baseModel 基础版本模型
 * @property localModel 本地版本模型
 * @property remoteModel 远程版本模型
 */
class ThreeWayMergeDialog(
    private val project: Project,
    private val conflicts: List<ConflictResolver.ConflictInfo>,
    private val baseModel: TestCaseModel?,
    private val localModel: TestCaseModel,
    private val remoteModel: TestCaseModel
) : DialogWrapper(project) {

    private lateinit var conflictList: JList<ConflictListItem>
    private lateinit var basePanel: TestCaseComparisonPanel
    private lateinit var localPanel: TestCaseComparisonPanel
    private lateinit var remotePanel: TestCaseComparisonPanel
    private lateinit var resolutionPanel: ResolutionPanel

    private val resolvedConflicts = mutableMapOf<String, ConflictResolver.ConflictResolution>()
    private val manualEdits = mutableMapOf<String, TestCase>()

    init {
        title = "解决合并冲突 - ${conflicts.size} 个冲突"
        init()
        setOKButtonText("应用解决")
        setCancelButtonText("取消")
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(1200, 700)

        // 顶部：冲突列表
        mainPanel.add(createConflictListPanel(), BorderLayout.NORTH)

        // 中部：三方比较视图
        mainPanel.add(createComparisonPanel(), BorderLayout.CENTER)

        // 底部：解决选项
        mainPanel.add(createResolutionPanel(), BorderLayout.SOUTH)

        // 选择第一个冲突
        if (conflicts.isNotEmpty()) {
            selectConflict(0)
        }

        return mainPanel
    }

    /**
     * 创建冲突列表面板
     */
    private fun createConflictListPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(5, 5, 5, 5)

        // 标题
        panel.add(JBLabel("冲突列表:").apply {
            font = font.deriveFont(Font.BOLD)
        }, BorderLayout.NORTH)

        // 冲突列表
        val listModel = DefaultListModel<ConflictListItem>()
        conflicts.forEachIndexed { index, conflict ->
            listModel.addElement(ConflictListItem(index, conflict))
        }

        conflictList = JList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = ConflictListCellRenderer()
            addListSelectionListener { event ->
                if (!event.valueIsAdjusting) {
                    selectedIndex.takeIf { it >= 0 }?.let { selectConflict(it) }
                }
            }
        }

        panel.add(JBScrollPane(conflictList), BorderLayout.CENTER)
        panel.preferredSize = Dimension(200, 150)

        return panel
    }

    /**
     * 创建比较面板（三列布局）
     */
    private fun createComparisonPanel(): JComponent {
        val splitter = JBSplitter(false, 0.33f)
        splitter.splitterProportionKey = "three.way.merge.left.splitter"

        val rightSplitter = JBSplitter(false, 0.5f)
        rightSplitter.splitterProportionKey = "three.way.merge.right.splitter"

        // 基础版本（左侧）
        basePanel = TestCaseComparisonPanel("基础版本 (BASE)", Color(240, 240, 240))
        splitter.firstComponent = basePanel

        // 本地版本（右上）
        localPanel = TestCaseComparisonPanel("本地版本 (HEAD)", Color(200, 255, 200))
        rightSplitter.firstComponent = localPanel

        // 远程版本（右下）
        remotePanel = TestCaseComparisonPanel("远程版本 (REMOTE)", Color(200, 200, 255))
        rightSplitter.secondComponent = remotePanel

        splitter.secondComponent = rightSplitter

        return splitter
    }

    /**
     * 创建解决选项面板
     */
    private fun createResolutionPanel(): JComponent {
        resolutionPanel = ResolutionPanel()
        resolutionPanel.onResolutionSelected = { resolution ->
            applyResolution(resolution)
        }
        resolutionPanel.onManualEdit = {
            showManualEditDialog()
        }
        return resolutionPanel
    }

    /**
     * 选择冲突
     */
    private fun selectConflict(index: Int) {
        val conflict = conflicts[index]

        // 更新三个面板
        basePanel.setTestCase(conflict.baseVersion, conflict.conflictFields)
        localPanel.setTestCase(conflict.localVersion, conflict.conflictFields)
        remotePanel.setTestCase(conflict.remoteVersion, conflict.conflictFields)

        // 更新解决状态
        val resolution = resolvedConflicts[conflict.testCaseId]
        resolutionPanel.setCurrentResolution(resolution)

        // 高亮当前选中的冲突
        conflictList.repaint()
    }

    /**
     * 应用解决方式
     */
    private fun applyResolution(resolution: ConflictResolver.ConflictResolution) {
        val selectedIndex = conflictList.selectedIndex
        if (selectedIndex < 0) return

        val conflict = conflicts[selectedIndex]
        resolvedConflicts[conflict.testCaseId] = resolution

        // 更新列表显示
        conflictList.repaint()

        // 自动选择下一个未解决的冲突
        val nextUnresolved = conflicts.indexOfFirst {
            !resolvedConflicts.containsKey(it.testCaseId)
        }
        if (nextUnresolved >= 0) {
            conflictList.selectedIndex = nextUnresolved
        }

        updateOKButtonStatus()
    }

    /**
     * 显示手动编辑对话框
     */
    private fun showManualEditDialog() {
        val selectedIndex = conflictList.selectedIndex
        if (selectedIndex < 0) return

        val conflict = conflicts[selectedIndex]
        val dialog = ManualMergeDialog(project, conflict)

        if (dialog.showAndGet()) {
            manualEdits[conflict.testCaseId] = dialog.getMergedTestCase()
            resolvedConflicts[conflict.testCaseId] = ConflictResolver.ConflictResolution.MERGE_MANUAL
            conflictList.repaint()
            updateOKButtonStatus()
        }
    }

    /**
     * 更新确定按钮状态
     */
    private fun updateOKButtonStatus() {
        val allResolved = conflicts.all { resolvedConflicts.containsKey(it.testCaseId) }
        isOKActionEnabled = allResolved
    }

    /**
     * 获取合并结果
     */
    fun getMergeResult(): MergeResult {
        val mergedModel = TestCaseModel()
        val allIds = mutableSetOf<String>()

        baseModel?.testCases?.forEach { allIds.add(it.id) }
        localModel.testCases.forEach { allIds.add(it.id) }
        remoteModel.testCases.forEach { allIds.add(it.id) }

        allIds.forEach { id ->
            // 检查是否是冲突项
            val conflict = conflicts.find { it.testCaseId == id }
            val testCase = when {
                conflict != null -> {
                    // 使用用户选择的解决方案
                    val resolution = resolvedConflicts[id] ?: ConflictResolver.ConflictResolution.ACCEPT_LOCAL
                    if (resolution == ConflictResolver.ConflictResolution.MERGE_MANUAL) {
                        manualEdits[id] ?: ConflictResolver(project).resolveConflict(conflict, resolution)
                    } else {
                        ConflictResolver(project).resolveConflict(conflict, resolution)
                    }
                }
                else -> {
                    // 非冲突项，使用本地版本
                    localModel.findById(id) ?: remoteModel.findById(id)
                }
            }

            testCase?.let { mergedModel.addTestCase(it) }
        }

        return MergeResult(mergedModel, resolvedConflicts, manualEdits)
    }

    /**
     * 合并结果数据类
     */
    data class MergeResult(
        val model: TestCaseModel,
        val resolutions: Map<String, ConflictResolver.ConflictResolution>,
        val manualEdits: Map<String, TestCase>
    )

    /**
     * 冲突列表项
     */
    private data class ConflictListItem(
        val index: Int,
        val conflict: ConflictResolver.ConflictInfo
    ) {
        override fun toString(): String {
            return "${index + 1}. ${conflict.testCaseId} (${conflict.conflictType.name})"
        }
    }

    /**
     * 冲突列表渲染器
     */
    private inner class ConflictListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            val item = value as ConflictListItem
            val isResolved = resolvedConflicts.containsKey(item.conflict.testCaseId)

            text = buildString {
                append(item.conflict.testCaseId)
                append(" - ")
                append(item.conflict.conflictType.name)
                if (isResolved) {
                    append(" ✓")
                }
            }

            foreground = when {
                isSelected -> list.selectionForeground
                isResolved -> Color(0, 128, 0)
                else -> Color(255, 0, 0)
            }

            return this
        }
    }
}

/**
 * 测试用例比较面板
 */
private class TestCaseComparisonPanel(title: String, private val bgColor: Color) : JPanel(BorderLayout()) {

    private val fieldsPanel = JPanel()
    private val fieldComponents = mutableMapOf<String, JComponent>()

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(title),
            EmptyBorder(5, 5, 5, 5)
        )
        background = bgColor

        fieldsPanel.layout = BoxLayout(fieldsPanel, BoxLayout.Y_AXIS)
        fieldsPanel.background = bgColor

        add(JBScrollPane(fieldsPanel), BorderLayout.CENTER)
    }

    fun setTestCase(testCase: TestCase?, conflictFields: List<String>) {
        fieldsPanel.removeAll()
        fieldComponents.clear()

        if (testCase == null) {
            fieldsPanel.add(JBLabel("（已删除）").apply {
                foreground = Color.GRAY
                font = font.deriveFont(Font.ITALIC)
            })
        } else {
            // ID
            addField("ID", testCase.id, conflictFields.contains("id"))
            // 名称
            addField("名称", testCase.name, conflictFields.contains("name"))
            // 优先级
            addField("优先级", testCase.priority.value, conflictFields.contains("priority"))
            // 状态
            addField("状态", testCase.status.value, conflictFields.contains("status"))
            // 预期结果
            addField("预期结果", testCase.expected, conflictFields.contains("expected"))
            // 测试步骤
            addField("测试步骤", testCase.getStepsAsString(), conflictFields.contains("steps"))
            // 标签
            addField("标签", testCase.getTagsAsString(), conflictFields.contains("tags"))
            // 描述
            addField("描述", testCase.description, conflictFields.contains("description"))
        }

        fieldsPanel.revalidate()
        fieldsPanel.repaint()
    }

    private fun addField(name: String, value: String, isConflict: Boolean) {
        val panel = JPanel(BorderLayout())
        panel.background = if (isConflict) Color(255, 200, 200) else background
        panel.border = EmptyBorder(2, 5, 2, 5)

        val label = JBLabel("$name: ").apply {
            font = font.deriveFont(Font.BOLD)
        }

        val valueComponent = if (value.contains("\n")) {
            JBTextArea(value).apply {
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
                rows = 3
                background = panel.background
            }
        } else {
            JBLabel(value)
        }

        panel.add(label, BorderLayout.WEST)
        panel.add(valueComponent, BorderLayout.CENTER)

        if (isConflict) {
            panel.border = BorderFactory.createCompoundBorder(
                LineBorder(Color.RED, 1),
                EmptyBorder(2, 5, 2, 5)
            )
        }

        fieldsPanel.add(panel)
        fieldComponents[name] = panel
    }
}

/**
 * 解决选项面板
 */
private class ResolutionPanel : JPanel(BorderLayout()) {

    var onResolutionSelected: ((ConflictResolver.ConflictResolution) -> Unit)? = null
    var onManualEdit: (() -> Unit)? = null

    private val buttonGroup = ButtonGroup()
    private val localRadio = JRadioButton("接受本地版本 (HEAD)")
    private val remoteRadio = JRadioButton("接受远程版本 (REMOTE)")
    private val baseRadio = JRadioButton("接受基础版本 (BASE)")
    private val manualRadio = JRadioButton("手动合并...")

    init {
        border = BorderFactory.createTitledBorder("解决方式")

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        buttonGroup.add(localRadio)
        buttonGroup.add(remoteRadio)
        buttonGroup.add(baseRadio)
        buttonGroup.add(manualRadio)

        panel.add(localRadio)
        panel.add(remoteRadio)
        panel.add(baseRadio)
        panel.add(manualRadio)

        // 添加事件监听
        val listener = ActionListener { e ->
            when (e.source) {
                localRadio -> onResolutionSelected?.invoke(ConflictResolver.ConflictResolution.ACCEPT_LOCAL)
                remoteRadio -> onResolutionSelected?.invoke(ConflictResolver.ConflictResolution.ACCEPT_REMOTE)
                baseRadio -> onResolutionSelected?.invoke(ConflictResolver.ConflictResolution.ACCEPT_BASE)
                manualRadio -> onManualEdit?.invoke()
            }
        }

        localRadio.addActionListener(listener)
        remoteRadio.addActionListener(listener)
        baseRadio.addActionListener(listener)
        manualRadio.addActionListener(listener)

        add(panel, BorderLayout.CENTER)
    }

    fun setCurrentResolution(resolution: ConflictResolver.ConflictResolution?) {
        when (resolution) {
            ConflictResolver.ConflictResolution.ACCEPT_LOCAL -> localRadio.isSelected = true
            ConflictResolver.ConflictResolution.ACCEPT_REMOTE -> remoteRadio.isSelected = true
            ConflictResolver.ConflictResolution.ACCEPT_BASE -> baseRadio.isSelected = true
            ConflictResolver.ConflictResolution.MERGE_MANUAL -> manualRadio.isSelected = true
            null -> buttonGroup.clearSelection()
        }
    }
}

/**
 * 手动合并对话框
 */
private class ManualMergeDialog(
    project: Project,
    private val conflict: ConflictResolver.ConflictInfo
) : DialogWrapper(project) {

    private val fieldEditors = mutableMapOf<String, JComponent>()

    init {
        title = "手动合并 - ${conflict.testCaseId}"
        init()
        setOKButtonText("保存")
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 500)

        val fieldsPanel = JPanel()
        fieldsPanel.layout = BoxLayout(fieldsPanel, BoxLayout.Y_AXIS)

        // 创建字段编辑器
        createFieldEditors(fieldsPanel)

        panel.add(JBScrollPane(fieldsPanel), BorderLayout.CENTER)
        return panel
    }

    private fun createFieldEditors(container: JPanel) {
        val local = conflict.localVersion
        val remote = conflict.remoteVersion
        val base = conflict.baseVersion

        // ID（只读）
        addReadOnlyField(container, "ID", conflict.testCaseId)

        // 名称
        addEditableField(container, "名称", "name",
            local?.name, remote?.name, base?.name,
            conflict.conflictFields.contains("name"))

        // 优先级
        addEditableField(container, "优先级", "priority",
            local?.priority?.value, remote?.priority?.value, base?.priority?.value,
            conflict.conflictFields.contains("priority"))

        // 状态
        addEditableField(container, "状态", "status",
            local?.status?.value, remote?.status?.value, base?.status?.value,
            conflict.conflictFields.contains("status"))

        // 预期结果
        addEditableTextArea(container, "预期结果", "expected",
            local?.expected, remote?.expected, base?.expected,
            conflict.conflictFields.contains("expected"))

        // 测试步骤
        addEditableTextArea(container, "测试步骤", "steps",
            local?.getStepsAsString(), remote?.getStepsAsString(), base?.getStepsAsString(),
            conflict.conflictFields.contains("steps"))

        // 标签
        addEditableField(container, "标签", "tags",
            local?.getTagsAsString(), remote?.getTagsAsString(), base?.getTagsAsString(),
            conflict.conflictFields.contains("tags"))

        // 描述
        addEditableTextArea(container, "描述", "description",
            local?.description, remote?.description, base?.description,
            conflict.conflictFields.contains("description"))
    }

    private fun addReadOnlyField(container: JPanel, label: String, value: String) {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(5, 5, 5, 5)

        panel.add(JBLabel("$label: "), BorderLayout.WEST)
        panel.add(JTextField(value).apply { isEditable = false }, BorderLayout.CENTER)

        container.add(panel)
    }

    private fun addEditableField(
        container: JPanel,
        label: String,
        fieldName: String,
        local: String?,
        remote: String?,
        base: String?,
        isConflict: Boolean
    ) {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(5, 5, 5, 5)
        if (isConflict) {
            panel.background = Color(255, 240, 240)
        }

        // 标签
        val labelPanel = JPanel(BorderLayout())
        labelPanel.add(JBLabel("$label: "), BorderLayout.NORTH)

        // 显示三个版本
        val versionsPanel = JPanel(GridLayout(3, 2, 5, 2))
        versionsPanel.add(JBLabel("本地:").apply {
            if (isConflict) foreground = Color.RED
        })
        versionsPanel.add(JBLabel(local ?: "(无)"))
        versionsPanel.add(JBLabel("远程:"))
        versionsPanel.add(JBLabel(remote ?: "(无)"))
        versionsPanel.add(JBLabel("基础:"))
        versionsPanel.add(JBLabel(base ?: "(无)"))

        labelPanel.add(versionsPanel, BorderLayout.CENTER)
        panel.add(labelPanel, BorderLayout.WEST)

        // 编辑器
        val textField = JTextField(local ?: "")
        fieldEditors[fieldName] = textField
        panel.add(textField, BorderLayout.CENTER)

        container.add(panel)
    }

    private fun addEditableTextArea(
        container: JPanel,
        label: String,
        fieldName: String,
        local: String?,
        remote: String?,
        base: String?,
        isConflict: Boolean
    ) {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(5, 5, 5, 5)

        // 标签和版本信息
        val topPanel = JPanel(BorderLayout())
        topPanel.add(JBLabel("$label: ").apply {
            if (isConflict) foreground = Color.RED
        }, BorderLayout.WEST)

        // 版本信息
        val versionsText = buildString {
            append("本地: ${local?.take(30) ?: "(无)"}")
            if (local != null && local.length > 30) append("...")
            append(" | ")
            append("远程: ${remote?.take(30) ?: "(无)"}")
            if (remote != null && remote.length > 30) append("...")
        }
        topPanel.add(JBLabel(versionsText).apply {
            foreground = Color.GRAY
            font = font.deriveFont(10f)
        }, BorderLayout.EAST)

        panel.add(topPanel, BorderLayout.NORTH)

        // 文本区域
        val textArea = JTextArea(local ?: "", 4, 40)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        fieldEditors[fieldName] = textArea
        panel.add(JBScrollPane(textArea), BorderLayout.CENTER)

        container.add(panel)
    }

    fun getMergedTestCase(): TestCase {
        val testCase = TestCase(id = conflict.testCaseId)

        conflict.localVersion?.let { local ->
            testCase.name = (fieldEditors["name"] as? JTextField)?.text ?: local.name
            testCase.priority = local.priority
            testCase.status = local.status
            testCase.expected = (fieldEditors["expected"] as? JTextComponent)?.text ?: local.expected
            testCase.description = (fieldEditors["description"] as? JTextComponent)?.text ?: local.description

            val stepsText = (fieldEditors["steps"] as? JTextComponent)?.text ?: local.getStepsAsString()
            testCase.setStepsFromString(stepsText)

            val tagsText = (fieldEditors["tags"] as? JTextField)?.text ?: local.getTagsAsString()
            testCase.setTagsFromString(tagsText)
        }

        return testCase
    }
}
