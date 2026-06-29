package com.huxi.java.ai.langchain4j;

import com.huxi.java.ai.langchain4j.rag.CachedContentRetriever;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG Fallback / Degradation Tests")
class RAGFallbackTest {

    @Mock
    private ContentRetriever delegateRetriever;

    private CachedContentRetriever cachedRetriever;

    @BeforeEach
    void setUp() {
        cachedRetriever = new CachedContentRetriever(delegateRetriever);
    }

    // ==================== 模拟 Pinecone 网络超时 ====================

    @Test
    @DisplayName("Pinecone 超时：降级返回空列表，不抛异常")
    void testPineconeTimeoutReturnsEmpty() {
        when(delegateRetriever.retrieve(any(Query.class)))
                .thenThrow(new RuntimeException("Pinecone connection timeout"));

        List<Content> result = cachedRetriever.retrieve(Query.from("头痛挂什么科"));

        assertThat(result).isEmpty();
        verify(delegateRetriever).retrieve(any(Query.class));
    }

    @Test
    @DisplayName("Pinecone 超时但有缓存：应返回缓存数据")
    void testCacheHitWhenPineconeTimeout() {
        Query query = Query.from("头痛挂什么科");
        List<Content> cachedContent = List.of(
                Content.from("神经内科：诊治头痛、眩晕、癫痫等")
        );

        when(delegateRetriever.retrieve(query)).thenReturn(cachedContent);
        cachedRetriever.retrieve(query);

        reset(delegateRetriever);

        List<Content> result = cachedRetriever.retrieve(query);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).textSegment().text()).contains("神经内科");
        verify(delegateRetriever, never()).retrieve(any(Query.class));
    }

    // ==================== 模拟 Embedding 模型限流 ====================

    @Test
    @DisplayName("Embedding 模型限流：降级返回空列表")
    void testEmbeddingRateLimitedReturnsEmpty() {
        when(delegateRetriever.retrieve(any(Query.class)))
                .thenReturn(List.of());

        List<Content> result = cachedRetriever.retrieve(Query.from("头痛挂什么科"));

        assertThat(result).isEmpty();
        verify(delegateRetriever).retrieve(any(Query.class));
    }

    @Test
    @DisplayName("Embedding 限流但有缓存：应返回缓存数据")
    void testCacheHitWhenEmbeddingRateLimited() {
        Query query = Query.from("皮肤红点很痒");
        List<Content> cachedContent = List.of(
                Content.from("皮肤科：诊治湿疹、银屑病、痤疮等")
        );

        when(delegateRetriever.retrieve(query)).thenReturn(cachedContent);
        cachedRetriever.retrieve(query);

        reset(delegateRetriever);

        List<Content> result = cachedRetriever.retrieve(query);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).textSegment().text()).contains("皮肤科");
        verify(delegateRetriever, never()).retrieve(any(Query.class));
    }

    // ==================== 模拟 minScore 过滤后无结果 ====================

    @Test
    @DisplayName("minScore 过滤：无匹配结果返回空列表")
    void testMinScoreFilterReturnsEmpty() {
        when(delegateRetriever.retrieve(any(Query.class)))
                .thenReturn(List.of());

        List<Content> result = cachedRetriever.retrieve(Query.from("今天天气真好"));

        assertThat(result).isEmpty();
    }

    // ==================== 正常流程不受影响 ====================

    @Test
    @DisplayName("正常流程：检索成功返回结果并写入缓存")
    void testNormalRetrievalSucceeds() {
        Query query = Query.from("头痛挂什么科");
        List<Content> contents = List.of(
                Content.from("神经内科：诊治头痛、眩晕、癫痫、帕金森病、脑血管疾病等。")
        );
        when(delegateRetriever.retrieve(query)).thenReturn(contents);

        List<Content> result = cachedRetriever.retrieve(query);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).textSegment().text()).contains("神经内科");
        verify(delegateRetriever).retrieve(query);
    }

    @Test
    @DisplayName("缓存命中：正常流程不走 delegate")
    void testCacheHitSkipsDelegate() {
        Query query = Query.from("头痛挂什么科");
        List<Content> contents = List.of(
                Content.from("神经内科：诊治头痛、眩晕、癫痫、帕金森病、脑血管疾病等。")
        );
        when(delegateRetriever.retrieve(query)).thenReturn(contents);

        cachedRetriever.retrieve(query);
        reset(delegateRetriever);

        List<Content> result = cachedRetriever.retrieve(query);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).textSegment().text()).contains("神经内科");
        verify(delegateRetriever, never()).retrieve(any(Query.class));
    }

    @Test
    @DisplayName("不同查询：各自独立走 delegate 并分别缓存")
    void testDifferentQueriesIndependentCaching() {
        Query query1 = Query.from("头痛挂什么科");
        Query query2 = Query.from("皮肤红点很痒");
        List<Content> contents1 = List.of(Content.from("神经内科"));
        List<Content> contents2 = List.of(Content.from("皮肤科"));

        when(delegateRetriever.retrieve(query1)).thenReturn(contents1);
        when(delegateRetriever.retrieve(query2)).thenReturn(contents2);

        List<Content> result1 = cachedRetriever.retrieve(query1);
        List<Content> result2 = cachedRetriever.retrieve(query2);

        assertThat(result1.get(0).textSegment().text()).contains("神经内科");
        assertThat(result2.get(0).textSegment().text()).contains("皮肤科");
        verify(delegateRetriever).retrieve(query1);
        verify(delegateRetriever).retrieve(query2);
    }
}