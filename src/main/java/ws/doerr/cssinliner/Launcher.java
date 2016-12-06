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
package ws.doerr.cssinliner;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import ws.doerr.configuration.Configuration;
import ws.doerr.cssinliner.server.InlinerApp;
import ws.doerr.projects.emailtemplates.TemplateProcessor;

/**
 *
 * @author greg
 */
public class Launcher {
    private static final Logger LOG = Logger.getLogger(Launcher.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // Build the configuration
            Configuration.process("cssinliner", args);
            MainConfiguration main = Configuration.get(MainConfiguration.class);

            if(main.printHelp) {
                printHelp();
            } else if(!main.dataPath.isEmpty() && !main.inline) {
                if(main.inputPath.isEmpty()) {
                    System.out.println("Interactive mode specified without input path");
                    printHelp();
                    return;
                }
                runService(main);
            } else if(!main.inputPath.isEmpty() || !Configuration.getUnhandled().isEmpty() || main.inline) {
                runInline(main);
            } else
                printHelp();
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, "Exception launching application", ex);
        }
    }

    private static void printHelp() {
        System.out.println("CssInliner\n\n"
            + "CssInliner is designed to simplify the iterative process"
            + " to write, test and maintain email templates.\n");

        System.out.println("The normal version of the program processes html files supplied on the command line or scanned from a directory\n");
        System.out.println("\tinliner file[s]\t\t\t\tprocesses each file with the result being writted to the same");
        System.out.println("\t\t\t\t\t\tfolder with the .out extension");
        System.out.println("\tinliner -i input-path\t\t\tprocesses each html file from input-path with the result being");
        System.out.println("\t\t\t\t\t\twritten to the same folder with the .out extension");
        System.out.println("\tinliner -i input-path -o output-path\tprocesses each html file from input-path with the result being");
        System.out.println("\t\t\t\t\t\twritten to output-path\n");

        System.out.println("The Interactive version of the program processes files in real-time and provides a processed and viewable");
        System.out.println("  version of the template through a web-browser\n");
        System.out.println("\tinliner -i input-path -d data-path\twhere input path points to the templates and data-path points");
        System.out.println("\t\t\t\t\t\tto the json files used to merge\n");
        System.out.println("\tOnce running, press Control-D on the console to terminate\n");

        Configuration.printHelp(System.out);
    }

    private static void runService(MainConfiguration cfg) {
        try {
            InlinerApp.start(Paths.get(cfg.inputPath), Paths.get(cfg.dataPath));
            LOG.log(Level.INFO, "Press Ctrl-D to stop server");
            System.in.read();
            InlinerApp.stop();
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, "Exception running interactive", ex);
        }
    }

    private static void runInline(MainConfiguration config) {
            TemplateProcessor processor = new TemplateProcessor();

            Set<String> sources = new HashSet<>(Configuration.getUnhandled());

            if(config.inputPath != null && Configuration.getUnhandled().isEmpty()) {
                File input = new File(config.inputPath);
                File[] files = input.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".html");
                    }
                });

                for(File file : files)
                    sources.add(file.getPath());
            }

            for(String source : sources) {
                try {
                    Path src = Paths.get(source);
                    Path dest = appendFileName(src, ".out");

                    if(!config.outputPath.isEmpty())
                        dest = Paths.get(config.outputPath, src.getFileName().toString());

                    processor.process(src, dest);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
    }

    private static Path appendFileName(Path src, String add) {
        String name = src.getFileName().toString();

        int idx = name.lastIndexOf('.');
        if(idx != -1) {
            return src.resolveSibling(name.substring(0, idx) + add + name.substring(idx));
        } else
            return src.resolveSibling(name + add);
    }
}
