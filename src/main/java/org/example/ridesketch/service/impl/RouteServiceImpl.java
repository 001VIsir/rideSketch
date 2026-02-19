package org.example.ridesketch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.dto.RoutePlanningRequest;
import org.example.ridesketch.dto.RoutePlanningResult;
import org.example.ridesketch.service.RouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 路线规划服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RestTemplate restTemplate;

    /**
     * 高德地图Web服务API基础URL
     */
    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v4";

    @Value("${amap.key}")
    private String amapKey;

    @Override
    public RoutePlanningResult planRoute(RoutePlanningRequest request) {
        String mode = request.getMode();
        if ("riding".equalsIgnoreCase(mode)) {
            return planRidingRoute(request.getOrigin(), request.getDestination(), request.getWaypoints());
        } else {
            return planWalkingRoute(request.getOrigin(), request.getDestination(), request.getWaypoints());
        }
    }

    @Override
    public RoutePlanningResult planRidingRoute(String origin, String destination, String waypoints) {
        // 如果没有途经点，直接调用API
        if (StringUtils.isBlank(waypoints)) {
            return planSingleSegmentRoute(origin, destination, "bicycling");
        }

        // 有途经点，需要分段规划并合并结果
        return planMultiSegmentRoute(origin, destination, waypoints, "bicycling");
    }

    @Override
    public RoutePlanningResult planWalkingRoute(String origin, String destination, String waypoints) {
        // 如果没有途经点，直接调用API
        if (StringUtils.isBlank(waypoints)) {
            return planSingleSegmentRoute(origin, destination, "walking");
        }

        // 有途经点，需要分段规划并合并结果
        return planMultiSegmentRoute(origin, destination, waypoints, "walking");
    }

    /**
     * 单段路线规划（无途经点）
     */
    private RoutePlanningResult planSingleSegmentRoute(String origin, String destination, String mode) {
        try {
            String url = AMAP_BASE_URL + "/direction/" + mode + "?key=" + amapKey
                    + "&origin=" + origin
                    + "&destination=" + destination;

            log.debug("单段路线规划请求URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.debug("路线规划返回结果: {}", body);

            return parseRoutePlanningResult(body);
        } catch (Exception e) {
            log.error("路线规划失败: {}", e.getMessage(), e);
            return RoutePlanningResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 多段路线规划（多途经点）
     * 将路线拆分为多个单段，分别调用API，然后合并结果
     */
    private RoutePlanningResult planMultiSegmentRoute(String origin, String destination, String waypoints, String mode) {
        try {
            // 解析途经点
            String[] waypointArray = waypoints.split("\\|");
            List<String> allPoints = new ArrayList<>();
            allPoints.add(origin);
            for (String wp : waypointArray) {
                if (StringUtils.isNotBlank(wp.trim())) {
                    allPoints.add(wp.trim());
                }
            }
            allPoints.add(destination);

            // 如果只有一个点（起点=终点），直接返回
            if (allPoints.size() < 2) {
                return RoutePlanningResult.builder()
                        .status("0")
                        .info("途经点格式错误")
                        .build();
            }

            // 存储每段的路线结果
            List<RoutePlanningResult> segmentResults = new ArrayList<>();

            // 分段调用API
            for (int i = 0; i < allPoints.size() - 1; i++) {
                String segOrigin = allPoints.get(i);
                String segDestination = allPoints.get(i + 1);

                log.debug("分段路线: {} -> {}", segOrigin, segDestination);

                RoutePlanningResult segResult = planSingleSegmentRoute(segOrigin, segDestination, mode);
                if (!"1".equals(segResult.getStatus())) {
                    // 如果某段路线规划失败，返回失败结果
                    log.warn("分段路线规划失败: {}", segResult.getInfo());
                    return RoutePlanningResult.builder()
                            .status("0")
                            .info("途经点 " + segDestination + " 路线规划失败: " + segResult.getInfo())
                            .build();
                }
                segmentResults.add(segResult);
            }

            // 合并所有分段结果
            return mergeSegmentResults(segmentResults, origin, destination, waypoints);

        } catch (Exception e) {
            log.error("多途经点路线规划失败: {}", e.getMessage(), e);
            return RoutePlanningResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .build();
        }
    }

    /**
     * 合并多段路线结果
     */
    private RoutePlanningResult mergeSegmentResults(List<RoutePlanningResult> segmentResults, String origin, String destination, String waypoints) {
        if (segmentResults.isEmpty()) {
            return RoutePlanningResult.builder()
                    .status("0")
                    .info("无路线结果")
                    .build();
        }

        if (segmentResults.size() == 1) {
            return segmentResults.get(0);
        }

        // 计算总距离和总时间
        long totalDistance = 0;
        long totalDuration = 0;
        List<RoutePlanningResult.StepInfo> mergedSteps = new ArrayList<>();
        StringBuilder mergedPath = new StringBuilder();

        for (RoutePlanningResult segResult : segmentResults) {
            RoutePlanningResult.RouteInfo routeInfo = segResult.getRoute();
            if (routeInfo != null && routeInfo.getPaths() != null && !routeInfo.getPaths().isEmpty()) {
                // 取第一条路径
                RoutePlanningResult.PathInfo pathInfo = routeInfo.getPaths().get(0);

                // 累加距离和时间
                totalDistance += parseLongSafe(pathInfo.getDistance());
                totalDuration += parseLongSafe(pathInfo.getDuration());

                // 合并步骤
                if (pathInfo.getSteps() != null) {
                    for (RoutePlanningResult.StepInfo step : pathInfo.getSteps()) {
                        mergedSteps.add(step);

                        // 合并路径坐标
                        if (step.getPath() != null && !step.getPath().isEmpty()) {
                            if (mergedPath.length() > 0) {
                                mergedPath.append(";");
                            }
                            mergedPath.append(step.getPath());
                        }
                    }
                }
            }
        }

        // 构建合并后的结果
        RoutePlanningResult.PathInfo mergedPathInfo = RoutePlanningResult.PathInfo.builder()
                .distance(String.valueOf(totalDistance))
                .duration(String.valueOf(totalDuration))
                .strategy("multi_waypoint")
                .steps(mergedSteps)
                .path(mergedPath.toString())
                .build();

        List<RoutePlanningResult.PathInfo> paths = new ArrayList<>();
        paths.add(mergedPathInfo);

        RoutePlanningResult.RouteInfo routeInfo = RoutePlanningResult.RouteInfo.builder()
                .origin(origin)
                .destination(destination)
                .waypoints(waypoints)
                .paths(paths)
                .build();

        return RoutePlanningResult.builder()
                .status("1")
                .info("OK")
                .route(routeInfo)
                .build();
    }

    /**
     * 安全解析Long值
     */
    private long parseLongSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 解析路线规划结果
     */
    private RoutePlanningResult parseRoutePlanningResult(String json) {
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            RoutePlanningResult result = new RoutePlanningResult();
            result.setStatus(jsonObject.getString("status"));
            result.setInfo(jsonObject.getString("info"));

            if ("1".equals(result.getStatus())) {
                JSONObject routeObj = jsonObject.getJSONObject("route");
                if (routeObj != null) {
                    RoutePlanningResult.RouteInfo routeInfo = RoutePlanningResult.RouteInfo.builder()
                            .origin(routeObj.getString("origin"))
                            .destination(routeObj.getString("destination"))
                            .waypoints("")
                            .build();

                    // 解析路径列表
                    JSONArray pathsArray = routeObj.getJSONArray("paths");
                    if (pathsArray != null && pathsArray.size() > 0) {
                        List<RoutePlanningResult.PathInfo> paths = new ArrayList<>();
                        for (int i = 0; i < pathsArray.size(); i++) {
                            JSONObject pathObj = pathsArray.getJSONObject(i);
                            RoutePlanningResult.PathInfo pathInfo = parsePathInfo(pathObj);
                            paths.add(pathInfo);
                        }
                        routeInfo.setPaths(paths);
                    }

                    result.setRoute(routeInfo);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("解析路线规划结果失败: {}", e.getMessage(), e);
            return RoutePlanningResult.builder()
                    .status("0")
                    .info("解析失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析路径信息
     */
    private RoutePlanningResult.PathInfo parsePathInfo(JSONObject pathObj) {
        RoutePlanningResult.PathInfo pathInfo = RoutePlanningResult.PathInfo.builder()
                .distance(pathObj.getString("distance"))
                .duration(pathObj.getString("duration"))
                .strategy(pathObj.getString("strategy"))
                .build();

        // 解析步骤列表
        JSONArray stepsArray = pathObj.getJSONArray("steps");
        if (stepsArray != null && stepsArray.size() > 0) {
            List<RoutePlanningResult.StepInfo> steps = new ArrayList<>();
            for (int i = 0; i < stepsArray.size(); i++) {
                JSONObject stepObj = stepsArray.getJSONObject(i);
                RoutePlanningResult.StepInfo stepInfo = RoutePlanningResult.StepInfo.builder()
                        .instruction(stepObj.getString("instruction"))
                        .distance(stepObj.getString("distance"))
                        .duration(stepObj.getString("duration"))
                        .road(stepObj.getString("road"))
                        .orientation(stepObj.getString("orientation"))
                        .path(stepObj.getString("path"))
                        .build();
                steps.add(stepInfo);
            }
            pathInfo.setSteps(steps);
        }

        return pathInfo;
    }
}
