package com.huxi.java.ai.langchain4j;


import com.huxi.java.ai.langchain4j.Main;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SpringBootTest
public class aitext {

    @Autowired

    private OpenAiChatModel openAiChatModel;
    @Test
    public void testSpringBoot() {
        String answer = openAiChatModel.chat("你是谁");
        System.out.println(answer);
    }

    @Test
    public void testChinese() {
        System.out.println("测试中文输出");
        String answer = openAiChatModel.chat("你好");
        System.out.println("原始字节: " + Arrays.toString(answer.getBytes(StandardCharsets.UTF_8)));
        System.out.println("GBK 解码: " + new String(answer.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
    }

}
