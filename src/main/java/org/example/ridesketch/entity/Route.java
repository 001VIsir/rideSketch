package org.example.ridesketch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑行路线实体类
 *
 * 对应数据库表：route
 * 存储用户发布的骑行路线信息
 *
 * @author rideSketch
 */
@Data
@TableName("route")
public class Route {

    /**
     * 路线ID（主键、自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布用户ID（外键关联user表）
     */
    private Long userId;

    /**
     * 路线标题
     */
    private String title;

    /**
     * 路线描述/详细介绍
     */
    private String description;

    /**
     * 起点（文本描述，如"天安门广场"）
     */
    private String startPoint;

    /**
     * 终点（文本描述，如"颐和园"）
     */
    private String endPoint;

    /**
     * 途经点（JSON数组格式）
     * 如：["景山公园", "北海公园"]
     */
    private String waypoints;

    /**
     * 骑行轨迹路径（经纬度坐标点序列）
     * 格式："经度,纬度;经度,纬度;..."
     * 如："116.397,39.908;116.398,39.909;..."
     */
    private String routePath;

    /**
     * 总距离（单位：公里）
     * 使用BigDecimal保证精度
     */
    private BigDecimal totalDistance;

    /**
     * 预计骑行时间（单位：分钟）
     */
    private Integer estimatedTime;

    /**
     * 难度等级
     * 1-简单
     * 2-中等
     * 3-困难
     */
    private Integer difficulty;

    /**
     * 路线标签（JSON数组格式）
     * 如：["休闲", "风景", "亲子"]
     */
    private String tags;

    /**
     * 点赞数（冗余字段，提高查询性能）
     */
    private Integer likes;

    /**
     * 浏览量（冗余字段，提高查询性能）
     */
    private Integer views;

    /**
     * 是否公开
     * 0-私有（仅自己可见）
     * 1-公开（社区可见）
     */
    private Integer isPublic;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
