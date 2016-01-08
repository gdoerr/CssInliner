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
package ws.doerr.cssinliner.repo;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import ws.doerr.configuration.Configuration;

/**
 *
 * @author greg
 */
public class Github {
    private static final Logger LOG = Logger.getLogger(Github.class.getName());

    private final GithubConfig config;

    public Github() {
        config = Configuration.get(GithubConfig.class);
    }

    public void getRepo() throws IOException {
        RepositoryService service = new RepositoryService();

        configClient(service.getClient());

        Repository repo = service.getRepository(getId());
    }

    private void configClient(GitHubClient client) {
        if(!config.username.isEmpty() && !config.password.isEmpty()) {
            client = new GitHubClient();
            client.setCredentials(config.username, config.password);
            return;
        }

        if(!config.oauthtoken.isEmpty()) {
            client = new GitHubClient();
            client.setOAuth2Token(config.oauthtoken);
            return;
        }

        LOG.log(Level.INFO, "No configuration for Github provided. Interface not available");
    }

    private RepositoryId getId() {
        return new RepositoryId(config.owner, config.repo);
    }
}
