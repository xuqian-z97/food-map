package com.foodmap.common.security;

import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacTokenCodecTest {

    @Test
    void shouldIssueAndParseUserIdOnlyAccessToken() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        OffsetDateTime expiresTime = OffsetDateTime.now().plusHours(1);

        String token = codec.issueAccessToken(2001L, expiresTime);
        TokenClaims claims = codec.parseAccessToken(token);

        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.accountId()).isNull();
        assertThat(claims.userId()).isEqualTo(2001L);
        assertThat(claims.expiresTime()).isEqualTo(expiresTime);
    }

    @Test
    void shouldIssueAndParseUserIdOnlyRefreshToken() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        OffsetDateTime expiresTime = OffsetDateTime.now().plusDays(30);

        String token = codec.issueRefreshToken(2001L, expiresTime);
        TokenClaims claims = codec.parseRefreshToken(token);

        assertThat(claims.tokenType()).isEqualTo(TokenType.REFRESH);
        assertThat(claims.accountId()).isNull();
        assertThat(claims.userId()).isEqualTo(2001L);
        assertThat(claims.expiresTime()).isEqualTo(expiresTime);
    }

    @Test
    void shouldParseLegacyAccessTokenDuringMigration() {
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

    @Test
    void shouldRejectAccessTokenWithAppendedSegment() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String token = codec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));

        assertThatThrownBy(() -> codec.parseAccessToken(token + ".appended"))
                .isInstanceOf(FoodMapException.class)
                .satisfies(ex -> assertThat(((FoodMapException) ex).status()).isEqualTo(401));
    }

    @Test
    void shouldRejectRefreshTokenWithAppendedSegment() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String token = codec.issueRefreshToken(2001L, OffsetDateTime.now().plusDays(30));

        assertThatThrownBy(() -> codec.parseRefreshToken(token + ".appended"))
                .isInstanceOf(FoodMapException.class)
                .satisfies(ex -> assertThat(((FoodMapException) ex).status()).isEqualTo(401));
    }

    @Test
    void shouldRejectTokenWithTamperedPayload() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String token = codec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x" + "." + parts[2];

        assertThatThrownBy(() -> codec.parseAccessToken(tampered))
                .isInstanceOf(FoodMapException.class)
                .satisfies(ex -> assertThat(((FoodMapException) ex).status()).isEqualTo(401));
    }

    @Test
    void shouldRejectRefreshTokenPassedAsAccessToken() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String refreshToken = codec.issueRefreshToken(2001L, OffsetDateTime.now().plusDays(30));

        assertThatThrownBy(() -> codec.parseAccessToken(refreshToken))
                .isInstanceOf(FoodMapException.class)
                .satisfies(ex -> assertThat(((FoodMapException) ex).status()).isEqualTo(401));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {".", "..", ".suffix", ".suffix."})
    void shouldRejectAppendedSegmentsIncludingTrailingEmptyParts(String suffix) {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String access = codec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));
        String refresh = codec.issueRefreshToken(2001L, OffsetDateTime.now().plusDays(30));

        assertThatThrownBy(() -> codec.parseAccessToken(access + suffix))
                .isInstanceOfSatisfying(FoodMapException.class, ex -> assertThat(ex.status()).isEqualTo(401));
        assertThatThrownBy(() -> codec.parseRefreshToken(refresh + suffix))
                .isInstanceOfSatisfying(FoodMapException.class, ex -> assertThat(ex.status()).isEqualTo(401));
    }

    @Test
    void shouldRejectRefreshTokenWithEmptyNonce() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String refresh = codec.issueRefreshToken(2001L, OffsetDateTime.now().plusDays(30));
        String withoutNonce = refresh.substring(0, refresh.lastIndexOf('.') + 1);

        assertThatThrownBy(() -> codec.parseRefreshToken(withoutNonce))
                .isInstanceOfSatisfying(FoodMapException.class, ex -> assertThat(ex.status()).isEqualTo(401));
    }

    @Test
    void shouldRejectAccessTokenPassedAsRefreshToken() {
        HmacTokenCodec codec = new HmacTokenCodec("0123456789abcdef0123456789abcdef");
        String accessToken = codec.issueAccessToken(2001L, OffsetDateTime.now().plusHours(1));

        assertThatThrownBy(() -> codec.parseRefreshToken(accessToken))
                .isInstanceOf(FoodMapException.class)
                .satisfies(ex -> assertThat(((FoodMapException) ex).status()).isEqualTo(401));
    }
}
