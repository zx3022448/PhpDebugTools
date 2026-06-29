<?php

if (!function_exists('php_debug_tools_extract_runtime_request')) {
    /**
     * 提取运行时请求上下文，统一 controller/service 调用时的输入结构。
     *
     * @param array $payload 方法调试 payload
     * @return array<string, mixed>
     */
    function php_debug_tools_extract_runtime_request($payload)
    {
        $request = isset($payload['request']) && is_array($payload['request'])
            ? $payload['request']
            : array();
        $body = isset($request['body']) && is_array($request['body'])
            ? $request['body']
            : array();

        return array(
            'method' => isset($request['method']) ? strtoupper((string) $request['method']) : 'GET',
            'query' => isset($request['query']) && is_array($request['query']) ? $request['query'] : array(),
            'headers' => isset($request['headers']) && is_array($request['headers']) ? $request['headers'] : array(),
            'bodyMode' => isset($body['mode']) ? (string) $body['mode'] : 'none',
            'body' => isset($body['content']) && is_array($body['content']) ? $body['content'] : array(),
        );
    }
}

if (!function_exists('php_debug_tools_normalize_value')) {
    /**
     * 规范化返回值，确保既可 JSON 编码又可被页面友好展示。
     *
     * @param mixed $value 原始返回值
     * @param int $depth 当前递归层级
     * @return mixed
     */
    function php_debug_tools_normalize_value($value, $depth = 0)
    {
        if ($depth >= 4) {
            if (is_object($value)) {
                return 'object(' . get_class($value) . ')';
            }
            if (is_array($value)) {
                return 'array(' . count($value) . ')';
            }
            return $value;
        }

        if ($value === null || is_scalar($value)) {
            return $value;
        }

        // 提取 ThinkPHP Response 对象中的数据
        if (is_object($value) && is_a($value, 'think\\Response', false)) {
            if (method_exists($value, 'getData')) {
                return php_debug_tools_normalize_value($value->getData(), $depth + 1);
            }
            if (method_exists($value, 'getContent')) {
                $content = $value->getContent();
                $decoded = json_decode($content, true);
                return $decoded !== null ? $decoded : $content;
            }
        }

        // PHP 8+ 的 Stringable 接口；老版本通过 method_exists 兜底
        if (interface_exists('\\Stringable', false) && $value instanceof \Stringable) {
            return (string) $value;
        }
        if (is_object($value) && !($value instanceof \JsonSerializable) && method_exists($value, '__toString')) {
            return (string) $value;
        }

        if (is_array($value)) {
            $normalized = array();
            foreach ($value as $key => $item) {
                $normalized[$key] = php_debug_tools_normalize_value($item, $depth + 1);
            }
            return $normalized;
        }

        if ($value instanceof \JsonSerializable) {
            return php_debug_tools_normalize_value($value->jsonSerialize(), $depth + 1);
        }

        if (is_object($value)) {
            $publicProperties = get_object_vars($value);
            if (!empty($publicProperties)) {
                $normalized = array();
                foreach ($publicProperties as $key => $item) {
                    $normalized[$key] = php_debug_tools_normalize_value($item, $depth + 1);
                }
                return array(
                    '__class' => get_class($value),
                    '__data' => $normalized,
                );
            }

            return 'object(' . get_class($value) . ')';
        }

        return (string) $value;
    }
}

if (!function_exists('php_debug_tools_render_result_text')) {
    /**
     * 渲染统一的结果文本，避免 Kotlin 侧必须依赖完整 JSON 解析器。
     *
     * @param mixed $value 规范化后的结果值
     * @return string
     */
    function php_debug_tools_render_result_text($value)
    {
        if ($value === null) {
            return 'null';
        }

        if (is_bool($value)) {
            return $value ? 'true' : 'false';
        }

        if (is_scalar($value)) {
            return (string) $value;
        }

        $encoded = json_encode($value, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        if ($encoded !== false) {
            return $encoded;
        }

        return is_object($value)
            ? 'object(' . get_class($value) . ')'
            : '[unserializable result]';
    }
}

if (!function_exists('php_debug_tools_build_exception_payload')) {
    /**
     * 构建统一异常结构。
     *
     * @param \Throwable $throwable 捕获到的异常
     * @return array<string, string>
     */
    function php_debug_tools_build_exception_payload($throwable)
    {
        return array(
            'type' => get_class($throwable),
            'message' => $throwable->getMessage(),
            'file' => $throwable->getFile(),
            'line' => (string) $throwable->getLine(),
        );
    }
}

if (!function_exists('php_debug_tools_render_exception_text')) {
    /**
     * 将异常结构渲染为可直接展示的文本。
     *
     * @param array<string, string> $exception 异常信息
     * @return string
     */
    function php_debug_tools_render_exception_text($exception)
    {
        $parts = array();
        if (!empty($exception['type'])) {
            $parts[] = $exception['type'];
        }
        if (!empty($exception['message'])) {
            $parts[] = $exception['message'];
        }
        if (!empty($exception['file']) && !empty($exception['line'])) {
            $parts[] = $exception['file'] . ':' . $exception['line'];
        }
        return implode(PHP_EOL, $parts);
    }
}

if (!function_exists('php_debug_tools_convert_args_to_map')) {
    /**
     * 将位置参数转换为按顺序编号的映射，便于注入请求体字段。
     *
     * @param array<int, mixed> $args 参数列表
     * @return array<string, mixed>
     */
    function php_debug_tools_convert_args_to_map($args)
    {
        $result = array();
        foreach ($args as $index => $item) {
            $result['arg' . $index] = $item;
        }
        return $result;
    }
}

if (!function_exists('php_debug_tools_prepare_superglobals')) {
    /**
     * 同步请求上下文到超全局变量，为依赖原生请求数据的控制器提供兼容支持。
     *
     * @param array<string, mixed> $payload 方法调试 payload
     * @return void
     */
    function php_debug_tools_prepare_superglobals($payload)
    {
        $request = php_debug_tools_extract_runtime_request($payload);
        $query = $request['query'];
        $headers = $request['headers'];
        $body = $request['body'];
        $bodyMode = $request['bodyMode'];

        $_GET = $query;
        $_REQUEST = $query;
        $_POST = array();
        $_SERVER['REQUEST_METHOD'] = $request['method'];
        $_SERVER['CONTENT_TYPE'] = '';

        foreach ($headers as $name => $value) {
            $serverKey = 'HTTP_' . strtoupper(str_replace('-', '_', (string) $name));
            $_SERVER[$serverKey] = is_scalar($value) ? (string) $value : json_encode($value, JSON_UNESCAPED_UNICODE);
        }

        if ($bodyMode === 'form-data' || $bodyMode === 'x-www-form-urlencoded') {
            $_POST = $body;
            $_REQUEST = array_merge($_REQUEST, $body);
            $_SERVER['CONTENT_TYPE'] = $bodyMode === 'form-data'
                ? 'multipart/form-data'
                : 'application/x-www-form-urlencoded';
            return;
        }

        if ($bodyMode === 'json') {
            $_POST = $body;
            $_REQUEST = array_merge($_REQUEST, $body);
            $_SERVER['CONTENT_TYPE'] = 'application/json';
        }
    }
}

if (!function_exists('php_debug_tools_safely_invoke')) {
    /**
     * 兼容 PHP 5.6 与 7.0+ 的统一调用入口。
     *
     * PHP 5.6 没有 Throwable 接口，只能捕获 Exception；这里在运行时检测
     * 接口是否存在，以便在新版本上也能兜住 Error。
     *
     * @param array<string, mixed> $payload 方法调试 payload
     * @return array<string, mixed>
     */
    function php_debug_tools_safely_invoke($payload)
    {
        $buildError = function ($throwable) {
            $exception = php_debug_tools_build_exception_payload($throwable);
            return array(
                'status' => 'error',
                'stage' => 'target',
                'message' => $throwable->getMessage(),
                'result' => null,
                'resultType' => '',
                'resultText' => '',
                'consoleText' => php_debug_tools_consume_console_buffer(),
                'exception' => $exception,
                'exceptionText' => php_debug_tools_render_exception_text($exception),
            );
        };

        php_debug_tools_start_console_buffer();

        // 优先使用 Throwable（PHP 7.0+），兼容 5.6 的 Exception
        if (interface_exists('Throwable', false)) {
            try {
                return php_debug_tools_attach_console_text(php_debug_tools_call_target_method($payload));
            } catch (\Throwable $throwable) {
                return $buildError($throwable);
            }
        }

        try {
            return php_debug_tools_attach_console_text(php_debug_tools_call_target_method($payload));
        } catch (\Exception $throwable) {
            return $buildError($throwable);
        }
    }
}

if (!function_exists('php_debug_tools_start_console_buffer')) {
    /**
     * 捕获目标方法中 echo、print、var_dump 等直接输出，避免破坏最终 JSON。
     *
     * @return void
     */
    function php_debug_tools_start_console_buffer()
    {
        $GLOBALS['phpDebugToolsConsoleBufferLevel'] = ob_get_level();
        ob_start();
    }
}

if (!function_exists('php_debug_tools_consume_console_buffer')) {
    /**
     * 读取并关闭由运行时创建的输出缓冲区。
     *
     * @return string
     */
    function php_debug_tools_consume_console_buffer()
    {
        $startLevel = isset($GLOBALS['phpDebugToolsConsoleBufferLevel'])
            ? (int) $GLOBALS['phpDebugToolsConsoleBufferLevel']
            : ob_get_level();
        $consoleText = '';

        while (ob_get_level() > $startLevel) {
            $chunk = ob_get_clean();
            if ($chunk !== false) {
                $consoleText = $chunk . $consoleText;
            }
        }

        unset($GLOBALS['phpDebugToolsConsoleBufferLevel']);
        return $consoleText;
    }
}

if (!function_exists('php_debug_tools_attach_console_text')) {
    /**
     * 将目标方法产生的控制台输出附加到结构化结果中。
     *
     * @param array<string, mixed> $result 目标方法结果
     * @return array<string, mixed>
     */
    function php_debug_tools_attach_console_text($result)
    {
        if (!is_array($result)) {
            $result = array();
        }
        $result['consoleText'] = php_debug_tools_consume_console_buffer();
        return $result;
    }
}

if (!function_exists('php_debug_tools_ensure_autoload')) {
    /**
     * 确保项目 Composer autoload 已加载，使服务类可被 class_exists 找到。
     * adapter 位于 .php-debug-tools/adapters/，项目根为上两级。
     *
     * @return void
     */
    function php_debug_tools_ensure_autoload()
    {
        static $loaded = false;
        if ($loaded) {
            return;
        }
        $loaded = true;
        $projectRoot = dirname(dirname(__DIR__));
        
        // ThinkPHP 的 base.php 会加载 autoload 并初始化门面系统
        $thinkBasePath = $projectRoot . '/thinkphp/base.php';
        if (is_file($thinkBasePath)) {
            require_once $thinkBasePath;
            
            // 确保辅助函数被加载
            $helperPath = $projectRoot . '/thinkphp/helper.php';
            if (is_file($helperPath)) {
                require_once $helperPath;
            }
            
            // base.php 已包含 autoload，直接返回
            return;
        }
        
        // 如果没有 base.php，则手动加载 autoload
        $autoload = $projectRoot . '/vendor/autoload.php';
        if (is_file($autoload)) {
            require_once $autoload;
        }
        
        // 尝试初始化 App 以支持门面
        if (function_exists('app')) {
            try {
                app();
            } catch (\Exception $e) {
                // 忽略
            }
        }
    }
}

if (!function_exists('php_debug_tools_create_request_object')) {
    /**
     * 创建并初始化 ThinkPHP Request 对象。
     *
     * @return object|null
     */
    function php_debug_tools_create_request_object()
    {
        if (!class_exists('think\\Request', false)) {
            return null;
        }
        
        try {
            // 优先使用 instance() 方法（TP5）
            if (method_exists('think\\Request', 'instance')) {
                $request = \think\Request::instance();
                if ($request !== null) {
                    return $request;
                }
            }
            
            // 尝试直接创建
            return new \think\Request();
        } catch (\Exception $e) {
            return null;
        }
    }
}

if (!function_exists('php_debug_tools_build_route_context')) {
    /**
     * 根据目标控制器推断路由分发后 Request 中常见的上下文信息。
     *
     * @param string $class 控制器完整类名
     * @param string $method 当前 action 方法名
     * @param array<string, mixed> $payload 方法调试 payload
     * @return array<string, mixed>
     */
    function php_debug_tools_build_route_context($class, $method, $payload)
    {
        $normalizedClass = trim($class, '\\');
        $parts = $normalizedClass === '' ? array() : explode('\\', $normalizedClass);
        $shortClass = empty($parts) ? '' : end($parts);
        $controller = preg_replace('/Controller$/', '', $shortClass);
        $module = '';

        $controllerIndex = -1;
        foreach ($parts as $index => $part) {
            if (strtolower($part) === 'controller') {
                $controllerIndex = $index;
                break;
            }
        }
        if ($controllerIndex > 0) {
            $module = (string) $parts[$controllerIndex - 1];
        }

        $request = php_debug_tools_extract_runtime_request($payload);
        $route = isset($payload['route']) && is_array($payload['route'])
            ? $payload['route']
            : array_merge($request['query'], $request['body']);

        $pathParts = array();
        if ($module !== '') {
            $pathParts[] = $module;
        }
        if ($controller !== '') {
            $pathParts[] = $controller;
        }
        if ($method !== '') {
            $pathParts[] = $method;
        }

        $pathinfo = implode('/', array_map('strtolower', $pathParts));
        $url = '/' . $pathinfo;
        if (!empty($request['query'])) {
            $queryString = http_build_query($request['query']);
            if ($queryString !== '') {
                $url .= '?' . $queryString;
            }
        }

        return array(
            'module' => $module,
            'controller' => $controller,
            'action' => $method,
            'route' => $route,
            'dispatch' => array(
                'type' => 'controller',
                'module' => $module,
                'controller' => $controller,
                'action' => $method,
                'class' => $normalizedClass,
                'method' => $method,
            ),
            'pathinfo' => $pathinfo,
            'path' => $pathinfo,
            'url' => $url,
            'baseUrl' => '/' . $pathinfo,
            'baseFile' => isset($_SERVER['SCRIPT_NAME']) ? $_SERVER['SCRIPT_NAME'] : '',
            'root' => '',
            'domain' => isset($_SERVER['HTTP_HOST']) ? $_SERVER['HTTP_HOST'] : '',
            'ext' => '',
            'method' => $request['method'],
            'get' => $request['query'],
            'post' => $_POST,
            'request' => $_REQUEST,
            'param' => array_merge($request['query'], $_POST, $route),
            'header' => $request['headers'],
            'server' => $_SERVER,
        );
    }
}

if (!function_exists('php_debug_tools_apply_request_context')) {
    /**
     * 将路由分发阶段通常写入的上下文补到 Request 对象中。
     *
     * @param object $request Request 对象
     * @param array<string, mixed> $context 路由上下文
     * @return void
     */
    function php_debug_tools_apply_request_context($request, $context)
    {
        if (!is_object($request)) {
            return;
        }

        $methodFields = array(
            'module',
            'controller',
            'action',
            'route',
            'dispatch',
            'pathinfo',
            'path',
            'url',
            'baseUrl',
            'baseFile',
            'root',
            'domain',
            'ext',
        );
        foreach ($methodFields as $field) {
            if (array_key_exists($field, $context) && method_exists($request, $field)) {
                try {
                    $request->$field($context[$field]);
                } catch (\Exception $e) {
                    // 某些 ThinkPHP 版本的 getter 不接受写入，继续走反射兜底。
                }
            }
        }

        foreach ($context as $field => $value) {
            php_debug_tools_set_object_member($request, $field, $value);
        }
    }
}

if (!function_exists('php_debug_tools_bind_request_to_container')) {
    /**
     * 将补齐上下文后的 Request 放回 ThinkPHP 容器，兼容 request() 辅助函数。
     *
     * @param object $request Request 对象
     * @return void
     */
    function php_debug_tools_bind_request_to_container($request)
    {
        if (!is_object($request)) {
            return;
        }

        if (function_exists('app')) {
            try {
                $app = app();
                if ($app && method_exists($app, 'instance')) {
                    $app->instance('request', $request);
                }
            } catch (\Exception $e) {
                // 容器绑定失败时继续尝试静态 Container。
            }
        }

        if (class_exists('think\\Container', false)) {
            try {
                $container = \think\Container::getInstance();
                if ($container && method_exists($container, 'instance')) {
                    $container->instance('request', $request);
                }
            } catch (\Exception $e) {
                // 不阻断目标方法执行。
            }
        }
    }
}

if (!function_exists('php_debug_tools_set_object_member')) {
    /**
     * 尽量兼容不同 ThinkPHP 版本的 Request 私有/受保护字段写入。
     *
     * @param object $object 目标对象
     * @param string $name 字段名
     * @param mixed $value 字段值
     * @return void
     */
    function php_debug_tools_set_object_member($object, $name, $value)
    {
        try {
            $refClass = new \ReflectionClass($object);
            while ($refClass) {
                if ($refClass->hasProperty($name)) {
                    $property = $refClass->getProperty($name);
                    $property->setAccessible(true);
                    $property->setValue($object, $value);
                    return;
                }
                $refClass = $refClass->getParentClass();
            }
        } catch (\Exception $e) {
            // 反射失败不阻断目标方法执行。
        }
    }
}

if (!class_exists('PhpDebugToolsFallbackRequest', false)) {
    /**
     * PHP 5 兼容的 Request 兜底对象。
     *
     * 匿名类需要 PHP 7+，TP5 项目仍可能运行在 PHP 5.x，因此这里使用命名类，
     * 避免运行时在解析 adapter 文件时直接触发语法错误。
     */
    class PhpDebugToolsFallbackRequest
    {
        private $module = '';
        private $controller = '';
        private $action = '';
        private $route = array();
        private $dispatch = null;
        private $param = array();
        private $pathinfo = '';
        private $path = '';
        private $url = '';
        private $baseUrl = '';
        private $baseFile = '';
        private $root = '';
        private $domain = '';
        private $ext = '';
        private $header = array();

        public function post($name = '', $default = null) {
            if ($name === '') {
                return $_POST;
            }
            return isset($_POST[$name]) ? $_POST[$name] : $default;
        }

        public function get($name = '', $default = null) {
            if ($name === '') {
                return $_GET;
            }
            return isset($_GET[$name]) ? $_GET[$name] : $default;
        }

        public function param($name = '', $default = null) {
            $params = !empty($this->param)
                ? $this->param
                : array_merge($_GET, $_POST, $this->route);
            if ($name === '') {
                return $params;
            }
            if (isset($params[$name])) {
                return $params[$name];
            }
            return $default;
        }

        public function request($name = '', $default = null) {
            return $this->param($name, $default);
        }

        public function method() {
            return isset($_SERVER['REQUEST_METHOD']) ? $_SERVER['REQUEST_METHOD'] : 'GET';
        }

        public function isPost() {
            return $this->method() === 'POST';
        }

        public function isGet() {
            return $this->method() === 'GET';
        }

        public function module($module = null) {
            return $this->setOrGet('module', $module);
        }

        public function controller($controller = null) {
            return $this->setOrGet('controller', $controller);
        }

        public function action($action = null) {
            return $this->setOrGet('action', $action);
        }

        public function route($name = '', $default = null) {
            if (is_array($name)) {
                $this->route = $name;
                $this->param = array_merge($_GET, $_POST, $this->route);
                return $this->route;
            }
            if ($name === '') {
                return $this->route;
            }
            return isset($this->route[$name]) ? $this->route[$name] : $default;
        }

        public function dispatch($dispatch = null) {
            return $this->setOrGet('dispatch', $dispatch);
        }

        public function pathinfo($pathinfo = null) {
            return $this->setOrGet('pathinfo', $pathinfo);
        }

        public function path($path = null) {
            return $this->setOrGet('path', $path);
        }

        public function url($url = null) {
            return $this->setOrGet('url', $url);
        }

        public function baseUrl($baseUrl = null) {
            return $this->setOrGet('baseUrl', $baseUrl);
        }

        public function baseFile($baseFile = null) {
            return $this->setOrGet('baseFile', $baseFile);
        }

        public function root($root = null) {
            return $this->setOrGet('root', $root);
        }

        public function domain($domain = null) {
            return $this->setOrGet('domain', $domain);
        }

        public function ext($ext = null) {
            return $this->setOrGet('ext', $ext);
        }

        public function header($name = '', $default = null) {
            if ($name === '') {
                return $this->header;
            }
            $key = strtolower((string) $name);
            foreach ($this->header as $headerName => $value) {
                if (strtolower((string) $headerName) === $key) {
                    return $value;
                }
            }
            return $default;
        }

        private function setOrGet($property, $value = null) {
            if ($value !== null) {
                $this->$property = $value;
            }
            return $this->$property;
        }
    }
}

if (!function_exists('php_debug_tools_create_fallback_request')) {
    /**
     * 创建一个简单的 Request 替代对象，提供基本的 post/get 方法。
     *
     * @return object
     */
    function php_debug_tools_create_fallback_request()
    {
        return new \PhpDebugToolsFallbackRequest();
    }
}

if (!function_exists('php_debug_tools_resolve_service_instance')) {
    /**
     * 按设计文档优先级解析实例：容器 make > 无参构造 > 反射强构造。
     *
     * @param string $class 规范化后的类名（无前导 \）
     * @param bool $isController 是否为控制器
     * @return object
     */
    function php_debug_tools_resolve_service_instance($class, $isController = false)
    {
        // 控制器需要确保 App 已初始化
        if ($isController && function_exists('app')) {
            try {
                $app = app();
                // 确保 Request 已绑定到容器
                if ($app && method_exists($app, 'has') && !$app->has('request')) {
                    $requestObj = php_debug_tools_create_request_object();
                    if ($requestObj !== null && method_exists($app, 'instance')) {
                        $app->instance('request', $requestObj);
                    }
                }
            } catch (\Exception $e) {
                // 忽略
            }
        }
        
        // 1. TP5/TP6 全局 app() 容器
        if (function_exists('app')) {
            try {
                $instance = app($class);
                if (is_object($instance)) {
                    return $instance;
                }
            } catch (\Exception $e) {
                // 容器解析失败，继续下一级
            }
        }
        // 2. think\Container 静态方式（TP5/TP6 均支持）
        if (class_exists('think\\Container', false)) {
            try {
                $instance = \think\Container::getInstance()->make($class);
                if (is_object($instance)) {
                    return $instance;
                }
            } catch (\Exception $e) {
                // 容器解析失败，继续下一级
            }
        }
        // 3. 反射兜底：无参或强制构造
        $ref = new \ReflectionClass($class);
        $constructor = $ref->getConstructor();
        if ($constructor === null || $constructor->getNumberOfRequiredParameters() === 0) {
            return $ref->newInstance();
        }
        return $ref->newInstanceWithoutConstructor();
    }
}

if (!function_exists('php_debug_tools_call_target_method')) {
    /**
     * 调用目标 controller/service 方法。
     *
     * @param array<string, mixed> $payload 方法调试 payload
     * @return array<string, mixed>
     */
    function php_debug_tools_call_target_method($payload)
    {
        $className = isset($payload['class']) ? (string) $payload['class'] : '';
        $methodName = isset($payload['method']) ? (string) $payload['method'] : '';
        $isStatic = !empty($payload['static']);
        $kind = isset($payload['type']) ? (string) $payload['type'] : 'service';
        $args = isset($payload['args']) && is_array($payload['args']) ? $payload['args'] : array();

        if ($className === '' || $methodName === '') {
            throw new \RuntimeException('Missing target class or method');
        }

        // 规范化类名：去掉前导 \，避免部分 autoload 实现无法识别
        $normalizedClass = ltrim($className, '\\');

        // 加载项目 Composer autoload，让框架类和服务类可被发现
        php_debug_tools_ensure_autoload();

        if (!class_exists($normalizedClass)) {
            throw new \RuntimeException('Target class not found: ' . $className);
        }

        if (!method_exists($normalizedClass, $methodName)) {
            throw new \RuntimeException('Target method not found: ' . $className . '::' . $methodName);
        }

        if ($kind === 'controller') {
            php_debug_tools_prepare_superglobals($payload);
            $requestObj = php_debug_tools_create_request_object();
            if ($requestObj === null && function_exists('request')) {
                $requestObj = request();
            }
            if ($requestObj === null) {
                $requestObj = php_debug_tools_create_fallback_request();
            }
            php_debug_tools_apply_request_context(
                $requestObj,
                php_debug_tools_build_route_context($normalizedClass, $methodName, $payload)
            );
            php_debug_tools_bind_request_to_container($requestObj);

            if (empty($args)) {
                $request = php_debug_tools_extract_runtime_request($payload);
                $args = array_merge($request['query'], $request['body']);
                if (!is_array($args)) {
                    $args = array();
                }
                $args = array_values($args);
            }
        }

        $reflectionMethod = new \ReflectionMethod($normalizedClass, $methodName);
        $targetInstance = null;
        if (!$isStatic) {
            if ($reflectionMethod->isConstructor()) {
                throw new \RuntimeException('Target method cannot be a constructor');
            }
            // 容器优先解析实例，保证依赖注入生效
            $targetInstance = php_debug_tools_resolve_service_instance($normalizedClass, $kind === 'controller');
            
            // 为控制器注入 Request 对象
            if ($kind === 'controller') {
                // 使用反射强制设置 request 属性
                $refClass = new \ReflectionClass($targetInstance);
                if ($refClass->hasProperty('request')) {
                    $refProp = $refClass->getProperty('request');
                    $refProp->setAccessible(true);
                    $refProp->setValue($targetInstance, $requestObj);
                }
            }
        }

        $result = $reflectionMethod->invokeArgs($targetInstance, $args);
        $normalized = php_debug_tools_normalize_value($result);

        return array(
            'status' => 'ok',
            'stage' => 'target',
            'message' => $kind . ' method invoked',
            'result' => $normalized,
            'resultType' => is_object($result) ? get_class($result) : gettype($result),
            'resultText' => php_debug_tools_render_result_text($normalized),
            'exception' => null,
            'exceptionText' => '',
        );
    }
}

return true;
