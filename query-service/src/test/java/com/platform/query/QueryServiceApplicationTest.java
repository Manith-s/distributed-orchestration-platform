package com.platform.query;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class QueryServiceApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}
