package org.example.ridesketch.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 服务
 * 整合 RidingAgentCore、Memory 的核心服务
 *
 * 实现功能：
 * 1. 智能对话 - 基于 Spring AI + 大模型
 * 2. Agent 记忆机制 - 对话历史 + 用户偏好
 * 3. 上下文管理 - 整合用户信息到对话中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final RidingAgentCore ridingAgentCore;
    private final AgentMemoryService memoryService;

    /**
     * 处理用户对话
     * 核心方法：智能对话 + 记忆机制
     *
     * @param userId  用户ID（可以为 null，表示匿名用户）
     * @param message 用户消息
     * @return AI 回复
     */
    public String chat(Long userId, String message) {
        try {
            log.info("Agent 处理对话: userId={}, message={}", userId, message);

            String response;

            // 如果是已登录用户，注入上下文
            if (userId != null && userId > 0) {
                // 增加对话计数
                memoryService.incrementConversationCount(userId);

                // 获取对话历史
                List<Map<String, String>> history = memoryService.getHistory(userId);

                // 获取用户偏好，构建上下文
                Map<String, String> preferences = memoryService.getAllPreferences(userId);

                // 构造增强的上下文
                String enhancedMessage = buildEnhancedMessage(message, preferences, history);

                // 调用 Agent
                response = ridingAgentCore.chat(enhancedMessage);

                // 保存对话历史
                memoryService.saveMessage(userId, "user", message);
                memoryService.saveMessage(userId, "assistant", response);
            } else {
                // 匿名用户
                response = ridingAgentCore.chat(message);
            }

            return response;

        } catch (Exception e) {
            log.error("Agent 处理对话失败: {}", e.getMessage(), e);
            return "抱歉，我遇到了一些问题，请稍后再试。错误信息: " + e.getMessage();
        }
    }

    /**
     * 构建增强的上下文消息
     */
    private String buildEnhancedMessage(String message, Map<String, String> preferences, List<Map<String, String>> history) {
        StringBuilder sb = new StringBuilder();

        // 添加用户偏好
        if (!preferences.isEmpty()) {
            sb.append("\n\n## 用户偏好\n");
            preferences.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        // 添加最近对话历史（最多3轮）
        if (!history.isEmpty()) {
            sb.append("\n\n## 最近对话\n");
            int count = 0;
            for (Map<String, String> msg : history) {
                if (count >= 6) break;  // 最多3轮（用户+助手）
                String role = "user".equals(msg.get("role")) ? "用户" : "助手";
                sb.append(role).append(": ").append(msg.get("content")).append("\n");
                count++;
            }
        }

        sb.append("\n\n## 当前问题\n").append(message);

        return sb.toString();
    }

    /**
     * 清除用户对话历史
     *
     * @param userId 用户ID
     */
    public void clearMemory(Long userId) {
        memoryService.clearHistory(userId);
    }

    /**
     * 保存用户偏好
     *
     * @param userId  用户ID
     * @param key     偏好键
     * @param value   偏好值
     */
    public void savePreference(Long userId, String key, String value) {
        memoryService.savePreference(userId, key, value);
    }

    /**
     * 获取用户偏好
     *
     * @param userId 用户ID
     * @return 偏好 Map
     */
    public Map<String, String> getPreferences(Long userId) {
        return memoryService.getAllPreferences(userId);
    }

    /**
     * 初始化用户画像
     *
     * @param userId   用户ID
     * @param nickname 用户昵称
     */
    public void initUserProfile(Long userId, String nickname) {
        memoryService.initUserProfile(userId, nickname);
    }

    /**
     * 获取帮助信息
     */
    public String getHelp() {
        return ridingAgentCore.getHelp();
    }
}
