<?php

if (!function_exists('php_debug_tools_fail')) {
    /**
     * 输出统一错误结构，避免运行时脚本直接抛出难以理解的错误。
     *
     * @param array $error 错误信息
     */
    function php_debug_tools_fail($error)
    {
        $payload = json_encode($error, JSON_UNESCAPED_UNICODE);

        if (PHP_SAPI === 'cli') {
            fwrite(STDERR, $payload !== false ? $payload : '');
            exit(1);
        }

        if (!headers_sent()) {
            http_response_code(500);
            header('Content-Type: application/json; charset=utf-8');
        }

        echo $payload !== false ? $payload : '';
        exit(1);
    }
}

$configPath = __DIR__ . '/runtime-config.json';
$configContent = file_get_contents($configPath);
$config = json_decode($configContent !== false ? $configContent : '', true);
if (!is_array($config)) {
    $config = array();
}
$adapter = isset($config['frameworkAdapter']) ? $config['frameworkAdapter'] : 'thinkphp6';
$runtimePayload = isset($GLOBALS['phpDebugToolsPayload']) && is_array($GLOBALS['phpDebugToolsPayload'])
    ? $GLOBALS['phpDebugToolsPayload']
    : array();
$allowedAdapters = array('thinkphp5', 'thinkphp6');

if (!in_array($adapter, $allowedAdapters, true)) {
    php_debug_tools_fail(array(
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter is not allowed',
    ));
}

$adapterFile = __DIR__ . '/adapters/' . $adapter . '.php';

if (!is_file($adapterFile)) {
    php_debug_tools_fail(array(
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter file not found',
    ));
}

$adapterDefinition = require $adapterFile;

if (!is_array($adapterDefinition)) {
    php_debug_tools_fail(array(
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter definition must be an array',
    ));
}

$bootstrap = isset($adapterDefinition['bootstrap']) ? $adapterDefinition['bootstrap'] : null;

if (!is_callable($bootstrap)) {
    php_debug_tools_fail(array(
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter bootstrap is not callable',
    ));
}

return array(
    'status' => 'ok',
    'stage' => 'bootstrap',
    'adapter' => isset($adapterDefinition['name']) ? $adapterDefinition['name'] : $adapter,
    'result' => $bootstrap($runtimePayload),
);
