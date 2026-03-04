package org.example.ridesketch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * RestTemplate 配置类
 *
 * RestTemplate是Spring提供的HTTP客户端，用于调用第三方API（如高德地图、Ollama等）
 *
 * @author rideSketch
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建RestTemplate实例
     *
     * RestTemplate用于：
     * - 调用高德地图API（地理编码、路径规划）
     * - 调用Ollama API（生成向量、聊天）
     *
     * 配置UTF-8编码，确保正确处理中文响应
     *
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 添加UTF-8支持的StringHttpMessageConverter（放在最前面确保优先使用）
        restTemplate.getMessageConverters().add(0,
            new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
