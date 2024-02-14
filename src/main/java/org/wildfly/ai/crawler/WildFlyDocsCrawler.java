/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.crawler;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.wildfly.ai.document.loader.WildFlyHtmlContent;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class WildFlyDocsCrawler extends WebCrawler {

    private final static Pattern EXCLUSIONS = Pattern.compile(".*(\\.(css|js|xml|gif|jpg|png|mp3|mp4|zip|gz|pdf))$");

    private final Path basePath;
    private final String baseUrl;
    private final Pattern filter;
    private final List<WildFlyHtmlContent> content;

    public WildFlyDocsCrawler(Path basePath, String baseUrl, List<WildFlyHtmlContent> content) {
        this.basePath = basePath;
        this.baseUrl = baseUrl;
        this.filter = Pattern.compile(baseUrl + "([1-9](\\.1)(\\.2)??|1[0-9](\\.1)?|2[0-9](\\.1)?|3[0])/.*");
        this.content = content;
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String urlString = url.getURL().toLowerCase();
        if (urlString.startsWith("https://github.com")) {
            logger.warn("Error processing URL: {}", url);
        }
        return !EXCLUSIONS.matcher(urlString).matches()
                && !filter.matcher(urlString).matches()
                && urlString.startsWith(baseUrl);
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getPath().toLowerCase(Locale.ENGLISH);
        boolean isIndex = url.endsWith("/");
        StringTokenizer tokenizer = new StringTokenizer(url, "/");
        Path path = basePath;
        while (tokenizer.hasMoreTokens()) {
            path = path.resolve(tokenizer.nextToken());
        }
        try {
            Path createdPath;
            if (isIndex) {
                Files.createDirectories(path);
                createdPath = Files.write(path.resolve("index.html"), page.getContentData());
            } else {
                Files.createDirectories(path.getParent());
                String fileName = path.getFileName().toString();
                if (fileName.indexOf('.') < 0) {
                    path = path.resolveSibling(fileName + ".html");
                }
                createdPath = Files.write(path, page.getContentData(), StandardOpenOption.CREATE);
            }
            content.add(new WildFlyHtmlContent(
                    createdPath,
                    page.getLanguage(),
                    page.getWebURL().getURL().toLowerCase(Locale.ENGLISH),
                    page.getWebURL().getParentUrl()));
        } catch (IOException ex) {
            logger.error("Error processing URL: {} which lead to {}", page.getWebURL().getURL(), path, ex);
        }
    }

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser();
        Document document = FileSystemDocumentLoader.loadDocument(documentPath, documentParser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }
}
