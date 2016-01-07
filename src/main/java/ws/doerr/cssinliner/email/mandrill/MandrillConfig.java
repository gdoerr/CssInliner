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

import ws.doerr.configuration.ConfigElement;
import ws.doerr.configuration.ConfigFragment;
import ws.doerr.configuration.ConfigHelp;

/**
 * Mandrill Interface Configuration
 * @author greg
 */
@ConfigFragment
@ConfigHelp(value = {
    "Mandrill Interface"
}, priority = 11)
public class MandrillConfig {
    @ConfigHelp({"\t-mkey, --mandrillkey\t\tMandrill API Key"})
    @ConfigElement(shortName = "mk", longName = "mandrillkey", configName = "mandrillkey")
    String mandrillKey;

    @ConfigHelp({"\t-msub, --mandrillsub\t\tMandrill Subaccount"})
    @ConfigElement(shortName = "ms", longName = "mandrillsub", configName = "mandrillsub")
    String mandrillSubacct;

    @ConfigHelp({"\t-mfe, --mandrillfromemail\tMandrill From Email Address"})
    @ConfigElement(shortName = "mfe", longName = "mandrillfromemail", configName = "mandrillfromemail", defaultValue = "test@service.clearcontract.com")
    String mandrillFromEmail;

    @ConfigHelp({"\t-mfn, --mandrillfromname\tMandrill From Name\n"})
    @ConfigElement(shortName = "mfn", longName = "mandrillfromname", configName = "mandrillfromname", defaultValue = "Email Test Service")
    String mandrillFromName;
}
