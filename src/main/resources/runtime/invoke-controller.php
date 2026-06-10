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
