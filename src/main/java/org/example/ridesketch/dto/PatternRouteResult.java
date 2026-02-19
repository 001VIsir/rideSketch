package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图案路书生成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternRouteResult {

    /**
     * 状态码，1表示成功
     */
    private String status;

    /**
     * 返回状态信息
     */
    private String info;

    /**
     * 图案类型
     */
    private String patternType;

    /**
     * 图案内容
     */
    private String pattern;

    /**
     * 城市
     */
    private String city;

    /**
     * 图案坐标点列表（用于前端绘制图案预览）
     */
    private List<PatternPoint> patternPoints;

    /**
     * 骑行路线坐标点（用于前端绘制实际路线）
     */
    private String routePath;

    /**
     * 量化数据
     */
    private QuantifiedData quantifiedData;

    /**
     * 原始高德路线数据
     */
    private RoutePlanningResult routeData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternPoint {
        /**
         * 经度
         */
        private Double longitude;

        /**
         * 纬度
         */
        private Double latitude;

        /**
         * 点序号
         */
        private Integer index;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuantifiedData {
        /**
         * 总距离(米)
         */
        private Double totalDistance;

        /**
         * 总距离(公里)
         */
        private Double totalDistanceKm;

        /**
         * 预计时间(秒)
         */
        private Long duration;

        /**
         * 预计时间(分钟)
         */
        private Double durationMinutes;

        /**
         * 预计时间(小时)
         */
        private Double durationHours;

        /**
         * 难度等级：easy(简单)/medium(中等)/hard(困难)
         */
        private String difficulty;

        /**
         * 爬升高度(米)
         */
        private Double elevation;

        /**
         * 途经点数
         */
        private Integer waypointCount;

        /**
         * 路段数
         */
        private Integer segmentCount;
    }
}
