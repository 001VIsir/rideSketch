package org.example.ridesketch.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 路线视图对象（VO）
 *
 * 用于返回给前端的路线详细信息
 * 包含路线本身的字段以及关联的用户信息
 *
 * @author rideSketch
 */
@Data
public class RouteVO {

    /**
     * 路线ID
     */
    private Long id;

    /**
     * 发布用户ID
     */
    private Long userId;

    /**
     * 发布用户名
     */
    private String username;

    /**
     * 发布用户昵称
     */
    private String nickname;

    /**
     * 发布用户头像
     */
    private String avatar;

    /**
     * 路线标题
     */
    private String title;

    /**
     * 路线描述
     */
    private String description;

    /**
     * 起点
     */
    private String startPoint;

    /**
     * 终点
     */
    private String endPoint;

    /**
     * 途经点
     */
    private String waypoints;

    /**
     * 骑行轨迹
     */
    private String routePath;

    /**
     * 总距离（公里）
     */
    private BigDecimal totalDistance;

    /**
     * 预计时间（分钟）
     */
    private Integer estimatedTime;

    /**
     * 难度等级
     */
    private Integer difficulty;

    /**
     * 标签
     */
    private String tags;

    /**
     * 点赞数
     */
    private Integer likes;

    /**
     * 浏览量
     */
    private Integer views;

    /**
     * 是否公开
     */
    private Integer isPublic;

    /**
     * 当前用户是否已点赞
     */
    private Boolean liked;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
