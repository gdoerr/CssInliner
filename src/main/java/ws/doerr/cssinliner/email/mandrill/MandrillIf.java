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
package ws.doerr.cssinliner.email.mandrill;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Options.Buffer;
import java.io.IOException;

/**
 *
 * @author greg
 */
public class MandrillIf implements Helper<Object> {

    @Override
    public CharSequence apply(Object context, Options options) throws IOException {
        Buffer buffer = options.buffer();

        if(options.params.length == 0) {
            if (options.isFalsy(context)) {
                buffer.append(options.inverse());
            } else {
                buffer.append(options.fn());
            }
        } else if(options.params.length == 2) {
            switch((String)options.params[0]) {
                case "==":
                case "===":
                    if(context.equals(options.param(1)))
                        buffer.append(options.fn());
                    else
                        buffer.append(options.inverse());

                case "!=":
                case "!==":
                    if(context.equals(options.param(1)))
                        buffer.append(options.inverse());
                    else
                        buffer.append(options.fn());

            }
        }
        return buffer;
    }

}
