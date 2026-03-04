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

    /**
     * AI生成图案坐标点的prompt模板
     */
    private static final String GENERATE_PATTERN_PROMPT_TEMPLATE = """
            生成一个简单的%s形状骑行路线坐标点。城市中心：经度%s，纬度%s。

            返回JSON格式（只返回JSON，不要其他内容）：
            {"points": [{"longitude":经度,"latitude":纬度,"index":0},...]}

            要求：5-8个点，第一点和最后一点相同形成闭环。
            """;

    /**
     * 根据图案内容自动判断是文字还是图形
     */
    private String guessPatternType(String pattern) {
        if (pattern == null) {
            return "text";
        }

        String p = pattern.toLowerCase();

        // 图形关键字
        String[] shapeKeywords = {"circle", "圆形", "star", "五角星", "heart", "心形",
                "triangle", "三角形", "square", "正方形", "pentagon", "五边形"};

        for (String keyword : shapeKeywords) {
            if (p.contains(keyword)) {
                return "shape";
            }
        }

        // 默认是文字
        return "text";
    }

    @Override
    public PatternRouteResult generatePatternRoute(PatternRouteRequest request) {
        try {
            log.info("开始生成图案路书: {}", request.getDescription());

            // 步骤1: 解析用户输入（只有description不为空时才调用LLM）
            String parsedInfo = "{}";
            if (StringUtils.isNotBlank(request.getDescription())) {
                parsedInfo = parsePatternInput(request.getDescription());
            }
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
            } else if (StringUtils.isNotBlank(pattern)) {
                // 根据pattern自动判断类型
                patternType = guessPatternType(pattern);
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

            // 步骤4: 计算缩放比例（增大默认值，使图案覆盖更大区域）
            double scale = request.getScale() != null ? request.getScale() : 0.1;
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

            // 只有当用户提供了description时才使用AI，否则用代码算法
            if (StringUtils.isNotBlank(request.getDescription())) {
                // 使用AI生成图案
                patternPoints = generatePatternWithAI(pattern, centerLng, centerLat, scale);
                log.debug("AI生成了 {} 个图案坐标点", patternPoints.size());

                // 如果AI生成失败，回退到代码算法
                if (patternPoints.isEmpty()) {
                    log.warn("AI生成图案失败，回退到代码算法");
                    if ("shape".equals(patternType)) {
                        patternPoints = generateShapePoints(pattern, centerLng, centerLat, scale);
                    } else {
                        patternPoints = generateTextPoints(pattern, centerLng, centerLat, scale);
                    }
                }
            } else {
                // 使用代码算法生成图案
                if ("shape".equals(patternType)) {
                    patternPoints = generateShapePoints(pattern, centerLng, centerLat, scale);
                } else {
                    patternPoints = generateTextPoints(pattern, centerLng, centerLat, scale);
                }
            }

            if (patternPoints.isEmpty()) {
                return PatternRouteResult.builder()
                        .status("0")
                        .info("无法生成图案坐标点")
                        .build();
            }

            log.debug("最终生成了 {} 个图案坐标点", patternPoints.size());

            // 直接返回图案点，跳过路线规划（避免高德API限流）
            return PatternRouteResult.builder()
                    .status("1")
                    .info("图案路书生成成功")
                    .pattern(pattern)
                    .patternType(patternType)
                    .city(city)
                    .patternPoints(patternPoints)
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
     * 使用AI生成图案坐标点
     */
    private List<PatternRouteResult.PatternPoint> generatePatternWithAI(
            String pattern, double centerLng, double centerLat, double scale) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        try {
            // 调用AI生成图案坐标点
            String prompt = String.format(GENERATE_PATTERN_PROMPT_TEMPLATE,
                    pattern, centerLng, centerLat);

            log.debug("AI图案生成请求: {}", prompt);

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
                String aiResult = ollamaResponse.getResponse();

                log.debug("AI图案生成结果: {}", aiResult);

                // 解析AI返回的坐标点
                if (StringUtils.isNotBlank(aiResult) && aiResult.contains("points")) {
                    JSONObject jsonObj = JSON.parseObject(aiResult);
                    JSONArray pointsArray = jsonObj.getJSONArray("points");

                    if (pointsArray != null && !pointsArray.isEmpty()) {
                        for (int i = 0; i < pointsArray.size(); i++) {
                            JSONObject p = pointsArray.getJSONObject(i);
                            Double lng = p.getDouble("longitude");
                            Double lat = p.getDouble("latitude");

                            if (lng != null && lat != null) {
                                points.add(PatternRouteResult.PatternPoint.builder()
                                        .longitude(lng)
                                        .latitude(lat)
                                        .index(i)
                                        .build());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("AI生成图案坐标点失败: {}", e.getMessage());
        }

        return points;
    }

    /**
     * 根据图案类型和复杂度获取目标点数
     */
    private int getTargetPointCount(String pattern, String patternType) {
        if (pattern == null) {
            return 10;
        }

        String p = pattern.toLowerCase();

        if ("shape".equals(patternType)) {
            switch (p) {
                case "circle":
                case "圆形":
                    return 20; // 圆形需要较多点保持平滑
                case "heart":
                case "心形":
                    return 15; // 心形比较复杂
                case "star":
                case "五角星":
                    return 10; // 五角星10个顶点
                case "triangle":
                case "三角形":
                    return 4; // 三角形4个点
                case "square":
                case "正方形":
                    return 5; // 正方形5个点（4角+起点）
                case "pentagon":
                case "五边形":
                    return 6; // 五边形6个点
                default:
                    return 10;
            }
        } else {
            // 文字图案：根据字符数计算，每个字符约5个点
            return Math.max(pattern.length() * 5, 5);
        }
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

            // 简化点数量，根据图案类型设置不同的目标点数
            int targetPoints = getTargetPointCount(text, "text");
            if (points.size() > targetPoints) {
                points = simplifyPoints(points, targetPoints);
            }

            // 添加闭环点
            if (!points.isEmpty()) {
                points.add(PatternRouteResult.PatternPoint.builder()
                        .longitude(points.get(0).getLongitude())
                        .latitude(points.get(0).getLatitude())
                        .index(points.size())
                        .build());
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
            // 基础半径，根据scale调整（经纬度1度约111公里）
            double radius = scale * 0.5; // 直接使用scale作为半径（单位：度）

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

        // 根据图案类型简化点数量
        int targetPoints = getTargetPointCount(shape, "shape");
        if (points.size() > targetPoints) {
            points = simplifyPoints(points, targetPoints);
        }

        // 添加闭环点
        if (!points.isEmpty()) {
            points.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(points.get(0).getLongitude())
                    .latitude(points.get(0).getLatitude())
                    .index(points.size())
                    .build());
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
        for (double t = 0; t <= 2 * Math.PI; t += 0.3) {
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);

            // 缩放（直接使用size作为经纬度偏移）
            double lng = centerLng + x * size * 0.00001;
            double lat = centerLat + y * size * 0.00001;

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

        int numPoints = 20; // 减少点数
        for (int i = 0; i <= numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double lng = centerLng + radius * Math.cos(angle);
            double lat = centerLat + radius * Math.sin(angle);

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

        double half = size;
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

        double height = size * Math.sqrt(3) / 2;
        double half = size;

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
     * 生成五角星点（10个顶点：5个外顶点+5个内顶点）
     */
    private List<PatternRouteResult.PatternPoint> generateStarPoints(
            double centerLng, double centerLat, double size, int points) {
        List<PatternRouteResult.PatternPoint> starPoints = new ArrayList<>();

        // 生成五角星：交替的外半径和内半径
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / 2 + 2 * Math.PI * i / (points * 2);
            // 外半径和内半径交替
            double r = (i % 2 == 0) ? size : size * 0.4;
            double lng = centerLng + r * Math.cos(angle);
            double lat = centerLat + r * Math.sin(angle);

            starPoints.add(PatternRouteResult.PatternPoint.builder()
                    .longitude(lng)
                    .latitude(lat)
                    .index(i)
                    .build());
        }

        return starPoints;
    }

    /**
     * 生成多边形点
     */
    private List<PatternRouteResult.PatternPoint> generatePolygonPoints(
            double centerLng, double centerLat, double radius, int sides) {
        List<PatternRouteResult.PatternPoint> points = new ArrayList<>();

        for (int i = 0; i <= sides; i++) {
            double angle = 2 * Math.PI * i / sides - Math.PI / 2;
            double lng = centerLng + radius * Math.cos(angle);
            double lat = centerLat + radius * Math.sin(angle);

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
        List<PatternRouteResult.PatternPoint> result = new ArrayList<>();

        if (points.size() <= targetSize) {
            // 点数不足或刚好，直接使用原点数
            result.addAll(points);
        } else {
            // 简化点数量
            double step = (double) points.size() / targetSize;

            for (int i = 0; i < targetSize; i++) {
                int index = (int) (i * step);
                if (index >= points.size()) {
                    index = points.size() - 1;
                }
                PatternRouteResult.PatternPoint p = points.get(index);
                result.add(PatternRouteResult.PatternPoint.builder()
                        .longitude(p.getLongitude())
                        .latitude(p.getLatitude())
                        .index(i)
                        .build());
            }
        }

        return result;
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
