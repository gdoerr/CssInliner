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
package ws.doerr.httpserver;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import ws.doerr.configuration.Configuration;
import ws.doerr.cssinliner.server.PathSerializer;

/**
 *
 * @author greg
 */
public class Server {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    public static final Server INSTANCE = new Server();

    private ObjectMapper mapper;
    private HttpServer server;

    private WebSocketApp app;

    private Server() {
        Logger.getLogger("org.glassfish.grizzly").setLevel(Level.WARNING);
    }

    private void intStart(String packages) throws Exception {
        // Set up Jackson
        mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
        );

        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Path.class, new PathSerializer());
        mapper.registerModule(module);

        ResourceConfig resourceConfig = new ResourceConfig()
                .packages(packages)
                .register(provider);

        ServerConfiguration cfg = Configuration.get(ServerConfiguration.class);
        server = GrizzlyHttpServerFactory.createHttpServer(cfg.getServerUri(), resourceConfig, false);

        // Handle the static content
        // Override onMissingResource for SPA
        HttpHandler httpHandler;
        if(Configuration.isDebug()) {
            // If we're in debug mode, serve the content from the folder. This
            // allows you to live edit the static content without having to
            // restart the server every time.
            httpHandler = new StaticHttpHandler("src/main/resources/web", "");
            server.getListener("grizzly").getFileCache().setEnabled(false);
        } else {
            // Normal run, serve the content from the jar file
            httpHandler = new CLStaticHttpHandler(Server.class.getClassLoader(), "/web/");
        }
        server.getServerConfiguration().addHttpHandler(httpHandler, "/");

        // Configure the WebSocket handler
        WebSocketAddOn webSocket = new WebSocketAddOn();
        server.getListener("grizzly").registerAddOn(webSocket);
        app = new WebSocketApp();

        server.start();

        LOG.log(Level.INFO, "Server running on http://{0}:{1,number,#}", new Object[]{
            cfg.hostname, cfg.port
        });
    }

    public static void start(String packages) throws Exception {
        if(INSTANCE.server == null) {
            INSTANCE.intStart(packages);
        }
    }

    public static void registerWebsocket(String path) {
        if(INSTANCE.server != null) {
            // Set up the web socket
            WebSocketEngine.getEngine().register("", path, INSTANCE.app);
        }
    }

    public static void stop() {
        if(INSTANCE.server != null) {
            INSTANCE.server.shutdownNow();
            INSTANCE.server = null;
        }
    }

    public static final ObjectMapper getMapper() {
        return INSTANCE.mapper;
    }

    public static final void send(Object object) throws Exception {
        if(INSTANCE.app != null)
            INSTANCE.app.send(INSTANCE.mapper.writeValueAsString(object));
    }
}
