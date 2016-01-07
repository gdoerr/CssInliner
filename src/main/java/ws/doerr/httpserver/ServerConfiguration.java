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
package ws.doerr.httpserver;

import java.net.URI;
import ws.doerr.configuration.ConfigElement;
import ws.doerr.configuration.ConfigFragment;
import ws.doerr.configuration.ConfigHelp;

/**
 *
 * @author greg
 */
@ConfigFragment
@ConfigHelp({
    "Interactive Server Configuration\n",
    "\t-h, --host\t\tHost name to start server on (defaults to localhost)\n",
    "\t-p, --port\t\tPort to start server on (defaults to 8081)\n"
})
public class ServerConfiguration {
    @ConfigElement(shortName = "p", longName = "port", configName = "port", defaultValue = "8081")
    int port;

    @ConfigElement(shortName = "hn", longName = "hostname", configName = "output", defaultValue = "localhost")
    String hostname;

    public URI getServerUri() {
        StringBuilder sb = new StringBuilder("http://");
        sb.append(hostname);
        if(port != 80)
            sb.append(':').append(port);

        sb.append("/api");

        return URI.create(sb.toString());
    }
}
