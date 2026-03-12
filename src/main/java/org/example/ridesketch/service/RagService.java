package org.example.ridesketch.service;

import java.util.List;
import java.util.Map;

/**
 * RAG知识库服务接口
 * 提供基于关键词匹配的知识检索功能
 */
public interface RagService {

    /**
     * 问答接口
     *
     * @param question 用户问题
     * @return 回答内容
     */
    String answerQuestion(String question);

    /**
     * 搜索相关知识
     *
     * @param keyword 关键词
     * @return 相关的知识条目
     */
    List<Map<String, String>> searchKnowledge(String keyword);

    /**
     * 获取所有知识类别
     *
     * @return 知识类别列表
     */
    List<String> getCategories();
}
