<?php

declare(strict_types=1);

if (!function_exists('php_debug_tools_fail')) {
    function php_debug_tools_fail(array $error): void
    {
        $payload = json_encode($error, JSON_UNESCAPED_UNICODE);

        if (PHP_SAPI === 'cli') {
            fwrite(STDERR, (string) $payload);
            exit(1);
        }

        if (!headers_sent()) {
            http_response_code(500);
            header('Content-Type: application/json; charset=utf-8');
        }

        echo $payload;
        exit(1);
    }
}

$configPath = __DIR__ . '/runtime-config.json';
$config = json_decode((string) file_get_contents($configPath), true) ?: [];
$adapter = $config['frameworkAdapter'] ?? 'thinkphp6';
$runtimePayload = $GLOBALS['phpDebugToolsPayload'] ?? [];
$allowedAdapters = ['thinkphp5', 'thinkphp6'];

if (!in_array($adapter, $allowedAdapters, true)) {
    php_debug_tools_fail([
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter is not allowed',
    ]);
}

$adapterFile = __DIR__ . '/adapters/' . $adapter . '.php';

if (!is_file($adapterFile)) {
    php_debug_tools_fail([
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter file not found',
    ]);
}

$adapterDefinition = require $adapterFile;

if (!is_array($adapterDefinition)) {
    php_debug_tools_fail([
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter definition must be an array',
    ]);
}

$bootstrap = $adapterDefinition['bootstrap'] ?? null;

if (!is_callable($bootstrap)) {
    php_debug_tools_fail([
        'status' => 'error',
        'stage' => 'framework',
        'message' => 'Framework adapter bootstrap is not callable',
    ]);
}

return [
    'status' => 'ok',
    'stage' => 'bootstrap',
    'adapter' => $adapterDefinition['name'] ?? $adapter,
    'result' => $bootstrap(is_array($runtimePayload) ? $runtimePayload : []),
];
