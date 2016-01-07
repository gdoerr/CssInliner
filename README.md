# CssInliner
Css Inliner Tool for Html Email Templates

I was working on a customer project that required the sending of emails. I had worked with MailChimp previously and knew that
they had an email service, Mandrill, that supported templates and had recently added support for Handlebars to
support merging of variable content into the template.

While developing the templates, I was struck by the lack of available tooling to to simplify not only the process
of creating the templates but also for maintaining them long term. In order to simplify my life and to make
ongoing maintenance easier, I decided to create my own solition.

I identified a couple of key goals:
* *Template Support* - rather than duplicating the markup for email in every single email, support the use of
templates that contain the boilerplate markup
* *HTML Import* - have the ability to import canned sections of html, preferrably with parameters
* *CSS Inlining* - make coding and maintenance easier by supporting css styling. The tool should handle
the process to change the css classes to inline styles. This should be selective and still allow css styles
to be present in the final document for optimal rendering across different email renderers
* *Merged Preview* - provide a preview of the processed emails merged with sample data
* *Real time* - I didn't want to create an editor, I want to be able to use whatever editor I want
to edit the various document pieces

**CssInliner** provides the above functionallity through a single jar. The tool can be run in either batch mode
to process various templates into the final html email documents or interactively supporting live preview of the processed documents through a web browser.

# Syntax
### Templates
Using a syntax similar to Java Server Faces, you construct a template as a normal HTML document. This document would include your stylesheets along with any markup required to render the basic email layout.

In the template, mark areas where content will be inserted with `<ui:include section="section name" />`. When generating the final document, this tag will be replaced by the content provided by a specific email document.

The `<head>` section of the final document is created by merging the head section of the template with the head section of the email document. In the case of element conflicts, elements from the email document take precedence over the template.

```
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <link rel="stylesheet" href="fragments/normal styles.css"/>
        <link rel="stylesheet" href="fragments/inline styles.css" ui:inline />
    </head>
    <body>
        ... (body formatting)
        <ui:include section="header" />
        ... (more formatting)
        <ui:include section="content" />
        ... (final formatting)
    </body>
</html>
```
Notice from this sample the inclusion of two style sheets. The first one will be read and be included as css classes defined directly in the head of the final document. The second one will be read and have the resulting styles applied inline on the appropriate DOM elements. The stylesheet content will not be present in the final document. The stylesheets require no special formatting. I considered supporting advanced css formats like sass or less but given that all styles need to be inlined I believe the more verbose css style is more appropriate here.

### Email documents
Email templates are also normal html document formatted to reference the template and to identify the content that is to be inserted into the template.

```
<html ui:template="fragments/template.tmpl">
    <head>
        <title>My Email Title</title>
    </head>
    <body>
        <ui:section name="header">
            <strong>My Email</strong> Title
        </ui:section>

        <ui:section name="content">
            <p>This is the body of my email.</p>
        </ui:section>
    </body>
</html>
```

This document, when processed, will render a complete HTML document ready for publishing to pretty much any email service that supports templates.

### HTML Fragments
Often times when constructing emails, you will use common markup segments either multiple times in the same document and/or across multiple documents. Also, in order to support proper rendering, it's nice to be able encapsulate certain content like buttons that require unique syntax to support clients such as Outlook. This is what **HTML Fragments** are for.

Below is a simple fragment that can be imported into a document. It defines three parameters:
* `imageclass` - which is the class that should be applied to the img element
* `imageurl` - which is the url that should be applied to the img elements `src` attribute
* `content` - which is the content that should be included in the table element cell
```
<tr class="padding-top">
    <td class="image-container">
        <img width="64" height="64">
            <parameter name="imageclass" attr="class" />
            <parameter name="imageurl" attr="src" />
        </img>
    </td>
    <td>
        <parameter name="content" />
    </td>
</tr>
```
Notice that we support both content parameters as well as parameters that apply an attribute to their enclosing element. After processing, the `parameter` tags are removed from the document.

We can now go back to our original email document and include this fragment.
```
    <ui:section name="content">
        <link rel="fragment.inc">
            <parameter name="imageclass">avatar</parameter>
            <parameter name="imageurl">http://static.cdn.com/image.png</parameter>
            <parameter name="content">
                <p>This is the body of my email.</p>
            </parameter>
        </link>
    </ui:section>
```

# Build Instructions

CssInliner uses [Gradle](http://gradle.org) and [Webpack](https://webpack.github.io/) to build the application. Gradle handles the overall build
and webpack handles the html/javascript side for Interactive mode.

Dependencies
* Gradle 2.10 or later
* Node / Npm

Build Steps
* `gradle npmInstall` installs npm dependencies
* `gradle build` builds a normal jar
* `gradle fatjar` builds a jar with all dependencies


# Future Plans
* Prettier UI
* Extract the Inliner code to a separate repository
* Release my Gradle plugin for deploying templates to Mandrill
 * Publish the plugin to the Gradle plugin repo
* Fully support Mandrill handlebars [unique syntax items](https://mandrill.zendesk.com/hc/en-us/articles/205582537-Using-Handlebars-for-Dynamic-Content)
 * There are some funky syntax in Mandrill Handlebars that doesn't even make past the initial handlebars parse. Any [Java Handlebars](https://github.com/jknack/handlebars.java) experts willing to lend a hand?
* Add support for email platforms other than Mandrill
* Set up on CI service like **Travis CI**
* Publish artifacts to Maven Central

# Acknowledgements
* [Jsoup HTML Parser](https://github.com/jhy/jsoup)
* [CSS Parser](http://cssparser.sourceforge.net/)
* [Grizzly](https://grizzly.java.net/) Http Server / Websockets
* [Jersey](https://jersey.java.net/) REST API for real time viewer
* [Jackson](https://github.com/FasterXML/jackson) JSON formatting
* [Java Handlebars](https://github.com/jknack/handlebars.java)
* [Angular](https://angularjs.org/)
* [Angular Material](https://material.angularjs.org/latest/)
* [Moment](http://momentjs.com/)
* [Angular Moment](https://github.com/urish/angular-moment)

Please contact me if you're interested in contributing.
