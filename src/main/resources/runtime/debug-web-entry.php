<?php

declare(strict_types=1);

$GLOBALS['phpDebugToolsPayload'] = [
    'stage' => 'debug_web_entry',
    'query' => $_GET,
];
$bootstrapResult = require __DIR__ . '/bootstrap.php';

if (!headers_sent()) {
    header('Content-Type: application/json; charset=utf-8');
}

echo json_encode([
    'status' => 'ok',
    'stage' => 'debug_web_entry',
    'query' => $_GET,
    'bootstrap' => $bootstrapResult,
], JSON_UNESCAPED_UNICODE);
