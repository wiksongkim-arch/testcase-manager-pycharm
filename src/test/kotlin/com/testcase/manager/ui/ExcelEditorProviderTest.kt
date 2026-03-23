package com.testcase.manager.ui

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.Mockito.mock

class ExcelEditorProviderTest : BasePlatformTestCase() {

    fun testAcceptYamlFile() {
        val provider = ExcelEditorProvider()
        val yamlFile = myFixture.createFile("test.yaml", "test: data")
        
        assertTrue(provider.accept(project, yamlFile))
    }

    fun testAcceptYmlFile() {
        val provider = ExcelEditorProvider()
        val ymlFile = myFixture.createFile("test.yml", "test: data")
        
        assertTrue(provider.accept(project, ymlFile))
    }

    fun testRejectNonYamlFile() {
        val provider = ExcelEditorProvider()
        val txtFile = myFixture.createFile("test.txt", "text content")
        
        assertFalse(provider.accept(project, txtFile))
    }

    fun testEditorTypeId() {
        val provider = ExcelEditorProvider()
        assertEquals("testcase-excel-editor", provider.editorTypeId)
    }
}
