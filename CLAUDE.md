# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指引。

## 语言
本项目始终使用简体中文思考和回答

## 常用命令

```bash
# 在沙箱 IDE 实例中运行插件
./gradlew runIde

# 运行测试
./gradlew test

# 验证插件兼容性
./gradlew verifyPlugin

# 构建插件发布包
./gradlew buildPlugin
```

## 架构

本项目是一个 **JetBrains IntelliJ Platform 插件**，目标平台为 PhpStorm（以及安装了 PHP 插件的 IntelliJ IDEA）。使用 Kotlin 开发，基于 [IntelliJ Platform Gradle Plugin v2](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)。

**`build.gradle.kts` 中声明的关键依赖：**
- 目标 IDE：IntelliJ IDEA `2025.3.5`
- 内置插件：`com.intellij.java`、`org.jetbrains.kotlin`、`JavaScript`、`com.intellij.modules.json`、`org.intellij.plugins.markdown`、`com.intellij.database`
- 兼容插件：`com.jetbrains.php`（PHP 插件 — 主要目标）

**入口：** `plugin.xml` 声明所有扩展点。当前通过 `MyToolWindowFactory` 注册了一个工具窗口（`MyToolWindow`）。

**源码结构：**
- `src/main/kotlin/` — Kotlin 插件代码（包名 `com.example`）
- `src/main/resources/META-INF/plugin.xml` — 插件清单及扩展声明
- `src/main/resources/messages/MyMessageBundle.properties` — UI 字符串（通过 `MyMessageBundle` 访问）

**插件 ID：** `com.example.PhpDebugTools` — 版本间不可更改。

添加新功能时，在 `plugin.xml` 对应的 `<extensions>` 块中注册。新的 UI 字符串写入 `MyMessageBundle.properties`，通过 `MyMessageBundle.message("key")` 访问。
