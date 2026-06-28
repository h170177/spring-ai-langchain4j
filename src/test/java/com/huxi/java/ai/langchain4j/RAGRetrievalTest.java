package com.huxi.java.ai.langchain4j;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("RAG Retrieval Quality Test")
class RAGRetrievalTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    private InMemoryEmbeddingStore<TextSegment> knowledgeStore;

    @BeforeEach
    void setUp() {
        knowledgeStore = new InMemoryEmbeddingStore<>();
        TextSegment[] docs = {
                TextSegment.from("神经内科：诊治头痛、眩晕、癫痫、帕金森病、脑血管疾病等。"),
                TextSegment.from("心内科：诊治冠心病、高血压、心律失常、心力衰竭等心血管疾病。"),
                TextSegment.from("皮肤科：诊治湿疹、银屑病、痤疮、荨麻疹、皮肤过敏等。"),
                TextSegment.from("骨科：诊治骨折、关节损伤、腰椎间盘突出、关节炎等。"),
                TextSegment.from("儿科：诊治小儿感冒发烧、哮喘、腹泻、生长发育迟缓等。"),
                TextSegment.from("消化内科：诊治胃炎、胃溃疡、肠炎、肝炎、胰腺炎等。"),
        };
        for (TextSegment doc : docs) {
            Embedding emb = embeddingModel.embed(doc).content();
            knowledgeStore.add(emb, doc);
        }
    }

    @Test
    @DisplayName("头痛症状 -> 应返回神经内科")
    void testHeadacheReturnsNeurology() {
        Embedding queryEmb = embeddingModel.embed("我经常头痛应该挂什么科").content();
        EmbeddingSearchResult<TextSegment> result = knowledgeStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmb)
                        .maxResults(2)
                        .minScore(0.5)
                        .build());

        assertThat(result.matches())
                .isNotEmpty()
                .first()
                .extracting(m -> m.embedded().text())
                .asString()
                .contains("神经内科");
    }

    @Test
    @DisplayName("皮肤问题 -> 应返回皮肤科")
    void testSkinProblemReturnsDermatology() {
        Embedding queryEmb = embeddingModel.embed("皮肤上长了好多红点很痒").content();
        EmbeddingSearchResult<TextSegment> result = knowledgeStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmb)
                        .maxResults(1)
                        .minScore(0.5)
                        .build());

        assertThat(result.matches())
                .isNotEmpty()
                .first()
                .extracting(m -> m.embedded().text())
                .asString()
                .contains("皮肤科");
    }

    @Test
    @DisplayName("批量科室查询准确率 100%")
    void testBatchDepartmentAccuracy() {
        Map<String, String> cases = Map.of(
                "胸口闷疼", "心内科",
                "腰疼得直不起来", "骨科",
                "孩子一直咳嗽发烧", "儿科",
                "吃不下饭胃总疼", "消化内科"
        );

        int success = 0;
        for (var entry : cases.entrySet()) {
            Embedding qEmb = embeddingModel.embed(entry.getKey()).content();
            EmbeddingSearchResult<TextSegment> r = knowledgeStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(qEmb)
                            .maxResults(1)
                            .minScore(0.45)
                            .build());
            if (!r.matches().isEmpty()
                    && r.matches().get(0).embedded().text().contains(entry.getValue())) {
                success++;
            }
        }

        assertThat(success).isEqualTo(cases.size());
    }

    @Test
    @DisplayName("minScore 阈值过滤：低相关查询应无结果")
    void testLowRelevanceFilteredByMinScore() {
        Embedding queryEmb = embeddingModel.embed("今天天气真好适合出去玩").content();
        EmbeddingSearchResult<TextSegment> result = knowledgeStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmb)
                        .maxResults(3)
                        .minScore(0.7)
                        .build());

        assertThat(result.matches()).isEmpty();
    }

    @Test
    @DisplayName("Embedding 维度一致性验证")
    void testEmbeddingDimension() {
        Response<Embedding> embed = embeddingModel.embed("测试文本");
        int dim = embed.content().vector().length;
        assertThat(dim).isGreaterThan(100);
    }

    @Test
    @DisplayName("相关查询得分高于无关查询")
    void testRelevantQueryScoresHigherThanIrrelevant() {
        Embedding relevantEmb = embeddingModel.embed("头痛挂什么科").content();
        Embedding irrelevantEmb = embeddingModel.embed("今天吃什么好").content();

        EmbeddingSearchResult<TextSegment> relevantR = knowledgeStore.search(
                EmbeddingSearchRequest.builder().queryEmbedding(relevantEmb)
                        .maxResults(1).build());
        EmbeddingSearchResult<TextSegment> irrelevantR = knowledgeStore.search(
                EmbeddingSearchRequest.builder().queryEmbedding(irrelevantEmb)
                        .maxResults(1).build());

        double relevantScore = relevantR.matches().get(0).score();
        double irrelevantScore = irrelevantR.matches().get(0).score();

        assertThat(relevantScore).isGreaterThan(irrelevantScore);
    }
}
