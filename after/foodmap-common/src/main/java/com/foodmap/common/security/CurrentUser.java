package com.foodmap.common.security;

public record CurrentUser(
        Long userId,
        Long accountId,
        String accountName
) {
}
