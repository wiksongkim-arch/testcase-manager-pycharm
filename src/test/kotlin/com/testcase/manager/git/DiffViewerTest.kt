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

/**
 * DiffViewer 测试类
 *
 * 测试 Diff 查看器功能。
 */
class DiffViewerTest : BasePlatformTestCase() {

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
     * 测试 DiffViewer 创建
     */
    @Test
    fun testDiffViewerCreation() {
        every { mockFile.path } returns "/test/file.yaml"
        every { mockFile.name } returns "file.yaml"
        every { mockFile.isValid } returns true

        val diffViewer = DiffViewer(mockProject, mockFile)

        assertNotNull(diffViewer)
        assertNotNull(diffViewer.component)
        assertEquals("Diff Viewer", diffViewer.name)
        assertTrue(diffViewer.isValid)
        assertFalse(diffViewer.isModified)
    }

    /**
     * 测试 DiffViewer 编辑器名称
     */
    @Test
    fun testDiffViewerEditorName() {
        every { mockFile.isValid } returns true

        val diffViewer = DiffViewer(mockProject, mockFile)
        assertEquals("Diff Viewer", diffViewer.name)
    }

    /**
     * 测试 DiffRequestHelper 创建
     */
    @Test
    fun testDiffRequestHelperCreation() {
        val helper = DiffRequestHelper(mockProject)
        assertNotNull(helper)
    }
}
