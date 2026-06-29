# AGENTS.md

本文件为 Codex (Codex.ai/code) 在此仓库中工作时提供指引。

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

## 版本管理
1. **版本号格式**: `{主版本}.{次版本}.{修订版本}`，例如 `1.0.0`
2. **版本号更新**: 
   - 主版本：不兼容的 API 修改
   - 次版本：新增功能
   - 修订版本：向后兼容的 bug 修复
   - 每次更新：自动更新版本号


## 架构

本项目是一个 **JetBrains IntelliJ Platform 插件**，目标平台为 PhpStorm（以及安装了 PHP 插件的 IntelliJ IDEA）。使用 Kotlin 开发，基于 [IntelliJ Platform Gradle Plugin v2](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)。

**`build.gradle.kts` 中声明的关键依赖：**
- 目标 IDE：IntelliJ IDEA `2025.3.5`
- 内置插件：`com.intellij.java`、`org.jetbrains.kotlin`、`JavaScript`、`com.intellij.modules.json`、`org.intellij.plugins.markdown`、`com.intellij.database`
- 兼容插件：`com.jetbrains.php`（PHP 插件 — 主要目标）

**入口：** `plugin.xml` 声明所有扩展点。当前通过 `MyToolWindowFactory` 注册了一个工具窗口（`MyToolWindow`）。

**源码结构：**
- `src/main/kotlin/` — Kotlin 插件代码（包名 `com.zx3022448`）
- `src/main/resources/META-INF/plugin.xml` — 插件清单及扩展声明
- `src/main/resources/messages/MyMessageBundle.properties` — UI 字符串（通过 `MyMessageBundle` 访问）

**插件 ID：** `com.zx3022448.PhpDebugTools` — 版本间不可更改。

添加新功能时，在 `plugin.xml` 对应的 `<extensions>` 块中注册。新的 UI 字符串写入 `MyMessageBundle.properties`，通过 `MyMessageBundle.message("key")` 访问。

## 注释
- **代码注释**: 代码中重要的逻辑和关键点添加注释，解释为什么这样做
- **文档注释**: 重要的函数和方法添加文档注释，说明参数和返回值

## 需求文档
1. **文档位置**: `docs/{功能名}/` 目录下。
2. **命名规范**: `{日期}-{时分}-{功能名}-需求文档.md`
    - 日期格式：YYYYMMDD（例如：20260327）
    - 时分格式：HHMM（例如：1430）
    - 功能名：简短的中文功能描述
    - 示例：`20260327-1430-用户登录-需求文档.md`、`20260327-1530-项目查询-需求文档.md`
3. **文件格式**: Markdown (.md)

## 开发计划
1. **文档位置**: `docs/{功能名}/` 目录下。
2. **命名规范**: `{日期}-{时分}-{功能名}-开发计划.md`
    - 日期格式：YYYYMMDD（例如：20260327）
    - 时分格式：HHMM（例如：1430）F
    - 功能名：简短的中文功能描述
    - 示例：`20260327-1430-用户登录-开发计划.md`、`20260327-1530-项目查询-开发计划.md`
3. **文件格式**: Markdown (.md)


## 测试
1. 测试代码在任务结束之后删除
2. 测试文件在任务结束之后删除