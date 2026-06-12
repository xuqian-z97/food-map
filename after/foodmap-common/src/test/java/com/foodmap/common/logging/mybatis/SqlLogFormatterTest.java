package com.foodmap.common.logging.mybatis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlLogFormatterTest {

    @Test
    void shouldReplacePlaceholdersWithMaskedSqlLiterals() {
        String actualSql = SqlLogFormatter.formatActualSql(
                "select * from auth_credentials where phone = ? and password_hash = ? and account_id = ?",
                List.of(
                        new SqlParameterValue("phone", "13812345678"),
                        new SqlParameterValue("password", "secret123"),
                        new SqlParameterValue("accountId", 10001L)
                ),
                2000
        );

        assertThat(actualSql)
                .contains("phone = '138****5678'")
                .contains("password_hash = '***'")
                .contains("account_id = 10001")
                .doesNotContain("13812345678")
                .doesNotContain("secret123");
    }

    @Test
    void shouldNormalizeDetectTypeAndTruncateSql() {
        String normalized = SqlLogFormatter.normalizeSql("  \n  update   users   set nickname = ?   where user_id = ?  ");

        assertThat(normalized).isEqualTo("update users set nickname = ? where user_id = ?");
        assertThat(SqlLogFormatter.detectSqlType(normalized)).isEqualTo("UPDATE");
        assertThat(SqlLogFormatter.truncate("abcdef", 4)).isEqualTo("abcd...");
    }
}
