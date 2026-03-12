package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI路线规划结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRoutePlanningResult {

    /**
     * 状态：1-成功，0-失败
     */
    private String status;

    /**
     * 提示信息
     */
    private String info;

    /**
     * 解析出的起点坐标 (longitude,latitude)
     */
    private String origin;

    /**
     * 解析出的终点坐标 (longitude,latitude)
     */
    private String destination;

    /**
     * 推荐途经点列表
     */
    private List<Waypoint> recommendedWaypoints;

    /**
     * AI分析和建议
     */
    private String analysis;

    /**
     * 途经点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Waypoint {
        /**
         * 名称
         */
        private String name;

        /**
         * 经纬度坐标 (longitude,latitude)
         */
        private String location;

        /**
         * 类型/标签
         */
        private String type;

        /**
         * 描述
         */
        private String description;
    }
}
