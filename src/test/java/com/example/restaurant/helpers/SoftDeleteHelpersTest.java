package com.example.restaurant.helpers;

import com.example.restaurant.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoftDeleteHelpersTest {
    @Test
    @DisplayName("markAsDelete: should return null when input is null")
    void markAsDelete_ShouldReturnNull_WhenInputIsNull() {
        String result = SoftDeleteHelpers.markAsDelete(null);

        assertNull(result);
    }

    @Test
    @DisplayName("markAsDelete: Should prepend DELETED and timestamp to the value")
    void markAsDelete_ShouldPrependPrefix_WhenInputIsValid() {
        String originValue = TestConstants.FAKE_EMAIL;

        String result = SoftDeleteHelpers.markAsDelete(originValue);

        assertNotNull(result);
        assertTrue(result.startsWith("DELETED_"));
        assertTrue(result.endsWith("_" + originValue));

        String[] parts = result.split("_");
        assertEquals(3, parts.length);
        assertDoesNotThrow(
                () -> Long.parseLong(parts[1]),
                "Middle part should be a valid timestamp (Long)"
        );
    }
}
