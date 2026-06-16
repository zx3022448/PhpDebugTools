<?php

$payloadFile = isset($argv[1]) ? $argv[1] : '';
$payload = array();

if (is_file($payloadFile) && is_readable($payloadFile)) {
    $payloadContent = file_get_contents($payloadFile);
    $decodedPayload = json_decode($payloadContent !== false ? $payloadContent : '', true);
    if (is_array($decodedPayload)) {
        $payload = $decodedPayload;
    }
}

$GLOBALS['phpDebugToolsPayload'] = array(
    'stage' => 'invoke_service',
    'request' => $payload,
);
$bootstrapResult = require __DIR__ . '/bootstrap.php';

echo json_encode(array(
    'status' => 'ok',
    'stage' => 'invoke_service',
    'request' => $payload,
    'bootstrap' => $bootstrapResult,
), JSON_UNESCAPED_UNICODE);
