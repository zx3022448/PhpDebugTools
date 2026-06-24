# PhpDebugTools 使用说明文档

## 1. 文档概述

PhpDebugTools 是一个面向 ThinkPHP 项目的 PhpStorm 插件，用于在 IDE 内完成本地方法直调、请求上下文组装、PHP CLI 调试触发和运行时辅助脚本同步。

当前版本的核心使用场景是：

1. 在 PhpStorm 中识别 ThinkPHP 项目。
2. 自动安装或更新项目内 `.php-debug-tools/` 运行时目录。
3. 在工具窗口中搜索 PHP 类方法并执行直调。
4. 对服务类方法传入 `args` 参数执行。
5. 对控制器方法配置 Query、Body、Headers 等请求上下文后执行。
6. 在编辑器右键菜单中快速打开服务方法或控制器方法调试入口。

本文面向插件使用者，说明如何准备环境、打开入口、填写参数、执行调试以及处理常见问题。

## 2. 适用范围

### 2.1 适用项目

插件当前主要面向 ThinkPHP 项目，识别逻辑会优先检查以下特征：

1. `composer.json` 中是否声明 `topthink/framework`。
2. 项目是否包含 `public/index.php`。
3. 项目是否包含 `app/`、`config/` 等 ThinkPHP 常见目录。

识别成功后，工具窗口概览区域会显示类似：

```text
ThinkPHP 6 project detected
```

如果未识别为 ThinkPHP 项目，工具窗口仍可打开，但运行时安装和方法执行能力可能不可用或不完整。

### 2.2 支持的 ThinkPHP 版本

当前运行时包含以下适配入口：

1. `thinkphp5`
2. `thinkphp6`

项目识别到 ThinkPHP 5 或 ThinkPHP 6 后，会将对应适配器写入 `.php-debug-tools/runtime-config.json`。当前 ThinkPHP 5 入口复用通用调用实现，重点保障服务方法和控制器方法直调链路可用。

### 2.3 运行环境

推荐环境：

1. Windows 本机开发环境。
2. PhpStorm 或安装 PHP 插件的 IntelliJ IDEA。
3. 本机已安装 PHP CLI。
4. 如需命中断点，本机 PHP CLI 需要安装并启用 Xdebug。
5. 目标项目已安装 Composer 依赖，能够正常加载 `vendor/autoload.php` 或 ThinkPHP 自身引导文件。

## 3. 使用前准备

### 3.1 打开项目

使用 PhpStorm 打开 ThinkPHP 项目根目录。建议打开包含 `composer.json`、`public/index.php`、`app/` 和 `config/` 的目录。

如果打开的是上级目录或子目录，插件可能无法正确识别项目，也可能无法正确安装 `.php-debug-tools/`。

### 3.2 确认 PHP CLI 可用

在系统终端中确认 PHP 命令可运行：

```powershell
php -v
```

如果你有多个 PHP 版本，也可以准备完整路径，例如：

```text
D:\phpstudy_pro\Extensions\php\php8.2.9nts\php.exe
```

工具窗口会自动探测常见 PHP 命令和 Windows 常见安装目录，包括 PATH 中的 `php`、`php7`、`php80`、`php81`、`php82`、`php83`、`php84`，以及 `phpstudy`、`PhpWebStudy`、`C:\php`、`D:\php` 等常见位置。

### 3.3 确认 Xdebug

方法直调本身可以通过 PHP CLI 执行；如果希望进入断点，需要确保 PHP CLI 加载 Xdebug。

插件执行方法时会自动附加以下 CLI 参数，用于尝试触发调试会话：

```text
-dxdebug.mode=debug
-dxdebug.start_with_request=yes
-dxdebug.remote_enable=1
-dxdebug.remote_autostart=1
```

说明：

1. `xdebug.mode=debug` 和 `xdebug.start_with_request=yes` 面向 Xdebug 3。
2. `xdebug.remote_enable=1` 和 `xdebug.remote_autostart=1` 用于兼容旧版 Xdebug 配置。
3. 是否真正命中断点仍取决于 PhpStorm 是否正在监听调试连接、Xdebug 端口是否匹配、路径映射是否正确。

## 4. 工具窗口

### 4.1 打开方式

在 IDE 侧边栏打开：

```text
PHP Debug Tools
```

插件注册的工具窗口 ID 为 `PhpDebugTools`，侧边栏标题显示为 `PHP Debug Tools`。

### 4.2 工具窗口启动后会做什么

打开工具窗口后，插件会在后台执行以下动作：

1. 扫描项目中的 PHP 方法。
2. 探测本机可用 PHP CLI。
3. 判断当前项目是否为 ThinkPHP 项目。
4. 如果识别为 ThinkPHP 项目，自动创建或更新 `.php-debug-tools/`。
5. 将识别结果、运行时状态、方法列表和 PHP 版本同步到工具窗口页面。

启动过程中页面可能短暂显示：

```text
PHP Debug Tools 加载中...
```

等待后台扫描结束后，页面会显示方法选择、运行时选择、参数配置和执行结果区域。

### 4.3 自动安装的运行时目录

识别到 ThinkPHP 项目后，插件会在项目根目录下安装：

```text
.php-debug-tools/
```

当前包含以下文件：

```text
.php-debug-tools/bootstrap.php
.php-debug-tools/adapters/thinkphp5.php
.php-debug-tools/adapters/thinkphp6.php
.php-debug-tools/invoke-service.php
.php-debug-tools/invoke-controller.php
.php-debug-tools/runtime-config.json
```

执行方法时还会生成：

```text
.php-debug-tools/toolwindow-payload.json
```

这些文件用于在目标项目上下文内加载 Composer、初始化 ThinkPHP 相关能力、调用目标方法并输出结构化执行结果。

建议将 `.php-debug-tools/` 加入业务项目的 `.gitignore`，避免将本地调试辅助文件提交到业务仓库。

## 5. 工具窗口界面说明

### 5.1 顶部操作区

工具窗口顶部包含以下主要区域：

1. PHP 运行时选择：显示当前使用的 PHP 版本或 PHP 命令。
2. 方法搜索框：输入类名、方法名、命名空间或方法类型筛选候选方法。
3. 请求方法选择：控制器方法可选择 `GET`、`POST`、`PUT`、`PATCH`、`DELETE`、`HEAD`、`OPTIONS`。
4. 执行按钮：点击后通过 `.php-debug-tools/` 执行当前方法。
5. 刷新按钮：重新扫描项目方法和本机 PHP 运行时。

### 5.2 PHP 运行时选择

点击顶部的 PHP 运行时区域，可以从自动探测到的 PHP CLI 中选择一个执行环境。

如果没有探测到 PHP，可直接在输入框中填写：

1. `php`
2. `php82`
3. PHP 可执行文件完整路径

示例：

```text
D:\phpstudy_pro\Extensions\php\php8.2.9nts\php.exe
```

如果填写的是完整路径，插件会检查该路径是否存在。如果填写的是 `php` 这类命令别名，插件会交给系统 PATH 解析。

### 5.3 方法搜索与选择

方法搜索框支持按以下信息检索：

1. 完整类名。
2. 命名空间片段。
3. 方法名。
4. 方法类型：`CONTROLLER` 或 `SERVICE`。

方法显示格式为：

```text
\App\Service\UserService::findUser
```

插件会扫描项目 PHP 文件中的类方法，并按以下规则分类：

1. 类完整命名空间包含 `\controller\`，按控制器方法处理。
2. 类名以 `Controller` 结尾，按控制器方法处理。
3. 其他类方法按服务方法处理。

如果某个方法分类不符合预期，请优先检查类名和命名空间是否符合以上规则。

### 5.4 页面标签

选中方法后，工具窗口会根据方法类型显示不同标签。

服务方法通常显示：

1. `概览`：展示参数列表和必填状态。
2. `参数`：编辑传给方法的 `args JSON`。
3. `路径`：展示类名、方法名、项目识别和运行时摘要。
4. `脚本`：预览本次执行即将提交的 payload。

控制器方法通常显示：

1. `概览`：展示请求上下文和参数列表。
2. `路径`：展示类名、方法名、项目识别和运行时摘要。
3. `请求体`：编辑 Query、Body、Headers。
4. `脚本`：预览本次执行即将提交的 payload。

### 5.5 执行结果区

页面底部包含结果区域，默认显示 `执行结果`。

执行后可能看到：

1. `status: waiting`：等待执行。
2. `status: running`：正在执行。
3. `status: ok`：执行成功。
4. `status: error`：执行异常。

结果区还提供 `参数` 标签，用于查看本次执行的 payload 预览。

点击复制按钮可以复制当前结果区内容到剪贴板。

## 6. 服务方法直调

### 6.1 适用场景

服务方法直调适合调试业务服务、工具类、任务类、模型辅助类等不强依赖完整 HTTP 请求链路的方法。

常见目标示例：

```php
namespace app\service;

class UserService
{
    public function findUser(int $id): array
    {
        return ['id' => $id];
    }
}
```

### 6.2 操作步骤

1. 打开 `PHP Debug Tools` 工具窗口。
2. 在方法搜索框中输入类名或方法名。
3. 选择目标服务方法。
4. 打开 `参数` 标签。
5. 按参数顺序填写 `args JSON`。
6. 选择合适的 PHP 运行时。
7. 点击执行按钮。
8. 在底部 `执行结果` 中查看返回值或异常信息。

### 6.3 参数格式

服务方法使用 JSON 数组作为参数，数组顺序必须与方法签名中的参数顺序一致。

无参数方法：

```json
[]
```

单参数方法：

```json
[1]
```

多个参数：

```json
[1, "active", true]
```

数组或对象参数：

```json
[
  1,
  {
    "name": "张三",
    "roles": ["admin", "editor"]
  }
]
```

### 6.4 实例方法与静态方法

执行服务方法时，运行时会按以下方式调用：

1. 静态方法：直接通过反射调用静态方法。
2. 实例方法：优先通过 ThinkPHP 容器解析实例。
3. 容器解析失败时，尝试无参构造。
4. 如果构造函数存在必填参数，运行时会使用反射兜底创建对象。

注意：如果方法依赖构造函数初始化后的状态，反射兜底创建对象可能无法满足业务预期。此时建议让服务类可被容器正常解析，或为调试准备更明确的入口方法。

## 7. 控制器方法直调

### 7.1 适用场景

控制器方法直调用于模拟一次受控请求，然后在本地 CLI 中调用目标控制器方法。

适合调试：

1. 依赖 Query 参数的方法。
2. 依赖 POST 表单的方法。
3. 依赖 JSON 请求体的方法。
4. 需要 Header 或 Content-Type 的方法。

### 7.2 操作步骤

1. 打开 `PHP Debug Tools` 工具窗口。
2. 搜索并选择目标控制器方法。
3. 在顶部选择请求方法，例如 `GET` 或 `POST`。
4. 打开 `请求体` 标签。
5. 在 `Query`、`Body`、`Headers` 子标签中维护请求参数。
6. 选择 Body 类型：`none`、`form-data`、`x-www-form-urlencoded` 或 `JSON`。
7. 点击执行按钮。
8. 查看执行结果。

### 7.3 请求方法自动推断

插件会尝试根据方法名和方法内容推断请求方法：

1. 方法内容包含 `@method post`、`isPost()`、`->post()`、`request()->post()` 时，倾向识别为 `POST`。
2. 方法内容包含 `@method put`、`isPut()` 时，倾向识别为 `PUT`。
3. 方法内容包含 `@method delete`、`isDelete()` 时，倾向识别为 `DELETE`。
4. 方法名以 `create`、`save`、`store`、`add`、`upload`、`submit` 开头时，倾向识别为 `POST`。
5. 方法名以 `update`、`edit`、`put` 开头时，倾向识别为 `PUT`。
6. 方法名以 `delete`、`remove`、`destroy` 开头时，倾向识别为 `DELETE`。
7. 方法名以 `index`、`list`、`show`、`detail`、`get`、`query`、`search`、`find` 开头时，倾向识别为 `GET`。

推断结果只是默认值，执行前可以在顶部请求方法选择框中手动调整。

### 7.4 Body 类型自动推断

插件会尝试根据方法内容推断 Body 类型：

1. 出现 `->file()`、`request()->file()`、`$_FILES`、`multipart/form-data` 时，倾向使用 `form-data`。
2. 出现 `application/json`、`php://input`、`json_decode()`、`getInput()` 时，倾向使用 `JSON`。
3. 出现 `->post()`、`request()->post()`、`input('post.` 时，倾向使用 `x-www-form-urlencoded`。
4. `PUT` 或 `PATCH` 默认倾向使用 `JSON`。
5. `GET`、`DELETE`、`HEAD`、`OPTIONS` 默认不启用 Body。

### 7.5 Query 参数

在 `Query` 子标签中维护 URL 查询参数。每行包含：

1. 参数名。
2. 类型。
3. 示例值。
4. 说明。

支持的类型：

```text
string
integer
number
boolean
array
object
```

示例：

| 参数名 | 类型 | 示例值 |
| --- | --- | --- |
| id | integer | 100 |
| keyword | string | test |
| debug | boolean | true |

执行时会转换为 JSON 对象：

```json
{
  "id": 100,
  "keyword": "test",
  "debug": true
}
```

### 7.6 Body 参数

`Body` 子标签支持四种模式。

#### none

不传请求体。适合 `GET`、`DELETE`、`HEAD`、`OPTIONS` 等不带 Body 的请求。

#### form-data

以键值表格方式填写请求体。适合普通表单或文件上传相关方法。

当前工具窗口主要负责普通字段的组织；真实文件上传依赖完整 `$_FILES` 的场景可能需要后续增强。

#### x-www-form-urlencoded

以键值表格方式填写请求体。适合常规 POST 表单。

#### JSON

直接编辑 JSON 对象。

示例：

```json
{
  "name": "张三",
  "age": 18,
  "enabled": true
}
```

注意：Body JSON 必须是 JSON 对象，而不是 JSON 数组或普通字符串。

### 7.7 Headers

在 `Headers` 子标签中维护请求头。

如果 Body 模式需要 Content-Type，插件会根据模式自动生成默认 Header，例如：

```text
Content-Type: application/json
```

你也可以手动新增其他 Header，例如：

```text
Authorization: Bearer token
X-Request-Id: debug-001
```

### 7.8 控制器参数传递规则

控制器方法执行时，运行时会做以下处理：

1. 将 Query 写入 `$_GET`。
2. 将表单或 JSON Body 写入 `$_POST`。
3. 将 Query 和 Body 合并到 `$_REQUEST`。
4. 将 Headers 转换后写入 `$_SERVER`。
5. 设置 `$_SERVER['REQUEST_METHOD']`。
6. 设置 `$_SERVER['CONTENT_TYPE']`。
7. 尝试为控制器实例注入 ThinkPHP Request 对象。

如果控制器方法本身声明了参数，并且 `args` 为空，运行时会将 Query 和 Body 合并后按值顺序传入目标方法。

## 8. 编辑器右键入口

### 8.1 调试服务方法

在 PHP 文件中，将光标放到服务类方法内部或方法声明附近，右键打开编辑器菜单。

如果当前方法被识别为服务方法，菜单会显示：

```text
调试服务方法
```

点击后会打开服务方法参数对话框。对话框会根据方法签名生成参数输入项，并可以展开编辑复杂参数。

### 8.2 调试控制器方法

在控制器方法内部或方法声明附近右键。

如果当前方法被识别为控制器方法，菜单会显示：

```text
调试控制器方法
```

点击后会打开控制器请求编辑对话框，可编辑请求方法、Query、Body 和 Headers。

### 8.3 菜单不显示的原因

右键菜单按当前光标所在方法动态显示。以下情况可能导致菜单不显示：

1. 光标不在 PHP 类方法内部。
2. 当前文件不是 PHP PSI 文件。
3. 方法无法解析到所属类。
4. 类名或命名空间未满足控制器识别规则。
5. IDE 正在索引，方法解析暂时不可用。

## 9. 执行流程

点击执行按钮后，插件内部流程如下：

1. 读取当前选中的方法、PHP 命令和参数。
2. 检查当前项目是否有可用 `basePath`。
3. 检查 PHP 命令是否为空或明显不可用。
4. 重新安装或更新 `.php-debug-tools/`。
5. 生成 `.php-debug-tools/toolwindow-payload.json`。
6. 根据方法类型选择入口脚本：
   - 服务方法：`.php-debug-tools/invoke-service.php`
   - 控制器方法：`.php-debug-tools/invoke-controller.php`
7. 使用所选 PHP CLI 执行入口脚本。
8. 附加 Xdebug CLI 参数尝试触发断点。
9. 解析运行时输出的结构化 JSON。
10. 在工具窗口展示返回值、异常或原始输出。

执行命令的逻辑结构类似：

```text
php -dxdebug.mode=debug -dxdebug.start_with_request=yes .php-debug-tools/invoke-service.php .php-debug-tools/toolwindow-payload.json
```

实际命令还会附加兼容旧版 Xdebug 的参数。

## 10. 返回结果说明

运行时会输出结构化结果，核心字段包括：

| 字段 | 含义 |
| --- | --- |
| status | 执行状态，常见值为 `ok` 或 `error` |
| stage | 当前阶段，例如 `bootstrap`、`framework`、`target`、`runtime` |
| message | 简要消息 |
| resultText | 可展示的返回值文本 |
| resultType | 返回值类型 |
| exceptionText | 异常文本 |
| rawOutput | 原始输出 |

工具窗口会优先展示：

1. `exceptionText`：如果存在异常。
2. `resultType` 和 `resultText`：如果方法有返回值。
3. 默认完成提示：如果方法执行成功但没有可展示返回值。
4. 原始输出：如果无法解析结构化结果。

## 11. 最近记录与缓存

插件会在项目级别保存最近状态，存储名为：

```text
PhpDebugToolsRecentDebugStore
```

主要保存内容：

1. 最近执行的方法。
2. 最近使用的 PHP 可执行命令。
3. 已选择的 PHP 命令。
4. 最近扫描到的方法缓存。
5. 最近探测到的 PHP 运行时缓存。

缓存用于提升工具窗口下次打开时的响应速度。如果方法列表或 PHP 版本不准确，点击工具窗口右上角刷新按钮重新扫描。

## 12. 常见问题

### 12.1 工具窗口显示不是 ThinkPHP 项目

可能原因：

1. 当前 IDE 打开的目录不是项目根目录。
2. 项目缺少 `composer.json`。
3. `composer.json` 中没有 `topthink/framework`。
4. 项目缺少 `public/index.php`、`app/` 或 `config/`。

处理建议：

1. 重新用 PhpStorm 打开正确的项目根目录。
2. 检查 Composer 依赖是否完整。
3. 确认 ThinkPHP 入口文件和目录结构是否符合常规布局。

### 12.2 没有扫描到方法

可能原因：

1. IDE 仍在索引。
2. PHP 文件不在项目源码范围内。
3. 方法所在文件未被 PhpStorm PHP 插件正确识别。
4. 工具窗口尚未刷新缓存。

处理建议：

1. 等待 PhpStorm 索引完成。
2. 点击工具窗口刷新按钮。
3. 确认 PHP 文件位于 `app/`、`application/` 或项目源码目录中。

### 12.3 PHP 运行时未探测到

可能原因：

1. PHP 未加入系统 PATH。
2. PHP 安装在非常规目录。
3. PHP 可执行文件不可运行。

处理建议：

1. 在 PHP 运行时输入框中手动填写完整 `php.exe` 路径。
2. 在终端执行 `php -v` 确认命令可用。
3. 检查 PHP 安装目录权限。

### 12.4 提示 PHP 命令不可用

如果看到类似提示：

```text
未找到可用的 PHP 命令
```

请检查：

1. 完整路径是否存在。
2. 路径中是否误填了目录而不是 `php.exe`。
3. 如果使用命令别名，是否已加入 PATH。

### 12.5 执行后没有进入断点

可能原因：

1. PhpStorm 没有开启监听 PHP Debug 连接。
2. PHP CLI 没有加载 Xdebug。
3. Xdebug 端口与 PhpStorm 配置不一致。
4. Xdebug IDE Key、触发策略或防火墙配置不正确。
5. 断点所在代码没有被本次直调链路执行到。
6. 路径映射不正确。

处理建议：

1. 在 PhpStorm 中开启 PHP Debug 监听。
2. 执行 `php -v` 或 `php --ri xdebug` 确认 Xdebug 已加载。
3. 检查 PhpStorm 的 PHP Debug 端口。
4. 将断点打在目标方法第一行或运行时入口附近验证链路。
5. 确认工具窗口选择的 PHP 与你配置 Xdebug 的 PHP 是同一个版本。

### 12.6 提示 Target class not found

可能原因：

1. Composer autoload 未生成。
2. 类命名空间与文件路径不匹配。
3. 目标类不在 Composer 或 ThinkPHP 可加载范围内。
4. 项目依赖未安装。

处理建议：

1. 在项目根目录执行 `composer install`。
2. 必要时执行 `composer dump-autoload`。
3. 检查类命名空间和文件路径。

### 12.7 提示 Target method not found

可能原因：

1. 方法名已修改但工具窗口缓存未刷新。
2. 当前选择的方法来自旧缓存。
3. 方法不可被 `method_exists` 识别。

处理建议：

1. 点击刷新按钮重新扫描项目。
2. 重新选择目标方法。
3. 确认方法真实存在于目标类中。

### 12.8 控制器方法拿不到请求参数

请检查：

1. 参数是否填在正确位置：Query、Body 或 Headers。
2. 请求方法是否正确。
3. Body 模式是否正确，例如 JSON 或 `x-www-form-urlencoded`。
4. JSON Body 是否是合法 JSON 对象。
5. 控制器代码读取的是 `$_GET`、`$_POST`、`$_REQUEST`、`request()->param()` 还是其他来源。

如果控制器依赖完整路由中间件、登录态、Session、Cookie 或复杂请求生命周期，当前直调链路可能无法完全模拟真实 Web 请求。

### 12.9 服务方法实例状态异常

实例方法会优先使用 ThinkPHP 容器解析。如果容器解析失败，运行时会尝试反射创建对象。

如果服务类构造函数中依赖数据库连接、配置、登录态或其他复杂状态，建议：

1. 确保服务类已注册到 ThinkPHP 容器。
2. 准备更轻量的调试方法。
3. 将复杂依赖改为可注入、可默认初始化的形式。

## 13. 使用建议

### 13.1 推荐的调试方式

服务方法：

1. 优先调试无副作用或副作用可控的方法。
2. 参数使用标准 JSON 数组。
3. 复杂数组参数先在 `脚本` 标签中检查 payload 是否符合预期。

控制器方法：

1. 先确认请求方法和 Body 模式。
2. 简单参数优先使用表格填写。
3. 复杂结构优先使用 JSON Body。
4. 对依赖登录态、中间件或路由绑定的接口，优先在真实 Web 请求中验证。

### 13.2 推荐的断点位置

首次验证链路时，建议按顺序设置断点：

1. `.php-debug-tools/invoke-service.php` 或 `.php-debug-tools/invoke-controller.php`。
2. `.php-debug-tools/adapters/thinkphp6.php` 中的目标调用逻辑。
3. 目标类构造函数。
4. 目标方法第一行。

这样可以快速判断问题发生在 PHP CLI、运行时引导、容器解析还是业务方法内部。

### 13.3 参数填写建议

1. `string` 类型不需要手动加引号，表格会自动序列化。
2. `integer`、`number` 类型请填写数字。
3. `boolean` 类型填写 `true`、`false` 或 `1`。
4. `array` 类型填写合法 JSON 数组，例如 `["a", "b"]`。
5. `object` 类型填写合法 JSON 对象，例如 `{"id": 1}`。
6. `args JSON` 必须整体是 JSON 数组。
7. Query、Headers、Body 表格最终会转换成 JSON 对象。

## 14. 当前能力边界

当前版本重点是 IDE 内受控方法直调，不等同于完整 Web 请求录制或完整框架请求生命周期模拟。

需要注意的边界：

1. 不会自动接管所有浏览器请求。
2. 不保证 Docker、WSL、远程服务器环境可用。
3. 不会自动构造复杂对象参数。
4. 文件上传相关场景仅有基础 `form-data` 字段组织能力。
5. 依赖登录态、Session、Cookie、中间件、路由模型绑定的控制器可能需要额外手工准备上下文。
6. ThinkPHP 5/6 已有适配入口，但具体项目结构差异仍可能影响引导和调用。

## 15. 快速上手示例

### 15.1 调试服务方法

假设服务方法为：

```php
public function getUser(int $id, bool $withProfile = false)
```

操作：

1. 打开 `PHP Debug Tools`。
2. 搜索 `getUser`。
3. 选择目标方法。
4. 打开 `参数` 标签。
5. 填写：

```json
[100, true]
```

6. 点击执行按钮。
7. 在 `执行结果` 查看返回值。

### 15.2 调试控制器 JSON 请求

假设控制器方法需要接收 JSON：

```php
public function save()
{
    $data = request()->post();
    return json($data);
}
```

操作：

1. 搜索并选择 `save` 方法。
2. 请求方法选择 `POST`。
3. 打开 `请求体` 标签。
4. 切到 `Body` 子标签。
5. Body 模式选择 `JSON`。
6. 填写：

```json
{
  "name": "张三",
  "age": 18
}
```

7. 点击执行按钮。
8. 查看返回值或异常信息。

### 15.3 调试控制器 Query 请求

假设控制器方法通过 Query 获取参数：

```php
public function detail()
{
    $id = request()->get('id');
    return ['id' => $id];
}
```

操作：

1. 搜索并选择 `detail` 方法。
2. 请求方法选择 `GET`。
3. 打开 `请求体` 标签。
4. 切到 `Query` 子标签。
5. 新增参数：

| 参数名 | 类型 | 示例值 | 说明 |
| --- | --- | --- | --- |
| id | integer | 100 | 用户 ID |

6. 点击执行按钮。
7. 查看 `执行结果`。

## 16. 附录：运行时文件说明

| 文件 | 作用 |
| --- | --- |
| `.php-debug-tools/bootstrap.php` | 读取运行时配置，选择 ThinkPHP 适配器，统一处理基础错误 |
| `.php-debug-tools/adapters/thinkphp5.php` | ThinkPHP 5 适配入口 |
| `.php-debug-tools/adapters/thinkphp6.php` | 通用调用实现，包含自动加载、请求上下文、容器解析、反射调用和结果格式化 |
| `.php-debug-tools/invoke-service.php` | 服务方法 CLI 入口 |
| `.php-debug-tools/invoke-controller.php` | 控制器方法 CLI 入口 |
| `.php-debug-tools/runtime-config.json` | 记录运行时版本、框架适配器和入口文件 |
| `.php-debug-tools/toolwindow-payload.json` | 工具窗口执行时生成的本次调用参数 |

## 17. 附录：常用检查命令

检查 PHP：

```powershell
php -v
```

检查 Xdebug：

```powershell
php --ri xdebug
```

检查 Composer 自动加载：

```powershell
composer dump-autoload
```

运行插件开发环境：

```powershell
.\gradlew.bat runIde
```

运行测试：

```powershell
.\gradlew.bat test
```

验证插件兼容性：

```powershell
.\gradlew.bat verifyPlugin
```
