package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.agent.AgentMemoryService;
import org.example.ridesketch.agent.AgentService;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Agent 控制器
 * 提供智能对话、记忆管理、偏好设置等功能
 *
 * 核心功能：
 * 1. 智能对话 - 基于 LangChain4j Agent + Function Calling
 * 2. 记忆管理 - 对话历史自动保存
 * 3. 偏好设置 - 用户骑行偏好持久化
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentMemoryService memoryService;

    /**
     * 智能对话接口
     * 核心接口：AI Agent 自动推理 + Function Calling
     *
     * 工作流程：
     * 1. 用户发送消息
     * 2. Agent 分析用户意图
     * 3. 如需调用工具，自动调用（天气、路线规划等）
     * 4. 整合工具返回结果生成最终回复
     * 5. 保存对话到记忆
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return Result.error("消息不能为空");
        }

        log.info("收到 Agent 对话请求: {}", message);

        // 获取用户ID（可能为 null，表示匿名用户）
        Long userId = userDetails != null ? userDetails.getId() : null;

        // 首次对话时初始化用户画像
        if (userId != null && userDetails.getUser() != null && userDetails.getUser().getNickname() != null) {
            // 检查是否首次对话（通过是否有过往偏好判断）
            if (memoryService.getAllPreferences(userId).isEmpty()) {
                memoryService.initUserProfile(userId, userDetails.getUser().getNickname());
            }
        }

        // 调用 Agent 处理对话
        String response = agentService.chat(userId, message);

        // 构建返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("response", response);
        data.put("userId", userId);
        data.put("memoryEnabled", userId != null);  // 是否启用了记忆功能

        return Result.success(data);
    }

    /**
     * 清除对话记忆
     * 用户可以清除所有对话历史
     */
    @DeleteMapping("/memory")
    public Result<Void> clearMemory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("请先登录");
        }

        agentService.clearMemory(userDetails.getId());
        return Result.success(null);
    }

    /**
     * 获取对话历史
     * 查看保存的对话记录
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return Result.error("请先登录");
        }

        var history = memoryService.getHistory(userDetails.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("history", history);
        data.put("count", history.size());

        return Result.success(data);
    }

    /**
     * 保存用户偏好
     * 手动保存用户的骑行偏好
     */
    @PostMapping("/preference")
    public Result<Void> savePreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> request) {

        if (userDetails == null) {
            return Result.error("请先登录");
        }

        String key = request.get("key");
        String value = request.get("value");

        if (key == null || value == null) {
            return Result.error("偏好键和值不能为空");
        }

        agentService.savePreference(userDetails.getId(), key, value);

        return Result.success(null);
    }

    /**
     * 获取用户偏好
     * 查看已保存的所有偏好
     */
    @GetMapping("/preference")
    public Result<Map<String, String>> getPreferences(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return Result.error("请先登录");
        }

        var preferences = agentService.getPreferences(userDetails.getId());
        return Result.success(preferences);
    }

    /**
     * 快速开始对话
     * 提供预设问题，引导用户使用
     */
    @GetMapping("/suggestions")
    public Result<Map<String, Object>> getSuggestions() {
        Map<String, Object> data = new HashMap<>();

        data.put("suggestions", new String[]{
            "帮我规划从天安门到颐和园的骑行路线",
            "今天天气怎么样？适合骑行吗？",
            "给我推荐一条风景好的骑行路线",
            "我上周骑了多远？",
            "我喜欢骑风景路线，请记住我的偏好"
        });

        data.put("preferences", new String[]{
            "难度偏好",
            "距离偏好",
            "最常去的城市",
            "喜欢的路线类型"
        });

        return Result.success(data);
    }

    /**
     * Agent 健康检查
     * 检查 Agent 服务是否可用
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ok");
        status.put("service", "RidingAgent");
        status.put("features", new String[]{
            "Function Calling",
            "Memory",
            "Tools"
        });

        return Result.success(status);
    }
}
