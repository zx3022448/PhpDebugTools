<?php

declare(strict_types=1);

return [
    'name' => 'thinkphp5',
    'bootstrap' => static function (array $payload): array {
        return ['status' => 'ok', 'framework' => 'thinkphp5', 'payload' => $payload];
    },
];
