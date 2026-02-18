package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 路线规划请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanningRequest {

    /**
     * 出行方式: riding(骑行) 或 walking(步行)
     */
    private String mode;

    /**
     * 起点经纬度，格式: longitude,latitude
     * 例如: 116.306346,39.898323
     */
    private String origin;

    /**
     * 终点经纬度，格式: longitude,latitude
     * 例如: 116.385199,39.992158
     */
    private String destination;

    /**
     * 途经点，多个途经点用|分隔
     * 格式: longitude,latitude
     */
    private String waypoints;
}
