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
        try {
            String url = AMAP_BASE_URL + "/direction/bicycling?key=" + amapKey
                    + "&origin=" + origin
                    + "&destination=" + destination;

            if (StringUtils.isNotBlank(waypoints)) {
                url += "&waypoints=" + waypoints;
            }

            log.debug("骑行路线规划请求URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.debug("骑行路线规划返回结果: {}", body);

            return parseRoutePlanningResult(body);
        } catch (Exception e) {
            log.error("骑行路线规划失败: {}", e.getMessage(), e);
            return RoutePlanningResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .build();
        }
    }

    @Override
    public RoutePlanningResult planWalkingRoute(String origin, String destination, String waypoints) {
        try {
            String url = AMAP_BASE_URL + "/direction/walking?key=" + amapKey
                    + "&origin=" + origin
                    + "&destination=" + destination;

            if (StringUtils.isNotBlank(waypoints)) {
                url += "&waypoints=" + waypoints;
            }

            log.debug("步行路线规划请求URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            log.debug("步行路线规划返回结果: {}", body);

            return parseRoutePlanningResult(body);
        } catch (Exception e) {
            log.error("步行路线规划失败: {}", e.getMessage(), e);
            return RoutePlanningResult.builder()
                    .status("0")
                    .info(e.getMessage())
                    .build();
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
