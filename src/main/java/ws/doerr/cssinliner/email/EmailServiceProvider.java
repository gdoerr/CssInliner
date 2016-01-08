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
package ws.doerr.cssinliner.email;

import com.github.jknack.handlebars.Helper;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *  Abstract Email Service
 */
public interface EmailServiceProvider {
    /**
     * Send an email using an ESP
     * @param to list of email addresses to send the emails to
     * @param title title of the email
     * @param content html formatted email body
     * @return
     */
    String sendEmail(List<String> to, String title, String content);

    /**
     * Get the list of Handlebars Helpers specific to the ESP
     *
     * @return
     */
    default Map<String, Helper<?>> getHelpers() { return Collections.emptyMap(); }

    /**
     * Check if a template has changed compared to the current document
     *
     * @param body
     * @param meta
     * @param templateNamePrefix
     * @return
     * @throws Exception
     */
    boolean isChanged(String body, Map<String, String> meta, String templateNamePrefix) throws Exception;

    /**
     * Status of a publish activity
     */
    public enum PublishStatus {
        NO_CHANGE,
        ADDED,
        UPDATED
    }

    /**
     * Publish an email template
     * @param title email title
     * @param body html formatted email body
     * @param meta ESP specific metadata tags
     * @param templateNamePrefix template name prefix
     *
     * @return
     * @throws Exception
     */
    PublishStatus publish(String title, String body, Map<String, String> meta, String templateNamePrefix) throws Exception;
}
