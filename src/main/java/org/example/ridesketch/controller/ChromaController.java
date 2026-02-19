package org.example.ridesketch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.common.Result;
import org.example.ridesketch.service.ChromaService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chroma向量库控制器
 * 提供语义搜索和RAG问答功能
 */
@Slf4j
@RestController
@RequestMapping("/api/chroma")
@RequiredArgsConstructor
public class ChromaController {

    private final ChromaService chromaService;

    /**
     * 语义搜索
     *
     * @param query     查询文本
     * @param nResults  返回结果数量
     * @return 搜索结果
     */
    @PostMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Integer nResults = request.get("n_results") != null ? (Integer) request.get("n_results") : 3;

        log.info("Chroma语义搜索: {}", query);

        List<Map<String, Object>> results = chromaService.semanticSearch(query, nResults);

        return Result.success(results);
    }

    /**
     * 添加文档
     *
     * @param request 添加请求
     * @return 添加结果
     */
    @PostMapping("/document")
    public Result<Map<String, Object>> addDocument(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> documents = (List<String>) request.get("documents");
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) request.get("ids");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> metadata = (List<Map<String, String>>) request.get("metadata");

        log.info("添加文档到Chroma: {} 条", documents != null ? documents.size() : 0);

        Map<String, Object> result = chromaService.addDocuments(documents, ids, metadata);

        return Result.success(result);
    }

    /**
     * 统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = chromaService.getStats();
        return Result.success(stats);
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        boolean healthy = chromaService.healthCheck();

        Map<String, Object> status = new HashMap<>();
        status.put("status", healthy ? "ok" : "error");
        status.put("service", "Chroma");
        status.put("message", healthy ? "服务正常" : "服务不可用");

        if (healthy) {
            status.put("stats", chromaService.getStats());
        }

        return Result.success(status);
    }

    /**
     * RAG问答
     *
     * @param request 包含问题的请求
     * @return 回答结果
     */
    @PostMapping("/question")
    public Result<Map<String, Object>> answerQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        log.info("Chroma RAG问答: {}", question);

        String answer = chromaService.answerQuestion(question);

        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("answer", answer);

        return Result.success(data);
    }
}
