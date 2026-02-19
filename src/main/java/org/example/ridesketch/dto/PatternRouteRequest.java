package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图案路书生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternRouteRequest {

    /**
     * 用户描述，如"我想在北京骑行出一个2026形状的路线"
     */
    private String description;

    /**
     * 图案类型：text(文字)/shape(图形)
     */
    private String patternType;

    /**
     * 图案内容，如"2026"或"心形"
     */
    private String pattern;

    /**
     * 中心点城市
     */
    private String city;

    /**
     * 骑行模式：riding(骑行)/walking(步行)
     */
    private String mode;

    /**
     * 图案缩放比例
     */
    private Double scale;

    /**
     * 图案偏移量X（米）
     */
    private Double offsetX;

    /**
     * 图案偏移量Y（米）
     */
    private Double offsetY;
}
