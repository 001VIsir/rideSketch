package org.example.ridesketch.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternRouteNormalizerTest {

    @Test
    void shouldNormalizeShapeAliases() {
        assertEquals("shape", PatternRouteNormalizer.normalizePatternType(null, "爱心"));
        assertEquals("心形", PatternRouteNormalizer.normalizePattern("爱心", "shape"));

        assertEquals("shape", PatternRouteNormalizer.normalizePatternType(null, "星星"));
        assertEquals("五角星", PatternRouteNormalizer.normalizePattern("星星", "shape"));
    }

    @Test
    void shouldNormalizeTextAliases() {
        assertEquals("text", PatternRouteNormalizer.normalizePatternType("text", "数字8"));
        assertEquals("8", PatternRouteNormalizer.normalizePattern("数字8", "text"));

        assertEquals("M", PatternRouteNormalizer.normalizePattern("字母M", "text"));
        assertEquals("Z", PatternRouteNormalizer.normalizePattern("字母Z", "text"));
    }

    @Test
    void shouldClampScaleIntoSafeRange() {
        assertEquals(0.05, PatternRouteNormalizer.normalizeScale(null));
        assertEquals(0.005, PatternRouteNormalizer.normalizeScale(0.001));
        assertEquals(0.2, PatternRouteNormalizer.normalizeScale(10.0));
        assertEquals(0.08, PatternRouteNormalizer.normalizeScale(0.08));
    }
}
