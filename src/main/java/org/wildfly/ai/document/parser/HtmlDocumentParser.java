/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.document.parser;

import com.helger.css.decl.CSSSelector;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import static org.jsoup.internal.StringUtil.in;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.wildfly.ai.document.loader.WildFlyHtmlContent;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class HtmlDocumentParser {

    public List<TextSegment> parsePage(WildFlyHtmlContent content, String cssSelector) {
        List<TextSegment> segments = new ArrayList<>();
        try {
            Document htmlDoc = Jsoup.parse(content.getPath().toFile());
            if (isStructured(htmlDoc, cssSelector)) {
                for (Element elt : htmlDoc.select(cssSelector)) {
                    NodeVisitor visitor = new TextExtractingVisitor();
                    NodeTraversor.traverse(visitor, elt);
                    segments.add(new TextSegment(visitor.toString(), content.metadata()));
                }
            }
            htmlDoc.traverse(new TextExtractingVisitor());
            return segments;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isStructured(Document htmlDoc, String cssSelector) {
        return !htmlDoc.select(cssSelector).isEmpty();
    }

    private static class TextExtractingVisitor implements NodeVisitor {

        private final StringBuilder textBuilder = new StringBuilder();

        @Override
        public void head(Node node, int depth) { // hit when the node is first seen
            String name = node.nodeName();
            if (node instanceof TextNode) {
                textBuilder.append(((TextNode) node).text());
            } else if (name.equals("li")) {
                textBuilder.append("\n * ");
            } else if (name.equals("dt")) {
                textBuilder.append("  ");
            } else if (in(name, "p", "h1", "h2", "h3", "h4", "h5", "h6", "tr")) {
                textBuilder.append("\n");
            }
        }

        @Override
        public void tail(Node node, int depth) { // hit when all the node's children (if any) have been visited
            String name = node.nodeName();
            if (in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5", "h6")) {
                textBuilder.append("\n");
            }
        }

        @Override
        public String toString() {
            return textBuilder.toString();
        }
    }
}
