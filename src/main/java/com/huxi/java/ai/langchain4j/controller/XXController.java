package com.huxi.java.ai.langchain4j.controller;

import com.huxi.java.ai.langchain4j.assistant.XXAgent;
import com.huxi.java.ai.langchain4j.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@Tag(name = "XX",description = "XX相关接口")
@RestController
@RequestMapping("/xx")
public class XXController {

    @Autowired
    private XXAgent xxAgent;


    @Operation(summary = "对话")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@Valid @RequestBody ChatForm chatForm)  {
        return xxAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
    }
}
