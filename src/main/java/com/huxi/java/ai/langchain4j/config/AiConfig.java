package com.huxi.java.ai.langchain4j.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置类
 * 定义 langchain4j 所需的 bean
 */
@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    /**
     * 定义名为 "model" 的 ChatLanguageModel bean
     * 用于 @AiService(wiringMode = EXPLICIT, chatModel = "model")
     */
    @Bean(name = "model")
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    /**
     * 定义名为 "chatMemory" 的 MessageWindowChatMemory bean
     * 用于 @AiService(wiringMode = EXPLICIT, chatMemory = "chatMemory")
     */
    @Bean(name = "chatMemory")
    public MessageWindowChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }


}
