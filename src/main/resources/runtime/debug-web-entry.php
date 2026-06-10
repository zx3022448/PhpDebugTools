<?php

declare(strict_types=1);

$payloadName = $_GET['payload'] ?? '';
$payloadFile = __DIR__ . '/' . basename($payloadName);
$payload = is_file($payloadFile)
    ? json_decode((string) file_get_contents($payloadFile), true) ?: []
    : [];

$GLOBALS['phpDebugToolsPayload'] = [
    'stage' => 'debug_web_entry',
    'query' => $_GET,
    'payload' => $payload,
];
$bootstrapResult = require __DIR__ . '/bootstrap.php';

if (!headers_sent()) {
    header('Content-Type: application/json; charset=utf-8');
}

echo json_encode([
    'status' => 'ok',
    'stage' => 'target',
    'message' => 'web debug entry reached',
    'payload' => $payload,
    'bootstrap' => $bootstrapResult,
], JSON_UNESCAPED_UNICODE);
