package org.example.ridesketch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("route")
public class Route {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private String startPoint;

    private String endPoint;

    private String waypoints;

    private String routePath;

    private BigDecimal totalDistance;

    private Integer estimatedTime;

    private Integer difficulty;

    private String tags;

    private Integer likes;

    private Integer views;

    private Integer isPublic;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
