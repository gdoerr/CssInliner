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

import ws.doerr.cssinliner.email.EmailServiceProvider;
import ws.doerr.httpserver.Server;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Helper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import ws.doerr.configuration.Configuration;

/**
 *
 * @author greg
 */
public class MandrillEmailService implements EmailServiceProvider {
    private static final Logger LOG = Logger.getLogger(MandrillEmailService.class.getName());

    private static final String MANDRILL_URL = "https://mandrillapp.com/api/1.0/";

    private WebTarget target;

    private final MandrillConfig config;

    public MandrillEmailService() {
        config = Configuration.get(MandrillConfig.class);
    }

    @Override
    public String sendEmail(List<String> emails, String title, String content) {
        ObjectNode data = Server.getMapper().createObjectNode();
        data.put("key", config.mandrillKey);

        ObjectNode message = data.putObject("message");
        message.put("html", content);
        message.put("subject", title);
        message.put("from_email", config.mandrillFromEmail);
        message.put("from_name", config.mandrillFromName);

        message.put("track_opens", false);
        message.put("track_clicks", false);

        message.put("subaccount", config.mandrillSubacct);

        ArrayNode to = message.putArray("to");
        emails.forEach(email -> addTo(to, email));

        message.put("merge", false);

        Response rsp = target
                .path("/messages/send.json")
                .request()
                .post(Entity.entity(data, MediaType.APPLICATION_JSON_TYPE));

        String rc = rsp.readEntity(String.class);
        LOG.log(Level.INFO, "Send = {0}", rc);

        return rc;
    }

    private void addTo(ArrayNode to, String email) {
        ObjectNode to1 = to.addObject();
        to1.put("email", email);
        to1.put("name", "Fred Flintstone");
        to1.put("type", "to");
    }

    @Override
    public void initialize() {
        Client client = ClientBuilder.newClient();
        target = client.target(MANDRILL_URL);
    }

    private static final Map<String, Helper<?>> HELPERS = new HashMap<>();
    static {
        HELPERS.put("if", new MandrillIf());
    }

    @Override
    public Map<String, Helper<?>> getHelpers() {
        return HELPERS;
    }
}
