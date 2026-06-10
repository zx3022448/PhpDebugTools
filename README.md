# PhpDebugTools

PhpDebugTools 是一个面向 ThinkPHP 项目的 PhpStorm 插件 MVP，用于在 IDE 内集中查看项目诊断状态，并触发请求调试、方法直调与运行时辅助能力。

## MVP Usage

1. Open a ThinkPHP project in PhpStorm.
2. Open the `PHP Debug Tools` tool window and confirm the overview tab detects ThinkPHP and shows runtime status.
3. Run diagnostics and confirm the selected PHP CLI reports whether Xdebug is loaded.
4. Right-click a service method and open the service debug dialog.
5. Right-click a controller method and open the controller debug dialog.
6. Trigger a controlled web debug URL through the request debug tab.

## 开发与验证

在当前 PowerShell 会话中先临时设置测试使用的 JBR：

```powershell
$env:JAVA_HOME='C:\Users\PC\.gradle\caches\9.5.0\transforms\6e91db0e4228bbafc2c17c13574a5938\transformed\idea-2025.3.5-win\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

常用命令：

```powershell
.\gradlew.bat test
.\gradlew.bat runIde
.\gradlew.bat verifyPlugin
```

`runIde` 验收建议：

- 确认插件启动时没有 `plugin.xml` 动作注册错误。
- 打开 `PHP Debug Tools` 工具窗口，确认包含概览、请求调试、方法直调、诊断四个标签页。
- 在 ThinkPHP 示例项目中确认概览页可以识别框架并显示运行时状态。
- 验证运行时安装器可以创建 `.php-debug-tools/`。
- 在编辑器右键菜单中确认“调试服务方法”和“调试控制器方法”都能弹出对应对话框。
