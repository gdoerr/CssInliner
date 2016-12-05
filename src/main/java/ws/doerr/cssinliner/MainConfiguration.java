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

import ws.doerr.configuration.ConfigElement;
import ws.doerr.configuration.ConfigFragment;
import ws.doerr.configuration.ConfigHelp;

/**
 *
 * @author greg
 */
@ConfigFragment
@ConfigHelp( value = {
    "Common Configuration Values\n",
    "\t-i, --input\t\t\t\tSpecify the input directory. Application looks for .html files\n\t\t\t\t\t\tin this folder\n",
    "\t-o, --output\t\t\t\tSpecify the output directory. Completed files are written to\n\t\t\t\t\t\tthis folder with the same name as the source\n",
    "\t-d, --data\t\t\t\tSpecify the data directory for interactive mode. Grabs files\n\t\t\t\t\t\tmatching [name.html].json from this folder for handlebars merge.\n"
}, priority = 1)
public class MainConfiguration {

    @ConfigElement(shortName = "h", longName = "help", argCount = 0)
    boolean printHelp;

    @ConfigElement(shortName = "cl", longName = "cmdline", argCount = 0)
    boolean inline;

    @ConfigElement(shortName = "i", longName = "input", configName = "input")
    String inputPath = "";

    @ConfigElement(shortName = "o", longName = "output", configName = "output")
    String outputPath = "";

    @ConfigElement(shortName = "d", longName = "data", configName = "data")
    String dataPath = "";
}
