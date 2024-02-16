/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.embedding;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.wildfly.ai.document.loader.WildFlyHtmlContent;
import org.wildfly.ai.document.parser.HtmlDocumentParser;

/**
 *
 * podman run -d -v ollama:/home/ehsavoie/tmp/ollama -p 11434:11434 --name
 * ollama langchain4j/ollama-mistral:latest
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class EmbeddingTestCase {

    public static final String OPENAI_KEY = "demo";

    /**
     * Test of parsePage method, of class HtmlDocumentParser.
     */
    @Test
    public void testEmbedPageFromDocs() {
        WildFlyHtmlContent content = new WildFlyHtmlContent(new File("target").toPath().resolve("test-classes").resolve("admin_guide.html"), "en", "https://docs.wildfly.org/31/Admin_Guide.html", "https://docs.wildfly.org/31/");
        String cssSelector = ".sect3";
        String parentSelector = "h2";
        HtmlDocumentParser instance = new HtmlDocumentParser();
        List<TextSegment> result = instance.parsePage(content, cssSelector, parentSelector);
        assertEquals(190, result.size());
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        /*      EmbeddingModel embeddingModel = new OllamaEmbeddingModel.OllamaEmbeddingModelBuilder()
//                .baseUrl("http://ollama-mchomaredhatcom.apps.ai-hackathon.qic7.p1.openshiftapps.com")
                .baseUrl("http://localhost:11434")
                .modelName("mistral")
                .maxRetries(1)
                .timeout(Duration.ofSeconds(30))
                .build();*/
        EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createEmbeddingStore(result, embeddingModel);
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(2) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
                .build();

        String ollamaUrl = "http://localhost:11434";
        String modelName = "mistral:7b-text-q2_K";
        modelName = "mistral:7b-instruct-v0.2-q2_K";

//        String ollamaUrl = "http://ollama-mchomaredhatcom.apps.ai-hackathon.qic7.p1.openshiftapps.com";
//        String modelName = "7b-text-q2_Kchat";
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(210))
                .build();

//        ChatLanguageModel model = OpenAiChatModel
//                .builder()
//                .apiKey(OPENAI_KEY)
//                .maxRetries(5)
//                .modelName(OpenAiChatModelName.GPT_3_5_TURBO)
//                .logRequests(Boolean.TRUE)
//                .logResponses(Boolean.TRUE)
//                .build();
        String question = "How do I set up a ConnectionFactory to a remote jms broker ?";
        List<Content> ragContents = contentRetriever.retrieve(Query.from(question));

        List<ChatMessage> messages = new ArrayList<>();

        SystemMessage systemMessage = SystemMessage.systemMessage("You are a wildfly expert who understands well how to administrate the wildfly server and its components");
        String promptTemplate = "You are a WildFly expert who understands well how to administrate the WildFly application server and its components\n"
                + "You are also knowledgeable in the Java language.\n"
                + "You excel at teaching and explaining concepts of the language."
                + "If you don't know the answer to the question, say that you don't know the answer, and that the user should refer to the WildFly documentation.\n\n"
                + "Answer the following question to the best of your ability: \n"
                + "{{userMessage}}\n\n"
                + "Base your answer exclusively on the following information from the WildFly documentation:\n\n"
                + "{{contents}}";
        String completePrompt = "Objective: answer the user question delimited by  ---\n"
                + "\n"
                + "---\n"
                + "%s\n"
                + "---"
                + "\n Here is a few data to help you:\n"
                + "";
        String basePrompt = String.format(completePrompt, question);
        StringBuilder messageBuilder = new StringBuilder(basePrompt);
        for (Content ragContent : ragContents) {
            messageBuilder.append(ragContent.textSegment().text());
        }

        ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
                .chatLanguageModel(model)
                .contentRetriever(contentRetriever)
                .retrievalAugmentor(DefaultRetrievalAugmentor.builder()
                        .contentInjector(DefaultContentInjector.builder()
                                .promptTemplate(PromptTemplate.from(promptTemplate))
                                .build())
                        .queryRouter(new DefaultQueryRouter(contentRetriever))
                        .build())
                .build();
        System.out.println("Answer from chain " + chain.execute(question));
        System.out.println("Answer from model " + model.generate(systemMessage, UserMessage.from(messageBuilder.toString().substring(0, 4096))).content().text());

    }

}
