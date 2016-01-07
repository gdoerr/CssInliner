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
package ws.doerr.cssinliner.server;

import ws.doerr.cssinliner.email.EmailService;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author greg
 */
@javax.ws.rs.Path("/")
public class BaseApi {
    private static final Logger LOG = Logger.getLogger(BaseApi.class.getName());

    @javax.ws.rs.Path("files")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFiles() {
        return Response.ok(InlinerApp.getInstance().getSources()).build();
    }

    @javax.ws.rs.Path("files/{id}")
    @GET
    public Response getFile(@PathParam("id") UUID id) {
        SourceInstance instance = InlinerApp.getInstance().getSource(id);
        if(instance != null) {
            File file = instance.getMerged().toFile();

            try  {
                return Response.ok(new FileInputStream(file), Files.probeContentType(file.toPath())).build();
            } catch(Exception ex) {
                LOG.log(Level.WARNING, "", ex);
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @javax.ws.rs.Path("files/{id}/sendtest")
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendTestEmail(@PathParam("id") UUID id,
            @QueryParam("email") List<String> emails) {
        SourceInstance instance = InlinerApp.getInstance().getSource(id);
        if(instance != null && emails != null && !emails.isEmpty()) {
            try {
                byte[] html = Files.readAllBytes(instance.getMerged());
                String rc = EmailService.getInstance()
                        .sendTestEmail(emails, instance.getTitle(), new String(html, Charsets.UTF_8));
                return Response.ok(rc).build();
            } catch(Exception ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex).build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
