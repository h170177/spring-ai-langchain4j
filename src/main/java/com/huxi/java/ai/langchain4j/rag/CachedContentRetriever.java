package com.huxi.java.ai.langchain4j.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CachedContentRetriever implements ContentRetriever {

    private static final Logger log = LoggerFactory.getLogger(CachedContentRetriever.class);

    private final ContentRetriever delegate;
    private final Cache<String, List<Content>> cache;

    public CachedContentRetriever(ContentRetriever delegate) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> cached = cache.getIfPresent(query.text());
        if (cached != null) {
            log.debug("缓存命中，直接返回缓存结果: {}", query.text());
            return cached;
        }

        try {
            List<Content> result = delegate.retrieve(query);
            if (result != null && !result.isEmpty()) {
                cache.put(query.text(), result);
            }
            return result;
        } catch (Exception e) {
            log.error("向量检索失败（Pinecone/Embedding异常），降级处理: {}", e.getMessage());
            return List.of();
        }
    }
}