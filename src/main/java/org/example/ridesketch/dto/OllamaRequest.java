package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ollama API请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 提示内容
     */
    private String prompt;

    /**
     * 是否流式输出
     */
    private boolean stream = false;

    /**
     * 选项配置
     */
    private Options options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {
        /**
         * 温度参数
         */
        private Float temperature = 0.3f;

        /**
         * 最大Tokens
         */
        private Integer num_predict = 2048;
    }
}
