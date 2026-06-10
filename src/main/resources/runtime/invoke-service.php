<?php

declare(strict_types=1);

$payloadFile = $argv[1] ?? '';
$payload = [];

if (is_file($payloadFile) && is_readable($payloadFile)) {
    $payload = json_decode((string) file_get_contents($payloadFile), true) ?: [];
}

$GLOBALS['phpDebugToolsPayload'] = [
    'stage' => 'invoke_service',
    'request' => $payload,
];
$bootstrapResult = require __DIR__ . '/bootstrap.php';

echo json_encode([
    'status' => 'ok',
    'stage' => 'invoke_service',
    'request' => $payload,
    'bootstrap' => $bootstrapResult,
], JSON_UNESCAPED_UNICODE);
