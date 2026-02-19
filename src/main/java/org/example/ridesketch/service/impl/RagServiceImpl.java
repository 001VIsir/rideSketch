package org.example.ridesketch.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.ridesketch.service.RagService;
import org.example.ridesketch.service.AIRouteService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG知识库服务实现类
 * 使用Map-based关键词匹配进行知识检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final ChatClient chatClient;

    /**
     * 骑行知识库 - 按类别组织
     * 包含5类骑行相关知识
     */
    private static final Map<String, List<String>> KNOWLEDGE_BASE = new HashMap<>();

    static {
        // 骑行技巧
        KNOWLEDGE_BASE.put("骑行技巧", Arrays.asList(
                "保持正确的骑行姿势，身体略微前倾约15-30度",
                "踏频保持在80-100 RPM之间最省力",
                "爬坡时使用轻齿轮，sit/stand交替发力",
                "下坡时控制速度，优先使用后刹车",
                "长时间骑行记得补充水分和电解质，每15-20分钟补充一次"
        ));

        // 骑行安全
        KNOWLEDGE_BASE.put("骑行安全", Arrays.asList(
                "始终佩戴头盔，选择合适尺寸并系紧",
                "夜间骑行必须安装前后灯，前灯白色，后灯红色",
                "遵守交通规则，使用手势信号指示转弯和停车",
                "定期检查刹车系统、轮胎气压和链条状况",
                "避免戴耳机骑车，保持注意力集中",
                "与前车保持安全距离，特别是下坡时",
                "恶劣天气尽量减少外出"
        ));

        // 北京骑行路线
        KNOWLEDGE_BASE.put("北京骑行路线", Arrays.asList(
                "天安门-故宫-景山：适合初学者，路程短，约5公里",
                "长安街：沿着长安街骑行，经过天安门广场",
                "奥林匹克公园：绕鸟巢、水立方骑行一圈约8公里",
                "妙峰山：经典爬坡路线，全长约20公里",
                "什刹海-胡同：适合休闲骑行，穿梭老北京胡同",
                "温榆河绿道：新建成的骑行道，全长约30公里"
        ));

        // 骑行装备
        KNOWLEDGE_BASE.put("骑行装备", Arrays.asList(
                "头盔：保护头部安全，必不可少",
                "手套：防滑、减震，保护手部",
                "骑行裤：带坐垫的骑行裤可减少摩擦",
                "车灯：夜间骑行必备",
                "水壶：保持水分补充",
                "维修工具：备胎、充气筒、多功能工具",
                "码表：记录骑行数据"
        ));

        // 训练计划
        KNOWLEDGE_BASE.put("训练计划", Arrays.asList(
                "初学者第一周：每次骑行30分钟，距离10-15公里",
                "初学者第二周：每次骑行45分钟，距离15-20公里",
                "每次骑行前热身5-10分钟",
                "骑行结束后放松拉伸10-15分钟",
                "循序渐进，避免过度训练导致受伤",
                "每周至少休息1-2天"
        ));
    }

    @Override
    public String answerQuestion(String question) {
        try {
            if (StringUtils.isBlank(question)) {
                return "请输入问题";
            }

            log.info("RAG问答: {}", question);

            // 步骤1: 关键词匹配检索相关知识
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

        String lowerKeyword = keyword.toLowerCase();

        for (Map.Entry<String, List<String>> entry : KNOWLEDGE_BASE.entrySet()) {
            String category = entry.getKey();
            List<String> items = entry.getValue();

            for (String item : items) {
                if (item.toLowerCase().contains(lowerKeyword)) {
                    Map<String, String> result = new HashMap<>();
                    result.put("category", category);
                    result.put("content", item);
                    results.add(result);
                }
            }
        }

        return results;
    }

    @Override
    public List<String> getCategories() {
        return new ArrayList<>(KNOWLEDGE_BASE.keySet());
    }

    /**
     * 检索相关知识
     */
    private String searchRelevantKnowledge(String question) {
        StringBuilder context = new StringBuilder();
        String lowerQuestion = question.toLowerCase();

        // 定义关键词到类别的映射
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
        keywordToCategory.put("头盔", "骑行装备");
        keywordToCategory.put("手套", "骑行装备");
        keywordToCategory.put("车灯", "骑行装备");
        keywordToCategory.put("训练", "训练计划");
        keywordToCategory.put("初学者", "训练计划");
        keywordToCategory.put("计划", "训练计划");

        // 根据关键词确定相关类别
        Set<String> relevantCategories = new HashSet<>();
        for (Map.Entry<String, String> entry : keywordToCategory.entrySet()) {
            if (lowerQuestion.contains(entry.getKey())) {
                relevantCategories.add(entry.getValue());
            }
        }

        // 如果没有匹配到任何类别，返回所有知识
        if (relevantCategories.isEmpty()) {
            relevantCategories.addAll(KNOWLEDGE_BASE.keySet());
        }

        // 构建上下文
        for (String category : relevantCategories) {
            List<String> items = KNOWLEDGE_BASE.get(category);
            if (items != null) {
                context.append("【").append(category).append("】\n");
                for (String item : items) {
                    context.append("- ").append(item).append("\n");
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
            String systemPrompt = """
                    你是一个专业的骑行助手。请根据提供的知识库内容回答用户的问题。

                    ## 知识库内容：
                    {context}

                    ## 回答要求：
                    - 优先使用上述知识库中的信息
                    - 如果知识库中没有相关信息，请说明"未找到相关内容"
                    - 回答要简洁、准确
                    - 如果需要，可以结合多条知识给出综合回答
                    """.replace("{context}", context);

            String answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            // 如果AI返回空或只有标点符号，回退到基于关键词的简单回答
            if (StringUtils.isBlank(answer) || answer.matches("^[\\s\\p{Punct}]*$")) {
                return generateSimpleAnswer(question);
            }

            return answer;

        } catch (Exception e) {
            log.error("AI生成回答失败: {}", e.getMessage());
            // 回退到简单回答
            return generateSimpleAnswer(question);
        }
    }

    /**
     * 生成简单回答（基于关键词匹配，不调用AI）
     */
    private String generateSimpleAnswer(String question) {
        String lowerQuestion = question.toLowerCase();
        StringBuilder answer = new StringBuilder();

        // 安全相关
        if (lowerQuestion.contains("安全") || lowerQuestion.contains("注意")) {
            answer.append("【骑行安全注意事项】\n");
            for (String item : KNOWLEDGE_BASE.get("骑行安全")) {
                answer.append("• ").append(item).append("\n");
            }
            answer.append("\n");
        }

        // 路线相关
        if (lowerQuestion.contains("路线") || lowerQuestion.contains("北京")) {
            answer.append("【北京经典骑行路线】\n");
            for (String item : KNOWLEDGE_BASE.get("北京骑行路线")) {
                answer.append("• ").append(item).append("\n");
            }
            answer.append("\n");
        }

        // 装备相关
        if (lowerQuestion.contains("装备") || lowerQuestion.contains("头盔") || lowerQuestion.contains("手套")) {
            answer.append("【骑行装备推荐】\n");
            for (String item : KNOWLEDGE_BASE.get("骑行装备")) {
                answer.append("• ").append(item).append("\n");
            }
            answer.append("\n");
        }

        // 技巧相关
        if (lowerQuestion.contains("技巧") || lowerQuestion.contains("姿势")) {
            answer.append("【骑行技巧】\n");
            for (String item : KNOWLEDGE_BASE.get("骑行技巧")) {
                answer.append("• ").append(item).append("\n");
            }
            answer.append("\n");
        }

        // 训练相关
        if (lowerQuestion.contains("训练") || lowerQuestion.contains("初学者")) {
            answer.append("【初学者训练计划】\n");
            for (String item : KNOWLEDGE_BASE.get("训练计划")) {
                answer.append("• ").append(item).append("\n");
            }
            answer.append("\n");
        }

        if (answer.length() == 0) {
            return "未找到相关内容。您可以尝试询问关于骑行安全、路线、装备或训练计划方面的问题。";
        }

        return answer.toString();
    }
}
