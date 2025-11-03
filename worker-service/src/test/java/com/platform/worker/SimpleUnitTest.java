package com.platform.worker;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple unit test to verify that unit tests work correctly.
 * This test does not require Spring context or external dependencies.
 */
class SimpleUnitTest {

    @Test
    void simpleMathTest() {
        int result = 2 + 2;
        assertThat(result).isEqualTo(4);
    }

    @Test
    void simpleStringTest() {
        String text = "Hello";
        assertThat(text).isNotEmpty();
        assertThat(text).hasSize(5);
    }

    @Test
    void simpleNullCheckTest() {
        String notNull = "Value";
        String isNull = null;

        assertThat(notNull).isNotNull();
        assertThat(isNull).isNull();
    }

    @Test
    void simpleListTest() {
        var numbers = java.util.List.of(1, 2, 3, 4, 5);

        assertThat(numbers).hasSize(5);
        assertThat(numbers).contains(3);
        assertThat(numbers).doesNotContain(10);
    }
}
