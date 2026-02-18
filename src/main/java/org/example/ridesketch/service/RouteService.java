package org.example.ridesketch.service;

import org.example.ridesketch.dto.RoutePlanningRequest;
import org.example.ridesketch.dto.RoutePlanningResult;

/**
 * 路线规划服务接口
 */
public interface RouteService {

    /**
     * 路线规划
     *
     * @param request 路线规划请求参数
     * @return 路线规划结果
     */
    RoutePlanningResult planRoute(RoutePlanningRequest request);

    /**
     * 骑行路线规划
     *
     * @param origin      起点经纬度，格式: longitude,latitude
     * @param destination 终点经纬度，格式: longitude,latitude
     * @param waypoints   途经点（可选），多个途经点用|分隔
     * @return 路线规划结果
     */
    RoutePlanningResult planRidingRoute(String origin, String destination, String waypoints);

    /**
     * 步行路线规划
     *
     * @param origin      起点经纬度，格式: longitude,latitude
     * @param destination 终点经纬度，格式: longitude,latitude
     * @param waypoints   途经点（可选），多个途经点用|分隔
     * @return 路线规划结果
     */
    RoutePlanningResult planWalkingRoute(String origin, String destination, String waypoints);
}
