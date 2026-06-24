// 新文件: src/main/java/com/huxi/java/ai/langchain4j/handler/GlobalExceptionHandler.java
package com.huxi.java.ai.langchain4j.handler;

import dev.langchain4j.exception.LangChain4jException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LangChain4jException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Flux<String> handleLangChain4jException(LangChain4jException e) {
        log.error("AI 服务调用异常", e);
        return Flux.just("抱歉，AI 服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Flux<String> handleIllegalArgument(IllegalArgumentException e) {
        return Flux.just("请求参数有误：" + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Flux<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Flux.just("系统繁忙，请稍后重试");
    }
}