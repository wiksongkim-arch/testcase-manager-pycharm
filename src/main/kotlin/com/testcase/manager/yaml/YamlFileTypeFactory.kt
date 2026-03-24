package com.testcase.manager.yaml

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

/**
 * YAML 文件类型工厂
 *
 * 用于注册 YAML 文件类型到 IntelliJ Platform。
 * 支持 .yaml 和 .yml 扩展名。
 */
class YamlFileTypeFactory : FileTypeFactory() {

    /**
     * 创建并注册文件类型
     *
     * @param consumer 文件类型消费者
     */
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(
            YamlFileType.INSTANCE,
            "yaml;yml"
        )
    }
}
