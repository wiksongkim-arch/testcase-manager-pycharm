package com.testcase.manager

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * TestCase Manager 插件主入口类
 *
 * 负责插件的初始化和生命周期管理，在 IDE 启动时自动加载。
 *
 * @property PLUGIN_ID 插件唯一标识符
 * @property PLUGIN_NAME 插件显示名称
 */
class TestCaseManagerPlugin : StartupActivity {

    companion object {
        /** 插件唯一标识符 */
        const val PLUGIN_ID = "com.testcase.manager"

        /** 插件显示名称 */
        const val PLUGIN_NAME = "TestCase Manager"

        /** 日志记录器实例 */
        private val LOG = Logger.getInstance(TestCaseManagerPlugin::class.java)
    }

    /**
     * 执行插件启动活动
     *
     * @param project 当前打开的项目实例
     */
    override fun runActivity(project: Project) {
        LOG.info("$PLUGIN_NAME 插件已启动")
    }
}
