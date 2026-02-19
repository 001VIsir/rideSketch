package org.example.ridesketch.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * 使用Spring AI的自动配置
 */
@Configuration
public class AIConfig {

    /**
     * 创建ChatClient
     * 由于spring-ai-ollama-starter会自动配置OllamaChatModel，
     * 这里只需要创建ChatClient
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
