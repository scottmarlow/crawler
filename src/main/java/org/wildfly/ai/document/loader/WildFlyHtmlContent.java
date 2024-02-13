/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ai.document.loader;

import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class WildFlyHtmlContent implements DocumentSource {

    private final Path path;
    private final String language;
    private final String url;
    private final String parentUrl;

    public WildFlyHtmlContent(Path path, String language, String url, String parentUrl) {
        this.path = path;
        this.language = language;
        this.url = url;
        this.parentUrl = parentUrl;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + Objects.hashCode(this.path);
        hash = 31 * hash + Objects.hashCode(this.language);
        hash = 31 * hash + Objects.hashCode(this.url);
        hash = 31 * hash + Objects.hashCode(this.parentUrl);
        return hash;
    }

    public Path getPath() {
        return path;
    }

    public String getLanguage() {
        return language;
    }

    public String getUrl() {
        return url;
    }

    public String getParentUrl() {
        return parentUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WildFlyHtmlContent other = (WildFlyHtmlContent) obj;
        if (!Objects.equals(this.language, other.language)) {
            return false;
        }
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        if (!Objects.equals(this.parentUrl, other.parentUrl)) {
            return false;
        }
        return Objects.equals(this.path, other.path);
    }

    @Override
    public InputStream inputStream() throws IOException {
        return Files.newInputStream(getPath());
    }

    @Override
    public Metadata metadata() {
        Metadata metadata = new Metadata();
        metadata.add("url", getUrl());
        metadata.add("language", getLanguage());
        metadata.add("parent_rul", getParentUrl());
        metadata.add("file_name", getPath().getFileName().toString());
        metadata.add("file_path", getPath().toString());
        return metadata;
    }
}
