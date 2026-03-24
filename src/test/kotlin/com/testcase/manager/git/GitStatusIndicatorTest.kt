package com.testcase.manager.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.awt.Color

/**
 * GitStatusIndicator 测试类
 *
 * 测试 Git 状态指示器组件。
 */
class GitStatusIndicatorTest : BasePlatformTestCase() {

    private lateinit var mockProject: Project
    private lateinit var mockFile: VirtualFile

    @Before
    override fun setUp() {
        super.setUp()
        mockProject = mockk(relaxed = true)
        mockFile = mockk(relaxed = true)
    }

    @After
    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    /**
     * 测试 GitStatusIndicator 创建
     */
    @Test
    fun testGitStatusIndicatorCreation() {
        every { mockFile.path } returns "/test/file.yaml"

        val indicator = GitStatusIndicator(mockProject, mockFile)

        assertNotNull(indicator)
        assertNotNull(indicator.component)
    }

    /**
     * 测试 GitStatusBadge 创建
     */
    @Test
    fun testGitStatusBadgeCreation() {
        val badge = GitStatusBadge(mockProject)
        assertNotNull(badge)
    }

    /**
     * 测试 Git 状态图标
     */
    @Test
    fun testGitStatusIcons() {
        val modifiedIcon = GitStatusIcons.getIcon(GitFileStatus.MODIFIED)
        val addedIcon = GitStatusIcons.getIcon(GitFileStatus.ADDED)
        val deletedIcon = GitStatusIcons.getIcon(GitFileStatus.DELETED)

        assertNotNull(modifiedIcon)
        assertNotNull(addedIcon)
        assertNotNull(deletedIcon)

        assertEquals(12, modifiedIcon.iconWidth)
        assertEquals(12, modifiedIcon.iconHeight)
    }

    /**
     * 测试所有状态图标
     */
    @Test
    fun testAllStatusIcons() {
        GitFileStatus.values().forEach { status ->
            val icon = GitStatusIcons.getIcon(status)
            assertNotNull("Icon for $status should not be null", icon)
            assertTrue("Icon width should be positive", icon.iconWidth > 0)
            assertTrue("Icon height should be positive", icon.iconHeight > 0)
        }
    }
}
