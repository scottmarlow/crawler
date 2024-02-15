/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.document.parser;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import static org.jsoup.internal.StringUtil.in;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.wildfly.ai.document.loader.WildFlyHtmlContent;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class HtmlDocumentParser {

    public List<TextSegment> parsePage(WildFlyHtmlContent content, String cssSelector, String parentSelector) {
        String selector = (cssSelector == null || cssSelector.isBlank()) ? "*" : cssSelector;
        List<TextSegment> segments = new ArrayList<>();
        try {
            Document htmlDoc = Jsoup.parse(content.getPath().toFile());
            Elements parents = htmlDoc.select(parentSelector + "," + selector);
            String title = htmlDoc.title();
            if (isStructured(htmlDoc, selector)) {

                for (Element elt : htmlDoc.select(selector)) {
                    NodeVisitor visitor = new TextExtractingVisitor();
                    NodeTraversor.traverse(visitor, elt);
                    String text = visitor.toString();
                    if (text != null && !text.isBlank()) {
                        Metadata metadata = content.metadata().copy();
                        if (title != null) {
                            metadata.add("title", title);
                        }
                        boolean found = false;
                        for (int i = parents.size() - 1; i >= 0; i--) {
                            Element parent = parents.get(i);
                            if (elt.equals(parent)) {
                                found = true;
                            }
                            if (found && parent.is(parentSelector)) {
                                metadata.add("subtitle", parent.text());
                                break;
                            }
                        }
                        segments.add(new TextSegment(text, metadata));
                    }
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
                if (node.hasParent() && "li".equals(node.parentNode().nodeName())) {
                    return;
                }
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
