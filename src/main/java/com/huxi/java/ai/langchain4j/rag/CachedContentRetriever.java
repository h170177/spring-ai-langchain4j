package com.huxi.java.ai.langchain4j.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CachedContentRetriever implements ContentRetriever {

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
        return cache.get(query.text(), k -> delegate.retrieve(query));
    }
}