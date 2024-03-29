<%--
    basiclti - Building Block to provide support for Basic LTI
    Copyright (C) 2018  Stephen P Vickers

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
        import="java.net.URLEncoder,
        blackboard.data.content.Content,
        blackboard.persist.content.ContentDbLoader,
        blackboard.persist.BbPersistenceManager,
        blackboard.persist.Id,
        blackboard.platform.persistence.PersistenceServiceFactory,
        org.oscelot.blackboard.lti.Utils"
        errorPage="error.jsp"%>
<%@taglib uri="/bbNG" prefix="bbNG"%>
<%@include file="lti_props.jsp" %>
<bbNG:includedPage entitlement="system.generic.VIEW">
  <%    String url = "";
      String courseId = URLEncoder.encode(b2Context.getRequestParameter("course_id", ""), "UTF-8");
      String contentId = URLEncoder.encode(b2Context.getRequestParameter("content_id", ""), "UTF-8");
      String groupId = URLEncoder.encode(b2Context.getRequestParameter("group_id", ""), "UTF-8");
      String tabId = URLEncoder.encode(b2Context.getRequestParameter(Constants.TAB_PARAMETER_NAME, ""), "UTF-8");
      String cTabId = URLEncoder.encode(b2Context.getRequestParameter(Constants.COURSE_TAB_PARAMETER_NAME, ""), "UTF-8");
      String sourcePage = b2Context.getRequestParameter(Constants.PAGE_PARAMETER_NAME, "");
      String mode = b2Context.getRequestParameter("mode", "");
      String toolName = tool.getName();
      boolean isIframe = b2Context.getRequestParameter("if", "").length() > 0;
      if (!isIframe) {
          if (contentId.length() > 0) {
              BbPersistenceManager bbPm = PersistenceServiceFactory.getInstance().getDbPersistenceManager();
              ContentDbLoader courseDocumentLoader = (ContentDbLoader) bbPm.getLoader(ContentDbLoader.TYPE);
              Id id = bbPm.generateId(Content.DATA_TYPE, contentId);
              Content content = courseDocumentLoader.loadById(id);
              if (tool.getPrefix() == null) {
                  toolName = content.getTitle();
              }
              if (!content.getIsFolder() || content.getIsLesson()) {
                  id = content.getParentId();
                  contentId = id.toExternalString();
              }
              String navItem = "cp_content_quickdisplay";
              if (B2Context.getEditMode()) {
                  navItem = "cp_content_quickedit";
              }
              url = b2Context.getNavigationItem(navItem).getHref();
              url = url.replace("@X@course.pk_string@X@", courseId);
              url = url.replace("@X@content.pk_string@X@", contentId);
          } else if (tabId.length() > 0) {
              url = "/webapps/portal/execute/tabs/tabAction?" + Constants.TAB_PARAMETER_NAME + "=" + tabId;
          } else if ((moduleId != null) && (moduleId.length() > 0)) {
              url = "/webapps/blackboard/execute/modulepage/view?course_id=" + courseId + "&"
                      + Constants.COURSE_TAB_PARAMETER_NAME + "=" + cTabId;
          } else if (sourcePage.equals(Constants.TOOL_USERTOOL)) {
              url = b2Context.getRequestParameter("returnUrl", "");
          } else if (sourcePage.equals(Constants.COURSE_TOOLS_PAGE)) {
              url = b2Context.getPath() + "course/" + "tools.jsp?" + Utils.getQuery(request);
          } else if (sourcePage.equals(Constants.TOOLS_PAGE)) {
              url = b2Context.getPath() + "tools.jsp?" + Utils.getQuery(request);
          } else if (mode.length() > 0) {
              url = b2Context.getNavigationItem("course_top").getHref();
              url = url.replace("@X@course.pk_string@X@", courseId);
          } else if (groupId.length() > 0) {
              url = b2Context.getNavigationItem("agroup").getHref();
              url = url.replace("@X@course.pk_string@X@", courseId);
              url = url.replace("@X@group.pk_string@X@", groupId);
          } else {
              url = b2Context.getNavigationItem("course_tools_area").getHref();
              url = url.replace("@X@course.pk_string@X@", courseId);
          }
          url = b2Context.setReceiptOptions(url, String.format(b2Context.getResourceString("page.new.window"), toolName), null);
          pageContext.setAttribute("url", url);
      }
  %>
  <html>
    <head>
      <%
          if (params != null) {
      %>
      <script language="javascript" type="text/javascript">
        //<![CDATA[
        function osc_doOnLoad() {
        <%
            if (!isIframe) {
        %>
          if (window.opener) {
        <%
            if (!B2Context.getIsVersion(9, 1, 201404)) {
        %>
            window.opener.location.href = '${url}';
        <%
        } else {
        %>
            if (window.opener.top.location.href.indexOf('/ultra/') >= 0) {
              window.opener.location.href = '${url}';
            } else {
              window.opener.parent.location.href = '${url}';
            }
        <%
            }
        %>
          }
        <%
            }
        %>
          document.forms[0].submit();
        }
        window.onload = osc_doOnLoad;
        //]]>
      </script>
      <%
          }
      %>
    </head>
    <body>
      <%
          if (params != null) {
      %>
      <p>${bundle['page.course_tool.tool.redirect.label']}</p>
      <form action="<%=toolURL%>" method="post">
        <%
            for (Map.Entry<String, String> entry : params) {
                String name = Utils.htmlSpecialChars(entry.getKey());
                String value = Utils.htmlSpecialChars(entry.getValue());
        %>
        <input type="hidden" name="<%=name%>" value="<%=value%>">
        <%
            }
        %>
      </form>
      <%
      } else {
      %>
      <p>${bundle['page.error.introduction']}</p>
      <%
          }
      %>
    </body>
  </html>
</bbNG:includedPage>
