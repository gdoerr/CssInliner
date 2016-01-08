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
package ws.doerr.configuration;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.reflections.Reflections;

/**
 * Main Configuration Object
 */
public class Configuration {
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    private static final Configuration INSTANCE = new Configuration();

    private final Map<String, Object> configFragments = new HashMap<>();
    private final Multimap<String, ConfigItem> items = HashMultimap.create();

    private final Multimap<Integer, ConfigHelp> helps = ArrayListMultimap.create();

    private final List<String> unhandledArguments = new ArrayList<>();

    private boolean processed = false;

    private String configName;

    private Configuration() {}

    /**
     * Process defaults and configuration files
     * @param configName
     * @throws ConfigurationException
     */
    public static void process(String configName) throws ConfigurationException {
        INSTANCE.scanConfiguration();
        INSTANCE.processConfigFiles(configName);
        INSTANCE.processed = true;
    }

    /**
     * Process defaults and command line arguments
     * @param args
     * @throws ConfigurationException
     */
    public static void process(String[] args) throws ConfigurationException {
        INSTANCE.scanConfiguration();
        INSTANCE.processCommandLine(args);
    }

    /**
     * Process defaults, configuration files and command line arguments
     * @param configName
     * @param args
     * @throws ConfigurationException
     */
    public static void process(String configName, String[] args) throws ConfigurationException {
        INSTANCE.scanConfiguration();
        INSTANCE.processConfigFiles(configName);
        INSTANCE.processCommandLine(args);
    }

    /**
     * Process command line arguments
     *
     * @param args
     * @throws ConfigurationException
     */
    private void processCommandLine(String[] args) throws ConfigurationException {
        List<String> unknown = new ArrayList<>();

        for(int i = 0; i < args.length; i++) {
            String arg = args[i];

            if(arg.startsWith("--")) {
                i = handleArgument(args, i, arg.substring(2), true, unknown);
            } else if(arg.startsWith("-")) {
                i = handleArgument(args, i, arg.substring(1), false, unknown);
            } else
                unhandledArguments.add(arg);
        }

        processed = true;

        if(!unknown.isEmpty())
            throw new UnknownArgumentException(Joiner.on("\n").join(unknown));
    }

    /**
     * Find and process all matching ConfigItems
     *
     * @param args
     * @param index
     * @param arg
     * @param wasLong
     * @param unknown
     * @return
     * @throws ConfigurationException
     */
    private int handleArgument(String[] args, int index, String arg, boolean wasLong, List<String> unknown) throws ConfigurationException {
        List<ConfigItem> matchedItems = new ArrayList<>();

        // Get all the matching items
        items.values().stream().filter(item -> {
            return arg.startsWith(wasLong ? item.longName : item.shortName);
        }).forEach(matchedItems::add);

        if(matchedItems.isEmpty()) {
            // Nothing matched...
            unknown.add(MessageFormat.format("Unknown argument {0} at position {2}", args[index], index + 1));
        } else if(matchedItems.size() > 1) {
            // If we have multiple matches, make sure they use the same argument capture
            ConfigItem first = matchedItems.get(0);
            for(int l = 1; l < matchedItems.size(); l++) {
                ConfigItem next = matchedItems.get(l);
                if(first.argCount != next.argCount || first.afterArg != next.afterArg) {
                    throw new ConfigItemException(
                        "Argument {0} matched multiple items with different capture settings\n\t{1}, argument {2}\n\t{3}, argument {4}",
                        args[index],
                        first.getName(), wasLong ? first.longName : first.shortName,
                        next.getName(), wasLong ? next.longName : next.shortName
                    );
                }
            }
        }

        // Apply the arguments
        int rc = index;
        for(ConfigItem item : matchedItems) {
            rc = handleArgument(args, index, item, true);
        }

        return rc;
    }

    /**
     * Process a command line argument against a defined item
     * @param args command line argument array
     * @param index current argument index
     * @param item current command
     * @return last argument processed
     * @throws ConfigurationException
     */
    private int handleArgument(String[] args, int index, ConfigItem item, boolean wasLong) throws ConfigurationException {
        if(item.afterArg) {
            // Argument value is after the command option
            String value = args[index].substring(wasLong ? item.longName.length() + 2 : item.shortName.length() + 1);
            item.set(value, false, ConfigSource.COMMAND_LINE);
        } else if(item.argCount == -1) {
            // Eager argument consumer. Process arguments until the end or we
            // find another argument
            index++;

            while(index < args.length) {
                String arg = args[index];
                if(arg.startsWith("-"))
                    break;

                item.set(args[index++], false, ConfigSource.COMMAND_LINE);
            }
        } else if(item.argCount > 0) {
            // Consume the specified number of arguments. Throw an exception if
            // there are not enough arguments to satisfy the requirement
            if(item.argCount + index + 1 > args.length)
                throw new InsufficientArgumentsException("Argument {0} requested {1} elements but only {2} available",
                        args[index], item.argCount, args.length - index - 1);

            for(int i = 0; i < item.argCount; i++)
                item.set(args[index + i + 1], false, ConfigSource.COMMAND_LINE);

            index += item.argCount;
        } else {
            // No arguments requested, treat it as a boolean
            item.set("true", false, ConfigSource.COMMAND_LINE);
        }
        return index;
    }

    /**
     * Scan for all the configuration fragments, build the resulting configuration objects
     * and apply the defaults
     * @throws ConfigurationException
     */
    private void scanConfiguration() throws ConfigurationException {
        Reflections reflections = new Reflections();

        for(Class<?> clazz : reflections.getTypesAnnotatedWith(ConfigFragment.class)) {
            try {
                Object targetObject = clazz.newInstance();
                configFragments.put(clazz.getName(), targetObject);

                ConfigHelp classHelp = clazz.getAnnotation(ConfigHelp.class);
                if(classHelp != null)
                    helps.put(classHelp.priority(), classHelp);

                for(Field f : clazz.getDeclaredFields()) {
                    if(f.isAnnotationPresent(ConfigElement.class)) {
                        ConfigElement element = f.getAnnotation(ConfigElement.class);

                        ConfigItem item = new ConfigItem(element, targetObject, f);
                        items.put(clazz.getName(), item);
                    }

                    if(f.isAnnotationPresent(ConfigHelp.class)) {
                        ConfigHelp help = f.getAnnotation(ConfigHelp.class);
                        helps.put(classHelp != null ? classHelp.priority() : help.priority(), help);
                    }
                }
            } catch(IllegalAccessException | InstantiationException ex) {
                throw new ConfigurationException(ex, "Exception creating configuration fragment {0}. Object must have a no-arg constructor", clazz.getName());
            }
        }
    }

    /**
     * Read all the configuration files lowest precedent to highest
     */
    private void processConfigFiles(String configName) {
        this.configName = configName;

        // Read the Machine Specific configuration file
        // Windows...
        if(System.getenv().containsKey("PUBLIC")) {
            Path path = Paths.get(System.getenv().get("PUBLIC"), "." + configName);
            processConfigFile(path, ConfigSource.SYSTEM_CONFIG);
        }
        // *nix...
        {
            Path path = Paths.get("/user/local/share/." + configName);
            processConfigFile(path, ConfigSource.INSTANCE_CONFIG);

            path = Paths.get("/user/share/." + configName);
            processConfigFile(path, ConfigSource.SYSTEM_CONFIG);
        }

        // Read the User Specific configuration file
        // Windows...
        if(System.getenv().containsKey("USERPROFILE")) {
            Path path = Paths.get(System.getenv().get("USERPROFILE"), "." + configName);
            processConfigFile(path, ConfigSource.USER_CONFIG);
        }
        // *nix...
        {
            Path path = Paths.get("~/.config/." + configName);
            processConfigFile(path, ConfigSource.USER_CONFIG);
        }

        // Read the configuration settings from the directory the application was launched from
        Path path = Paths.get(System.getProperty("user.dir"), "." + configName);
        processConfigFile(path, ConfigSource.INSTANCE_CONFIG);
    }

    /**
     * Read an individual configuration file
     *
     * @param path
     * @param properties
     */
    private void processConfigFile(Path path, ConfigSource source) {
        try (InputStream in = Files.newInputStream(path)) {
            Properties prop = new Properties();
            prop.load(in);

            prop.forEach((key, value) -> {
                items.values().stream().filter(e -> { return e.configName.equals(key); }).forEach(item -> {
                    item.set((String)value, true, source);
                });
            });
        } catch(IOException ex) {
            //LOG.log(Level.WARNING, "Exception opening {0}", path.toString());
        }
    }

    /**
     * Get the configuration instance for a given fragment
     *
     * @param <T>
     * @param clazz the class type of the configuration fragment
     * @return The configuration instance or null if the configuration system is
     * is not initialized or if the instance is not found
     */
    public static <T> T get(Class<T> clazz) {
        if(!INSTANCE.processed) {
            LOG.log(Level.WARNING, "Configuration access prior to initialization");
            return null;
        }

        return clazz.cast(INSTANCE.configFragments.get(clazz.getName()));
    }

    /**
     * Set a configuration field as modified
     *
     * @param clazz
     * @param name
     */
    public static void setModified(Class<?> clazz, String name) {
        if(!INSTANCE.items.containsKey(clazz.getName()))
            LOG.log(Level.WARNING, "Supplied class {0} not managed", clazz.getName());

        for(ConfigItem item : INSTANCE.items.get(clazz.getName())) {
            if(item.isName(name)) {
                item.modified = true;
                break;
            }
        }
    }

    public static boolean save(boolean instanceConfig) {
        Properties properties = new Properties();
        for(ConfigItem item : INSTANCE.items.values()) {
            if(item.modified && !item.configName.isEmpty())
                properties.setProperty(item.configName, item.get());
        }

        if(instanceConfig) {
            // Write the configuration settings from the directory the application was launched from
            Path path = Paths.get(System.getProperty("user.dir"), "." + INSTANCE.configName);
            return save(path, properties);
        } else {
            // Write the User Specific configuration file
            // Windows...
            if(System.getenv().containsKey("USERPROFILE")) {
                Path path = Paths.get(System.getenv().get("USERPROFILE"), "." + INSTANCE.configName);
                if(save(path, properties))
                    return true;
            }
            // *nix...
            {
                Path path = Paths.get("~/.config/." + INSTANCE.configName);
                if(save(path, properties))
                    return true;
            }
        }

        return false;
    }

    private static boolean save(Path path, Properties properties) {
        try {
            File file = path.toFile();
            if(!file.exists())
                file.createNewFile();

            if(file.canWrite()) {
                try (OutputStream out = Files.newOutputStream(path)) {
                    properties.store(out, "");
                    return true;
                }
            }
        } catch(Exception ex) {}

        return false;
    }

    /**
     * Get the unhandled arguments
     *
     * Unhandled arguments are those that are not captured by any `@ConfigElement`
     *
     * @return List of arguments that were not handled by any command line switches
     */
    public static List<String> getUnhandled() {
        if(!INSTANCE.processed) {
            LOG.log(Level.WARNING, "Configuration access prior to initialization");
            return Collections.emptyList();
        }

        return INSTANCE.unhandledArguments;
    }

    /**
     * Print the Configuration Fragment Help strings in Priority order (1 = highest)
     *
     * @see ConfigHelp
     *
     * The order of help fragments at the same priority level is undefined
     *
     * @param out PrintStream to write to (commonly System.out)
     */
    public static void printHelp(PrintStream out) {
        if(!INSTANCE.processed) {
            LOG.log(Level.WARNING, "Configuration access prior to initialization");
            return;
        }

        INSTANCE.helps.entries().stream().forEach((hp) -> {
            for(String h : hp.getValue().value())
                out.print(h);

            out.println();
        });
    }

    /**
     * Determine if the JVM debug settings are active
     *
     * @return true if JVM debug mode is enabled
     */
    public static boolean isDebug() {
        String args = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
        return args.contains("debug") || args.contains("jdwp");
    }
}
