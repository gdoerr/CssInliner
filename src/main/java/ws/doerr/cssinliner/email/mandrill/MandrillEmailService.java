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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonNode;
import ws.doerr.cssinliner.email.EmailServiceProvider;
import ws.doerr.httpserver.Server;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Helper;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import ws.doerr.configuration.Configuration;

/**
 *
 * @author greg
 */
public class MandrillEmailService implements EmailServiceProvider {
    private static final Logger LOG = Logger.getLogger(MandrillEmailService.class.getName());

    private static final String MANDRILL_URL = "https://mandrillapp.com/api/1.0/";
    private static final String MANDRILL_UPDATETEMPLATE = "templates/update.json";
    private static final String MANDRILL_ADDTEMPLATE = "templates/add.json";
    private static final String MANDRILL_TEMPLATEINFO = "templates/info.json";

    private static final String META_TEMPLATE_NAME = "mandrill-template";
    private static final String META_LABELS = "mandrill-labels";

    private final WebTarget target;
    private final MandrillConfig config;
    private final SimpleDateFormat formatter;

    public MandrillEmailService() {
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        config = Configuration.get(MandrillConfig.class);
        Client client = ClientBuilder.newClient();
        target = client.target(MANDRILL_URL);
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

    private static final Map<String, Helper<?>> HELPERS = new HashMap<>();
    static {
        HELPERS.put("if", new MandrillIf());
    }

    @Override
    public Map<String, Helper<?>> getHelpers() {
        return HELPERS;
    }

    public boolean isChanged(String body, Map<String, String> meta, String templateNamePrefix) throws Exception {
        try {
            String templateName = templateNamePrefix + meta.get(META_TEMPLATE_NAME);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the hash from the current document
            String hash = DatatypeConverter.printHexBinary(digest.digest(body.getBytes("UTF-8")));
            String template = getTemplate(templateName);

            if(template == null) {
                return true;
            } else {
                String templateHash = DatatypeConverter.printHexBinary(digest.digest(template.getBytes("UTF-8")));
                return templateHash.equals(hash);
            }
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, "Exception communicating with Mandrill", ex);
            throw ex;
        } catch(NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, "Exception getting SHA-256 digest instance", ex);
            throw ex;
        }
    }

    @Override
    public PublishStatus publish(String title, String body, Map<String, String> meta, String templateNamePrefix) throws Exception {
        try {
            String templateName = templateNamePrefix + meta.get(META_TEMPLATE_NAME);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the hash from the current document
            String hash = DatatypeConverter.printHexBinary(digest.digest(body.getBytes("UTF-8")));
            String template = getTemplate(templateName);

            String[] labels = new String[]{};
            if(meta.containsKey(META_LABELS))
                labels = meta.get(META_LABELS).split(",");

            if(template == null) {
                addTemplate(templateName, title, body, labels);
                return PublishStatus.ADDED;
            } else {
                String templateHash = DatatypeConverter.printHexBinary(digest.digest(template.getBytes("UTF-8")));
                if(templateHash.equals(hash))
                    return PublishStatus.NO_CHANGE;

                updateTemplate(templateName, title, body, labels);
                return PublishStatus.UPDATED;
            }

        } catch(IOException ex) {
            LOG.log(Level.SEVERE, "Exception communicating with Mandrill", ex);
            throw ex;
        } catch(NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, "Exception getting SHA-256 digest instance", ex);
            throw ex;
        }
    }

    /**
     * Json Utility Class - Mandrill Template Info Request
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class MandrillInfoRequest {
        String key;
        String name;
    }

    private String getTemplate(String name) throws IOException {
        MandrillInfoRequest request = new MandrillInfoRequest();
        request.key = config.mandrillKey;
        request.name = name;

        Response rsp = target
                .path(MANDRILL_TEMPLATEINFO)
                .request()
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

        if(rsp.getStatus() == 200) {
            String json = rsp.readEntity(String.class);
            JsonNode response = Server.getMapper().readTree(json);

            return response.get("code").asText();
        } else
            return null;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class MandrillAddRequest {
        String key;
        String name;
        String subject;
        String code;
        String[] labels;
        boolean publish;
    }

    private boolean addTemplate(String templateName, String subject, String html, String[] keywords) {
        MandrillAddRequest add = new MandrillAddRequest();
        add.key = config.mandrillKey;
        add.name = templateName;
        add.subject = subject;
        add.code = html;
        add.labels = keywords;
        add.publish = true;

        Response rsp = target
                .path(MANDRILL_ADDTEMPLATE)
                .request()
                .post(Entity.entity(add, MediaType.APPLICATION_JSON_TYPE));

        if(rsp.getStatus() == 200)
            return true;
        else {
            LOG.log(Level.INFO, "Mandrill Error rc = {0}", rsp.getStatusInfo().getReasonPhrase());
            return false;
        }
    }

    private boolean updateTemplate(String templateName, String subject, String html, String[] keywords) {
        MandrillAddRequest add = new MandrillAddRequest();
        add.key = config.mandrillKey;
        add.name = templateName;
        add.subject = subject;
        add.code = html;
        add.labels = keywords;
        add.publish = true;

        Response rsp = target
                .path(MANDRILL_UPDATETEMPLATE)
                .request()
                .post(Entity.entity(add, MediaType.APPLICATION_JSON_TYPE));

        if(rsp.getStatus() == 200)
            return true;
        else {
            LOG.log(Level.INFO, "Mandrill Error rc = {0}", rsp.getStatusInfo().getReasonPhrase());
            return false;
        }
    }
}
