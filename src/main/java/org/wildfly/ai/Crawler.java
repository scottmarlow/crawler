/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai;

import org.wildfly.ai.document.loader.WildFlyHtmlContent;
import crawlercommons.filters.basic.BasicURLNormalizer;
import de.hshn.mi.crawler4j.frontier.HSQLDBFrontierConfiguration;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.wildfly.ai.crawler.WildFlyDocsCrawler;
import org.wildfly.ai.document.parser.HtmlDocumentParser;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class Crawler {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Path rootFolder = new File("wildfly").toPath();
        final Path basePath = rootFolder.resolve("docs");
        final String baseUrl = "https://docs.wildfly.org/";
        final int numCrawlers = 12;
        final List<WildFlyHtmlContent> contents = new ArrayList<>();
        HtmlDocumentParser parser = new HtmlDocumentParser();
        // Instantiate the controller for this crawl 
        CrawlController controller = createController("docs", baseUrl);
        CrawlController.WebCrawlerFactory<WildFlyDocsCrawler> factory = () -> new WildFlyDocsCrawler(basePath, baseUrl, contents);
        controller.start(factory, numCrawlers);
        controller.shutdown();
        List<TextSegment> myDocs = new ArrayList<>(contents.size());

        for (WildFlyHtmlContent content : contents) {
            myDocs.addAll(parser.parsePage(content, ".paragraph,.content", "h2"));
        }

        contents.clear();
        final Path wfBasePath = rootFolder.resolve("www");
        final String wfBaseUrl = "https://www.wildfly.org/";
        factory = () -> new WildFlyDocsCrawler(wfBasePath, wfBaseUrl, contents);
        controller = createController("www", wfBaseUrl);
        controller.addSeed(wfBaseUrl);
        controller.start(factory, numCrawlers);
        controller.shutdown();

        for (WildFlyHtmlContent content : contents) {
            myDocs.addAll(parser.parsePage(content, ".paragraph,.content", "h2"));
        }
        EmbeddingModel embeddingModel = new OllamaEmbeddingModel.OllamaEmbeddingModelBuilder()
                .baseUrl("http://ollama-mchomaredhatcom.apps.ai-hackathon.qic7.p1.openshiftapps.com")
                .modelName("mistral:latest")
                .maxRetries(20)
                .timeout(Duration.ofSeconds(500))
                .build();
        embed(myDocs, embeddingModel);
    }

    private static EmbeddingStore<TextSegment> embed(List<TextSegment> segments, EmbeddingModel embeddingModel) {
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
        return embeddingStore;
    }

    private static CrawlController createController(String name, String baseUrl) throws Exception {
        File crawlStorage = new File("crawler4j", name);
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorage.getAbsolutePath());
        config.setIncludeBinaryContentInCrawling(false);
        config.setFollowRedirects(true);
        config.setIncludeHttpsPages(true);
        // Instantiate the controller for this crawl 
        BasicURLNormalizer normalizer = BasicURLNormalizer.newBuilder().idnNormalization(BasicURLNormalizer.IdnNormalization.NONE).build();
        PageFetcher pageFetcher = new PageFetcher(config, normalizer);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        FrontierConfiguration frontierConfiguration = new HSQLDBFrontierConfiguration(config, 10);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher, frontierConfiguration.getWebURLFactory());
        CrawlController controller = new CrawlController(config, normalizer, pageFetcher, robotstxtServer, frontierConfiguration);
        controller.addSeed(baseUrl);
        return controller;
    }
}
