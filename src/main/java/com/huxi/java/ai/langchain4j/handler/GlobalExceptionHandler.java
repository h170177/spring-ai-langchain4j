package com.huxi.java.ai.langchain4j.handler;

import dev.langchain4j.exception.LangChain4jException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Flux;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MEDICAL_FALLBACK =
            "系统繁忙，请稍后重试。如您有紧急症状（胸痛、呼吸困难、大出血等），请立即拨打120或前往最近的急诊科就医。";

    @ExceptionHandler(LangChain4jException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Flux<String> handleLangChain4jException(LangChain4jException e) {
        log.error("AI 服务调用异常", e);
        return Flux.just(MEDICAL_FALLBACK);
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
        return Flux.just(MEDICAL_FALLBACK);
    }
}