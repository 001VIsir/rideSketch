package org.example.ridesketch.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 记忆服务
 * 使用 Redis 存储用户对话历史和偏好设置
 *
 * 记忆类型：
 * 1. 对话历史 - 短期记忆，保存最近 N 轮对话
 * 2. 用户偏好 - 长期记忆，保存用户的骑行偏好
 * 3. 用户画像 - 记住用户的基本信息和骑行习惯
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Key 前缀
    private static final String MEMORY_PREFIX = "agent:memory:";
    private static final String PREFERENCE_PREFIX = "agent:preference:";
    private static final String PROFILE_PREFIX = "agent:profile:";

    // 对话历史配置
    private static final int MAX_HISTORY_SIZE = 10;  // 最多保存 10 轮对话
    private static final Duration MEMORY_TTL = Duration.ofDays(7);  // 记忆保留 7 天

    /**
     * 保存用户对话消息
     *
     * @param userId   用户ID
     * @param role     角色：user/assistant
     * @param content  消息内容
     */
    public void saveMessage(Long userId, String role, String content) {
        String key = MEMORY_PREFIX + userId;

        try {
            // 获取现有历史
            List<Map<String, String>> history = getHistory(userId);

            // 添加新消息
            Map<String, String> message = new HashMap<>();
            message.put("role", role);
            message.put("content", content);
            message.put("timestamp", String.valueOf(System.currentTimeMillis()));
            history.add(message);

            // 超过最大长度时移除最早的对话
            while (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }

            // 保存到 Redis
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(key, json, MEMORY_TTL);

            log.debug("保存消息到记忆: userId={}, role={}", userId, role);

        } catch (JsonProcessingException e) {
            log.error("保存对话历史失败: {}", e.getMessage());
        }
    }

    /**
     * 获取用户对话历史
     *
     * @param userId 用户ID
     * @return 对话历史列表
     */
    public List<Map<String, String>> getHistory(Long userId) {
        String key = MEMORY_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (JsonProcessingException e) {
            log.error("读取对话历史失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取格式化后的对话历史（用于构建上下文）
     *
     * @param userId 用户ID
     * @return 格式化的历史消息字符串
     */
    public String getFormattedHistory(Long userId) {
        List<Map<String, String>> history = getHistory(userId);

        if (history.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 对话历史\n\n");

        for (Map<String, String> msg : history) {
            String role = msg.get("role");
            String content = msg.get("content");
            String roleName = "user".equals(role) ? "用户" : "助手";
            sb.append(String.format("%s: %s\n\n", roleName, content));
        }

        return sb.toString();
    }

    /**
     * 清除用户对话历史
     *
     * @param userId 用户ID
     */
    public void clearHistory(Long userId) {
        String key = MEMORY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("清除用户对话历史: userId={}", userId);
    }

    /**
     * 保存用户偏好
     *
     * @param userId   用户ID
     * @param key      偏好键（如：difficulty、preferred_distance、favorite_area）
     * @param value    偏好值
     */
    public void savePreference(Long userId, String key, String value) {
        String redisKey = PREFERENCE_PREFIX + userId + ":" + key;
        redisTemplate.opsForValue().set(redisKey, value, Duration.ofDays(365));  // 偏好保留 1 年
        log.debug("保存用户偏好: userId={}, {}={}", userId, key, value);
    }

    /**
     * 获取用户偏好
     *
     * @param userId 用户ID
     * @param key    偏好键
     * @return 偏好值，不存在返回 null
     */
    public String getPreference(Long userId, String key) {
        String redisKey = PREFERENCE_PREFIX + userId + ":" + key;
        return redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 获取用户所有偏好
     *
     * @param userId 用户ID
     * @return 偏好 Map
     */
    public Map<String, String> getAllPreferences(Long userId) {
        String pattern = PREFERENCE_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> preferences = new HashMap<>();
        for (String key : keys) {
            String value = redisTemplate.opsForValue().get(key);
            String preferenceKey = key.substring(key.lastIndexOf(":") + 1);
            preferences.put(preferenceKey, value);
        }

        return preferences;
    }

    /**
     * 保存用户画像信息
     *
     * @param userId    用户ID
     * @param field     字段名
     * @param value     字段值
     */
    public void saveProfile(Long userId, String field, Object value) {
        String redisKey = PROFILE_PREFIX + userId + ":" + field;
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofDays(365));
        } catch (JsonProcessingException e) {
            log.error("保存用户画像失败: {}", e.getMessage());
        }
    }

    /**
     * 获取用户画像
     *
     * @param userId 用户ID
     * @return 用户画像 Map
     */
    public Map<String, Object> getProfile(Long userId) {
        String pattern = PROFILE_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> profile = new HashMap<>();
        for (String key : keys) {
            String value = redisTemplate.opsForValue().get(key);
            String field = key.substring(key.lastIndexOf(":") + 1);
            profile.put(field, value);
        }

        return profile;
    }

    /**
     * 初始化新用户画像
     * 当用户首次使用 Agent 时调用
     *
     * @param userId   用户ID
     * @param nickname 用户昵称
     */
    public void initUserProfile(Long userId, String nickname) {
        Map<String, Object> initialProfile = new HashMap<>();
        initialProfile.put("nickname", nickname);
        initialProfile.put("first_use_time", System.currentTimeMillis());
        initialProfile.put("total_conversations", 0);

        for (Map.Entry<String, Object> entry : initialProfile.entrySet()) {
            saveProfile(userId, entry.getKey(), entry.getValue());
        }

        log.info("初始化用户画像: userId={}, nickname={}", userId, nickname);
    }

    /**
     * 增加对话计数
     *
     * @param userId 用户ID
     */
    public void incrementConversationCount(Long userId) {
        String key = PROFILE_PREFIX + userId + ":total_conversations";
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次设置，设置过期时间
            redisTemplate.expire(key, Duration.ofDays(365));
        }
    }

    /**
     * 构建用户上下文
     * 整合用户画像和偏好，生成用于 LLM 上下文的字符串
     *
     * @param userId 用户ID
     * @return 上下文字符串
     */
    public String buildUserContext(Long userId) {
        StringBuilder context = new StringBuilder();

        // 添加用户画像
        Map<String, Object> profile = getProfile(userId);
        if (!profile.isEmpty()) {
            context.append("\n\n## 用户信息\n");
            profile.forEach((key, value) -> {
                context.append(String.format("- %s: %s\n", key, value));
            });
        }

        // 添加用户偏好
        Map<String, String> preferences = getAllPreferences(userId);
        if (!preferences.isEmpty()) {
            context.append("\n\n## 用户偏好\n");
            preferences.forEach((key, value) -> {
                context.append(String.format("- %s: %s\n", key, value));
            });
        }

        return context.toString();
    }
}
