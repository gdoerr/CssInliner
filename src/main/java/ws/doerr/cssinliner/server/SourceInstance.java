/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2016 Greg Doerr
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ws.doerr.cssinliner.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ws.doerr.projects.emailtemplates.Dependency;
import ws.doerr.projects.emailtemplates.ProcessorContext;

/**
 *
 * @author greg
 */
public class SourceInstance {
    private final UUID id;
    private final String name;

    private final Path source;
    private final Path data;

    private final Path inlined;
    private final Path merged;

    private String title;

    private final Set<Dependency> dependencies = new HashSet<>();

    private final String viewPath;

    private final Map<String, String> meta = new HashMap<>();
    private final long created;
    private long modified;
    private long size;

    private final Map<String, String> errors = new HashMap<>();

    public SourceInstance(Path source, Path dataFolder, Path tempFolder) throws IOException {
        id = UUID.randomUUID();

        this.source = source;
        this.name = source.getFileName().toString();

        this.data = dataFolder.resolve(source.getFileName().toString() + ".json");

        inlined = tempFolder.resolve(source.getFileName());
        merged = tempFolder.resolve(source.getFileName().toString().replace(".html", "merged.html"));

        viewPath = "/api/files/" + id.toString();

        BasicFileAttributes attr = Files.readAttributes(source, BasicFileAttributes.class);
        this.created = attr.creationTime().toMillis();
    }

    public UUID getId() {
        return id;
    }

    public void update(ProcessorContext context) throws IOException {
        errors.clear();

        dependencies.clear();
        dependencies.addAll(context.getDependencies());

        meta.clear();
        meta.putAll(context.getMeta());

        this.size = source.toFile().length();
        this.modified = source.toFile().lastModified();
    }

    public void logError(String error, String message) {
        errors.put(error, message);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Path getSource() {
        return source;
    }

    public Path getInlined() {
        return inlined;
    }

    public Path getMerged() {
        return merged;
    }

    public Path getData() {
        return data;
    }

    public Set<Dependency> getDependencies() {
        return dependencies;
    }

    public Map<String, String> getMeta() {
        return meta;
    }
}
