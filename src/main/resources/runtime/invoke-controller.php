<?php

declare(strict_types=1);

$payloadPath = $argv[1] ?? '';
$payload = [];

if (is_file($payloadPath) && is_readable($payloadPath)) {
    $payload = json_decode((string) file_get_contents($payloadPath), true) ?: [];
}

$GLOBALS['phpDebugToolsPayload'] = [
    'stage' => 'invoke_controller',
    'request' => $payload,
];
$bootstrapResult = require __DIR__ . '/bootstrap.php';

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
    'bootstrap' => $bootstrapResult,
], JSON_UNESCAPED_UNICODE);
