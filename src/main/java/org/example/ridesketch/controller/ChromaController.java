package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.rag.ChromaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Chroma向量库控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chroma")
@RequiredArgsConstructor
public class ChromaController {

    private final ChromaService chromaService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "healthy", chromaService.isHealthy(),
                "documentCount", chromaService.getDocumentCount()
        );
    }

    /**
     * 查询相似文档
     */
    @PostMapping("/search")
    public Map<String, Object> search(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.get("topK") != null ? (Integer) request.get("topK") : 3;

        if (query == null || query.trim().isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "查询不能为空"
            );
        }

        try {
            List<String> results = chromaService.similaritySearch(query, topK);
            return Map.of(
                    "success", true,
                    "data", results
            );
        } catch (Exception e) {
            log.error("Chroma查询失败: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 添加文档
     */
    @PostMapping("/document")
    public Map<String, Object> addDocument(@RequestBody Map<String, String> request) {
        String id = request.get("id");
        String content = request.get("content");
        String category = request.get("category");

        if (content == null || content.trim().isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "内容不能为空"
            );
        }

        try {
            boolean success = chromaService.addDocument(
                    id != null ? id : "doc_" + System.currentTimeMillis(),
                    content,
                    category != null ? category : "general"
            );
            return Map.of(
                    "success", success,
                    "message", success ? "添加成功" : "添加失败"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 问答
     */
    @PostMapping("/question")
    public Map<String, Object> question(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "问题不能为空"
            );
        }

        try {
            String answer = chromaService.questionAnswer(question);
            return Map.of(
                    "success", true,
                    "data", answer
            );
        } catch (Exception e) {
            log.error("问答失败: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }
}
