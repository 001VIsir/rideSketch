package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG知识库控制器
 * 提供基于知识库的问答服务
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 问答接口
     *
     * @param request 包含问题的请求
     * @return 回答结果
     */
    @PostMapping("/question")
    public Result<Map<String, Object>> answerQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        log.info("收到问答请求: {}", question);

        String answer = ragService.answerQuestion(question);

        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("answer", answer);

        return Result.success(data);
    }

    /**
     * 搜索知识
     *
     * @param keyword 关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<List<Map<String, String>>> search(@RequestParam String keyword) {
        log.info("搜索知识: {}", keyword);

        List<Map<String, String>> results = ragService.searchKnowledge(keyword);

        return Result.success(results);
    }

    /**
     * 获取所有知识类别
     *
     * @return 知识类别列表
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        List<String> categories = ragService.getCategories();
        return Result.success(categories);
    }

    /**
     * 健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ok");
        status.put("service", "RAG");
        status.put("categories", ragService.getCategories().size());

        return Result.success(status);
    }
}
