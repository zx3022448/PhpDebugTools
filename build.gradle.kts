import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        phpstorm("2025.3.5")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("org.intellij.plugins.markdown")
        bundledPlugin("com.intellij.database")
        bundledPlugin("com.jetbrains.php")
    }
}

intellijPlatform {
    pluginVerification {
        freeArgs.set(listOf("-mute", "ForbiddenPluginIdPrefix", "-offline"))
        ides {
            local(intellijPlatform.platformPath.toFile())
        }
    }
}
