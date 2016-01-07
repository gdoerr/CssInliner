# CssInliner
Css Inliner Tool for Html Email Templates

One of the top recommendations when writing email templates that need to work
across many viewer is to inline your CSS styles.

While there are various tools that support various parts of this process, I have
not found any that give me the expressiveness I need while allowing me to be as
productive as possible.

Css Inliner is an attempt to create tooling that allows developers, creative and
devops users to collaborate not only on the initial design of the emails but
also their ongoing maintenance.

This tooling provides the following:
* a CssInliner tool that can be run from the command line
* a Gradle plugin for running the inliner and publishing the content to Mandrill
* an interactive development tool that provides real time, browser based previews
of your content
* Handlebars syntax merger to support the preview process

CssInliner supports some unique syntax items to support the creation of flexible
templates
* Selective inliner for css
 * `<link rel="stylesheet" href="local path to css file" />` Read a local CSS file
 and includes the content into the merged file. No CSS style inlining is performed
  * `<link rel="stylesheet" href="local path to css file" ui:inline />` parse
  a CSS file and apply to the DOM elements
* `<link rel="import" href="local path to html fragment" />` include an html
  fragment into your templates
 * Parameterized - build reusable html fragments such as buttons and other UI elements
 * `<parameter name="label">This is my Label</parameter>` as a child of the link
 element defines **label** as a parameter to the included fragment with a value
 of **This is my Label**
  * In a fragment `<parameter name="label" />` will be replaced by the text **This is my Label**
  * You can also use `<parameter name="bold" attr="class" />` as a child of an
   element that you want to set an attribute on. This is useful for passing URLs
   into a template to set the `href` of an `<a>` element although use is
   not restricted to that.
* Templates - borrowing from Java Server Pages, **CssInliner** supports
 templating which, combined with parameterized includes, allows you to build
 consistent, functional templates.
  * For your template, create a normal html document and add your include elements
  like `<ui:include section="content" />`
  * To use the template, reference the template file in your email source by
  adding a `ui:template` attribute on the `html` element like
  `<html ui:template="common/template.html">`
  * The `<head>` sections from both documents are combined into the resulting document
  * Provide content for the include sections by inserting
  `<ui:section name="content"></ui:section>` elements. The contents of this tag
  will be added to the node.
  * Imported content (`<link rel="import">`) is supported in the template and the content sections


# Build Instructions

Dependencies
* Gradle 2.10 or later
* Node / Npm

Build Steps
* gradle npmInstall
* gradle build - builds a normal jar
* gradle fatjar - builds a jar with all dependencies
