package org.example.ridesketch.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Chroma向量库服务
 * 通过HTTP API调用Python版Chroma服务
 */
@Slf4j
@Service
public class ChromaService {

    private final RestTemplate restTemplate;
    private final ChatModel chatModel;
    private final ChatClient.Builder chatClientBuilder;

    @Value("${chroma.service.url:http://localhost:5000}")
    private String chromaServiceUrl;

    public ChromaService(ChatModel chatModel, ChatClient.Builder chatClientBuilder) {
        this.restTemplate = new RestTemplate();
        this.chatModel = chatModel;
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 查询相似文档
     */
    public List<String> similaritySearch(String query, int topK) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("n_results", topK);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    chromaServiceUrl + "/query",
                    request,
                    Map.class
            );

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                if (results != null) {
                    return results.stream()
                            .map(r -> (String) r.get("content"))
                            .toList();
                }
            }
        } catch (Exception e) {
            log.warn("Chroma服务查询失败: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * 添加文档到知识库
     */
    public boolean addDocument(String id, String content, String category) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("documents", List.of(content));
            requestBody.put("ids", List.of(id));
            requestBody.put("metadatas", List.of(Map.of("category", category)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    chromaServiceUrl + "/add",
                    request,
                    Map.class
            );

            return response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"));
        } catch (Exception e) {
            log.error("添加文档失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取知识库文档数量
     */
    public int getDocumentCount() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    chromaServiceUrl + "/count",
                    Map.class
            );
            if (response.getBody() != null) {
                Object count = response.getBody().get("count");
                if (count instanceof Number) {
                    return ((Number) count).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("获取文档数量失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    chromaServiceUrl + "/health",
                    Map.class
            );
            return response.getBody() != null && "ok".equals(response.getBody().get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 带RAG的问答
     */
    public String questionAnswer(String question) {
        try {
            // 1. 从Chroma检索相关知识
            List<String> contextDocs = similaritySearch(question, 3);

            if (contextDocs.isEmpty()) {
                return "知识库未初始化，请稍后再试";
            }

            // 2. 构建增强提示词
            String context = String.join("\n\n", contextDocs);

            String systemPrompt = """
                    你是一个专业的骑行路线规划助手。
                    请根据以下知识库信息回答用户的问题。
                    如果知识库中没有相关信息，请基于你的知识回答，但要说明这是通用建议。

                    ## 知识库：
                    %s

                    ## 回答要求：
                    1. 优先使用知识库中的信息
                    2. 回答要简洁明了
                    3. 如果不确定，说明"根据一般建议"
                    """.formatted(context);

            // 3. 调用AI生成回答
            var chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            return response;

        } catch (Exception e) {
            log.error("RAG问答失败: {}", e.getMessage());
            return "抱歉，处理您的问题时出现错误：" + e.getMessage();
        }
    }
}
