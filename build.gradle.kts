import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
                "org.jetbrains.plugins.yaml",
                "Git4Idea"
            )
        )
        testFramework(TestFrameworkType.Platform)
    }
    
    implementation(libs.snakeyaml)
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    test {
        useJUnitPlatform()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "223"
            untilBuild = "242.*"
        }
    }
}
