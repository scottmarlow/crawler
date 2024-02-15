/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.List;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class EmbeddingStoreFactory {

    public static EmbeddingStore<TextSegment> createEmbeddingStore(List<TextSegment> segments, EmbeddingModel embeddingModel) {
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        for(TextSegment segment : segments) {
            Response<Embedding> reponse = embeddingModel.embed(segment);
            embeddingStore.add(reponse.content(), segment);
        }
        return embeddingStore;
    }
}
