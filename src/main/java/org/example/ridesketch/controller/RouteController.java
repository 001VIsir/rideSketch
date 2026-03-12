package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.dto.AIRoutePlanningRequest;
import org.example.ridesketch.dto.AIRoutePlanningResult;
import org.example.ridesketch.dto.PatternRouteRequest;
import org.example.ridesketch.dto.PatternRouteResult;
import org.example.ridesketch.dto.RoutePlanningRequest;
import org.example.ridesketch.dto.RoutePlanningResult;
import org.example.ridesketch.service.AIRouteService;
import org.example.ridesketch.service.PatternRouteService;
import org.example.ridesketch.service.RouteService;
import org.springframework.web.bind.annotation.*;

/**
 * 路线规划控制器
 * <p>
 * 提供多种路线规划功能，包括基础骑行/步行路线规划、AI智能路线规划
 * 以及趣味图案路书生成。所有接口前缀为 /api/route
 *
 * 功能说明：
 * <ul>
 *   <li>基础路线规划：支持骑行、步行两种出行方式</li>
 *   <li>AI智能规划：基于自然语言描述生成骑行路线</li>
 *   <li>图案路书：生成骑行轨迹形成有趣图案</li>
 * </ul>
 *
 * @author rideSketch
 * @version 1.0.0
 * @see RouteService
 * @see AIRouteService
 * @see PatternRouteService
 */
@Slf4j
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final AIRouteService aiRouteService;
    private final PatternRouteService patternRouteService;

    /**
     * 多途经点路线规划接口
     * <p>
     * 支持起点、终点及多个途经点的路线规划，可选择骑行或步行模式。
     * 基于高德地图路径规划API实现。
     *
     * @api POST /api/route/plan
     * @param request 路线规划请求体
     *                - mode: 出行模式（riding骑行/walking步行），必填
     *                - origin: 起点经纬度，格式"经度,纬度"，必填
     *                - destination: 终点经纬度，格式"经度,纬度"，必填
     *                - waypoints: 途经点列表（可选），格式"经度,纬度|经度,纬度"
     * @return Result 包含RoutePlanningResult
     *         - paths: 路线列表，每条路线包含points（路径点集合）、
     *                  distance（总距离，单位米）、duration（预计时间，单位秒）
     * @see RoutePlanningRequest
     * @see RoutePlanningResult
     */
    @PostMapping("/plan")
    public Result<RoutePlanningResult> planRoute(@RequestBody RoutePlanningRequest request) {
        log.info("路线规划请求: mode={}, origin={}, destination={}, waypoints={}",
                request.getMode(), request.getOrigin(), request.getDestination(), request.getWaypoints());

        // 参数校验：起点和终点不能为空
        if (request.getOrigin() == null || request.getDestination() == null) {
            return Result.error("起点和终点不能为空");
        }

        // 调用路线规划服务获取规划结果
        RoutePlanningResult result = routeService.planRoute(request);
        return Result.success(result);
    }

    /**
     * 骑行路线规划接口
     * <p>
     * 专门针对骑行场景的路线规划，返回适合骑行的路线。
     *
     * @api GET /api/route/riding?origin=经纬度&destination=经纬度&waypoints=途经点
     * @param origin 起点经纬度，格式: "经度,纬度"（如"116.397428,39.90923"），必填
     * @param destination 终点经纬度，格式: "经度,纬度"，必填
     * @param waypoints 途经点（可选），多个途经点用"|"分隔
     *                  例如："116.397428,39.90923|116.387428,39.90923"
     * @return Result 包含RoutePlanningResult
     *         - paths: 骑行路线列表
     * @see RoutePlanningResult
     */
    @GetMapping("/riding")
    public Result<RoutePlanningResult> planRidingRoute(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String waypoints) {
        log.info("骑行路线规划请求: origin={}, destination={}, waypoints={}", origin, destination, waypoints);
        // 调用服务层进行骑行路线规划
        RoutePlanningResult result = routeService.planRidingRoute(origin, destination, waypoints);
        return Result.success(result);
    }

    /**
     * 步行路线规划接口
     * <p>
     * 针对步行场景的路线规划，返回适合行走的路线。
     * 注意：高德地图对步行路线支持可能有限。
     *
     * @api GET /api/route/walking?origin=经纬度&destination=经纬度&waypoints=途经点
     * @param origin 起点经纬度，格式: "经度,纬度"，必填
     * @param destination 终点经纬度，格式: "经度,纬度"，必填
     * @param waypoints 途经点（可选），多个途经点用"|"分隔
     * @return Result 包含RoutePlanningResult
     *         - paths: 步行路线列表
     * @see RoutePlanningResult
     */
    @GetMapping("/walking")
    public Result<RoutePlanningResult> planWalkingRoute(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String waypoints) {
        log.info("步行路线规划请求: origin={}, destination={}, waypoints={}", origin, destination, waypoints);
        // 调用服务层进行步行路线规划
        RoutePlanningResult result = routeService.planWalkingRoute(origin, destination, waypoints);
        return Result.success(result);
    }

    /**
     * AI智能路线规划接口
     * <p>
     * 基于自然语言描述智能生成骑行路线。通过AI理解用户的路线需求描述，
     * 自动规划起点、途经点和终点。适合不确定具体地点的用户。
     *
     * @api POST /api/route/ai-plan
     * @param request AI路线规划请求体
     *               - description: 路线描述，如"从故宫出发，途经天安门广场，到达颐和园"
     *               - city: 城市名称，用于限定搜索范围
     * @return Result 包含AIRoutePlanningResult
     *         - origin/destination: 起点和终点坐标
     *         - waypoints: AI推荐的途经点列表
     *         - description: 路线说明
     * @see AIRoutePlanningRequest
     * @see AIRoutePlanningResult
     */
    @PostMapping("/ai-plan")
    public Result<AIRoutePlanningResult> planAIRoute(@RequestBody AIRoutePlanningRequest request) {
        log.info("AI智能路线规划请求: {}", request.getDescription());

        // 参数校验：路线描述不能为空
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return Result.error("请输入路线描述");
        }

        // 调用AI路线规划服务
        AIRoutePlanningResult result = aiRouteService.planAIRoute(request);
        return Result.success(result);
    }

    /**
     * 图案路书生成接口
     * <p>
     * 生成沿特定图案轨迹骑行的路书。用户可以输入想要骑行的图案描述，
     * 系统会生成相应的骑行轨迹。例如：心形、五角星、字母等。
     *
     * @api POST /api/route/pattern
     * @param request 图案路书请求体
     *               - description: 图案描述（可选），如"爱心"、"星星"
     *               - pattern: 图案内容（可选），支持字符画或特殊格式
     *               - city: 城市名称，必填，用于确定骑行区域
     *               - size: 图案大小（可选），默认适中
     * @return Result 包含PatternRouteResult
     *         - points: 图案轨迹的经纬度点序列
     *         - totalDistance: 总距离（米）
     *         - description: 图案说明
     * @see PatternRouteRequest
     * @see PatternRouteResult
     */
    @PostMapping("/pattern")
    public Result<PatternRouteResult> generatePatternRoute(@RequestBody PatternRouteRequest request) {
        log.info("图案路书生成请求: {}", request.getDescription());

        // 参数校验：图案描述和图案内容至少提供一个
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            if (request.getPattern() == null || request.getPattern().isBlank()) {
                return Result.error("请输入图案描述或图案内容");
            }
        }

        // 城市名称为必填
        if (request.getCity() == null || request.getCity().isBlank()) {
            return Result.error("请提供城市名称");
        }

        // 调用图案路书生成服务
        PatternRouteResult result = patternRouteService.generatePatternRoute(request);
        return Result.success(result);
    }
}
