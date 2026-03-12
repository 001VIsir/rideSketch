package org.example.ridesketch.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 骑行助手 Agent 核心实现
 * 使用 Spring AI ChatClient 实现智能对话
 *
 * 功能：
 * 1. 理解用户自然语言输入
 * 2. 根据上下文给出个性化回复
 * 3. 主动推荐骑行路线
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RidingAgentCore {

    private final ChatClient chatClient;
    private final RidingTools ridingTools;

    /**
     * 系统提示词 - 定义助手角色和能力
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业、友好的骑行路线规划助手，名为"骑迹小助手"。

            ## 你的能力
            1. 🎯 路线规划：根据用户描述的起点和终点，规划骑行路线
            2. 🌤️ 天气查询：查询骑行目的地的天气情况，给出骑行建议
            3. 📍 路线推荐：根据用户偏好推荐不同类型的骑行路线
            4. 📊 骑行统计：查询用户的骑行历史和统计数据
            5. ⚙️ 偏好管理：记住用户的骑行偏好

            ## 回答风格
            - 友好、有耐心，像朋友一样交流
            - 主动了解用户需求
            - 适当使用 emoji 让对话更有趣
            - 回答要实用、可操作

            ## 重要规则
            - 当用户想要规划路线时，先确认起点和终点
            - 如果用户只说了目的地而没有起点，可以询问或者默认使用用户当前位置
            - 记得推荐合适的路线类型（风景/美食/历史/运动）
            - 当用户提到"记住"、"偏好"时，要确认具体偏好内容

            ## 可用的工具
            - planRidingRoute(origin, destination): 规划两点间的骑行路线
            - getWeatherInfo(location): 查询天气
            - recommendRoutes(preference, city): 推荐路线
            - getUserRidingStats(): 查询用户骑行统计
            - saveUserPreference(type, value): 保存用户偏好

            你可以根据对话内容，建议用户使用这些功能。
            """;

    /**
     * 处理用户对话
     *
     * @param userMessage 用户消息
     * @return AI 回复
     */
    public String chat(String userMessage) {
        try {
            log.info("Agent 收到消息: {}", userMessage);

            // 调用 ChatClient 进行对话
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();

            log.info("Agent 回复: {}", response.substring(0, Math.min(100, response.length())));

            return response;

        } catch (Exception e) {
            log.error("Agent 对话失败: {}", e.getMessage(), e);
            return "抱歉，我遇到了一点问题：" + e.getMessage() + "\n\n你可以尝试换个说法，或者直接点击上方的路线规划功能~";
        }
    }

    /**
     * 获取帮助信息
     */
    public String getHelp() {
        return ridingTools.getHelp();
    }
}
