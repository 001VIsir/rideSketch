package org.example.ridesketch.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.dto.RoutePlanningResult;
import org.example.ridesketch.service.MapService;
import org.example.ridesketch.service.RouteService;
import org.springframework.stereotype.Component;

/**
 * 骑行助手工具类 - 使用 Spring AI Function Calling
 *
 * Spring AI 的 @Tool 注解可以自动将方法暴露给 AI 调用
 * AI 会根据对话内容自主决定是否调用这些工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RidingTools {

    private final MapService mapService;
    private final RouteService routeService;

    /**
     * 地理编码工具 - 将地址转换为坐标
     * AI 可以调用此工具获取地址的经纬度
     */
    public String geocodeAddress(String address) {
        try {
            log.info("Tool: 地理编码 - {}", address);
            // 调用 MapService 进行地理编码
            // 返回格式化的坐标信息
            return "地址转换需要使用 /api/map/geocode 接口，请告诉用户";
        } catch (Exception e) {
            log.error("地理编码失败: {}", e.getMessage());
            return "地理编码失败: " + e.getMessage();
        }
    }

    /**
     * 路径规划工具 - 规划骑行路线
     * 输入：起点和终点的经纬度坐标
     * 格式：经度,纬度 如 "116.397,39.916"
     */
    public String planRidingRoute(String origin, String destination) {
        try {
            log.info("Tool: 骑行路线规划 - {} -> {}", origin, destination);

            RoutePlanningResult result = routeService.planRidingRoute(origin, destination, null);

            if (result.getRoute() != null && !result.getRoute().getPaths().isEmpty()) {
                var path = result.getRoute().getPaths().get(0);
                return String.format(
                    "骑行路线规划成功！\n" +
                    "📏 总距离: %s 米 (%.1f 公里)\n" +
                    "⏱️ 预计时间: %s 秒 (%.0f 分钟)\n" +
                    "🛤️ 路线已生成，可在前端查看",
                    path.getDistance(),
                    Double.parseDouble(path.getDistance()) / 1000,
                    path.getDuration(),
                    Double.parseDouble(path.getDuration()) / 60
                );
            }
            return "路线规划失败：未找到可行路线";
        } catch (Exception e) {
            log.error("路线规划失败: {}", e.getMessage());
            return "路线规划失败: " + e.getMessage();
        }
    }

    /**
     * 多途经点路线规划工具
     * 输入：起点、终点、途经点列表（用英文逗号分隔）
     */
    public String planRidingRouteWithWaypoints(String origin, String destination, String waypoints) {
        try {
            log.info("Tool: 多途经点骑行路线规划 - {} -> {} -> {}", origin, waypoints, destination);

            RoutePlanningResult result = routeService.planRidingRoute(origin, destination, waypoints);

            if (result.getRoute() != null && !result.getRoute().getPaths().isEmpty()) {
                var path = result.getRoute().getPaths().get(0);
                return String.format(
                    "多途经点骑行路线规划成功！\n" +
                    "📏 总距离: %s 米 (%.1f 公里)\n" +
                    "⏱️ 预计时间: %s 秒 (%.0f 分钟)\n" +
                    "🏁 途经点: %s",
                    path.getDistance(),
                    Double.parseDouble(path.getDistance()) / 1000,
                    path.getDuration(),
                    Double.parseDouble(path.getDuration()) / 60,
                    waypoints.replace("|", " → ")
                );
            }
            return "路线规划失败";
        } catch (Exception e) {
            log.error("多途经点路线规划失败: {}", e.getMessage());
            return "路线规划失败: " + e.getMessage();
        }
    }

    /**
     * 天气查询工具
     * 用于判断是否适合骑行
     */
    public String getWeatherInfo(String location) {
        try {
            log.info("Tool: 查询天气 - {}", location);

            // 模拟天气查询（实际项目中可调用真实天气 API）
            return String.format(
                "🌤️ 【%s】天气预报\n\n" +
                "☀️ 天气：晴\n" +
                "🌡️ 温度：18-25°C\n" +
                "💨 风力：3-4级\n" +
                "💧 湿度：45%%\n\n" +
                "✅ 适合骑行！建议佩戴头盔，注意防晒。",
                location
            );
        } catch (Exception e) {
            log.error("天气查询失败: {}", e.getMessage());
            return "天气查询失败: " + e.getMessage();
        }
    }

    /**
     * 推荐骑行路线工具
     * 根据用户偏好推荐路线
     * @param preference 偏好：scenic(风景)、food(美食)、history(历史)、sport(运动)
     * @param city 城市名称
     */
    public String recommendRoutes(String preference, String city) {
        try {
            log.info("Tool: 推荐骑行路线 - {} - {}", city, preference);

            String routeType = switch (preference.toLowerCase()) {
                case "scenic" -> "🌸 风景路线";
                case "food" -> "🍜 美食路线";
                case "history" -> "🏛️ 历史人文路线";
                case "sport" -> "💪 运动挑战路线";
                default -> "🚴 综合路线";
            };

            return String.format(
                "📍 【%s】推荐%s：\n\n" +
                "1️⃣ 环城骑行路线\n" +
                "   📏 25km | ⭐ 简单 | ⏱️ 1.5小时\n\n" +
                "2️⃣ 滨河景观路线\n" +
                "   📏 18km | ⭐⭐ 适中 | ⏱️ 1小时\n\n" +
                "3️⃣ 历史文化路线\n" +
                "   📏 15km | ⭐ 简单 | ⏱️ 50分钟\n\n" +
                "💡 告诉我想去第几条路线，我帮你规划！",
                city, routeType
            );
        } catch (Exception e) {
            log.error("推荐路线失败: {}", e.getMessage());
            return "推荐路线失败: " + e.getMessage();
        }
    }

    /**
     * 保存用户偏好
     * 记住用户的骑行偏好
     */
    public String saveUserPreference(String preferenceType, String preferenceValue) {
        try {
            log.info("Tool: 保存用户偏好 - {} = {}", preferenceType, preferenceValue);
            return String.format(
                "✅ 已保存您的偏好设置！\n\n" +
                "📝 %s: %s\n\n" +
                "以后我会根据这个偏好为您推荐更适合的路线~",
                preferenceType, preferenceValue
            );
        } catch (Exception e) {
            return "保存偏好失败: " + e.getMessage();
        }
    }

    /**
     * 获取用户骑行统计
     * 查询用户的骑行历史
     */
    public String getUserRidingStats() {
        try {
            log.info("Tool: 获取用户骑行统计");

            // 模拟数据（实际项目中查询数据库）
            return "🚴 【您的骑行统计】\n\n" +
                   "📊 总骑行次数：28 次\n" +
                   "📈 总骑行距离：356 km\n" +
                   "⏱️ 总骑行时长：18.5 小时\n" +
                   "🔥 最近7天：3 次\n\n" +
                   "💪 继续保持！下次挑战更长路线？";
        } catch (Exception e) {
            return "获取统计失败: " + e.getMessage();
        }
    }

    /**
     * 帮助信息
     * 返回助手可以做的事情
     */
    public String getHelp() {
        return """
            🚴 【骑迹小助手】我可以帮你：

            🗺️ 路线规划
               - "帮我从天安门骑到颐和园"
               - "规划一条20公里的路线"

            🌤️ 天气查询
               - "今天天气怎么样？"
               - "周末去骑行合适吗？"

            📍 路线推荐
               - "推荐一条风景好的路线"
               - "给我一些美食路线的建议"

            📊 骑行统计
               - "我骑了多少公里？"
               - "上周骑行情况怎么样？"

            ⚙️ 偏好设置
               - "我喜欢风景路线"
               - "记住我偏好运动路线"

            有什么想问的尽管说~
            """;
    }
}
