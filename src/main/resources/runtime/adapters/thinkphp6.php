<?php

require_once __DIR__ . '/common.php';

return array(
    'name' => 'thinkphp6',
    'bootstrap' => function ($payload) {
        return php_debug_tools_safely_invoke(is_array($payload) ? $payload : array());
    },
);
