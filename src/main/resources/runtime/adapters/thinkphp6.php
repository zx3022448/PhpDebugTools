<?php

declare(strict_types=1);

return [
    'name' => 'thinkphp6',
    'bootstrap' => static function (array $payload): array {
        return ['status' => 'ok', 'framework' => 'thinkphp6', 'payload' => $payload];
    },
];
