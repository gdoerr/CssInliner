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
package ws.doerr.cssinliner.parser;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

/**
 * Jsoup Node Visitor to extract minified HTML
 *
 * @author Greg Doerr <greg@doerr.ws>
 */
class MinifyHtmlVisitor implements NodeVisitor {
    private final StringBuilder sb = new StringBuilder();

    String getHtml() {
        return sb.toString();
    }

    @Override
    public void head(Node node, int i) {
        if(node instanceof DocumentType) {
            DocumentType dt = (DocumentType) node;
            sb.append("<!DOCTYPE html PUBLIC");

            if(dt.hasAttr("publicId"))
                sb.append(' ').append(dt.attr("publicId"));

            if(dt.hasAttr("systemId"))
                sb.append(' ').append(dt.attr("systemId"));

            sb.append('>');
        } else if(node instanceof Element) {
            Element e = (Element) node;

            if("#root".equals(e.tagName()))
                return;

            sb.append('<')
                .append(e.tagName());

            sb.append(e.attributes().html());

            if(e.childNodes().isEmpty() && e.tag().isSelfClosing()) {
                if(e.tag().isEmpty())
                    sb.append('>');
                else
                    sb.append(" />");
            } else
                sb.append('>');
        } else if(node instanceof TextNode) {
            TextNode tn = (TextNode) node;

            sb.append(tn.getWholeText().replaceAll("\\s+", " "));
        } else if(node instanceof Comment) {
            Comment c = (Comment) node;

            sb.append("<!--")
                .append(c.getData().replaceAll("\\s+", " "))
                .append("-->");
        }
    }

    @Override
    public void tail(Node node, int i) {
        if(node instanceof Element) {
            Element e = (Element) node;

            if("#root".equals(e.tagName()))
                return;

            if(!(e.childNodes().isEmpty() && e.tag().isSelfClosing()))
                sb.append("</").append(e.tagName()).append('>');
        }
    }
}
