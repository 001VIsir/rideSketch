package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 路线规划结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanningResult {

    /**
     * 状态码，1表示成功
     */
    private String status;

    /**
     * 返回状态信息
     */
    private String info;

    /**
     * 路线信息
     */
    private RouteInfo route;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInfo {

        /**
         * 起点经纬度
         */
        private String origin;

        /**
         * 终点经纬度
         */
        private String destination;

        /**
         * 途经点列表（多个用|分隔）
         */
        private String waypoints;

        /**
         * 路线列表
         */
        private List<PathInfo> paths;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathInfo {

        /**
         * 路线总距离(米)
         */
        private String distance;

        /**
         * 路线总时间(秒)
         */
        private String duration;

        /**
         * 路线策略
         */
        private String strategy;

        /**
         * 路线步骤列表
         */
        private List<StepInfo> steps;

        /**
         * 路径坐标点，多个坐标用;分隔（用于合并后的路线）
         */
        private String path;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {

        /**
         * 导航描述
         */
        private String instruction;

        /**
         * 步骤距离(米)
         */
        private String distance;

        /**
         * 步骤耗时(秒)
         */
        private String duration;

        /**
         * 道路名称
         */
        private String road;

        /**
         * 方向
         */
        private String orientation;

        /**
         * 路径坐标点，多个坐标用;分隔
         */
        private String path;
    }
}
