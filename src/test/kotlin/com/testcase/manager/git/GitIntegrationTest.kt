package com.testcase.manager.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * GitIntegration 测试类
 *
 * 测试 Git 集成功能，包括状态检测、版本获取等。
 */
class GitIntegrationTest : BasePlatformTestCase() {

    private lateinit var gitIntegration: GitIntegration
    private lateinit var mockProject: Project
    private lateinit var mockFile: VirtualFile

    @Before
    override fun setUp() {
        super.setUp()
        mockProject = mockk(relaxed = true)
        mockFile = mockk(relaxed = true)
        gitIntegration = GitIntegration(mockProject)
    }

    @After
    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    /**
     * 测试 Git 状态枚举
     */
    @Test
    fun testGitFileStatusEnum() {
        // 验证所有状态值
        assertEquals(11, GitFileStatus.values().size)
        assertNotNull(GitFileStatus.UNCHANGED)
        assertNotNull(GitFileStatus.MODIFIED)
        assertNotNull(GitFileStatus.ADDED)
        assertNotNull(GitFileStatus.DELETED)
        assertNotNull(GitFileStatus.UNTRACKED)
        assertNotNull(GitFileStatus.IGNORED)
        assertNotNull(GitFileStatus.CONFLICT)
        assertNotNull(GitFileStatus.MERGED)
        assertNotNull(GitFileStatus.UNKNOWN)
        assertNotNull(GitFileStatus.NOT_TRACKED)
    }

    /**
     * 测试 GitCommitInfo 数据类
     */
    @Test
    fun testGitCommitInfo() {
        val commit = GitCommitInfo(
            hash = "abc123def456",
            authorName = "Test User",
            authorEmail = "test@example.com",
            date = "2024-01-15 10:30:00",
            subject = "Test commit message"
        )

        assertEquals("abc123def456", commit.hash)
        assertEquals("Test User", commit.authorName)
        assertEquals("test@example.com", commit.authorEmail)
        assertEquals("2024-01-15 10:30:00", commit.date)
        assertEquals("Test commit message", commit.subject)
    }

    /**
     * 测试 Git 状态颜色
     */
    @Test
    fun testGetStatusColor() {
        every { mockFile.path } returns "/test/file.yaml"
        every { mockFile.isValid } returns true

        // 由于无法完全模拟 Git 环境，测试颜色返回不为 null
        val color = gitIntegration.getStatusColor(mockFile)
        assertNotNull(color)
    }

    /**
     * 测试状态指示器文本
     */
    @Test
    fun testGetStatusIndicator() {
        every { mockFile.path } returns "/test/file.yaml"
        every { mockFile.isValid } returns true

        val indicator = gitIntegration.getStatusIndicator(mockFile)
        assertNotNull(indicator)
        // 未在 Git 控制下的文件返回空字符串
        assertEquals("", indicator)
    }

    /**
     * 测试 GitIntegration 实例获取
     */
    @Test
    fun testGetInstance() {
        val instance = GitIntegration.getInstance(mockProject)
        assertNotNull(instance)
    }

    /**
     * 测试文件不在 Git 控制下的情况
     */
    @Test
    fun testFileNotUnderGit() {
        every { mockFile.path } returns "/test/file.yaml"

        assertFalse(gitIntegration.isFileUnderGit(mockFile))
        assertEquals(GitFileStatus.NOT_TRACKED, gitIntegration.getFileStatus(mockFile))
    }

    /**
     * 测试未启用 Git 的情况
     */
    @Test
    fun testGitNotEnabled() {
        assertFalse(gitIntegration.isGitEnabled())
    }
}
