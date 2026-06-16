<?php

return array(
    'name' => 'thinkphp5',
    'bootstrap' => function ($payload) {
        return array('status' => 'ok', 'framework' => 'thinkphp5', 'payload' => is_array($payload) ? $payload : array());
    },
);
