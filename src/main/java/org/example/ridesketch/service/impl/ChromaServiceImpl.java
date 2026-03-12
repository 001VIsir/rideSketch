package org.example.ridesketch.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.service.ChromaService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Chroma向量库服务实现类
 * 通过HTTP调用Python Chroma服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChromaServiceImpl implements ChromaService {

    private final ChatClient chatClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${chroma.service.url:http://localhost:5000}")
    private String chromaServiceUrl;

    @Override
    public List<Map<String, Object>> semanticSearch(String query, int nResults) {
        try {
            log.info("Chroma语义搜索: {}, nResults={}", query, nResults);

            String url = chromaServiceUrl + "/query";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("n_results", nResults);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);

                if (Boolean.TRUE.equals(result.get("success"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
                    return results != null ? results : new ArrayList<>();
                }
            }

            log.warn("Chroma搜索返回空结果");
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Chroma语义搜索失败: {}", e.getMessage());
            throw new RuntimeException("Chroma服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> addDocuments(List<String> documents, List<String> ids, List<Map<String, String>> metadata) {
        try {
            log.info("添加文档到Chroma: {} 条", documents.size());

            String url = chromaServiceUrl + "/add";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("documents", documents);
            body.put("ids", ids);
            body.put("metadatas", metadata);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }

            return Map.of("success", false, "message", "添加失败");

        } catch (Exception e) {
            log.error("添加文档失败: {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getStats() {
        try {
            String url = chromaServiceUrl + "/count";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);

                Map<String, Object> stats = new HashMap<>();
                stats.put("count", result.get("count"));
                stats.put("service", "Chroma");
                stats.put("url", chromaServiceUrl);

                return stats;
            }

            return Map.of("count", 0);

        } catch (Exception e) {
            log.error("获取统计信息失败: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    @Override
    public boolean healthCheck() {
        try {
            String url = chromaServiceUrl + "/health";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            log.warn("Chroma服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String answerQuestion(String question) {
        try {
            if (StringUtils.isBlank(question)) {
                return "请输入问题";
            }

            log.info("Chroma RAG问答: {}", question);

            // 步骤1: 语义搜索获取相关知识
            List<Map<String, Object>> searchResults = semanticSearch(question, 3);

            if (searchResults.isEmpty()) {
                return "未找到相关内容";
            }

            // 步骤2: 构建上下文
            StringBuilder context = new StringBuilder();
            for (Map<String, Object> result : searchResults) {
                String content = (String) result.get("content");
                if (StringUtils.isNotBlank(content)) {
                    context.append("- ").append(content).append("\n\n");
                }
            }

            // 步骤3: 使用AI生成回答
            String answer = generateAnswer(question, context.toString());

            return answer;

        } catch (Exception e) {
            log.error("Chroma RAG问答失败: {}", e.getMessage(), e);
            return "抱歉，处理问题时发生错误: " + e.getMessage();
        }
    }

    /**
     * 使用AI生成回答
     */
    private String generateAnswer(String question, String context) {
        try {
            String systemPrompt = """
                    你是一个专业的骑行助手。请根据提供的知识库内容回答用户的问题。

                    ## 知识库内容：
                    {context}

                    ## 回答要求：
                    - 优先使用上述知识库中的信息
                    - 如果知识库中没有相关信息，请说明"未找到相关内容"
                    - 回答要简洁、准确
                    - 可以结合多条知识给出综合回答
                    """.replace("{context}", context);

            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            return StringUtils.isNotBlank(answer) ? answer : "未找到相关内容";

        } catch (Exception e) {
            log.error("AI生成回答失败: {}", e.getMessage());
            // 回退到直接返回搜索结果
            return "根据搜索结果：" + context;
        }
    }
}
