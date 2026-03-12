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
 * <p>
 * 提供基于知识库的问答和搜索服务，支持语义检索和AI问答功能。
 * 所有接口前缀为 /api/rag
 *
 * 功能说明：
 * <ul>
 *   <li>AI问答：基于知识库内容回答用户问题</li>
 *   <li>知识搜索：按关键词搜索知识库内容</li>
 *   <li>类别管理：获取知识库分类体系</li>
 *   <li>健康检查：监控RAG服务状态</li>
 * </ul>
 *
 * @author rideSketch
 * @version 1.0.0
 * @see RagService
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * AI问答接口
     * <p>
     * 基于RAG（检索增强生成）技术，结合知识库内容回答用户问题。
     * 系统会先从知识库中检索相关文档，再由AI生成答案。
     *
     * @api POST /api/rag/question
     * @param request 请求体，包含question字段
     *               - question: 用户问题，必填
     * @return Result 包含Map
     *         - question: 原始问题
     *         - answer: AI生成的答案
     * @see RagService#answerQuestion(String)
     */
    @PostMapping("/question")
    public Result<Map<String, Object>> answerQuestion(@RequestBody Map<String, String> request) {
        // 从请求中提取问题
        String question = request.get("question");
        log.info("收到问答请求: {}", question);

        // 调用RAG服务获取答案
        String answer = ragService.answerQuestion(question);

        // 构建响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("answer", answer);

        return Result.success(data);
    }

    /**
     * 知识搜索接口
     * <p>
     * 根据关键词在知识库中进行语义搜索，返回相关的知识条目。
     * 支持模糊匹配和语义相似度检索。
     *
     * @api GET /api/rag/search?keyword=关键词
     * @param keyword 搜索关键词，必填
     * @return Result 包含List&lt;Map&lt;String, String&gt;&gt;
     *         - 每条结果包含title（标题）、content（内容）、category（类别）等字段
     * @see RagService#searchKnowledge(String)
     */
    @GetMapping("/search")
    public Result<List<Map<String, String>>> search(@RequestParam String keyword) {
        log.info("搜索知识: {}", keyword);

        // 调用RAG服务进行知识搜索
        List<Map<String, String>> results = ragService.searchKnowledge(keyword);

        return Result.success(results);
    }

    /**
     * 获取知识类别列表接口
     * <p>
     * 获取知识库中所有可用的知识分类。
     * 可用于前端展示分类筛选或知识导航。
     *
     * @api GET /api/rag/categories
     * @return Result 包含List&lt;String&gt;
     *         - 知识类别名称列表
     * @see RagService#getCategories()
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        // 获取知识库中的所有类别
        List<String> categories = ragService.getCategories();
        return Result.success(categories);
    }

    /**
     * RAG服务健康检查接口
     * <p>
     * 检查RAG服务组件是否正常运行，包括Ollama连接、向量存储等。
     * 可用于服务监控和运维。
     *
     * @api GET /api/rag/health
     * @return Result 包含Map
     *         - status: 服务状态（"ok"表示正常）
     *         - service: 服务名称（"RAG"）
     *         - categories: 知识库类别数量
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        // 构建健康状态信息
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ok");
        status.put("service", "RAG");
        // 获取知识库类别数量作为服务可用性指标
        status.put("categories", ragService.getCategories().size());

        return Result.success(status);
    }
}
