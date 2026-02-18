package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.dto.RoutePlanningRequest;
import org.example.ridesketch.dto.RoutePlanningResult;
import org.example.ridesketch.service.RouteService;
import org.springframework.web.bind.annotation.*;

/**
 * 路线规划控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * 路线规划
     *
     * @param request 路线规划请求参数
     * @return 路线规划结果
     */
    @PostMapping("/plan")
    public Result<RoutePlanningResult> planRoute(@RequestBody RoutePlanningRequest request) {
        log.info("路线规划请求: mode={}, origin={}, destination={}, waypoints={}",
                request.getMode(), request.getOrigin(), request.getDestination(), request.getWaypoints());

        if (request.getOrigin() == null || request.getDestination() == null) {
            return Result.error("起点和终点不能为空");
        }

        RoutePlanningResult result = routeService.planRoute(request);
        return Result.success(result);
    }

    /**
     * 骑行路线规划
     *
     * @param origin      起点经纬度，格式: longitude,latitude
     * @param destination 终点经纬度，格式: longitude,latitude
     * @param waypoints   途经点（可选），多个途经点用|分隔
     * @return 路线规划结果
     */
    @GetMapping("/riding")
    public Result<RoutePlanningResult> planRidingRoute(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String waypoints) {
        log.info("骑行路线规划请求: origin={}, destination={}, waypoints={}", origin, destination, waypoints);
        RoutePlanningResult result = routeService.planRidingRoute(origin, destination, waypoints);
        return Result.success(result);
    }

    /**
     * 步行路线规划
     *
     * @param origin      起点经纬度，格式: longitude,latitude
     * @param destination 终点经纬度，格式: longitude,latitude
     * @param waypoints   途经点（可选），多个途经点用|分隔
     * @return 路线规划结果
     */
    @GetMapping("/walking")
    public Result<RoutePlanningResult> planWalkingRoute(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String waypoints) {
        log.info("步行路线规划请求: origin={}, destination={}, waypoints={}", origin, destination, waypoints);
        RoutePlanningResult result = routeService.planWalkingRoute(origin, destination, waypoints);
        return Result.success(result);
    }
}
