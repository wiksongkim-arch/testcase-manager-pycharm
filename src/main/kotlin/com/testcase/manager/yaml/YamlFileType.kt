package com.testcase.manager.yaml

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * YAML 文件类型定义
 *
 * 用于注册和识别测试用例 YAML 文件。
 * 实现 IntelliJ Platform 的 FileType 接口。
 */
class YamlFileType private constructor() : FileType {

    companion object {
        /** 单例实例 */
        @JvmStatic
        val INSTANCE = YamlFileType()

        /** 默认文件扩展名 */
        const val DEFAULT_EXTENSION = "yaml"

        /** 文件类型名称 */
        const val NAME = "TestCase YAML"

        /** 文件类型描述 */
        const val DESCRIPTION = "TestCase Manager YAML file"
    }

    /**
     * 获取文件类型名称
     *
     * @return 文件类型名称
     */
    override fun getName(): String = NAME

    /**
     * 获取文件类型描述
     *
     * @return 文件类型描述
     */
    override fun getDescription(): String = DESCRIPTION

    /**
     * 获取默认文件扩展名
     *
     * @return 默认扩展名
     */
    override fun getDefaultExtension(): String = DEFAULT_EXTENSION

    /**
     * 获取文件类型图标
     *
     * @return 图标对象，如果加载失败返回 null
     */
    override fun getIcon(): Icon? = try {
        IconLoader.getIcon(
            "/icons/testcase-yaml.svg",
            YamlFileType::class.java
        )
    } catch (e: Exception) {
        null
    }

    /**
     * 是否为二进制文件
     *
     * @return 始终返回 false
     */
    override fun isBinary(): Boolean = false

    /**
     * 是否为只读文件
     *
     * @return 始终返回 false
     */
    override fun isReadOnly(): Boolean = false

    /**
     * 获取文件字符集
     *
     * @param file 虚拟文件
     * @param content 文件内容字节数组
     * @return 字符集名称
     */
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = "UTF-8"
}
