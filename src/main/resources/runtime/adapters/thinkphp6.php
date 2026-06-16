<?php

return array(
    'name' => 'thinkphp6',
    'bootstrap' => function ($payload) {
        return array('status' => 'ok', 'framework' => 'thinkphp6', 'payload' => is_array($payload) ? $payload : array());
    },
);
