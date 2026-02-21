package org.example.ridesketch.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.service.RagService;
import org.example.ridesketch.service.KnowledgeBaseLoader;
import org.example.ridesketch.service.EmbeddingService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG知识库服务实现类
 * 使用向量语义检索进行知识检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final ChatClient chatClient;
    private final KnowledgeBaseLoader knowledgeBaseLoader;
    private final EmbeddingService embeddingService;

    @Override
    public String answerQuestion(String question) {
        try {
            if (StringUtils.isBlank(question)) {
                return "请输入问题";
            }

            log.info("RAG问答: {}", question);

            // 步骤1: 使用向量检索相关知识
            String context = searchRelevantKnowledge(question);
            log.debug("检索到的知识: {}", context);

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

        // 优先尝试向量检索
        try {
            float[] queryVector = embeddingService.embed(keyword);
            List<KnowledgeBaseLoader.KnowledgeEntry> entries =
                    knowledgeBaseLoader.searchByVector(queryVector, 10);

            for (KnowledgeBaseLoader.KnowledgeEntry entry : entries) {
                Map<String, String> result = new HashMap<>();
                result.put("category", entry.getCategory());
                result.put("content", entry.getContent());
                result.put("similarity", String.valueOf(entry.getSimilarity()));
                results.add(result);
            }
        } catch (Exception e) {
            // 回退到关键词搜索
            log.warn("向量检索失败，使用关键词搜索: {}", e.getMessage());
            List<KnowledgeBaseLoader.KnowledgeEntry> entries =
                    knowledgeBaseLoader.searchByKeyword(keyword);

            for (KnowledgeBaseLoader.KnowledgeEntry entry : entries) {
                Map<String, String> result = new HashMap<>();
                result.put("category", entry.getCategory());
                result.put("content", entry.getContent());
                results.add(result);
            }
        }

        return results;
    }

    @Override
    public List<String> getCategories() {
        return knowledgeBaseLoader.getCategories();
    }

    /**
     * 使用向量检索相关知识
     */
    private String searchRelevantKnowledge(String question) {
        try {
            // 生成问题的向量
            float[] queryVector = embeddingService.embed(question);

            // 向量检索top-K相关知识
            List<KnowledgeBaseLoader.KnowledgeEntry> entries =
                    knowledgeBaseLoader.searchByVector(queryVector, 8);

            // 构建上下文
            StringBuilder context = new StringBuilder();

            // 按类别分组显示
            Map<String, List<String>> categoryContent = new LinkedHashMap<>();
            for (KnowledgeBaseLoader.KnowledgeEntry entry : entries) {
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

            return context.toString();

        } catch (Exception e) {
            log.warn("向量检索失败，使用备用搜索: {}", e.getMessage());
            return fallbackSearch(question);
        }
    }

    /**
     * 备用搜索方法（当向量检索不可用时）
     */
    private String fallbackSearch(String question) {
        String lowerQuestion = question.toLowerCase();

        // 根据关键词确定相关类别
        Map<String, String> keywordToCategory = new HashMap<>();
        keywordToCategory.put("安全", "骑行安全");
        keywordToCategory.put("头盔", "骑行安全");
        keywordToCategory.put("刹车", "骑行安全");
        keywordToCategory.put("夜骑", "骑行安全");
        keywordToCategory.put("技巧", "骑行技巧");
        keywordToCategory.put("姿势", "骑行技巧");
        keywordToCategory.put("踏频", "骑行技巧");
        keywordToCategory.put("爬坡", "骑行技巧");
        keywordToCategory.put("路线", "北京骑行路线");
        keywordToCategory.put("景点", "北京骑行路线");
        keywordToCategory.put("北京", "北京骑行路线");
        keywordToCategory.put("装备", "骑行装备");
        keywordToCategory.put("手套", "骑行装备");
        keywordToCategory.put("车灯", "骑行装备");
        keywordToCategory.put("训练", "训练计划");
        keywordToCategory.put("初学者", "训练计划");
        keywordToCategory.put("计划", "训练计划");
        keywordToCategory.put("恢复", "健康与恢复");
        keywordToCategory.put("酸痛", "健康与恢复");
        keywordToCategory.put("天气", "天气与应对");
        keywordToCategory.put("雨天", "天气与应对");
        keywordToCategory.put("高温", "天气与应对");
        keywordToCategory.put("维护", "自行车维护");
        keywordToCategory.put("保养", "自行车维护");
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
                    """.replace("{context}", context);

            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            // 如果AI返回空或只有标点符号，回退到简单回答
            if (StringUtils.isBlank(answer) || answer.matches("^[\\s\\p{Punct}]*$")) {
                return generateSimpleAnswer(question, context);
            }

            return answer;

        } catch (Exception e) {
            log.error("AI生成回答失败: {}", e.getMessage());
            // 回退到简单回答
            return generateSimpleAnswer(question, context);
        }
    }

    /**
     * 生成简单回答（基于检索结果，不调用AI）
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
}
