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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI路线规划服务实现类 - 使用Spring AI重构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRouteServiceImpl implements AIRouteService {

    private final ChatClient chatClient;
    private final RouteService routeService;
    private final RestTemplate restTemplate;

    @Value("${amap.key}")
    private String amapKey;

    /**
     * 系统提示词 - 用于解析用户输入
     */
    private static final String PARSE_SYSTEM_PROMPT = """
            你是一个骑行路线规划助手。请分析用户的骑行需求，并提取关键信息。
            你必须只返回JSON格式的数据，不要包含任何其他内容。
            """;

    /**
     * 用户输入解析提示词模板
     */
    private static final String PARSE_USER_PROMPT_TEMPLATE = """
            请分析以下骑行路线需求，并提取关键信息。

            用户需求: {description}

            请返回以下格式的JSON:
            {{
              "origin": "起点中文地址（必须是中文全称，如北京市天安门广场）",
              "destination": "终点中文地址（必须是中文全称，如上海市外滩）",
              "distance": "预估距离(公里)",
              "preference": "偏好类型(scenic-景点/food-美食/history-历史/nature-自然/mix-综合)",
              "description": "路线特点描述"
            }}

            重要：必须返回中文地址！如果是外国地名请翻译成中文。
            如果无法确定起点或终点，请使用null。
            """;

    /**
     * 途经点推荐提示词模板
     */
    private static final String WAYPOINT_PROMPT_TEMPLATE = """
            基于以下信息，请推荐途经点:

            起点: {origin}
            终点: {destination}
            偏好: {preference}

            请返回以下格式的JSON:
            {{
              "waypoints": [
                {{"name": "途经点名称", "type": "类型", "reason": "推荐理由"}}
              ],
              "analysis": "路线分析和建议"
            }}

            推荐3-5个有特色的途经点。
            """;

    @Override
    public AIRoutePlanningResult planAIRoute(AIRoutePlanningRequest request) {
        try {
            log.info("开始AI路线规划: {}", request.getDescription());

            // 步骤1: 使用Spring AI解析用户输入
            String parsedInfo = parseUserInput(request.getDescription());
            log.debug("AI解析结果: {}", parsedInfo);

            // 步骤2: 提取起点和终点
            String origin = extractJsonField(parsedInfo, "origin");
            String destination = extractJsonField(parsedInfo, "destination");
            String preference = extractJsonField(parsedInfo, "preference");
            if (StringUtils.isBlank(preference)) {
                preference = "mix";
            }
            String description = extractJsonField(parsedInfo, "description");

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
                // 步骤5: 使用AI推荐途经点
                String routeRecommendation = getWaypointRecommendation(
                        origin, destination, preference);
                log.debug("途经点推荐: {}", routeRecommendation);

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
     * 使用Spring AI解析用户输入
     */
    private String parseUserInput(String userInput) {
        try {
            String userPrompt = PARSE_USER_PROMPT_TEMPLATE
                    .replace("{description}", userInput);

            String response = chatClient.prompt()
                    .system(PARSE_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            // 尝试提取JSON
            return extractJsonFromResponse(response);
        } catch (Exception e) {
            log.error("AI解析失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 使用Spring AI获取途经点推荐
     */
    private String getWaypointRecommendation(String origin, String destination, String preference) {
        try {
            String userPrompt = WAYPOINT_PROMPT_TEMPLATE
                    .replace("{origin}", origin)
                    .replace("{destination}", destination)
                    .replace("{preference}", preference);

            String response = chatClient.prompt()
                    .system("你是一个骑行路线规划助手，请推荐途经点。")
                    .user(userPrompt)
                    .call()
                    .content();

            return extractJsonFromResponse(response);
        } catch (Exception e) {
            log.error("途经点推荐失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 从AI响应中提取JSON
     */
    private String extractJsonFromResponse(String response) {
        if (StringUtils.isBlank(response)) {
            return "{}";
        }

        // 尝试找到JSON块
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return response;
    }

    /**
     * 地理编码 - 地址转坐标
     */
    private String geocodeAddress(String address) {
        try {
            String encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
            // 添加output=JSON参数确保返回JSON格式
            String url = "https://restapi.amap.com/v3/geocode/geo?key=" + amapKey
                    + "&address=" + encodedAddress
                    + "&output=JSON";

            log.debug("地理编码请求: {}", url);

            // 使用HttpURLConnection来确保正确的请求
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            log.debug("HTTP响应码: {}", responseCode);

            java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String body = response.toString();

            log.debug("地理编码响应: {}", body);

            if (body != null) {
                JSONObject jsonObject = JSON.parseObject(body);
                if ("1".equals(jsonObject.getString("status"))) {
                    JSONArray geocodes = jsonObject.getJSONArray("geocodes");
                    if (geocodes != null && !geocodes.isEmpty()) {
                        JSONObject locationObj = geocodes.getJSONObject(0);
                        String lng = locationObj.getString("location");
                        log.debug("地理编码结果: {} -> {}", address, lng);
                        if (StringUtils.isNotBlank(lng)) {
                            String[] parts = lng.split(",");
                            if (parts.length == 2) {
                                return parts[0] + "," + parts[1];
                            }
                            return lng;
                        }
                    } else {
                        log.warn("地理编码无结果: {}", address);
                    }
                } else {
                    log.warn("地理编码失败: {} - {}", address, jsonObject.getString("info"));
                }
            }
        } catch (Exception e) {
            log.error("地理编码异常: {} - {}", address, e.getMessage());
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
            log.debug("开始解析途经点推荐: {}", recommendation);

            // 尝试直接解析整个JSON响应
            if (recommendation.contains("waypoints")) {
                // 提取JSON对象
                JSONObject jsonObj = JSON.parseObject(recommendation);
                JSONArray waypointsArray = jsonObj.getJSONArray("waypoints");

                if (waypointsArray != null && !waypointsArray.isEmpty()) {
                    log.debug("找到 {} 个途经点", waypointsArray.size());

                    for (int i = 0; i < waypointsArray.size(); i++) {
                        JSONObject wp = waypointsArray.getJSONObject(i);
                        String name = wp.getString("name");
                        String type = wp.getString("type");
                        String reason = wp.getString("reason");

                        log.debug("处理途经点: {} - {}", name, type);

                        if (StringUtils.isNotBlank(name)) {
                            String coords = searchPOI(name, type);
                            if (coords != null) {
                                waypoints.add(AIRoutePlanningResult.Waypoint.builder()
                                        .name(name)
                                        .location(coords)
                                        .type(type)
                                        .description(reason)
                                        .build());
                                log.debug("途经点添加成功: {} -> {}", name, coords);
                            } else {
                                log.warn("途经点坐标获取失败: {}", name);
                            }
                        }
                    }
                } else {
                    log.warn("途经点数组为空或null");
                }
            } else {
                log.warn("推荐结果中未包含waypoints字段");
            }
        } catch (Exception e) {
            log.error("搜索推荐POI失败: {}", e.getMessage(), e);
        }

        log.debug("最终途经点数量: {}", waypoints.size());
        return waypoints;
    }

    /**
     * 搜索POI
     */
    private String searchPOI(String keyword, String type) {
        try {
            String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
            String encodedType = java.net.URLEncoder.encode(type != null ? type : "", "UTF-8");
            String url = "https://restapi.amap.com/v3/place/text?key=" + amapKey
                    + "&keywords=" + encodedKeyword
                    + "&types=" + encodedType
                    + "&offset=1&page=1&output=JSON";

            log.debug("搜索POI请求: {}", url);

            // 使用HttpURLConnection
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            log.debug("搜索POI响应码: {}", responseCode);

            java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String body = response.toString();

            log.debug("搜索POI响应: {}", body);

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
