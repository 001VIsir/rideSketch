package org.example.ridesketch.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 路线发布请求DTO
 *
 * 用于用户发布骑行路线到社区时的请求参数
 *
 * @author rideSketch
 */
@Data
public class RoutePublishRequest {

    /**
     * 路线标题
     */
    private String title;

    /**
     * 路线描述/详细介绍
     */
    private String description;

    /**
     * 起点（文本描述）
     */
    private String startPoint;

    /**
     * 终点（文本描述）
     */
    private String endPoint;

    /**
     * 途经点（JSON数组格式）
     */
    private String waypoints;

    /**
     * 骑行轨迹路径（经纬度坐标点序列）
     */
    private String routePath;

    /**
     * 总距离（单位：公里）
     */
    private BigDecimal totalDistance;

    /**
     * 预计骑行时间（单位：分钟）
     */
    private Integer estimatedTime;

    /**
     * 难度等级（1-简单, 2-中等, 3-困难）
     */
    private Integer difficulty;

    /**
     * 路线标签（JSON数组格式）
     */
    private String tags;

    /**
     * 是否公开（0-私有, 1-公开）
     */
    private Integer isPublic;
}
