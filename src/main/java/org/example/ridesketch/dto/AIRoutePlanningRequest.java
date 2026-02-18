package org.example.ridesketch.dto;

import lombok.Data;

/**
 * AI路线规划请求
 */
@Data
public class AIRoutePlanningRequest {

    /**
     * 用户自然语言描述
     * 例如：我想从北京邮电大学骑自行车出发，全程30公里，以看景点为主，请你给我推荐路线
     */
    private String description;

    /**
     * 出行方式：riding（骑行）/ walking（步行）
     */
    private String mode = "riding";

    /**
     * 偏好类型：scenic（景点）、food（美食）、history（历史）、nature（自然）
     */
    private String preference;
}
