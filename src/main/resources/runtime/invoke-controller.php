<?php

$payloadPath = isset($argv[1]) ? $argv[1] : '';
$payload = array();

if (is_file($payloadPath) && is_readable($payloadPath)) {
    $payloadContent = file_get_contents($payloadPath);
    $decodedPayload = json_decode($payloadContent !== false ? $payloadContent : '', true);
    if (is_array($decodedPayload)) {
        $payload = $decodedPayload;
    }
}

$GLOBALS['phpDebugToolsPayload'] = array(
    'stage' => 'invoke_controller',
    'request' => $payload,
);
$bootstrapResult = require __DIR__ . '/bootstrap.php';

echo json_encode(array(
    'status' => 'ok',
    'stage' => 'target',
    'message' => 'controller invoked',
    'request' => array(
        'class' => isset($payload['class']) ? $payload['class'] : '',
        'method' => isset($payload['method']) ? $payload['method'] : '',
        'request' => isset($payload['request']) && is_array($payload['request']) ? $payload['request'] : array(),
        'args' => isset($payload['args']) && is_array($payload['args']) ? $payload['args'] : array(),
    ),
    'bootstrap' => $bootstrapResult,
), JSON_UNESCAPED_UNICODE);
