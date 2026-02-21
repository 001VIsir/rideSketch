package org.example.ridesketch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 向量嵌入服务
 * 使用Ollama API直接生成文本向量
 */
@Slf4j
@Service
public class EmbeddingService {

    private final String ollamaBaseUrl;
    private final String embeddingModel;
    private final RestTemplate restTemplate;

    public EmbeddingService(
            @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl,
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}") String embeddingModel) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.embeddingModel = embeddingModel;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 生成单条文本的向量
     *
     * @param text 文本内容
     * @return 向量数组
     */
    public float[] embed(String text) {
        try {
            String url = ollamaBaseUrl + "/api/embeddings";

            Map<String, Object> request = new HashMap<>();
            request.put("model", embeddingModel);
            request.put("prompt", text);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("embedding")) {
                @SuppressWarnings("unchecked")
                List<Number> embeddingList = (List<Number>) response.get("embedding");
                float[] result = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    result[i] = embeddingList.get(i).floatValue();
                }
                return result;
            }

            throw new RuntimeException("Embedding结果为空");

        } catch (Exception e) {
            log.error("生成向量失败: {}", e.getMessage());
            throw new RuntimeException("生成向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量生成向量（使用embedding端点）
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<float[]> embed(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        return embeddings;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度得分
     */
    public float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        float dotProduct = 0;
        float norm1 = 0;
        float norm2 = 0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        float denominator = (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
        if (denominator == 0) {
            return 0;
        }

        return dotProduct / denominator;
    }
}
