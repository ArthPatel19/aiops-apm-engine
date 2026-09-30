package com.aiops.aiops_apm.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SanitizerServiceTest {

    private final SanitizerService s = new SanitizerService();

    @Test
    void redactsSampleLog1() {
        String in = "{\"userId\": 1042, \"authToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"password\": \"mySecretPassword123\"}";
        String out = s.sanitize(in);
        assertThat(out).doesNotContain("eyJ", "mySecretPassword123")
                .contains("\"userId\": 1042");
    }

    @Test
    void redactsSampleLog2() {
        String in = "{\"userId\": 8821, \"email\": \"user8821@gmail.com\", \"bearerToken\": \"Bearer secret_token_xyz99\"}";
        assertThat(s.sanitize(in)).doesNotContain("user8821", "secret_token_xyz99");
    }

    @Test
    void redactsTruncatedJwtInSampleLog3() {
        String in = "{\"deviceOs\": \"Android\", \"jwtToken\": \"eyJhbGciOiJIUzI1Ni...\"}";
        assertThat(s.sanitize(in)).doesNotContain("eyJ").contains("Android");
    }

    @Test
    void redactsEscapedJsonAndPlainText() {
        assertThat(s.sanitize("{\\\"password\\\": \\\"abc123\\\"}")).doesNotContain("abc123");
        assertThat(s.sanitize("login failed password=hunter2")).doesNotContain("hunter2");
        assertThat(s.sanitize("bearerToken=Bearer abc.def-ghi")).doesNotContain("abc.def", "ghi");
    }

    @Test
    void redactsPatternsWithoutKeyNames() {
        assertThat(s.sanitize("Authorization: Bearer abc123XYZ.tok")).doesNotContain("abc123XYZ");
        assertThat(s.sanitize("user john.doe+x@corp.co.in failed")).doesNotContain("john.doe");
        assertThat(s.sanitize("key AKIAIOSFODNN7EXAMPLE")).doesNotContain("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void isIdempotentAndLeavesCleanTextAlone() {
        String once = s.sanitize("{\"password\": \"x1\"} email=a@b.com");
        assertThat(s.sanitize(once)).isEqualTo(once).doesNotContain("]]");
        String clean = "com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:213)";
        assertThat(s.sanitize(clean)).isEqualTo(clean);
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(s.sanitize(null)).isNull();
        assertThat(s.sanitize("")).isEmpty();
    }
}