package org.example.ridesketch.service.impl;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * 图案路书参数归一化工具。
 */
public final class PatternRouteNormalizer {

    private PatternRouteNormalizer() {
    }

    public static String normalizePatternType(String requestPatternType, String pattern) {
        if (StringUtils.isNotBlank(requestPatternType)) {
            String type = requestPatternType.trim().toLowerCase(Locale.ROOT);
            if ("shape".equals(type) || "text".equals(type)) {
                return type;
            }
        }
        return guessPatternType(pattern);
    }

    public static String normalizePattern(String pattern, String patternType) {
        if (StringUtils.isBlank(pattern)) {
            return pattern;
        }
        String p = pattern.trim();
        String lower = p.toLowerCase(Locale.ROOT);

        if ("shape".equals(patternType)) {
            if (containsAny(lower, "爱心", "心", "heart")) {
                return "心形";
            }
            if (containsAny(lower, "星", "star")) {
                return "五角星";
            }
            if (containsAny(lower, "圆", "circle")) {
                return "圆形";
            }
            if (containsAny(lower, "方", "square")) {
                return "正方形";
            }
            if (containsAny(lower, "三角", "triangle")) {
                return "三角形";
            }
            if (containsAny(lower, "五边", "pentagon")) {
                return "五边形";
            }
            return p;
        }

        if (containsAny(lower, "数字8", "8")) {
            return "8";
        }
        if (containsAny(lower, "字母m")) {
            return "M";
        }
        if (containsAny(lower, "字母z")) {
            return "Z";
        }
        return p;
    }

    public static double normalizeScale(Double scale) {
        double defaultScale = 0.05;
        if (scale == null || scale.isNaN() || scale.isInfinite()) {
            return defaultScale;
        }
        return Math.max(0.005, Math.min(0.2, scale));
    }

    private static String guessPatternType(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return "text";
        }
        String lower = pattern.toLowerCase(Locale.ROOT);
        if (containsAny(lower,
                "circle", "圆", "heart", "爱心", "心形", "star", "星", "triangle", "三角", "square", "方形", "pentagon", "五边")) {
            return "shape";
        }
        return "text";
    }

    private static boolean containsAny(String source, String... candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
