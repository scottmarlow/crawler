/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.document.parser;

import dev.langchain4j.data.segment.TextSegment;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.wildfly.ai.document.loader.WildFlyHtmlContent;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class HtmlDocumentParserTest {
    
    public HtmlDocumentParserTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of parsePage method, of class HtmlDocumentParser.
     */
    @Test
    public void testParsePage() {
        System.out.println("parsePage " +new File("target").toPath().resolve("test-classes").resolve("admin_guide.html").toAbsolutePath());
        WildFlyHtmlContent content = new WildFlyHtmlContent(new File("target").toPath().resolve("test-classes").resolve("admin_guide.html"), "en", "https://docs.wildfly.org/31/Admin_Guide.html", "https://docs.wildfly.org/31/");
        String cssSelector = "";
        HtmlDocumentParser instance = new HtmlDocumentParser();
        /*List<TextSegment> result = instance.parsePage(content, cssSelector);
        assertEquals(16598, result.size());*/
        List<TextSegment> result = instance.parsePage(content, ".sect2");
        assertEquals(50, result.size());
    }
    
}
