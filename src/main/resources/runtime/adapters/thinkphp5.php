<?php

// 共享 thinkphp6 的实现，仅覆盖框架名以保持适配器独立性
require __DIR__ . '/thinkphp6.php';

return array(
    'name' => 'thinkphp5',
    'bootstrap' => function ($payload) {
        return php_debug_tools_safely_invoke(is_array($payload) ? $payload : array());
    },
);
