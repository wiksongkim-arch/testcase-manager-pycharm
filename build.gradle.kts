import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}

group = "com.testcase.manager"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("PY", "2023.3.5")
        bundledPlugins(
            listOf(
                "org.jetbrains.plugins.yaml"
            )
        )
        testFramework(TestFrameworkType.Platform)
    }
    
    implementation(libs.snakeyaml)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// 使用系统 Java，不强制 Toolchain 版本
// java {
//     toolchain {
//         languageVersion = JavaLanguageVersion.of(17)
//     }
// }

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    test {
        useJUnitPlatform()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "213"
            untilBuild = "242.*"
        }
    }
}
