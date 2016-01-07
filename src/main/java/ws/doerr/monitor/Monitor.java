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
package ws.doerr.monitor;

import ws.doerr.monitor.MonitorHandler.ChangeType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author greg
 */
public class Monitor {
    private static final Logger LOG = Logger.getLogger(Monitor.class.getName());

    private static Monitor instance = null;
    private static final Integer lock = new Integer(0);

    private Thread running = null;

    private WatchService service;

    // Folder to Watch Key
    private final Map<Path, WatchKey> folderToKey = new HashMap<>();
    private final Map<WatchKey, Path> keyToFolder = new HashMap<>();

    private final Multimap<Path, MonitorHandler> folderToHandler = HashMultimap.create();
    private final Set<MonitorHandler> handlers = new HashSet<>();

    private Monitor() {
        try {
            service = FileSystems.getDefault().newWatchService();

            running = new Thread(new Runnable() {
                @Override
                public void run() {
                    process();
                }
            });

            running.start();
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, "Could not get Watch service", ex);
        }
    }

    public static void stop() {
        if(instance != null && instance.running != null) {
            instance.running.interrupt();
            instance.running = null;
        }
    }

    private void process() {
        while(true) {
            try {
                // Wait for an event then put it back
                WatchKey k = service.take();

                // Now we sleep for a short time. In the case where there are
                // multiple writes, this prevents processing the same
                // files multiple times
                Thread.sleep(1000);

                handlers.forEach(action -> {
                    action.startSession();
                });

                keyToFolder.forEach((key, folder) -> {
                    key.pollEvents().forEach(ev -> {
                        if(ev.kind() == OVERFLOW)
                            return;

                        WatchEvent<Path> event = (WatchEvent<Path>) ev;

                        Path path = folder.resolve(event.context());

                        ChangeType change = event.kind() == ENTRY_CREATE ? ChangeType.CREATE : event.kind() == ENTRY_DELETE ? ChangeType.DELETE : ChangeType.MODIFY;

                        folderToHandler.get(folder).forEach(handler -> {
                            handler.change(change, path);
                        });
                    });
                });

                k.reset();
            } catch(InterruptedException | ClosedWatchServiceException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch(Exception ex) {}

            handlers.forEach(action -> {
                action.endSession();
            });
        }
    }

    public void register(MonitorHandler handler, Path path) {
        handlers.add(handler);
        folderToHandler.put(path, handler);
        updateWatches();
    }

    public void unRegister(MonitorHandler handler, Path path) {
        folderToHandler.remove(path, handler);
        updateWatches();
    }

    private void updateWatches() {
        // Add any missing watches
        synchronized(folderToKey) {
            folderToHandler.keySet().forEach(folder -> {
                try {
                    if(!folderToKey.containsKey(folder)) {
                        WatchKey key = folder.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        folderToKey.put(folder, key);
                        keyToFolder.put(key, folder);
                    }
                } catch(IOException ex) {
                    LOG.log(Level.WARNING, "Exception adding watch for " + folder.toString(), ex);
                }
            });

            Set<Path> remove = Sets.difference(folderToKey.keySet(), folderToHandler.keySet());
            remove.forEach(folder -> {
                WatchKey key = folderToKey.remove(folder);
                key.cancel();
                keyToFolder.remove(key);
            });
        }
    }

    public static Monitor getInstance() {
        if(instance == null) {
            synchronized(lock) {
                if(instance == null)
                    instance = new Monitor();
            }
        }
        return instance;
    }
}
