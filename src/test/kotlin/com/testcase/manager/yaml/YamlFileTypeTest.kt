package com.testcase.manager.yaml

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class YamlFileTypeTest : BasePlatformTestCase() {

    fun testFileTypeName() {
        val fileType = YamlFileType()
        assertEquals("TestCase YAML", fileType.name)
    }

    fun testFileTypeDescription() {
        val fileType = YamlFileType()
        assertEquals("TestCase YAML files", fileType.description)
    }

    fun testDefaultExtension() {
        val fileType = YamlFileType()
        assertEquals("yaml", fileType.defaultExtension)
    }

    fun testIconNotNull() {
        val fileType = YamlFileType()
        assertNotNull(fileType.icon)
    }
}
