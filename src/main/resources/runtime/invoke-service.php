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

$GLOBALS['phpDebugToolsPayload'] = $payload;
$bootstrapResult = require __DIR__ . '/bootstrap.php';

// bootstrap.php 把 adapter 返回值包进 result 字段，这里展开 adapter 的真实结构
$target = isset($bootstrapResult['result']) && is_array($bootstrapResult['result'])
    ? $bootstrapResult['result']
    : $bootstrapResult;

$adapterName = isset($bootstrapResult['adapter']) ? $bootstrapResult['adapter'] : '';

$response = array(
    'status' => isset($target['status']) ? $target['status'] : 'error',
    'stage' => isset($target['stage']) ? $target['stage'] : 'target',
    'message' => isset($target['message']) ? $target['message'] : '',
    'result' => isset($target['result']) ? $target['result'] : null,
    'resultType' => isset($target['resultType']) ? $target['resultType'] : '',
    'resultText' => isset($target['resultText']) ? $target['resultText'] : '',
    'consoleText' => isset($target['consoleText']) ? $target['consoleText'] : '',
    'exception' => isset($target['exception']) ? $target['exception'] : null,
    'exceptionText' => isset($target['exceptionText']) ? $target['exceptionText'] : '',
    'request' => array(
        'class' => isset($payload['class']) ? $payload['class'] : '',
        'method' => isset($payload['method']) ? $payload['method'] : '',
        'type' => isset($payload['type']) ? $payload['type'] : 'service',
        'args' => isset($payload['args']) && is_array($payload['args']) ? $payload['args'] : array(),
    ),
    'bootstrap' => array(
        'adapter' => $adapterName,
    ),
);

echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
