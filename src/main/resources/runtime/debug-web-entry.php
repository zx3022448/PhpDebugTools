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
