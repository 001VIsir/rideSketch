package org.example.ridesketch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.dto.*;
import org.example.ridesketch.service.AIRouteService;
import org.example.ridesketch.service.RouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI路线规划服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRouteServiceImpl implements AIRouteService {

    private final RestTemplate restTemplate;
    private final RouteService routeService;

    @Value("${ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Value("${amap.key}")
    private String amapKey;

    /**
     * 解析用户输入的prompt模板
     */
    private static final String PARSE_PROMPT_TEMPLATE = """
            请分析以下骑行路线需求，并提取关键信息。要求以JSON格式返回，不要包含其他内容。

            用户需求: %s

            请返回以下格式的JSON:
            {
              "origin": "起点地址或地名",
              "destination": "终点地址或地名",
              "distance": "预估距离(公里)",
              "preference": "偏好类型(scenic-景点/food-美食/history-历史/nature-自然/mix-综合)",
              "description": "路线特点描述"
            }

            如果无法确定起点或终点，请使用null。
            """;

    /**
     * 生成推荐路线的prompt模板
     */
    private static final String ROUTE_PROMPT_TEMPLATE = """
            基于以下信息，请推荐一条骑行路线:

            起点: %s
            终点: %s
            偏好: %s
            出行方式: %s

            请以JSON格式返回推荐路线信息:
            {
              "waypoints": [
                {"name": "途经点名称", "type": "类型", "reason": "推荐理由"}
              ],
              "analysis": "路线分析和建议"
            }

            推荐3-5个有特色的途经点，解释为什么推荐这些地点。
            """;

    @Override
    public AIRoutePlanningResult planAIRoute(AIRoutePlanningRequest request) {
        try {
            log.info("开始AI路线规划: {}", request.getDescription());

            // 步骤1: 使用LLM解析用户输入
            String parsedInfo = parseUserInput(request.getDescription());
            log.debug("LLM解析结果: {}", parsedInfo);

            // 步骤2: 提取起点和终点坐标
            String origin = parsedInfo.contains("\"origin\"") ?
                    extractJsonField(parsedInfo, "origin") : null;
            String destination = parsedInfo.contains("\"destination\"") ?
                    extractJsonField(parsedInfo, "destination") : null;
            String preference = parsedInfo.contains("\"preference\"") ?
                    extractJsonField(parsedInfo, "preference") : "mix";
            String description = parsedInfo.contains("\"description\"") ?
                    extractJsonField(parsedInfo, "description") : "";

            // 如果用户指定了偏好，优先使用用户指定的
            if (StringUtils.isNotBlank(request.getPreference())) {
                preference = request.getPreference();
            }

            // 步骤3: 地理编码 - 将地址转换为坐标
            String originCoords = null;
            String destCoords = null;

            if (StringUtils.isNotBlank(origin)) {
                originCoords = geocodeAddress(origin);
                log.debug("起点坐标: {} -> {}", origin, originCoords);
            }

            if (StringUtils.isNotBlank(destination)) {
                destCoords = geocodeAddress(destination);
                log.debug("终点坐标: {} -> {}", destination, destCoords);
            }

            // 步骤4: 如果有起点和终点，进行路线规划
            if (StringUtils.isNotBlank(originCoords) && StringUtils.isNotBlank(destCoords)) {
                // 步骤5: 使用LLM推荐途经点
                String routeRecommendation = getRouteRecommendation(
                        origin, destination, preference, request.getMode());
                log.debug("路线推荐: {}", routeRecommendation);

                // 步骤6: 搜索推荐POI
                List<AIRoutePlanningResult.Waypoint> waypoints = searchRecommendedPOIs(
                        routeRecommendation, preference);

                // 步骤7: 如果有途经点，进行多途经点路线规划
                if (!waypoints.isEmpty()) {
                    String waypointsStr = waypoints.stream()
                            .map(AIRoutePlanningResult.Waypoint::getLocation)
                            .filter(StringUtils::isNotBlank)
                            .reduce((a, b) -> a + "|" + b)
                            .orElse(null);

                    RoutePlanningResult routeResult;
                    if ("walking".equalsIgnoreCase(request.getMode())) {
                        routeResult = routeService.planWalkingRoute(originCoords, destCoords, waypointsStr);
                    } else {
                        routeResult = routeService.planRidingRoute(originCoords, destCoords, waypointsStr);
                    }

                    return AIRoutePlanningResult.builder()
                            .status("1")
                            .info("AI路线规划成功")
                            .origin(originCoords)
                            .destination(destCoords)
                            .recommendedWaypoints(waypoints)
                            .analysis(description)
                            .build();
                }

                // 没有途经点，直接规划路线
                RoutePlanningResult routeResult;
                if ("walking".equalsIgnoreCase(request.getMode())) {
                    routeResult = routeService.planWalkingRoute(originCoords, destCoords, null);
                } else {
                    routeResult = routeService.planRidingRoute(originCoords, destCoords, null);
                }

                return AIRoutePlanningResult.builder()
                        .status("1")
                        .info("AI路线规划成功")
                        .origin(originCoords)
                        .destination(destCoords)
                        .analysis(description)
                        .build();
            }

            // 无法完成路线规划，返回解析结果供用户确认
            return AIRoutePlanningResult.builder()
                    .status("0")
                    .info("请提供更详细的起点和终点信息")
                    .analysis(parsedInfo)
                    .build();

        } catch (Exception e) {
            log.error("AI路线规划失败: {}", e.getMessage(), e);
            return AIRoutePlanningResult.builder()
                    .status("0")
                    .info("AI路线规划失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用LLM解析用户输入
     */
    private String parseUserInput(String userInput) {
        try {
            String prompt = String.format(PARSE_PROMPT_TEMPLATE, userInput);

            OllamaRequest req = new OllamaRequest();
            req.setModel(ollamaModel);
            req.setPrompt(prompt);
            req.setStream(false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OllamaRequest> entity = new HttpEntity<>(req, headers);

            String url = ollamaBaseUrl + "/api/generate";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                OllamaResponse ollamaResponse = JSON.parseObject(
                        response.getBody(), OllamaResponse.class);
                return ollamaResponse.getResponse();
            }
        } catch (Exception e) {
            log.error("LLM解析失败: {}", e.getMessage());
        }
        return "{}";
    }

    /**
     * 使用LLM获取路线推荐
     */
    private String getRouteRecommendation(String origin, String destination,
                                          String preference, String mode) {
        try {
            String prompt = String.format(ROUTE_PROMPT_TEMPLATE,
                    origin, destination, preference, mode);

            OllamaRequest req = new OllamaRequest();
            req.setModel(ollamaModel);
            req.setPrompt(prompt);
            req.setStream(false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OllamaRequest> entity = new HttpEntity<>(req, headers);

            String url = ollamaBaseUrl + "/api/generate";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                OllamaResponse ollamaResponse = JSON.parseObject(
                        response.getBody(), OllamaResponse.class);
                return ollamaResponse.getResponse();
            }
        } catch (Exception e) {
            log.error("获取路线推荐失败: {}", e.getMessage());
        }
        return "{}";
    }

    /**
     * 地理编码 - 地址转坐标
     */
    private String geocodeAddress(String address) {
        try {
            String url = "https://restapi.amap.com/v3/geo?key=" + amapKey
                    + "&address=" + java.net.URLEncoder.encode(address, "UTF-8");

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            if (body != null) {
                JSONObject jsonObject = JSON.parseObject(body);
                if ("1".equals(jsonObject.getString("status"))) {
                    JSONArray geocodes = jsonObject.getJSONArray("geocodes");
                    if (geocodes != null && !geocodes.isEmpty()) {
                        JSONObject location = geocodes.getJSONObject(0);
                        String lng = location.getString("location");
                        if (StringUtils.isNotBlank(lng)) {
                            // 高德返回格式: 经度,纬度 -> 转为 longitude,latitude
                            String[] parts = lng.split(",");
                            if (parts.length == 2) {
                                return parts[0] + "," + parts[1];
                            }
                            return lng;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("地理编码失败: {} - {}", address, e.getMessage());
        }
        return null;
    }

    /**
     * 搜索推荐POI
     */
    private List<AIRoutePlanningResult.Waypoint> searchRecommendedPOIs(
            String recommendation, String preference) {
        List<AIRoutePlanningResult.Waypoint> waypoints = new ArrayList<>();

        try {
            // 从推荐中提取途经点名称
            if (recommendation.contains("waypoints")) {
                String waypointsJson = extractJsonField(recommendation, "waypoints");
                if (StringUtils.isNotBlank(waypointsJson)) {
                    JSONArray waypointsArray = JSON.parseArray(waypointsJson);
                    if (waypointsArray != null) {
                        for (int i = 0; i < waypointsArray.size(); i++) {
                            JSONObject wp = waypointsArray.getJSONObject(i);
                            String name = wp.getString("name");
                            String type = wp.getString("type");
                            String reason = wp.getString("reason");

                            if (StringUtils.isNotBlank(name)) {
                                // 搜索POI获取坐标
                                String coords = searchPOI(name, type);
                                if (coords != null) {
                                    waypoints.add(AIRoutePlanningResult.Waypoint.builder()
                                            .name(name)
                                            .location(coords)
                                            .type(type)
                                            .description(reason)
                                            .build());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("搜索推荐POI失败: {}", e.getMessage());
        }

        return waypoints;
    }

    /**
     * 搜索POI
     */
    private String searchPOI(String keyword, String type) {
        try {
            String url = "https://restapi.amap.com/v3/place/text?key=" + amapKey
                    + "&keywords=" + java.net.URLEncoder.encode(keyword, "UTF-8")
                    + "&types=" + (type != null ? type : "")
                    + "&offset=1&page=1";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();

            if (body != null) {
                JSONObject jsonObject = JSON.parseObject(body);
                if ("1".equals(jsonObject.getString("status"))) {
                    JSONArray pois = jsonObject.getJSONArray("pois");
                    if (pois != null && !pois.isEmpty()) {
                        JSONObject poi = pois.getJSONObject(0);
                        String location = poi.getString("location");
                        if (StringUtils.isNotBlank(location)) {
                            String[] parts = location.split(",");
                            if (parts.length == 2) {
                                return parts[0] + "," + parts[1];
                            }
                            return location;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("搜索POI失败: {} - {}", keyword, e.getMessage());
        }
        return null;
    }

    /**
     * 从JSON字符串中提取字段值
     */
    private String extractJsonField(String json, String field) {
        try {
            Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("提取JSON字段失败: {} - {}", field, e.getMessage());
        }
        return null;
    }
}
