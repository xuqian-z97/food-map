package com.foodmap.common.security;

import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacTokenCodecTest {

    @Test
    void shouldIssueAndParseAccessToken() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        OffsetDateTime expiresTime = OffsetDateTime.now().plusHours(1);

        String token = codec.issueAccessToken(1001L, 2001L, expiresTime);
        TokenClaims claims = codec.parseAccessToken(token);

        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.accountId()).isEqualTo(1001L);
        assertThat(claims.userId()).isEqualTo(2001L);
        assertThat(claims.expiresTime()).isEqualTo(expiresTime);
    }

    @Test
    void shouldRejectTamperedToken() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String token = codec.issueAccessToken(1001L, 2001L, OffsetDateTime.now().plusHours(1));

        assertThatThrownBy(() -> codec.parseAccessToken(token + "x"))
                .isInstanceOf(FoodMapException.class);
    }
}
