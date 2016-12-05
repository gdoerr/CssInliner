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
package ws.doerr.cssinliner.email;

import java.util.HashMap;
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
    private static final Boolean LOCK = false;

    private EmailServiceProvider provider;
    private final Map<String, String> providers = new HashMap<>();
    private String providerName;

    private EmailService() {
        Configuration.getReflections().getSubTypesOf(EmailServiceProvider.class).forEach(clazz -> {
            providers.put(clazz.getName(), clazz.getSimpleName());
        });

        EmailConfiguration config = Configuration.get(EmailConfiguration.class);
        providerName = config.emailClassName;
        setProvider();
    }

    public static EmailService getInstance() {
        if(instance == null) {
            synchronized(LOCK) {
                if(instance == null)
                    instance = new EmailService();
            }
        }

        return instance;
    }

    public static EmailServiceProvider get() {
        return getInstance().provider;
    }

    public static Map<String, String> getAvailable() {
        return getInstance().providers;
    }

    public static void setProvider(String className) {
        getInstance().providerName = className;
        getInstance().setProvider();
    }

    public static String getProvider() {
        return getInstance().providerName;
    }

    private void setProvider() {
        try {
            Class<?> providerClass = Class.forName(providerName);
            provider = (EmailServiceProvider) providerClass.newInstance();
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, "Exception instantiating Email Service Provider", ex);
        }
    }
}
