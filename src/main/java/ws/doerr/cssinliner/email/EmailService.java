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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ws.doerr.configuration.Configuration;

/**
 *
 * @author greg
 */
public class EmailService {
    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    private static EmailService instance;
    private static final Boolean lock = false;

    private EmailServiceProvider provider;

    private EmailService() {
        try {
            EmailConfiguration config = Configuration.get(EmailConfiguration.class);
            Class<?> providerClass = Class.forName(config.emailClassName);
            provider = (EmailServiceProvider) providerClass.newInstance();
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, "Exception instantiating Email Service Provider", ex);
        }
    }

    public static EmailService getInstance() {
        if(instance == null) {
            synchronized(lock) {
                if(instance == null)
                    instance = new EmailService();
            }
        }

        return instance;
    }

    public String sendTestEmail(List<String> emails, String title, String content) {
        return provider.sendEmail(emails, title, content);
    }

    public Map<String, Helper<?>> getHelpers() {
        return provider.getHelpers();
    }
}
