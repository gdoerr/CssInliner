/**
 * #Configuration Processor#
 *
 * **Annotation driven, modular configuration processor**
 *
 * Start with a POJO annotated with `@ConfigFragment`. Then create your
 * configuration elements and annotate them with `@ConfigElement`.
 *
 * ```java
 * {@literal @}ConfigFragment
 * class MainConfiguration {
 *     {@literal @}ConfigElement(
 *         shortName = "a",         // -a
 *         longName = "appname",    // --appname
 *         configName = "appname",  // value to grab from the configuration file(s)
 *         defaultValue = "My App"
 *     )
 *     String appName;
 * }
 * ```
 *
 * In your main, instantiate the Configuration object with your configuration file
 * name and the argument list `Configuration.process("example", args);`.
 *
 * Assuming no exceptions were thrown, grab your configuration data:
 *
 * ```java
 * MainConfiguration config = Configuration.get(MainConfiguration.class);
 * config.appName ...
 * ```
 *
 * Now invoke your program with either `-a "Better App"` or `--appname "Better App"`
 * and the values are set appropriately. If you don't pass the command line argument,
 * the default value will be set as specified by the `@ConfigElement`.
 *
 * You can have as many configuration fragments as you need. This allows you to modularly
 * configure your application without having to centralize the configuration in one
 * place. As an example, assume you had a pluggable class system
 *
 * **Configuration** also handles app specific, personal and system configuration files.
 * The file are formatted Java Properties style and are processed after the defaults
 * are applied but before the command line arguments.
 *
 * **Order of Precedence**
 *
 * - `@ConfigElement` defaults
 * - System configuration file (`/usr/local/share/.[name]`, `/usr/share/.[name]`, `$PUBLIC\.[name]`)
 * - Personal configuration file (`~/.config/.[name]`, `$USERPROFILE\.[name]`)
 * - Application configuration file (the directory the app was launched from)
 * - Command line arguments
 *
 * **{@literal @}ConfigElement**
 *
 * - shortName - text to match for the short command line switch (single -)
 * - longName - text to match for the long command line switch (double --)
 * - argCount - number of arguments, after this one, to capture for this field.
 * This works best when annotating a `Collection` field for `argCount` > 1
 * - afterArg - capture the remaining argument text, after the switch, as the field value.
 * This takes precedence over `argCount`. When using this option, you need to be careful
 * with collisions in the switch names since we have to use a `startsWith` match which can
 * make the matches a little ambiguous
 * - configName - text to match from the configuration files
 * - defaultValue - text to apply to the field as the default value
 *
 * **{@literal @}ConfigHelp**
 * Specifies help text for each configuration fragment.
 *
 * Apply this annotation along with the `@ConfigFragment` annotation to provide help
 * text for the options pertaining to the fragment. You can specify multiple strings
 * or a single long string, they are treated identically.
 *
 * You can also supply a `priority` for the help text to determine the rendering order.
 * The default priority if `5` with `1` being the highest priority and being rendered first.
 * Help Text with the same priority will be rendered in an indeterminate order.
 *
 * You can render the Help Text to a PrintWriter by calling `printHelp()`.
 */
package ws.doerr.configuration;
