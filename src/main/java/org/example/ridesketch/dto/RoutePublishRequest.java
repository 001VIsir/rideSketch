package org.example.ridesketch.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoutePublishRequest {

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

    private Integer isPublic;
}
