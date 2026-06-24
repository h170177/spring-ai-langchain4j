package com.huxi.java.ai.langchain4j;


import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest
public class AIServiceTest {

    @Autowired
    private OpenAiChatModel model;

    @Autowired
    private SeparateChatAssistant separateChatAssistant;
    @Test
    public void testChat() {

        Assistant assistant = AiServices.create(Assistant.class, model);
        String userMessage = "鲨鱼有多少根骨头";
        String response = assistant.chat(userMessage);
        log.info("Response: {}", response);
    }

    @Test
    public void testMemory() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatMemory(memory)
                .chatLanguageModel(model)
                .build();

        String userMessage = "一只鲨鱼大约多少斤";
        String response = assistant.chat(userMessage);
        log.info("Response: {}", response);
        String userMessage2 = "能吃吗";
        response = assistant.chat(userMessage2);
        log.info("Response: {}", response);
    }


    @Test
    public void testChatMemory5() {
        String answer1 = separateChatAssistant.chat(1, "我是环环");
        System.out.println(answer1);
        String answer2 = separateChatAssistant.chat(1, "我是谁");
        System.out.println(answer2);
        String answer3 = separateChatAssistant.chat(2, "我是谁");
        System.out.println(answer3);
    }
}
