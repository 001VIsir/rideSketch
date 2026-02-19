package org.example.ridesketch.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RouteVO {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

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

    private Boolean liked;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
