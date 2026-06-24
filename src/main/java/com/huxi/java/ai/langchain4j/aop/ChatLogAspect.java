// 新文件: src/main/java/com/huxi/java/ai/langchain4j/aop/ChatLogAspect.java
package com.huxi.java.ai.langchain4j.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class ChatLogAspect {

    @Around("execution(* com.huxi.java.ai.langchain4j.assistant.XXAgent.chat(..))")
    public Object logChat(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        log.info("AI 对话开始, memoryId={}, message={}", args[0], args[1]);
        StopWatch sw = new StopWatch();
        sw.start();
        Object result = joinPoint.proceed();
        sw.stop();
        log.info("AI 对话完成, 耗时={}ms", sw.getTotalTimeMillis());
        return result;
    }
}