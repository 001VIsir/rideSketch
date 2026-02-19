package org.example.ridesketch.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG服务 - 检索增强生成（简化版）
 * 使用Prompt Engineering增强AI回答
 */
@Slf4j
@Service
public class RagService {

    private final ChatModel chatModel;
    private ChatClient chatClient;

    // 知识库 - 键值对存储
    private final Map<String, String> knowledgeBase = new HashMap<>();

    // 系统提示词
    private static final String SYSTEM_PROMPT = """
            你是一个专业的骑行路线规划助手。
            请根据以下知识库信息回答用户的问题。
            如果知识库中没有相关信息，请基于你的知识回答，但要说明这是通用建议。

            ## 知识库：
            {context}

            ## 回答要求：
            1. 优先使用知识库中的信息
            2. 回答要简洁明了
            3. 如果不确定，说明"根据一般建议"
            """;

    public RagService(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
        initKnowledgeBase();
    }

    /**
     * 初始化知识库
     */
    private void initKnowledgeBase() {
        // 骑行技巧
        knowledgeBase.put("骑行技巧", """
                骑行是一项受欢迎的有氧运动，基本技巧包括：
                1. 保持正确的骑行姿势，身体略微前倾
                2. 踏频保持在80-100 RPM之间
                3. 爬坡时使用轻齿轮，sit/stand交替
                4. 下坡时控制速度，优先使用后刹车
                5. 长时间骑行记得补充水分和电解质
                """);

        // 骑行安全
        knowledgeBase.put("骑行安全", """
                骑行安全注意事项：
                1. 始终佩戴头盔，选择合适尺寸
                2. 夜间骑行必须安装前后灯
                3. 遵守交通规则，使用手势信号
                4. 定期检查刹车、轮胎气压
                5. 避免戴耳机骑车，保持注意力
                6. 保持车距，特别下坡时
                """);

        // 北京骑行路线
        knowledgeBase.put("北京骑行", """
                北京经典骑行路线推荐：
                1. 天安门-故宫-景山：适合初学者，路程短
                2. 长安街：沿着长安街骑行，经过天安门广场
                3. 奥林匹克公园：绕鸟巢、水立方骑行
                4. 妙峰山：经典爬坡路线，风景优美
                5. 什刹海-胡同：适合休闲骑行，观赏老北京
                """);

        // 骑行装备
        knowledgeBase.put("骑行装备", """
                骑行必备装备清单：
                1. 头盔：保护头部，必不可少
                2. 手套：防滑、减震
                3. 骑行裤：舒适坐垫，减少摩擦
                4. 车灯：夜间骑行必备
                5. 水壶：保持水分补充
                6. 维修工具：备胎、充气筒、多功能工具
                """);

        // 训练计划
        knowledgeBase.put("训练计划", """
                初学者训练计划：
                第一周：每次骑行30分钟，距离10-15公里
                第二周：每次骑行45分钟，距离15-20公里
                第三周：每次骑行60分钟，距离25-30公里
                第四周：尝试更长距离，逐步增加强度

                训练要点：
                - 先热身5-10分钟
                - 保持均匀踏频
                - 结束后放松拉伸
                - 循序渐进，避免过度训练
                """);

        log.info("知识库初始化完成，共{}条知识", knowledgeBase.size());
    }

    /**
     * 检索相关知识
     */
    private String retrieveKnowledge(String question) {
        StringBuilder context = new StringBuilder();

        // 简单关键词匹配
        for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 检查问题是否包含相关关键词
            if (question.contains(key) || key.contains(question)) {
                context.append("【").append(key).append("】\n").append(value).append("\n\n");
            } else {
                // 模糊匹配
                String[] keywords = key.split("骑行");
                for (String kw : keywords) {
                    if (kw.length() > 2 && question.contains(kw)) {
                        context.append("【").append(key).append("】\n").append(value).append("\n\n");
                        break;
                    }
                }
            }
        }

        // 如果没有匹配，返回所有知识的摘要
        if (context.length() == 0) {
            context.append("通用骑行知识：\n");
            for (String key : knowledgeBase.keySet()) {
                context.append("- ").append(key).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * 问答 - 使用RAG
     */
    public String questionAnswer(String question) {
        try {
            // 1. 检索相关知识
            String context = retrieveKnowledge(question);

            // 2. 构建增强提示词
            String systemPrompt = SYSTEM_PROMPT.replace("{context}", context);

            // 3. 调用AI生成回答
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            return response;

        } catch (Exception e) {
            log.error("RAG问答失败: {}", e.getMessage());
            return "抱歉，处理您的问题时出现错误。\n\n" +
                    "建议：您可以尝试询问以下内容：\n" +
                    "- 骑行技巧\n" +
                    "- 骑行安全\n" +
                    "- 北京骑行路线\n" +
                    "- 骑行装备\n" +
                    "- 训练计划";
        }
    }

    /**
     * 添加自定义知识
     */
    public void addKnowledge(String title, String content) {
        knowledgeBase.put(title, content);
        log.info("已添加新知识: {}", title);
    }

    /**
     * 获取所有知识标题
     */
    public List<String> getAllKnowledgeTitles() {
        return new ArrayList<>(knowledgeBase.keySet());
    }
}
