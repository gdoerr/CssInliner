/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Greg Doerr
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

import ws.doerr.httpserver.Server;
import ws.doerr.cssinliner.parser.CssInliner;
import ws.doerr.cssinliner.parser.InlinerContext;
import ws.doerr.monitor.Monitor;
import ws.doerr.monitor.MonitorHandler;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import ws.doerr.cssinliner.email.EmailService;

/**
 *
 * @author greg
 */
public class InlinerApp {
    private static final Logger LOG = Logger.getLogger(InlinerApp.class.getName());

    private static InlinerApp instance;

    private final CssInliner inliner = new CssInliner();
    private final Handlebars handlebars;

    private MonitorHandler sourceHandler = new SourceHandler();
    private MonitorHandler dependencyHandler = new DependencyHandler();
    private MonitorHandler dataHandler = new DataHandler();

    private Map<UUID, SourceInstance> sources = new HashMap<>();                // Instance ID to instance
    private Multimap<String, UUID> pathToInstance = HashMultimap.create();      // Source path to Instance ID

    private Set<String> folders = new HashSet<>();

    private InlinerApp(Path sourceFolder, Path dataFolder) throws Exception {
        TempFolder workingFolder = new TempFolder("cssinline");

        // Start the Http Server
        Server.start(getClass().getPackage().getName());
        Server.registerWebsocket("/connect");

        // Setup Handlebars
        FileTemplateLoader loader = new FileTemplateLoader(workingFolder.getFile());
        loader.setSuffix("");
        handlebars = new Handlebars(loader);

        EmailService.get().getHelpers().forEach((tag, helper) -> {
            handlebars.registerHelper(tag, helper);
        });

        // List all the source files from the folder
        File[] files = sourceFolder.toFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });

        for(File file : files) {
            try {
                SourceInstance instance = new SourceInstance(file.toPath(), dataFolder, workingFolder.getPath());
                InlinerContext context = inliner.process(instance.getSource(), instance.getInlined());
                instance.update(context);

                processHandlebars(instance);

                getDocumentTitle(instance);

                sources.put(instance.getId(), instance);

                pathToInstance.put(file.toPath().normalize().toString(), instance.getId());

                instance.getDependencies().forEach((dependency) -> {
                    pathToInstance.put(dependency.getPath().normalize().toString(), instance.getId());
                    folders.add(dependency.getPath().normalize().getParent().toString());
                });

                pathToInstance.put(instance.getData().toString(), instance.getId());
            } catch(Exception ex) {

            }
        }

        // Register the folder for change events
        Monitor.getInstance().register(sourceHandler, sourceFolder);
        Monitor.getInstance().register(dataHandler, dataFolder);

        folders.forEach(folder -> {
            Path f = Paths.get(folder);
            Monitor.getInstance().register(dependencyHandler, f);
        });
    }

    private Set<UUID> changes = new HashSet<>();

    /**
     * Handle changes to the source files
     */
    class SourceHandler implements MonitorHandler {

        @Override
        public void startSession() {
            changes.clear();
        }

        @Override
        public void endSession() {
            changes.forEach(id -> {
                SourceInstance instance = sources.get(id);
                if(instance != null) {
                    try {
                        InlinerContext context = inliner.process(instance.getSource(), instance.getInlined());
                        instance.update(context);
                        processHandlebars(instance);
                        getDocumentTitle(instance);
                        Server.send(instance);
                    } catch(Exception ex) {}
                }
            });
        }

        @Override
        public void change(ChangeType change, Path path) {
            String p = path.normalize().toString();
            switch(change) {
                case MODIFY:
                    changes.addAll(pathToInstance.get(p));
                    break;

                case DELETE:
                    Collection<UUID> source = pathToInstance.get(p);
                    if(source.size() > 1)
                        LOG.log(Level.WARNING, "Multiple sources mapped to path {0}", p);

                    else if(!source.isEmpty())
                        sources.remove(source.iterator().next());
                    break;
            }
        }
    }

    /**
     * Handle changes to the dependencies
     */
    class DependencyHandler implements MonitorHandler {

        @Override
        public void change(ChangeType change, Path path) {
            if(change == ChangeType.MODIFY) {
                String p = path.normalize().toString();
                changes.addAll(pathToInstance.get(p));
            }
        }
    }

    /**
     * Handle changes to the json data files
     */
    class DataHandler implements MonitorHandler {

        @Override
        public void change(ChangeType change, Path path) {
            String p = path.normalize().toString();
            changes.addAll(pathToInstance.get(p));
        }
    }

    private void processHandlebars(SourceInstance instance) throws IOException {
        try (FileWriter writer = new FileWriter(instance.getMerged().toFile())) {
            Template template = handlebars.compile(instance.getInlined().getFileName().toString());
            JsonNode data = Server.getMapper().readTree(instance.getData().toFile());
            Context handlebarsContext = Context
                    .newBuilder(data)
                    .resolver(JsonNodeValueResolver.INSTANCE)
                    .build();

            template.apply(handlebarsContext, writer);
        } catch(JsonMappingException ex) {
            instance.logError("JSON", ex.getMessage());
            Files.copy(instance.getInlined().toFile(), instance.getMerged().toFile());
        } catch(HandlebarsException ex) {
            instance.logError("Handlebars", ex.getError().reason);
            Files.copy(instance.getInlined().toFile(), instance.getMerged().toFile());
            LOG.log(Level.WARNING, "", ex);
        } catch(FileNotFoundException ex) {
            instance.logError("JSON", "No JSON data file found");
            Files.copy(instance.getInlined().toFile(), instance.getMerged().toFile());
        } catch(Exception ex) {
            instance.logError(ex.getClass().getSimpleName(), ex.getMessage());
            Files.copy(instance.getInlined().toFile(), instance.getMerged().toFile());
            LOG.log(Level.WARNING, "", ex);
        }
    }

    private void getDocumentTitle(SourceInstance instance) {
        try {
            instance.setTitle(inliner.getTitle(instance.getMerged()));
        } catch(Exception ex) {
            instance.logError("TITLE", ex.getMessage());
        }
    }

    public Set<SourceInstance> getSources() {
        return new HashSet<>(sources.values());
    }

    public SourceInstance getSource(UUID id) {
        return this.sources.get(id);
    }

    public static InlinerApp getInstance() {
        return instance;
    }

    public static void start(Path sourceFolder, Path dataFolder) throws Exception {
        if(instance == null)
            instance = new InlinerApp(sourceFolder, dataFolder);
    }

    public static void stop() {
        Server.stop();
        Monitor.stop();
    }
}
