package org.example.ridesketch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.dto.*;
import org.example.ridesketch.service.AIRouteService;
import org.example.ridesketch.service.MapService;
import org.example.ridesketch.service.PatternRouteService;
import org.example.ridesketch.service.RouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图案路书服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternRouteServiceImpl implements PatternRouteService {

    private final RestTemplate restTemplate;
    private final RouteService routeService;
    private final MapService mapService;

    @Value("${ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    @Value("${amap.key}")
    private String amapKey;

    /**
     * 解析用户输入的prompt模板
     */
    private static final String PARSE_PATTERN_PROMPT_TEMPLATE = """
            请分析以下图案路书需求，并提取关键信息。要求以JSON格式返回，不要包含其他内容。

            用户需求: %s

            请返回以下格式的JSON:
            {
              "pattern": "图案内容，如2026或心形",
              "patternType": "图案类型(text-文字/shape-图形)",
              "city": "城市名称，如北京市",
              "scale": 缩放比例(0.001-0.01之间的数值，表示图案大小),
              "description": "需求描述"
            }

            如果无法确定图案或城市，请使用null。
            """;

    @Override
    public PatternRouteResult generatePatternRoute(PatternRouteRequest request) {
        try {
            log.info("开始生成图案路书: {}", request.getDescription());

            // 步骤1: 解析用户输入
            String parsedInfo = parsePatternInput(request.getDescription());
            log.debug("LLM解析结果: {}", parsedInfo);

            // 步骤2: 提取图案信息
            String pattern = parsedInfo.contains("\"pattern\"") ?
                    extractJsonField(parsedInfo, "pattern") : null;
            String patternType = parsedInfo.contains("\"patternType\"") ?
                    extractJsonField(parsedInfo, "patternType") : "text";
            String city = parsedInfo.contains("\"city\"") ?
                    extractJsonField(parsedInfo, "city") : null;

            // 如果用户提供了参数，优先使用用户提供的
            if (StringUtils.isNotBlank(request.getPattern())) {
                pattern = request.getPattern();
            }
            if (StringUtils.isNotBlank(request.getPatternType())) {
                patternType = request.getPatternType();
            }
            if (StringUtils.isNotBlank(request.getCity())) {
                city = request.getCity();
            }

            if (StringUtils.isBlank(pattern)) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("请提供有效的图案内容")
                        .build();
            }

            if (StringUtils.isBlank(city)) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("请提供有效的城市名称")
                        .build();
            }

            // 步骤3: 获取城市中心坐标
            GeoCodeResult geoCodeResult = mapService.geocode(city);
            log.debug("地理编码结果: status={}, geocodes={}", geoCodeResult.getStatus(), geoCodeResult.getGeocodes());

            if (geoCodeResult == null) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("无法获取城市坐标: " + city)
                        .build();
            }

            if (geoCodeResult.getGeocodes() == null) {
                log.warn("地理编码返回成功但geocodes为null: status={}, info={}", geoCodeResult.getStatus(), geoCodeResult.getInfo());
                return PatternRouteResult.builder()
                        .status("0")
                        .info("无法获取城市坐标: " + city)
                        .build();
            }

            GeoCodeResult.GeocodeInfo geocodeInfo = geoCodeResult.getGeocodes();
            if (StringUtils.isBlank(geocodeInfo.getLng()) || StringUtils.isBlank(geocodeInfo.getLat())) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("无法获取城市坐标: " + city)
                        .build();
            }

            double centerLng = Double.parseDouble(geocodeInfo.getLng());
            double centerLat = Double.parseDouble(geocodeInfo.getLat());
            log.debug("城市中心坐标: {}, {}", centerLng, centerLat);

            // 步骤4: 计算缩放比例
            double scale = request.getScale() != null ? request.getScale() : 0.003;
            if (parsedInfo.contains("\"scale\"")) {
                try {
                    String scaleStr = extractJsonField(parsedInfo, "scale");
                    if (StringUtils.isNotBlank(scaleStr)) {
                        scale = Double.parseDouble(scaleStr);
                    }
                } catch (Exception e) {
                    log.warn("解析缩放比例失败，使用默认值");
                }
            }

            // 步骤5: 生成图案坐标点
            List<PatternRouteResult.PatternPoint> patternPoints;
            if ("shape".equals(patternType)) {
                patternPoints = generateShapePoints(pattern, centerLng, centerLat, scale);
            } else {
                patternPoints = generateTextPoints(pattern, centerLng, centerLat, scale);
            }

            if (patternPoints.isEmpty()) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("无法生成图案坐标点")
                        .build();
            }

            log.debug("生成了 {} 个图案坐标点", patternPoints.size());

            // 步骤6: 将图案坐标点转换为途经点，进行路线规划
            // 为了形成完整回路，需要将终点设为起点
            String origin = patternPoints.get(0).getLongitude() + "," + patternPoints.get(0).getLatitude();
            String destination = origin; // 回到起点

            // 构建途经点字符串
            StringBuilder waypointsBuilder = new StringBuilder();
            for (int i = 1; i < patternPoints.size(); i++) {
                if (i > 1) {
                    waypointsBuilder.append("|");
                }
                waypointsBuilder.append(patternPoints.get(i).getLongitude())
                        .append(",")
                        .append(patternPoints.get(i).getLatitude());
            }
            String waypoints = waypointsBuilder.toString();

            // 步骤7: 进行路线规划
            String mode = StringUtils.isNotBlank(request.getMode()) ? request.getMode() : "riding";
            RoutePlanningResult routeResult;
            if ("walking".equalsIgnoreCase(mode)) {
                routeResult = routeService.planWalkingRoute(origin, destination, waypoints);
            } else {
                routeResult = routeService.planRidingRoute(origin, destination, waypoints);
            }

            if (routeResult == null || routeResult.getRoute() == null ||
                    routeResult.getRoute().getPaths() == null ||
                    routeResult.getRoute().getPaths().isEmpty()) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("路线规划失败，请尝试调整图案或位置")
                        .pattern(pattern)
                        .patternType(patternType)
                        .city(city)
                        .patternPoints(patternPoints)
                        .build();
            }

            // 步骤8: 获取路线路径
            RoutePlanningResult.PathInfo pathInfo = routeResult.getRoute().getPaths().get(0);
            String routePath = pathInfo.getPath();

            // 步骤9: 计算量化数据
            PatternRouteResult.QuantifiedData quantifiedData = calculateQuantifiedData(pathInfo, patternPoints.size());

            return PatternRouteResult.builder()
                    .status("1")
                    .info("图案路书生成成功")
                    .pattern(pattern)
                    .patternType(patternType)
                    .city(city)
                    .patternPoints(patternPoints)
                    .routePath(routePath)
                    .quantifiedData(quantifiedData)
                    .routeData(routeResult)
                    .build();

        } catch (Exception e) {
            log.error("图案路书生成失败: {}", e.getMessage(), e);
            return PatternRouteResult.builder()
                    .status("0")
                    .info("图案路书生成失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用LLM解析用户输入
     */
    private String parsePatternInput(String userInput) {
        try {
            String prompt = String.format(PARSE_PATTERN_PROMPT_TEMPLATE, userInput);

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
     * 生成文字图案坐标点
     */
    private List<PatternRouteResult.PatternPoint> generateTextPoints(
            String text, double centerLng, double centerLat, double scale) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        // 简单的数字点阵映射（每个数字3x5的点阵）
        Map<Character, int[][]> digitPatterns = new HashMap<>();

        // 0
        digitPatterns.put('0', new int[][] {
            {1,1,1},
            {1,0,1},
            {1,0,1},
            {1,0,1},
            {1,1,1}
        });
        // 1
        digitPatterns.put('1', new int[][] {
            {0,1,0},
            {1,1,0},
            {0,1,0},
            {0,1,0},
            {1,1,1}
        });
        // 2
        digitPatterns.put('2', new int[][] {
            {1,1,1},
            {0,0,1},
            {1,1,1},
            {1,0,0},
            {1,1,1}
        });
        // 3
        digitPatterns.put('3', new int[][] {
            {1,1,1},
            {0,0,1},
            {1,1,1},
            {0,0,1},
            {1,1,1}
        });
        // 4
        digitPatterns.put('4', new int[][] {
            {1,0,1},
            {1,0,1},
            {1,1,1},
            {0,0,1},
            {0,0,1}
        });
        // 5
        digitPatterns.put('5', new int[][] {
            {1,1,1},
            {1,0,0},
            {1,1,1},
            {0,0,1},
            {1,1,1}
        });
        // 6
        digitPatterns.put('6', new int[][] {
            {1,1,1},
            {1,0,0},
            {1,1,1},
            {1,0,1},
            {1,1,1}
        });
        // 7
        digitPatterns.put('7', new int[][] {
            {1,1,1},
            {0,0,1},
            {0,0,1},
            {0,0,1},
            {0,0,1}
        });
        // 8
        digitPatterns.put('8', new int[][] {
            {1,1,1},
            {1,0,1},
            {1,1,1},
            {1,0,1},
            {1,1,1}
        });
        // 9
        digitPatterns.put('9', new int[][] {
            {1,1,1},
            {1,0,1},
            {1,1,1},
            {0,0,1},
            {1,1,1}
        });
        // A
        digitPatterns.put('a', new int[][] {
            {0,1,0},
            {1,0,1},
            {1,1,1},
            {1,0,1},
            {1,0,1}
        });
        digitPatterns.put('A', new int[][] {
            {0,1,0},
            {1,0,1},
            {1,1,1},
            {1,0,1},
            {1,0,1}
        });
        // B
        digitPatterns.put('b', new int[][] {
            {1,1,0},
            {1,0,1},
            {1,1,0},
            {1,0,1},
            {1,1,0}
        });
        digitPatterns.put('B', new int[][] {
            {1,1,0},
            {1,0,1},
            {1,1,0},
            {1,0,1},
            {1,1,0}
        });

        try {
            // 将输入转换为小写
            text = text.toLowerCase();

            // 计算每个字符的宽度
            double charWidth = 0.001 * scale;
            double charHeight = 0.002 * scale;
            double spacing = 0.0005 * scale;

            // 计算总宽度和起始位置
            double totalWidth = (charWidth + spacing) * text.length();
            double startLng = centerLng - totalWidth / 2;
            double startLat = centerLat + charHeight / 2;

            int globalIndex = 0;

            for (int c = 0; c < text.length(); c++) {
                char ch = text.charAt(c);
                int[][] pattern = digitPatterns.get(ch);

                if (pattern != null) {
                    // 绘制点阵
                    for (int row = 0; row < pattern.length; row++) {
                        for (int col = 0; col < pattern[row].length; col++) {
                            if (pattern[row][col] == 1) {
                                double lng = startLng + c * (charWidth + spacing) + col * charWidth;
                                double lat = startLat - row * charHeight;

                                points.add(PatternRouteResult.PatternPoint.builder()
                                        .longitude(lng)
                                        .latitude(lat)
                                        .index(globalIndex++)
                                        .build());
                            }
                        }
                    }
                } else {
                    // 对于未定义的字符，使用简单的圆形代替
                    double charCenterLng = startLng + c * (charWidth + spacing) + charWidth / 2;
                    double charCenterLat = startLat - charHeight / 2;
                    List<PatternRouteResult.PatternPoint> circlePoints =
                            generateCirclePoints(charCenterLng, charCenterLat, charWidth * 0.5);
                    for (PatternRouteResult.PatternPoint p : circlePoints) {
                        points.add(PatternRouteResult.PatternPoint.builder()
                                .longitude(p.getLongitude())
                                .latitude(p.getLatitude())
                                .index(globalIndex++)
                                .build());
                    }
                }
            }

            // 添加起点到终点，形成闭环
            if (points.size() > 1) {
                points.add(PatternRouteResult.PatternPoint.builder()
                        .longitude(points.get(0).getLongitude())
                        .latitude(points.get(0).getLatitude())
                        .index(points.size())
                        .build());
            }

            // 简化点数量（如果太多）
            if (points.size() > 100) {
                points = simplifyPoints(points, 100);
            }

        } catch (Exception e) {
            log.error("生成文字图案坐标点失败: {}", e.getMessage());
        }

        return points;
    }

    /**
     * 生成图形图案坐标点
     */
    private List<PatternRouteResult.PatternPoint> generateShapePoints(
            String shape, double centerLng, double centerLat, double scale) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        try {
            double radius = 0.01 * scale * 1000; // 基础半径

            switch (shape.toLowerCase()) {
                case "心形":
                case "heart":
                    points = generateHeartPoints(centerLng, centerLat, radius * 2);
                    break;
                case "圆形":
                case "circle":
                    points = generateCirclePoints(centerLng, centerLat, radius);
                    break;
                case "正方形":
                case "square":
                    points = generateSquarePoints(centerLng, centerLat, radius * 1.5);
                    break;
                case "三角形":
                case "triangle":
                    points = generateTrianglePoints(centerLng, centerLat, radius * 1.5);
                    break;
                case "五角星":
                case "star":
                    points = generateStarPoints(centerLng, centerLat, radius * 2, 5);
                    break;
                case "五边形":
                case "pentagon":
                    points = generatePolygonPoints(centerLng, centerLat, radius * 1.5, 5);
                    break;
                default:
                    // 默认为圆形
                    points = generateCirclePoints(centerLng, centerLat, radius);
            }

        } catch (Exception e) {
            log.error("生成图形图案坐标点失败: {}", e.getMessage());
        }

        return points;
    }

    /**
     * 生成心形点
     */
    private List<PatternRouteResult.PatternPoint> generateHeartPoints(
            double centerLng, double centerLat, double size) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        // 简化的心形参数方程
        for (double t = 0; t <= 2 * Math.PI; t += 0.1) {
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);

            // 缩放
            double lng = centerLng + x * size * 0.0001;
            double lat = centerLat + y * size * 0.0001;

            points.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(lng)
                    .latitude(lat)
                    .index(points.size())
                    .build());
        }

        return points;
    }

    /**
     * 生成圆形点
     */
    private List<PatternRouteResult.PatternPoint> generateCirclePoints(
            double centerLng, double centerLat, double radius) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        int numPoints = 36;
        for (int i = 0; i <= numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double lng = centerLng + radius * 0.0001 * Math.cos(angle);
            double lat = centerLat + radius * 0.0001 * Math.sin(angle);

            points.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(lng)
                    .latitude(lat)
                    .index(i)
                    .build());
        }

        return points;
    }

    /**
     * 生成正方形点
     */
    private List<PatternRouteResult.PatternPoint> generateSquarePoints(
            double centerLng, double centerLat, double size) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        double half = size * 0.0001;
        double[][] corners = {
                {centerLng - half, centerLat - half},
                {centerLng + half, centerLat - half},
                {centerLng + half, centerLat + half},
                {centerLng - half, centerLat + half},
                {centerLng - half, centerLat - half}
        };

        for (int i = 0; i < corners.length; i++) {
            points.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(corners[i][0])
                    .latitude(corners[i][1])
                    .index(i)
                    .build());
        }

        return points;
    }

    /**
     * 生成三角形点
     */
    private List<PatternRouteResult.PatternPoint> generateTrianglePoints(
            double centerLng, double centerLat, double size) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        double height = size * 0.0001 * Math.sqrt(3) / 2;
        double half = size * 0.0001;

        double[][] corners = {
                {centerLng, centerLat + height * 2 / 3},
                {centerLng - half, centerLat - height / 3},
                {centerLng + half, centerLat - height / 3},
                {centerLng, centerLat + height * 2 / 3}
        };

        for (int i = 0; i < corners.length; i++) {
            points.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(corners[i][0])
                    .latitude(corners[i][1])
                    .index(i)
                    .build());
        }

        return points;
    }

    /**
     * 生成五角星点
     */
    private List<PatternRouteResult.PatternPoint> generateStarPoints(
            double centerLng, double centerLat, double size, int points) {
        return generatePolygonPoints(centerLng, centerLat, size, points);
    }

    /**
     * 生成多边形点
     */
    private List<PatternRouteResult.PatternPoint> generatePolygonPoints(
            double centerLng, double centerLat, double radius, int sides) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        for (int i = 0; i <= sides; i++) {
            double angle = 2 * Math.PI * i / sides - Math.PI / 2;
            double lng = centerLng + radius * 0.0001 * Math.cos(angle);
            double lat = centerLat + radius * 0.0001 * Math.sin(angle);

            points.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(lng)
                    .latitude(lat)
                    .index(i)
                    .build());
        }

        return points;
    }

    /**
     * 简化点数量（Douglas-Peucker算法简化）
     */
    private List<PatternRouteResult.PatternPoint> simplifyPoints(
            List<PatternRouteResult.PatternPoint> points, int targetSize) {
        if (points.size() <= targetSize) {
            return points;
        }

        // 简单的间隔采样
        List<PatternRouteResult.PatternPoint> simplified = new ArrayList<>();
        double step = (double) points.size() / targetSize;

        for (int i = 0; i < targetSize; i++) {
            int index = (int) (i * step);
            if (index >= points.size()) {
                index = points.size() - 1;
            }
            PatternRouteResult.PatternPoint p = points.get(index);
            simplified.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(p.getLongitude())
                    .latitude(p.getLatitude())
                    .index(i)
                    .build());
        }

        // 确保起点和终点相同
        if (!simplified.isEmpty()) {
            simplified.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(simplified.get(0).getLongitude())
                    .latitude(simplified.get(0).getLatitude())
                    .index(simplified.size())
                    .build());
        }

        return simplified;
    }

    /**
     * 计算量化数据
     */
    private PatternRouteResult.QuantifiedData calculateQuantifiedData(
            RoutePlanningResult.PathInfo pathInfo, int waypointCount) {
        double distance = 0;
        long duration = 0;

        try {
            if (pathInfo.getDistance() != null) {
                distance = Double.parseDouble(pathInfo.getDistance());
            }
            if (pathInfo.getDuration() != null) {
                duration = Long.parseLong(pathInfo.getDuration());
            }
        } catch (Exception e) {
            log.warn("解析距离或时间失败");
        }

        // 计算难度
        String difficulty;
        double distanceKm = distance / 1000;
        if (distanceKm < 10) {
            difficulty = "easy";
        } else if (distanceKm < 30) {
            difficulty = "medium";
        } else {
            difficulty = "hard";
        }

        // 计算路段数
        int segmentCount = pathInfo.getSteps() != null ? pathInfo.getSteps().size() : 0;

        return PatternRouteResult.QuantifiedData.builder()
                .totalDistance(distance)
                .totalDistanceKm(distanceKm)
                .duration(duration)
                .durationMinutes(duration / 60.0)
                .durationHours(duration / 3600.0)
                .difficulty(difficulty)
                .elevation(0.0) // 高德API不返回爬升数据，暂时设为0
                .waypointCount(waypointCount)
                .segmentCount(segmentCount)
                .build();
    }

    /**
     * 从JSON字符串中提取字段值
     */
    private String extractJsonField(String json, String field) {
        try {
            Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^\",\\}]*)\"?");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("提取JSON字段失败: {} - {}", field, e.getMessage());
        }
        return null;
    }
}
