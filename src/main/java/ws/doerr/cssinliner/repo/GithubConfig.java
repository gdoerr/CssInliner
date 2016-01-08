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

import ws.doerr.configuration.ConfigElement;
import ws.doerr.configuration.ConfigFragment;
import ws.doerr.configuration.ConfigHelp;

@ConfigFragment
@ConfigHelp(value = {
    "GitHub Interface"
}, priority = 12)
public class GithubConfig {

    @ConfigHelp({"\t-ghu, --githubuser\t\t\tGitHub Account Username"})
    @ConfigElement(shortName = "ghu", longName = "githubuser", configName = "githubUsername")
    String username;

    @ConfigHelp({"\t-ghp, --githubpw\t\t\tGitHub Account Password"})
    @ConfigElement(shortName = "ghp", longName = "githubpw", configName = "githubPassword")
    String password;

    @ConfigHelp({"\t-gho, --githuboauth\t\t\tGitHub OAuth Token"})
    @ConfigElement(shortName = "gho", longName = "githuboauth", configName = "githubOAuth")
    String oauthtoken;

    @ConfigHelp({"\t-ghn, --githubowner\t\t\tGitHub Owner Name"})
    @ConfigElement(shortName = "ghn", longName = "githubowner", configName = "githubOwner")
    String owner;

    @ConfigHelp({"\t-ghr, --githubrepo\t\t\tGitHub Repository Name"})
    @ConfigElement(shortName = "ghr", longName = "githubrepo", configName = "githubRepository")
    String repo;

    @ConfigHelp({"\t-ghs, --githubsrc\t\t\tGitHub Source Path"})
    @ConfigElement(shortName = "ghs", longName = "githubsrc", configName = "githuSource")
    String sourcePath;
}
