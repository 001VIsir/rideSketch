package org.example.ridesketch.dto;

import lombok.Data;

/**
 * Ollama API响应
 */
@Data
public class OllamaResponse {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 创建时间戳
     */
    private String created_at;

    /**
     * 响应内容
     */
    private String response;

    /**
     * 是否完成
     */
    private boolean done;

    /**
     * 上下文ID
     */
    private Long contextId;

    /**
     * 加载时长
     */
    private Long totalDuration;

    /**
     * 加载模型时长
     */
    private Long loadDuration;

    /**
     * 提示Tokens数
     */
    private Integer promptEvalCount;

    /**
     * 评估Tokens数
     */
    private Integer evalCount;

    /**
     * 评估时长
     */
    private Long evalDuration;
}
