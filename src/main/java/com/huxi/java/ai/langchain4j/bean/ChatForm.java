package com.huxi.java.ai.langchain4j.bean;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatForm {
    @NotNull(message = "会话ID不能为空")
    private Long memoryId;
    @NotBlank(message = "消息内容不能为空")
    private String message;
}