package com.foodmap.common.security;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String accountName
) {
}
