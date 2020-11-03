<%--
    basiclti - Building Block to provide support for Basic LTI
    Copyright (C) 2020  Stephen P Vickers

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Contact: stephen@spvsoftwareproducts.com
--%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@page contentType="text/html" pageEncoding="UTF-8"
        import="java.util.regex.Pattern,
                java.util.regex.Matcher,
                blackboard.platform.intl.JsResource,
                blackboard.util.XSSUtil,
                com.spvsoftwareproducts.blackboard.utils.B2Context"
        errorPage="../error.jsp"%>
<%
    B2Context b2Context = new B2Context(request);
    pageContext.setAttribute("bundle", b2Context.getResourceStrings());

    String htmlParamName = "embedHtml";
    String html = XSSUtil.getUnfilteredParameter(request, htmlParamName);
    if (html == null) {
        html = (String)request.getAttribute(htmlParamName);
    }
    html = JsResource.encode(html);
    html = Pattern.compile("</\\s*script\\s*>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll(Matcher.quoteReplacement("</scr\"+\"ipt>"));
    pageContext.setAttribute("html", html);
%>
<html>
  <head>
    <title>${bundle['page.course_tool.tool.title']}</title>
    <script language="javascript" type="text/javascript">
      //<![CDATA[
      function osc_doOnLoad() {
        var html = '${html}';
        if (window.opener) {
          if (window.opener.tinyMceWrapper && window.opener.tinyMceWrapper.setMashupData) {
            window.opener.tinyMceWrapper.setMashupData(html);
          }
          if (window.opener.currentTextArea) {
            window.opener.currentTextArea.value += html;
            window.opener.currentTextArea = null;
          }
          self.close();
        } else if (window.parent) {
          parent.postMessage({mceAction: 'closeContentSelectorDialog'}, origin);
          if (window.parent.tinymce && window.parent.tinymce.activeEditor) {
            editor = window.parent.tinymce.activeEditor;
            editor.execCommand('mceInsertContent', false, html);
          }
          parent.postMessage({mceAction: 'closeAddContentDialog'}, origin);
        }
      }

      window.onload = osc_doOnLoad;
      //]]>
    </script>
  </head>
  <body>
    <p>${bundle['page.course_tool.tool.redirect.label']}</p>
  </body>
</html>
