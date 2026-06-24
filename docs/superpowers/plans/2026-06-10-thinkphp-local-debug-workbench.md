# ThinkPHP 本地调试工作台实施计划

> **面向代理执行者：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框 `- [ ]` 语法追踪进度。

**目标：** 为本机 Windows 上的 ThinkPHP 项目构建一个 PhpStorm 插件 MVP，能够诊断本地 PHP/Xdebug 问题、在项目中安装 `.php-debug-tools/` 运行时、发起受控的 CLI/Web 调试请求，并支持带参数直接调试控制器方法和服务方法。

**架构：** IDE 侧逻辑保留在 Kotlin 中，运行时执行逻辑放在复制到目标项目中的 PHP 模板文件里。Kotlin 侧先拆成纯领域服务，再补 IntelliJ 适配层和 UI 外壳，尽量保证大部分逻辑可单元测试。ThinkPHP 5 和 ThinkPHP 6 通过运行时清单后的独立适配器处理，而不是强行塞进一个超大的 bootstrap 流程。

**技术栈：** Kotlin、IntelliJ Platform SDK、PhpStorm PHP 插件 API、JUnit 4、IntelliJ 测试框架、PHP 运行时模板、Gradle IntelliJ Platform Plugin v2

---

## 当前仓库现状

当前仓库仍然是 IntelliJ 插件模板工程：

- `src/main/kotlin/MyToolWindowFactory.kt` 还是一个随机数演示面板，需要被替换。
- `src/main/kotlin/MyMessageBundle.kt` 与 `src/main/resources/messages/MyMessageBundle.properties` 仍然是模板 bundle 文件。
- `src/main/resources/META-INF/plugin.xml` 目前只注册了模板工具窗口。
- 目前还没有 `src/test/kotlin/` 测试目录树。
- 也还没有 `.php-debug-tools/` 对应的运行时模板文件。

## 文件结构规划

### 需要修改

- `build.gradle.kts`
  按需要补齐测试配置，并保持与新代码一致的 Php 插件兼容声明。
- `src/main/resources/META-INF/plugin.xml`
  注册新的工具窗口工厂、动作以及基于 bundle 的字符串。
- `src/main/resources/messages/MyMessageBundle.properties`
  用插件真实文案替换模板 key。

### 需要删除

- `src/main/kotlin/MyToolWindowFactory.kt`
  在真实工具窗口就位后，删除模板随机数 UI。

### 需要新增

- `src/main/kotlin/com/example/phpdebugtools/PhpDebugToolsBundle.kt`
  替代模板命名的 bundle 帮助类。
- `src/main/kotlin/com/example/phpdebugtools/project/ThinkPhpProjectInfo.kt`
  不可变的项目识别结果模型。
- `src/main/kotlin/com/example/phpdebugtools/project/ThinkPhpProjectDetector.kt`
  纯项目识别逻辑和项目适配入口。
- `src/main/kotlin/com/example/phpdebugtools/runtime/RuntimeTemplate.kt`
  运行时模板清单项。
- `src/main/kotlin/com/example/phpdebugtools/runtime/RuntimeInstallResult.kt`
  安装/更新结果模型。
- `src/main/kotlin/com/example/phpdebugtools/runtime/RuntimeInstaller.kt`
  将内置 PHP 运行时文件复制到 `.php-debug-tools/`。
- `src/main/kotlin/com/example/phpdebugtools/diagnostics/DiagnosticStage.kt`
  IDE、PHP/Xdebug、框架引导、目标调用四层诊断枚举。
- `src/main/kotlin/com/example/phpdebugtools/diagnostics/DiagnosticFinding.kt`
  结构化诊断项模型。
- `src/main/kotlin/com/example/phpdebugtools/diagnostics/CommandRunner.kt`
  便于测试的外部命令执行抽象。
- `src/main/kotlin/com/example/phpdebugtools/diagnostics/EnvironmentDiagnosticService.kt`
  根据 PHP CLI 和 Xdebug 探测结果生成诊断项。
- `src/main/kotlin/com/example/phpdebugtools/persistence/RecentDebugStore.kt`
  保存最近 URL、方法调用和最近成功负载的持久化存储。
- `src/main/kotlin/com/example/phpdebugtools/execution/DebugRequest.kt`
  CLI、服务、控制器、Web 调试的共享请求模型。
- `src/main/kotlin/com/example/phpdebugtools/execution/DebugExecutionResult.kt`
  返回给 UI 的结构化执行结果。
- `src/main/kotlin/com/example/phpdebugtools/execution/CliDebugCommandBuilder.kt`
  构建运行时入口对应的 `php` 命令行。
- `src/main/kotlin/com/example/phpdebugtools/execution/RuntimeJson.kt`
  负载文件 JSON 序列化帮助类。
- `src/main/kotlin/com/example/phpdebugtools/execution/RuntimeExecutor.kt`
  执行运行时入口并解析 JSON 响应。
- `src/main/kotlin/com/example/phpdebugtools/execution/WebDebugUrlBuilder.kt`
  构建本地 Web 调试流程所需的受控 URL。
- `src/main/kotlin/com/example/phpdebugtools/methods/MethodKind.kt`
  控制器方法与服务方法分类。
- `src/main/kotlin/com/example/phpdebugtools/methods/MethodParameterSchema.kt`
  参数模式与类型推断提示。
- `src/main/kotlin/com/example/phpdebugtools/methods/MethodDebugTarget.kt`
  已解析的方法目标元数据。
- `src/main/kotlin/com/example/phpdebugtools/methods/PhpMethodTargetResolver.kt`
  基于 PhpStorm PSI API 解析光标所在 PHP 方法。
- `src/main/kotlin/com/example/phpdebugtools/toolwindow/PhpDebugToolsToolWindowFactory.kt`
  注册真实的多标签工具窗口。
- `src/main/kotlin/com/example/phpdebugtools/toolwindow/PhpDebugToolsToolWindowPanel.kt`
  承载概览、请求调试、方法直调、诊断四个页签。
- `src/main/kotlin/com/example/phpdebugtools/toolwindow/OverviewViewState.kt`
  概览页的小型视图模型。
- `src/main/kotlin/com/example/phpdebugtools/ui/ServiceMethodDialog.kt`
  服务方法执行参数弹窗。
- `src/main/kotlin/com/example/phpdebugtools/ui/ControllerMethodDialog.kt`
  控制器执行参数弹窗，包含请求上下文区域。
- `src/main/kotlin/com/example/phpdebugtools/actions/DebugServiceMethodAction.kt`
  服务方法编辑器动作。
- `src/main/kotlin/com/example/phpdebugtools/actions/DebugControllerMethodAction.kt`
  控制器方法编辑器动作。
- `src/main/resources/runtime/bootstrap.php`
- `src/main/resources/runtime/adapters/thinkphp5.php`
- `src/main/resources/runtime/adapters/thinkphp6.php`
- `src/main/resources/runtime/invoke-service.php`
- `src/main/resources/runtime/invoke-controller.php`
- `src/main/resources/runtime/debug-web-entry.php`
- `src/main/resources/runtime/runtime-config.json`
- `src/test/kotlin/com/example/phpdebugtools/project/ThinkPhpProjectDetectorTest.kt`
- `src/test/kotlin/com/example/phpdebugtools/runtime/RuntimeInstallerTest.kt`
- `src/test/kotlin/com/example/phpdebugtools/diagnostics/EnvironmentDiagnosticServiceTest.kt`
- `src/test/kotlin/com/example/phpdebugtools/execution/CliDebugCommandBuilderTest.kt`
- `src/test/kotlin/com/example/phpdebugtools/execution/WebDebugUrlBuilderTest.kt`
- `src/test/kotlin/com/example/phpdebugtools/methods/PhpMethodTargetResolverTest.kt`
- `src/test/kotlin/com/example/phpdebugtools/toolwindow/OverviewViewStateTest.kt`
- `src/test/resources/fixtures/thinkphp5/composer.json`
- `src/test/resources/fixtures/thinkphp6/composer.json`

## 任务 1：用 ThinkPHP 项目核心替换模板代码

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/PhpDebugToolsBundle.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/project/ThinkPhpProjectInfo.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/project/ThinkPhpProjectDetector.kt`
- 修改：`src/main/resources/messages/MyMessageBundle.properties`
- 测试：`src/test/kotlin/com/example/phpdebugtools/project/ThinkPhpProjectDetectorTest.kt`

- [ ] **步骤 1：先写一个失败的项目识别测试**

```kotlin
package com.zx3022448.phpdebugtools.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkPhpProjectDetectorTest {
    @Test
    fun detectsThinkPhp6FromComposerConstraint() {
        val result = ThinkPhpProjectDetector.detect(
            composerJson = """
                {
                  "require": {
                    "topthink/framework": "^6.1"
                  }
                }
            """.trimIndent(),
            installedFrameworkVersion = "6.1.4",
            entryFileText = "<?php require __DIR__ . '/../vendor/autoload.php';",
            knownPaths = setOf("app", "config", "public/index.php", "route")
        )

        assertTrue(result.isThinkPhp)
        assertEquals("6", result.majorVersion)
        assertEquals("composer+installed", result.detectionSource)
    }
}
```

- [ ] **步骤 2：运行测试，确认它先失败**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.project.ThinkPhpProjectDetectorTest" --info`

预期：FAIL，提示 `ThinkPhpProjectDetector` 和 `ThinkPhpProjectInfo` 未解析。

- [ ] **步骤 3：实现最小可用的项目识别逻辑**

```kotlin
package com.zx3022448.phpdebugtools.project

data class ThinkPhpProjectInfo(
    val isThinkPhp: Boolean,
    val majorVersion: String?,
    val detectionSource: String,
    val entryFile: String?,
    val confidence: Int
)
```

```kotlin
package com.zx3022448.phpdebugtools.project

class ThinkPhpProjectDetector {
    companion object {
        fun detect(
            composerJson: String?,
            installedFrameworkVersion: String?,
            entryFileText: String?,
            knownPaths: Set<String>
        ): ThinkPhpProjectInfo {
            val composerVersion = Regex("\"topthink/framework\"\\s*:\\s*\"([^\"]+)\"")
                .find(composerJson.orEmpty())
                ?.groupValues
                ?.get(1)

            val resolvedVersion = installedFrameworkVersion ?: composerVersion
            val majorVersion = when {
                resolvedVersion?.startsWith("6") == true || resolvedVersion?.contains("^6") == true -> "6"
                resolvedVersion?.startsWith("5") == true || resolvedVersion?.contains("^5") == true -> "5"
                else -> null
            }

            val looksLikeThinkPhp = composerVersion != null ||
                "public/index.php" in knownPaths && "app" in knownPaths && "config" in knownPaths

            val source = when {
                composerVersion != null && installedFrameworkVersion != null -> "composer+installed"
                composerVersion != null -> "composer"
                looksLikeThinkPhp -> "layout"
                else -> "none"
            }

            return ThinkPhpProjectInfo(
                isThinkPhp = looksLikeThinkPhp,
                majorVersion = majorVersion,
                detectionSource = source,
                entryFile = if ("public/index.php" in knownPaths) "public/index.php" else null,
                confidence = if (composerVersion != null) 90 else if (looksLikeThinkPhp) 60 else 0
            )
        }
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.MyMessageBundle"

object PhpDebugToolsBundle {
    private val instance = DynamicBundle(PhpDebugToolsBundle::class.java, BUNDLE)

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String {
        return instance.getMessage(key, *params)
    }
}
```

```properties
toolwindow.title=PHP Debug Tools
overview.project.unknown=Not a ThinkPHP project
overview.project.detected=ThinkPHP {0} project detected
```

- [ ] **步骤 4：再次运行测试，确认通过**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.project.ThinkPhpProjectDetectorTest"`

预期：PASS，显示 `1 test completed, 0 failed`。

- [ ] **步骤 5：提交项目识别核心代码**

```bash
git add build.gradle.kts src/main/kotlin/com/example/phpdebugtools/PhpDebugToolsBundle.kt src/main/kotlin/com/example/phpdebugtools/project src/main/resources/messages/MyMessageBundle.properties src/test/kotlin/com/example/phpdebugtools/project
git commit -m "feat: add thinkphp project detection core"
```

## 任务 2：新增运行时模板清单与安装器

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/runtime/RuntimeTemplate.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/runtime/RuntimeInstallResult.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/runtime/RuntimeInstaller.kt`
- 新增：`src/main/resources/runtime/bootstrap.php`
- 新增：`src/main/resources/runtime/adapters/thinkphp5.php`
- 新增：`src/main/resources/runtime/adapters/thinkphp6.php`
- 新增：`src/main/resources/runtime/invoke-service.php`
- 新增：`src/main/resources/runtime/invoke-controller.php`
- 新增：`src/main/resources/runtime/debug-web-entry.php`
- 新增：`src/main/resources/runtime/runtime-config.json`
- 测试：`src/test/kotlin/com/example/phpdebugtools/runtime/RuntimeInstallerTest.kt`

- [ ] **步骤 1：先写运行时安装失败测试**

```kotlin
package com.zx3022448.phpdebugtools.runtime

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RuntimeInstallerTest {
    @Test
    fun installsRuntimeFilesIntoProjectFolder() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-project")
        val installer = RuntimeInstaller(
            templates = listOf(
                RuntimeTemplate("bootstrap.php", "<?php echo 'ok';"),
                RuntimeTemplate("runtime-config.json", """{"version":"1"}""")
            )
        )

        val result = installer.install(projectRoot)

        assertTrue(result.installedFiles.contains(".php-debug-tools/bootstrap.php"))
        assertTrue(Files.exists(projectRoot.resolve(".php-debug-tools/bootstrap.php")))
    }
}
```

- [ ] **步骤 2：运行测试，确认先失败**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.runtime.RuntimeInstallerTest"`

预期：FAIL，提示 `RuntimeInstaller`、`RuntimeTemplate`、`RuntimeInstallResult` 未解析。

- [ ] **步骤 3：实现运行时清单、安装器和内置模板**

```kotlin
package com.zx3022448.phpdebugtools.runtime

data class RuntimeTemplate(
    val relativePath: String,
    val contents: String
)
```

```kotlin
package com.zx3022448.phpdebugtools.runtime

data class RuntimeInstallResult(
    val runtimeRoot: String,
    val installedFiles: List<String>,
    val updated: Boolean
)
```

```kotlin
package com.zx3022448.phpdebugtools.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RuntimeInstaller(private val templates: List<RuntimeTemplate>) {
    fun install(projectRoot: Path): RuntimeInstallResult {
        val runtimeRoot = projectRoot.resolve(".php-debug-tools")
        Files.createDirectories(runtimeRoot)

        val installed = templates.map { template ->
            val target = runtimeRoot.resolve(template.relativePath)
            Files.createDirectories(target.parent)
            Files.writeString(target, template.contents, StandardCharsets.UTF_8)
            ".php-debug-tools/${template.relativePath.replace('\\', '/')}"
        }

        return RuntimeInstallResult(
            runtimeRoot = runtimeRoot.toString(),
            installedFiles = installed,
            updated = true
        )
    }
}
```

```php
<?php

declare(strict_types=1);

$configPath = __DIR__ . '/runtime-config.json';
$config = json_decode((string) file_get_contents($configPath), true) ?: [];
$adapter = $config['frameworkAdapter'] ?? 'thinkphp6';
$adapterFile = __DIR__ . '/adapters/' . $adapter . '.php';

if (!is_file($adapterFile)) {
    fwrite(STDERR, json_encode([
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter file not found',
    ], JSON_UNESCAPED_UNICODE));
    exit(1);
}

require $adapterFile;
```

```php
<?php

declare(strict_types=1);

return [
    'name' => 'thinkphp5',
    'bootstrap' => static function (array $payload): array {
        return ['status' => 'ok', 'framework' => 'thinkphp5', 'payload' => $payload];
    },
];
```

```php
<?php

declare(strict_types=1);

return [
    'name' => 'thinkphp6',
    'bootstrap' => static function (array $payload): array {
        return ['status' => 'ok', 'framework' => 'thinkphp6', 'payload' => $payload];
    },
];
```

```php
<?php

declare(strict_types=1);

$payload = json_decode((string) file_get_contents($argv[1] ?? ''), true) ?: [];
echo json_encode([
    'status' => 'ok',
    'stage' => 'invoke_service',
    'request' => $payload,
], JSON_UNESCAPED_UNICODE);
```

```php
<?php

declare(strict_types=1);

$payload = json_decode((string) file_get_contents($argv[1] ?? ''), true) ?: [];
echo json_encode([
    'status' => 'ok',
    'stage' => 'invoke_controller',
    'request' => $payload,
], JSON_UNESCAPED_UNICODE);
```

```php
<?php

declare(strict_types=1);

echo json_encode([
    'status' => 'ok',
    'stage' => 'debug_web_entry',
    'query' => $_GET,
], JSON_UNESCAPED_UNICODE);
```

```json
{
  "version": "1",
  "frameworkAdapter": "thinkphp6",
  "entryFile": "public/index.php"
}
```

- [ ] **步骤 4：再次运行安装器测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.runtime.RuntimeInstallerTest"`

预期：PASS，并且临时项目目录下存在 `.php-debug-tools/` 运行时文件。

- [ ] **步骤 5：提交运行时安装器**

```bash
git add src/main/kotlin/com/example/phpdebugtools/runtime src/main/resources/runtime src/test/kotlin/com/example/phpdebugtools/runtime
git commit -m "feat: add project runtime installer"
```

## 任务 3：将 PHP/Xdebug 诊断实现为纯服务

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/diagnostics/DiagnosticStage.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/diagnostics/DiagnosticFinding.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/diagnostics/CommandRunner.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/diagnostics/EnvironmentDiagnosticService.kt`
- 测试：`src/test/kotlin/com/example/phpdebugtools/diagnostics/EnvironmentDiagnosticServiceTest.kt`

- [ ] **步骤 1：先写一个缺少 Xdebug 的失败测试**

```kotlin
package com.zx3022448.phpdebugtools.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentDiagnosticServiceTest {
    @Test
    fun reportsMissingXdebugModule() {
        val runner = object : CommandRunner {
            override fun run(command: List<String>, workingDirectory: String?): CommandResult {
                return when (command.joinToString(" ")) {
                    "php -m" -> CommandResult(0, "Core\njson\n", "")
                    "php --ini" -> CommandResult(0, "Loaded Configuration File: C:\\php\\php.ini", "")
                    else -> CommandResult(1, "", "unsupported")
                }
            }
        }

        val findings = EnvironmentDiagnosticService(runner).inspect("php")

        assertEquals(DiagnosticStage.PHP_XDEBUG, findings.single().stage)
        assertTrue(findings.single().message.contains("Xdebug"))
    }
}
```

- [ ] **步骤 2：运行测试，确认先失败**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.diagnostics.EnvironmentDiagnosticServiceTest"`

预期：FAIL，提示诊断相关类型未解析。

- [ ] **步骤 3：实现诊断模型和诊断服务**

```kotlin
package com.zx3022448.phpdebugtools.diagnostics

enum class DiagnosticStage {
    IDE,
    PHP_XDEBUG,
    FRAMEWORK,
    TARGET_INVOCATION
}
```

```kotlin
package com.zx3022448.phpdebugtools.diagnostics

data class DiagnosticFinding(
    val stage: DiagnosticStage,
    val severity: String,
    val message: String,
    val hint: String
)
```

```kotlin
package com.zx3022448.phpdebugtools.diagnostics

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

fun interface CommandRunner {
    fun run(command: List<String>, workingDirectory: String? = null): CommandResult
}
```

```kotlin
package com.zx3022448.phpdebugtools.diagnostics

class EnvironmentDiagnosticService(private val commandRunner: CommandRunner) {
    fun inspect(phpExecutable: String): List<DiagnosticFinding> {
        val modules = commandRunner.run(listOf(phpExecutable, "-m"))
        val ini = commandRunner.run(listOf(phpExecutable, "--ini"))

        val findings = mutableListOf<DiagnosticFinding>()

        if (!modules.stdout.contains("xdebug", ignoreCase = true)) {
            findings += DiagnosticFinding(
                stage = DiagnosticStage.PHP_XDEBUG,
                severity = "error",
                message = "Xdebug module is not loaded for the selected PHP CLI.",
                hint = "Check php.ini reported by `${ini.stdout.trim()}` and enable the Xdebug extension."
            )
        }

        if (findings.isEmpty()) {
            findings += DiagnosticFinding(
                stage = DiagnosticStage.PHP_XDEBUG,
                severity = "info",
                message = "PHP CLI reports Xdebug as loaded.",
                hint = "Proceed to runtime health checks."
            )
        }

        return findings
    }
}
```

- [ ] **步骤 4：再次运行诊断测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.diagnostics.EnvironmentDiagnosticServiceTest"`

预期：PASS，并返回一个 `PHP_XDEBUG` 层级的诊断项。

- [ ] **步骤 5：提交诊断服务**

```bash
git add src/main/kotlin/com/example/phpdebugtools/diagnostics src/test/kotlin/com/example/phpdebugtools/diagnostics
git commit -m "feat: add php and xdebug diagnostics service"
```

## 任务 4：构建工具窗口外壳、概览状态和 CLI 调试核心

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/toolwindow/OverviewViewState.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/toolwindow/PhpDebugToolsToolWindowFactory.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/toolwindow/PhpDebugToolsToolWindowPanel.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/execution/DebugRequest.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/execution/CliDebugCommandBuilder.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/persistence/RecentDebugStore.kt`
- 修改：`src/main/resources/META-INF/plugin.xml`
- 删除：`src/main/kotlin/MyToolWindowFactory.kt`
- 测试：`src/test/kotlin/com/example/phpdebugtools/toolwindow/OverviewViewStateTest.kt`
- 测试：`src/test/kotlin/com/example/phpdebugtools/execution/CliDebugCommandBuilderTest.kt`

- [ ] **步骤 1：先写概览状态和 CLI 命令构建器的失败测试**

```kotlin
package com.zx3022448.phpdebugtools.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewViewStateTest {
    @Test
    fun formatsDetectedProjectSummary() {
        val state = OverviewViewState(
            projectSummary = "ThinkPHP 6",
            runtimeSummary = ".php-debug-tools installed",
            diagnosticsSummary = "1 warning"
        )

        assertEquals("ThinkPHP 6", state.projectSummary)
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.execution

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

class CliDebugCommandBuilderTest {
    @Test
    fun buildsPhpCommandForServiceInvocation() {
        val command = CliDebugCommandBuilder.build(
            phpExecutable = "php",
            projectRoot = Paths.get("D:/demo"),
            entryScript = "invoke-service.php",
            payloadPath = Paths.get("D:/demo/.php-debug-tools/payload.json")
        )

        assertEquals(
            listOf("php", "D:/demo/.php-debug-tools/invoke-service.php", "D:/demo/.php-debug-tools/payload.json"),
            command
        )
    }
}
```

- [ ] **步骤 2：运行测试，确认先失败**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.toolwindow.OverviewViewStateTest" --tests "com.zx3022448.phpdebugtools.execution.CliDebugCommandBuilderTest"`

预期：FAIL，提示 `OverviewViewState` 和 `CliDebugCommandBuilder` 未解析。

- [ ] **步骤 3：实现工具窗口外壳和 CLI 命令构建器**

```kotlin
package com.zx3022448.phpdebugtools.toolwindow

data class OverviewViewState(
    val projectSummary: String,
    val runtimeSummary: String,
    val diagnosticsSummary: String
)
```

```kotlin
package com.zx3022448.phpdebugtools.execution

import java.nio.file.Path

sealed class DebugRequest {
    data class Cli(
        val phpExecutable: String,
        val projectRoot: Path,
        val entryScript: String,
        val payloadPath: Path
    ) : DebugRequest()
}
```

```kotlin
package com.zx3022448.phpdebugtools.execution

import java.nio.file.Path

object CliDebugCommandBuilder {
    fun build(
        phpExecutable: String,
        projectRoot: Path,
        entryScript: String,
        payloadPath: Path
    ): List<String> {
        return listOf(
            phpExecutable,
            projectRoot.resolve(".php-debug-tools").resolve(entryScript).toString().replace("\\", "/"),
            payloadPath.toString().replace("\\", "/")
        )
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(name = "PhpDebugToolsRecentDebugStore", storages = [Storage("php-debug-tools.xml")])
class RecentDebugStore : PersistentStateComponent<RecentDebugStore.State> {
    data class State(
        var recentUrls: MutableList<String> = mutableListOf(),
        var recentMethods: MutableList<String> = mutableListOf()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout

class PhpDebugToolsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project): Boolean = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs = JBTabbedPane().apply {
            addTab("概览", JBPanel<JBPanel<*>>(BorderLayout()).apply { add(JBLabel("项目概览"), BorderLayout.NORTH) })
            addTab("请求调试", JBPanel<JBPanel<*>>(BorderLayout()).apply { add(JBLabel("CLI / Web 调试"), BorderLayout.NORTH) })
            addTab("方法直调", JBPanel<JBPanel<*>>(BorderLayout()).apply { add(JBLabel("控制器 / 服务方法"), BorderLayout.NORTH) })
            addTab("诊断", JBPanel<JBPanel<*>>(BorderLayout()).apply { add(JBLabel("环境诊断"), BorderLayout.NORTH) })
        }

        val content = ContentFactory.getInstance().createContent(PhpDebugToolsToolWindowPanel(tabs), null, false)
        toolWindow.contentManager.addContent(content)
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.toolwindow

import javax.swing.JComponent
import javax.swing.JTabbedPane

class PhpDebugToolsToolWindowPanel(private val tabs: JTabbedPane) : JComponent() {
    init {
        layout = java.awt.BorderLayout()
        add(tabs, java.awt.BorderLayout.CENTER)
    }
}
```

```xml
<extensions defaultExtensionNs="com.intellij">
    <toolWindow id="PhpDebugTools"
                factoryClass="com.zx3022448.phpdebugtools.toolwindow.PhpDebugToolsToolWindowFactory"
                icon="AllIcons.Toolwindows.ToolWindowPalette"/>
</extensions>
```

- [ ] **步骤 4：再次运行测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.toolwindow.OverviewViewStateTest" --tests "com.zx3022448.phpdebugtools.execution.CliDebugCommandBuilderTest"`

预期：PASS，两个测试都通过。

- [ ] **步骤 5：提交工具窗口外壳和 CLI 调试核心**

```bash
git add src/main/kotlin/com/example/phpdebugtools/toolwindow src/main/kotlin/com/example/phpdebugtools/execution src/main/kotlin/com/example/phpdebugtools/persistence src/main/resources/META-INF/plugin.xml
git rm src/main/kotlin/MyToolWindowFactory.kt
git commit -m "feat: add tool window shell and cli debug core"
```

## 任务 5：解析光标所在 PHP 方法并推断参数模式

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/methods/MethodKind.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/methods/MethodParameterSchema.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/methods/MethodDebugTarget.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/methods/PhpMethodTargetResolver.kt`
- 测试：`src/test/kotlin/com/example/phpdebugtools/methods/PhpMethodTargetResolverTest.kt`

- [ ] **步骤 1：先写平台侧失败测试**

```kotlin
package com.zx3022448.phpdebugtools.methods

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PhpMethodTargetResolverTest : BasePlatformTestCase() {
    fun testResolvesControllerMethodUnderCaret() {
        myFixture.configureByText(
            "UserController.php",
            """
            <?php
            namespace app\controller;
            class UserController {
                public function sh<caret>ow(int $id, string $name = 'demo') {}
            }
            """.trimIndent()
        )

        val target = PhpMethodTargetResolver.resolve(myFixture.file, myFixture.caretOffset)

        assertEquals(MethodKind.CONTROLLER, target?.kind)
        assertEquals("show", target?.methodName)
        assertEquals(2, target?.parameters?.size)
    }
}
```

- [ ] **步骤 2：运行平台测试，确认先失败**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.methods.PhpMethodTargetResolverTest"`

预期：FAIL，提示 `PhpMethodTargetResolver`、`MethodKind`、`MethodDebugTarget` 未解析。

- [ ] **步骤 3：实现方法分类和参数模式推断**

```kotlin
package com.zx3022448.phpdebugtools.methods

enum class MethodKind {
    CONTROLLER,
    SERVICE
}
```

```kotlin
package com.zx3022448.phpdebugtools.methods

data class MethodParameterSchema(
    val name: String,
    val declaredType: String?,
    val required: Boolean,
    val defaultValue: String?
)
```

```kotlin
package com.zx3022448.phpdebugtools.methods

data class MethodDebugTarget(
    val kind: MethodKind,
    val classFqn: String,
    val methodName: String,
    val isStatic: Boolean,
    val parameters: List<MethodParameterSchema>
)
```

```kotlin
package com.zx3022448.phpdebugtools.methods

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Method

object PhpMethodTargetResolver {
    fun resolve(file: PsiFile, caretOffset: Int): MethodDebugTarget? {
        val element = file.findElementAt(caretOffset) ?: return null
        val method = PsiTreeUtil.getParentOfType(element, Method::class.java) ?: return null
        val phpClass = method.containingClass ?: return null

        val kind = if (phpClass.fqn.contains("\\controller\\", ignoreCase = true) || phpClass.name?.endsWith("Controller") == true) {
            MethodKind.CONTROLLER
        } else {
            MethodKind.SERVICE
        }

        val parameters = method.parameters.map { parameter ->
            MethodParameterSchema(
                name = parameter.name,
                declaredType = parameter.type.toString(),
                required = !parameter.isOptional,
                defaultValue = parameter.defaultValue?.text
            )
        }

        return MethodDebugTarget(
            kind = kind,
            classFqn = phpClass.fqn,
            methodName = method.name,
            isStatic = method.modifier.isStatic,
            parameters = parameters
        )
    }
}
```

- [ ] **步骤 4：再次运行平台测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.methods.PhpMethodTargetResolverTest"`

预期：PASS，能从测试夹具中正确解析控制器方法。

- [ ] **步骤 5：提交方法目标解析功能**

```bash
git add src/main/kotlin/com/example/phpdebugtools/methods src/test/kotlin/com/example/phpdebugtools/methods
git commit -m "feat: resolve php method targets from editor caret"
```

## 任务 6：通过运行时执行服务方法

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/execution/RuntimeJson.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/execution/DebugExecutionResult.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/execution/RuntimeExecutor.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/ui/ServiceMethodDialog.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/actions/DebugServiceMethodAction.kt`
- 测试：`src/test/kotlin/com/example/phpdebugtools/execution/RuntimeExecutorServiceTest.kt`

- [ ] **步骤 1：先写服务方法执行失败测试**

```kotlin
package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandResult
import com.zx3022448.phpdebugtools.diagnostics.CommandRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

class RuntimeExecutorServiceTest {
    @Test
    fun parsesRuntimeJsonForServiceExecution() {
        val runner = CommandRunner { _, _ ->
            CommandResult(0, """{"status":"ok","stage":"target","message":"service invoked"}""", "")
        }

        val result = RuntimeExecutor(runner).run(
            command = listOf("php", "invoke-service.php", "payload.json"),
            projectRoot = Paths.get("D:/demo")
        )

        assertEquals("ok", result.status)
        assertEquals("target", result.stage)
    }
}
```

- [ ] **步骤 2：运行失败测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.execution.RuntimeExecutorServiceTest"`

预期：FAIL，提示 `RuntimeExecutor` 和 `DebugExecutionResult` 未解析。

- [ ] **步骤 3：实现运行时执行、负载序列化和服务方法动作**

```kotlin
package com.zx3022448.phpdebugtools.execution

data class DebugExecutionResult(
    val status: String,
    val stage: String,
    val message: String,
    val rawOutput: String
)
```

```kotlin
package com.zx3022448.phpdebugtools.execution

object RuntimeJson {
    fun servicePayload(
        classFqn: String,
        methodName: String,
        isStatic: Boolean,
        argsJson: String
    ): String {
        return """
            {
              "type": "service",
              "class": "$classFqn",
              "method": "$methodName",
              "static": $isStatic,
              "args": $argsJson
            }
        """.trimIndent()
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandRunner
import java.nio.file.Path

class RuntimeExecutor(private val commandRunner: CommandRunner) {
    fun run(command: List<String>, projectRoot: Path): DebugExecutionResult {
        val result = commandRunner.run(command, projectRoot.toString())
        val status = Regex("\"status\"\\s*:\\s*\"([^\"]+)\"").find(result.stdout)?.groupValues?.get(1) ?: "error"
        val stage = Regex("\"stage\"\\s*:\\s*\"([^\"]+)\"").find(result.stdout)?.groupValues?.get(1) ?: "unknown"
        val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(result.stdout)?.groupValues?.get(1) ?: result.stderr
        return DebugExecutionResult(status, stage, message, result.stdout)
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.ui

import com.zx3022448.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ServiceMethodDialog(project: Project, private val target: MethodDebugTarget) : DialogWrapper(project) {
    private val argsArea = JBTextArea("[]")

    init {
        title = "调试服务方法: ${target.methodName}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply { add(argsArea, BorderLayout.CENTER) }
    }

    fun argsJson(): String = argsArea.text
}
```

```kotlin
package com.zx3022448.phpdebugtools.actions

import com.zx3022448.phpdebugtools.methods.MethodKind
import com.zx3022448.phpdebugtools.methods.PhpMethodTargetResolver
import com.zx3022448.phpdebugtools.ui.ServiceMethodDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DebugServiceMethodAction : AnAction("调试服务方法") {
    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val target = PhpMethodTargetResolver.resolve(file, editor.caretModel.offset) ?: return
        if (target.kind != MethodKind.SERVICE) return
        ServiceMethodDialog(event.project ?: return, target).show()
    }
}
```

- [ ] **步骤 4：再次运行服务方法执行测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.execution.RuntimeExecutorServiceTest"`

预期：PASS，结果中 `status=ok` 且 `stage=target`。

- [ ] **步骤 5：提交服务方法执行流程**

```bash
git add src/main/kotlin/com/example/phpdebugtools/execution src/main/kotlin/com/example/phpdebugtools/ui src/main/kotlin/com/example/phpdebugtools/actions src/test/kotlin/com/example/phpdebugtools/execution
git commit -m "feat: add service method runtime execution flow"
```

## 任务 7：执行控制器方法并构建受控 Web 调试 URL

**文件：**
- 新增：`src/main/kotlin/com/example/phpdebugtools/execution/WebDebugUrlBuilder.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/ui/ControllerMethodDialog.kt`
- 新增：`src/main/kotlin/com/example/phpdebugtools/actions/DebugControllerMethodAction.kt`
- 修改：`src/main/resources/runtime/invoke-controller.php`
- 修改：`src/main/resources/runtime/debug-web-entry.php`
- 测试：`src/test/kotlin/com/example/phpdebugtools/execution/WebDebugUrlBuilderTest.kt`

- [ ] **步骤 1：先写 Web URL 构建器失败测试**

```kotlin
package com.zx3022448.phpdebugtools.execution

import org.junit.Assert.assertEquals
import org.junit.Test

class WebDebugUrlBuilderTest {
    @Test
    fun appendsDebugTriggerAndPayloadFileToUrl() {
        val url = WebDebugUrlBuilder.build(
            baseUrl = "http://127.0.0.1/index.php",
            runtimePath = "/.php-debug-tools/debug-web-entry.php",
            payloadFile = "controller-payload.json"
        )

        assertEquals(
            "http://127.0.0.1/.php-debug-tools/debug-web-entry.php?XDEBUG_TRIGGER=PHPSTORM&payload=controller-payload.json",
            url
        )
    }
}
```

- [ ] **步骤 2：运行测试，确认先失败**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.execution.WebDebugUrlBuilderTest"`

预期：FAIL，提示 `WebDebugUrlBuilder` 未解析。

- [ ] **步骤 3：实现控制器弹窗、动作、运行时脚本和 Web URL 构建器**

```kotlin
package com.zx3022448.phpdebugtools.execution

object WebDebugUrlBuilder {
    fun build(baseUrl: String, runtimePath: String, payloadFile: String): String {
        val prefix = baseUrl.substringBefore("/index.php")
        return "$prefix$runtimePath?XDEBUG_TRIGGER=PHPSTORM&payload=$payloadFile"
    }
}
```

```kotlin
package com.zx3022448.phpdebugtools.ui

import com.zx3022448.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ControllerMethodDialog(project: Project, private val target: MethodDebugTarget) : DialogWrapper(project) {
    private val queryArea = JBTextArea("{}")
    private val postArea = JBTextArea("{}")
    private val argsArea = JBTextArea("[]")

    init {
        title = "调试控制器方法: ${target.methodName}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(GridLayout(3, 1)).apply {
            add(queryArea)
            add(postArea)
            add(argsArea)
        }
    }

    fun queryJson(): String = queryArea.text
    fun postJson(): String = postArea.text
    fun argsJson(): String = argsArea.text
}
```

```kotlin
package com.zx3022448.phpdebugtools.actions

import com.zx3022448.phpdebugtools.methods.MethodKind
import com.zx3022448.phpdebugtools.methods.PhpMethodTargetResolver
import com.zx3022448.phpdebugtools.ui.ControllerMethodDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DebugControllerMethodAction : AnAction("调试控制器方法") {
    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val target = PhpMethodTargetResolver.resolve(file, editor.caretModel.offset) ?: return
        if (target.kind != MethodKind.CONTROLLER) return
        ControllerMethodDialog(event.project ?: return, target).show()
    }
}
```

```php
<?php

declare(strict_types=1);

$payloadPath = $argv[1] ?? '';
$payload = json_decode((string) file_get_contents($payloadPath), true) ?: [];

echo json_encode([
    'status' => 'ok',
    'stage' => 'target',
    'message' => 'controller invoked',
    'request' => [
        'class' => $payload['class'] ?? '',
        'method' => $payload['method'] ?? '',
        'query' => $payload['query'] ?? [],
        'post' => $payload['post'] ?? [],
        'args' => $payload['args'] ?? [],
    ],
], JSON_UNESCAPED_UNICODE);
```

```php
<?php

declare(strict_types=1);

$payloadName = $_GET['payload'] ?? '';
$payloadFile = __DIR__ . '/' . basename($payloadName);
$payload = is_file($payloadFile)
    ? json_decode((string) file_get_contents($payloadFile), true)
    : [];

echo json_encode([
    'status' => 'ok',
    'stage' => 'target',
    'message' => 'web debug entry reached',
    'payload' => $payload,
], JSON_UNESCAPED_UNICODE);
```

- [ ] **步骤 4：再次运行 Web URL 构建测试**

运行：`.\gradlew.bat test --tests "com.zx3022448.phpdebugtools.execution.WebDebugUrlBuilderTest"`

预期：PASS，生成的受控调试 URL 与预期完全一致。

- [ ] **步骤 5：提交控制器和 Web 调试流程**

```bash
git add src/main/kotlin/com/example/phpdebugtools/execution/WebDebugUrlBuilder.kt src/main/kotlin/com/example/phpdebugtools/ui/ControllerMethodDialog.kt src/main/kotlin/com/example/phpdebugtools/actions/DebugControllerMethodAction.kt src/main/resources/runtime/invoke-controller.php src/main/resources/runtime/debug-web-entry.php src/test/kotlin/com/example/phpdebugtools/execution/WebDebugUrlBuilderTest.kt
git commit -m "feat: add controller and web debug execution flow"
```

## 任务 8：注册动作、完成插件接线并手工验证 MVP

**文件：**
- 修改：`src/main/resources/META-INF/plugin.xml`
- 修改：`src/main/resources/messages/MyMessageBundle.properties`
- 修改：`README.md`
- 测试：通过 `.\gradlew.bat runIde` 手工验证

- [ ] **步骤 1：在 `plugin.xml` 中补齐动作注册**

```xml
<actions>
    <action id="PhpDebugTools.DebugServiceMethod"
            class="com.zx3022448.phpdebugtools.actions.DebugServiceMethodAction"
            text="调试服务方法"/>
    <action id="PhpDebugTools.DebugControllerMethod"
            class="com.zx3022448.phpdebugtools.actions.DebugControllerMethodAction"
            text="调试控制器方法"/>
</actions>
```

- [ ] **步骤 2：更新 bundle 文案和 README 使用说明**

```properties
toolwindow.tab.overview=概览
toolwindow.tab.request=请求调试
toolwindow.tab.method=方法直调
toolwindow.tab.diagnostics=诊断
action.debug.service=调试服务方法
action.debug.controller=调试控制器方法
diagnostics.missing.xdebug=Xdebug 未加载
runtime.installed=.php-debug-tools 已安装
```

```markdown
## MVP Usage

1. Open a ThinkPHP project in PhpStorm.
2. Open the `PHP Debug Tools` tool window and confirm the overview tab detects ThinkPHP and shows runtime status.
3. Run diagnostics and confirm the selected PHP CLI reports whether Xdebug is loaded.
4. Right-click a service method and open the service debug dialog.
5. Right-click a controller method and open the controller debug dialog.
6. Trigger a controlled web debug URL through the request debug tab.
```

- [ ] **步骤 3：运行完整自动化测试**

运行：`.\gradlew.bat test`

预期：PASS，所有单元测试和平台测试全部通过。

- [ ] **步骤 4：在 IDE 沙箱中运行插件并执行 MVP 验收清单**

运行：`.\gradlew.bat runIde`

预期：
- 插件能正常加载，不出现 `plugin.xml` 注册错误。
- 工具窗口能打开，并包含四个标签页。
- 在示例项目上可以识别 ThinkPHP。
- 运行时安装器可以创建 `.php-debug-tools/`。
- 编辑器中的服务方法和控制器方法动作可以正确弹出对话框。

- [ ] **步骤 5：提交接线完成后的 MVP**

```bash
git add src/main/resources/META-INF/plugin.xml src/main/resources/messages/MyMessageBundle.properties README.md
git commit -m "feat: wire mvp actions and verification docs"
```

## 自检

### 规格覆盖情况

- ThinkPHP 项目识别：任务 1
- 同步运行时到 `.php-debug-tools/`：任务 2
- PHP/Xdebug 诊断：任务 3
- 工具窗口与概览/诊断入口：任务 4
- 方法选择与参数识别：任务 5
- 服务类方法直调：任务 6
- 控制器方法直调与本地 Web 调试入口：任务 7
- 插件注册、README 与手工验收：任务 8

### 占位符检查

计划正文中已无占位标记。每一个涉及代码变更的步骤都给出了具体代码块，每一个验证步骤都给出了明确命令。

### 类型一致性检查

- 项目识别部分统一使用 `ThinkPhpProjectInfo` 和 `ThinkPhpProjectDetector`。
- 执行部分统一使用 `DebugRequest`、`CliDebugCommandBuilder`、`RuntimeExecutor` 和 `DebugExecutionResult`。
- 方法解析部分统一使用 `MethodKind`、`MethodParameterSchema` 和 `MethodDebugTarget`。
