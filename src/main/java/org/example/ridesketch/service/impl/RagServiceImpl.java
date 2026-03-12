package org.example.ridesketch.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.service.*;
import org.example.ridesketch.service.KnowledgeBaseLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG知识库服务实现类 - 优化版
 * 支持多种分块策略和结果重排
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final ChatClient chatClient;
    private final KnowledgeBaseLoader knowledgeBaseLoader;
    private final EmbeddingService embeddingService;
    private final TextChunker textChunker;
    private final ResultReranker resultReranker;

    /**
     * 分块策略配置
     */
    private static final Map<String, Object> FIXED_CHUNK_OPTIONS;
    private static final Map<String, Object> SLIDING_CHUNK_OPTIONS;
    private static final Map<String, Object> SEMANTIC_CHUNK_OPTIONS;

    static {
        FIXED_CHUNK_OPTIONS = new HashMap<>();
        FIXED_CHUNK_OPTIONS.put("chunkSize", 500);
        FIXED_CHUNK_OPTIONS.put("overlap", 50);

        SLIDING_CHUNK_OPTIONS = new HashMap<>();
        SLIDING_CHUNK_OPTIONS.put("windowSize", 300);
        SLIDING_CHUNK_OPTIONS.put("step", 150);

        SEMANTIC_CHUNK_OPTIONS = new HashMap<>();
        SEMANTIC_CHUNK_OPTIONS.put("minChunkSize", 200);
    }

    @Override
    public String answerQuestion(String question) {
        try {
            if (StringUtils.isBlank(question)) {
                return "请输入问题";
            }

            log.info("RAG问答: {}", question);

            // 步骤1: 使用优化后的向量检索（带重排）
            String context = searchRelevantKnowledge(question);

            // 步骤2: 使用AI生成回答
            String answer = generateAnswer(question, context);

            return answer;

        } catch (Exception e) {
            log.error("RAG问答失败: {}", e.getMessage(), e);
            return "抱歉，处理问题时发生错误: " + e.getMessage();
        }
    }

    @Override
    public List<Map<String, String>> searchKnowledge(String keyword) {
        List<Map<String, String>> results = new ArrayList<>();

        if (StringUtils.isBlank(keyword)) {
            return results;
        }

        try {
            // 向量检索
            float[] queryVector = embeddingService.embed(keyword);
            List<KnowledgeBaseLoader.KnowledgeEntry> entries =
                    knowledgeBaseLoader.searchByVector(queryVector, 10);

            // 应用重排
            List<KnowledgeBaseLoader.KnowledgeEntry> reranked =
                    resultReranker.mmrRerank(entries, keyword, 5);

            for (KnowledgeBaseLoader.KnowledgeEntry entry : reranked) {
                Map<String, String> result = new HashMap<>();
                result.put("category", entry.getCategory());
                result.put("content", entry.getContent());
                result.put("similarity", String.format("%.4f", entry.getSimilarity()));
                results.add(result);
            }
        } catch (Exception e) {
            log.warn("检索失败: {}", e.getMessage());
        }

        return results;
    }

    @Override
    public List<String> getCategories() {
        return knowledgeBaseLoader.getCategories();
    }

    /**
     * 使用优化后的检索 + 重排
     */
    private String searchRelevantKnowledge(String question) {
        try {
            // 生成问题的向量
            float[] queryVector = embeddingService.embed(question);

            // 初步检索（更多结果，用于重排）
            List<KnowledgeBaseLoader.KnowledgeEntry> initialResults =
                    knowledgeBaseLoader.searchByVector(queryVector, 15);

            if (initialResults.isEmpty()) {
                return "";
            }

            // 应用 MMR 重排（平衡相关性和多样性）
            List<KnowledgeBaseLoader.KnowledgeEntry> rerankedResults =
                    resultReranker.mmrRerank(initialResults, question, 8);

            // 构建上下文
            StringBuilder context = new StringBuilder();

            // 按类别分组显示
            Map<String, List<String>> categoryContent = new LinkedHashMap<>();
            for (KnowledgeBaseLoader.KnowledgeEntry entry : rerankedResults) {
                categoryContent.computeIfAbsent(entry.getCategory(), k -> new ArrayList<>())
                        .add(entry.getContent());
            }

            for (Map.Entry<String, List<String>> entry : categoryContent.entrySet()) {
                context.append("【").append(entry.getKey()).append("】\n");
                for (String content : entry.getValue()) {
                    context.append("- ").append(content).append("\n");
                }
                context.append("\n");
            }

            log.debug("检索结果: 初始={}, 重排后={}", initialResults.size(), rerankedResults.size());
            return context.toString();

        } catch (Exception e) {
            log.warn("优化检索失败，使用备用搜索: {}", e.getMessage());
            return fallbackSearch(question);
        }
    }

    /**
     * 备用搜索方法
     */
    private String fallbackSearch(String question) {
        String lowerQuestion = question.toLowerCase();

        // 根据关键词确定相关类别
        Map<String, String> keywordToCategory = new HashMap<>();
        keywordToCategory.put("安全", "骑行安全");
        keywordToCategory.put("头盔", "骑行安全");
        keywordToCategory.put("刹车", "骑行安全");
        keywordToCategory.put("技巧", "骑行技巧");
        keywordToCategory.put("路线", "北京骑行路线");
        keywordToCategory.put("装备", "骑行装备");
        keywordToCategory.put("训练", "训练计划");
        keywordToCategory.put("恢复", "健康与恢复");
        keywordToCategory.put("天气", "天气与应对");
        keywordToCategory.put("维护", "自行车维护");
        keywordToCategory.put("长途", "长途骑行");

        Set<String> relevantCategories = new HashSet<>();
        for (Map.Entry<String, String> entry : keywordToCategory.entrySet()) {
            if (lowerQuestion.contains(entry.getKey())) {
                relevantCategories.add(entry.getValue());
            }
        }

        if (relevantCategories.isEmpty()) {
            relevantCategories.addAll(getCategories());
        }

        StringBuilder context = new StringBuilder();
        List<String> categories = getCategories();

        for (String category : categories) {
            if (relevantCategories.contains(category)) {
                List<KnowledgeBaseLoader.KnowledgeEntry> entries =
                        knowledgeBaseLoader.getByCategory(category);
                context.append("【").append(category).append("】\n");
                for (KnowledgeBaseLoader.KnowledgeEntry entry : entries) {
                    context.append("- ").append(entry.getContent()).append("\n");
                }
                context.append("\n");
            }
        }

        return context.toString();
    }

    /**
     * 使用AI生成回答
     */
    private String generateAnswer(String question, String context) {
        try {
            if (StringUtils.isBlank(context)) {
                context = "没有找到相关的知识库内容。";
            }

            String systemPrompt = """
                    你是一个专业的骑行助手。请根据提供的知识库内容回答用户的问题。

                    ## 知识库内容：
                    {context}

                    ## 回答要求：
                    - 优先使用上述知识库中的信息
                    - 如果知识库中没有相关信息，请说明"未找到相关内容"
                    - 回答要简洁、准确
                    - 如果需要，可以结合多条知识给出综合回答
                    - 适当使用emoji让回答更生动
                    - 标明信息来源类别
                    """.replace("{context}", context);

            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            if (StringUtils.isBlank(answer) || answer.matches("^[\\s\\p{Punct}]*$")) {
                return generateSimpleAnswer(question, context);
            }

            return answer;

        } catch (Exception e) {
            log.error("AI生成回答失败: {}", e.getMessage());
            return generateSimpleAnswer(question, context);
        }
    }

    /**
     * 生成简单回答
     */
    private String generateSimpleAnswer(String question, String context) {
        if (StringUtils.isBlank(context) || context.equals("没有找到相关的知识库内容。")) {
            return "未找到相关内容。您可以尝试询问关于骑行安全、路线、装备、训练计划、健康恢复或天气应对等方面的问题。";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("根据知识库，我找到以下相关信息：\n\n");
        answer.append(context);

        return answer.toString();
    }

    /**
     * 测试分块功能
     */
    public String testChunking(String text, String strategy) {
        try {
            TextChunker.ChunkStrategy chunkStrategy = TextChunker.ChunkStrategy.valueOf(strategy.toUpperCase());
            List<TextChunker.Chunk> chunks = textChunker.smartChunk(text, chunkStrategy,
                getChunkOptions(strategy));

            StringBuilder result = new StringBuilder();
            result.append("分块策略: ").append(strategy).append("\n");
            result.append("分块数量: ").append(chunks.size()).append("\n\n");

            for (TextChunker.Chunk chunk : chunks) {
                result.append("--- 块 ").append(chunk.getChunkIndex() + 1)
                    .append(" (位置: ").append(chunk.getStartIndex())
                    .append("-").append(chunk.getEndIndex()).append(") ---\n");
                result.append(chunk.getContent()).append("\n\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "分块测试失败: " + e.getMessage();
        }
    }

    /**
     * 测试重排功能
     */
    public String testReranking(String query, int topK) {
        try {
            float[] queryVector = embeddingService.embed(query);
            List<KnowledgeBaseLoader.KnowledgeEntry> initial =
                knowledgeBaseLoader.searchByVector(queryVector, 15);

            if (initial.isEmpty()) {
                return "没有检索到结果";
            }

            // MMR 重排
            List<KnowledgeBaseLoader.KnowledgeEntry> reranked =
                resultReranker.mmrRerank(initial, query, topK);

            // 综合重排
            ResultReranker.RerankConfig config = new ResultReranker.RerankConfig();
            config.setTopK(topK);
            List<KnowledgeBaseLoader.KnowledgeEntry> comprehensive =
                resultReranker.comprehensiveRerank(initial, query, config);

            StringBuilder result = new StringBuilder();
            result.append("查询: ").append(query).append("\n");
            result.append("初始结果: ").append(initial.size()).append(" 条\n");
            result.append("MMR重排后: ").append(reranked.size()).append(" 条\n\n");

            result.append("--- MMR 重排结果 ---\n");
            for (int i = 0; i < reranked.size(); i++) {
                KnowledgeBaseLoader.KnowledgeEntry e = reranked.get(i);
                result.append(i + 1).append(". [")
                    .append(e.getCategory()).append("] ")
                    .append(e.getContent(), 0, Math.min(50, e.getContent().length()))
                    .append("... (相似度: ").append(String.format("%.4f", e.getSimilarity())).append(")\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "重排测试失败: " + e.getMessage();
        }
    }

    private Map<String, Object> getChunkOptions(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "fixed_size" -> FIXED_CHUNK_OPTIONS;
            case "sliding_window" -> SLIDING_CHUNK_OPTIONS;
            case "semantic" -> SEMANTIC_CHUNK_OPTIONS;
            default -> FIXED_CHUNK_OPTIONS;
        };
    }
}
