package org.example.ridesketch.service;

import java.util.List;
import java.util.Map;

/**
 * Chroma向量库服务接口
 * 提供语义搜索功能
 */
public interface ChromaService {

    /**
     * 语义搜索
     *
     * @param query     查询文本
     * @param nResults  返回结果数量
     * @return 搜索结果列表
     */
    List<Map<String, Object>> semanticSearch(String query, int nResults);

    /**
     * 添加文档到知识库
     *
     * @param documents 文档列表
     * @param ids      文档ID列表
     * @param metadata 元数据
     * @return 添加结果
     */
    Map<String, Object> addDocuments(List<String> documents, List<String> ids, List<Map<String, String>> metadata);

    /**
     * 获取知识库统计信息
     *
     * @return 统计信息
     */
    Map<String, Object> getStats();

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    boolean healthCheck();

    /**
     * RAG问答 - 结合语义搜索和AI生成
     *
     * @param question 用户问题
     * @return 回答内容
     */
    String answerQuestion(String question);
}
