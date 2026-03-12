package org.example.ridesketch.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 *
 * 配置AI聊天客户端，用于与Ollama大模型交互
 *
 * @author rideSketch
 */
@Configuration
public class AIConfig {

    /**
     * 创建ChatClient（AI聊天客户端）
     *
     * Spring AI的ChatClient提供了便捷的API来与大语言模型交互
     * 由于spring-ai-ollama-starter会自动配置OllamaChatModel，
     * 这里只需要创建ChatClient即可
     *
     * @param chatModel Ollama聊天模型（由Spring自动注入）
     * @return 配置好的ChatClient实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
