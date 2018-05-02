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
        blackboard.data.user.User,
        blackboard.portal.data.Module,
        blackboard.portal.persist.ModuleDbLoader,
        blackboard.persist.Id,
        blackboard.platform.persistence.PersistenceServiceFactory,
        blackboard.persist.BbPersistenceManager,
        blackboard.persist.content.ContentDbLoader,
        blackboard.data.content.Content,
        blackboard.persist.KeyNotFoundException,
        blackboard.persist.PersistenceException,
        com.spvsoftwareproducts.blackboard.utils.B2Context,
        org.oscelot.blackboard.lti.Constants,
        org.oscelot.blackboard.lti.Utils,
        org.oscelot.blackboard.lti.Tool"
        errorPage="error.jsp"%>
<%@taglib uri="/bbNG" prefix="bbNG"%>
<%
    String formName = "page.course_tool.splash";

    String moduleId = Utils.checkForModule(request);
    B2Context b2Context = new B2Context(request);
    Utils.checkInheritSettings(b2Context);
    Utils.checkCourse(b2Context);
    String courseId = b2Context.getRequestParameter("course_id", "");
    if (courseId.equals("@X@course.pk_string@X@")) {
        courseId = "";
    }
    String contentId = b2Context.getRequestParameter("content_id", "");
    if (contentId.equals("@X@content.pk_string@X@")) {
        contentId = "";
    }
    String groupId = b2Context.getRequestParameter("group_id", "");
    if (groupId.equals("@X@group.pk_string@X@")) {
        groupId = "";
    }
    String toolId = b2Context.getRequestParameter(Constants.TOOL_ID,
            b2Context.getSetting(false, true, Constants.TOOL_PARAMETER_PREFIX + "." + Constants.TOOL_ID, ""));
    String sourcePage = b2Context.getRequestParameter(Constants.PAGE_PARAMETER_NAME, "");
    Tool tool = Utils.getTool(b2Context, toolId);
    boolean allowLocal = b2Context.getSetting(Constants.TOOL_DELEGATE, Constants.DATA_FALSE).equals(Constants.DATA_TRUE);
    String actionUrl = "";
    if (courseId.length() > 0) {
        actionUrl = "course_id=" + courseId + "&";
    }
    if (contentId.length() > 0) {
        actionUrl += "content_id=" + contentId + "&";
    }
    if (groupId.length() > 0) {
        actionUrl += "group_id=" + groupId + "&";
    }
    String idParam = Constants.TOOL_ID + "=" + toolId;
    if (moduleId != null) {
        if (courseId.length() <= 0) {
            idParam = Constants.TOOL_MODULE + "=" + moduleId + "&"
                    + Constants.TAB_PARAMETER_NAME + "=" + b2Context.getRequestParameter(Constants.TAB_PARAMETER_NAME, "");
        } else {
            idParam = Constants.TOOL_MODULE + "=" + moduleId + "&"
                    + Constants.COURSE_TAB_PARAMETER_NAME + "=" + b2Context.getRequestParameter(Constants.COURSE_TAB_PARAMETER_NAME, "");
        }
        if (b2Context.getRequestParameter("n", "").length() > 0) {
            idParam += "&n=" + b2Context.getRequestParameter("n", "");
        }
    } else if (courseId.length() <= 0) {
        String url = b2Context.getRequestParameter("returnUrl", "");
        int pos = url.indexOf("?");
        if (pos >= 0) {
            url = url.substring(pos + 1);
            String[] params = url.split("&");
            String[] param;
            for (int i = 0; i < params.length; i++) {
                param = params[i].split("=");
                if ((param.length >= 2) && (param[0].equals(Constants.TAB_PARAMETER_NAME))) {
                    idParam += "&" + Constants.TAB_PARAMETER_NAME + "=" + param[1];
                    break;
                }
            }
        }
    }
    actionUrl = "tool.jsp?" + actionUrl + idParam + "&" + Constants.ACTION + "=redirect";
    pageContext.setAttribute("bundle", b2Context.getResourceStrings());
    pageContext.setAttribute("imageFiles", Constants.IMAGE_FILE);
    pageContext.setAttribute("imageAlt", Constants.IMAGE_ALT_RESOURCE);
    pageContext.setAttribute("actionUrl", actionUrl);
    pageContext.setAttribute("tool", tool);
    pageContext.setAttribute("courseId", courseId);
    if (moduleId != null) {
        pageContext.setAttribute("target", "_parent");
    } else {
        pageContext.setAttribute("target", "");
    }
    pageContext.setAttribute("iconUrl", "icon.jsp?course_id=" + courseId + "&amp;content_id=" + contentId + "&amp;group_id=" + groupId);
%>
<bbNG:genericPage title="${bundle['page.course_tool.splash.pagetitle']}" entitlement="system.generic.VIEW" showBreadcrumbBar="false">
  <bbNG:pageHeader instructions="${bundle['page.settings.instructions']}">
    <bbNG:pageTitleBar iconUrl="${iconUrl}" showTitleBar="true" title="${bundle['page.course_tool.splash.title']} ${tool.name}"/>
  </bbNG:pageHeader>
  <bbNG:form action="${actionUrl}" method="post" onsubmit="return validateForm();" isSecure="true" nonceId="<%=formName%>" target="${target}">
    <bbNG:dataCollection markUnsavedChanges="true" showSubmitButtons="true">
      <%
          if (tool.getSplash().equals("true") && (tool.getSplashText().length() > 0)) {
      %>
      <bbNG:step hideNumber="false" title="${bundle['page.course_tool.splash.step1.title']}">
        ${tool.splashText}
      </bbNG:step>
      <%
          }
      %>
      <bbNG:step hideNumber="false" title="${bundle['page.course_tool.splash.step2.title']}">
        <bbNG:dataElement isRequired="false" label="${bundle['page.course_tool.splash.step2.userid.label']}">
          <%
              String userIdSetting = tool.getSendUserId();
              if (userIdSetting.equals(Constants.DATA_MANDATORY)) {
          %>
          <img src="${imageFiles['true']}" alt="${bundle[imageAlt['true']]}" title="${bundle[imageAlt['true']]}" />
          <%
          } else if (userIdSetting.equals(Constants.DATA_NOTUSED)) {
          %>
          <img src="${imageFiles['false']}" alt="${bundle[imageAlt['false']]}" title="${bundle[imageAlt['false']]}" />
          <%
          } else {
          %>
          <bbNG:checkboxElement isSelected="${tool.userUserId}" name="<%=Constants.TOOL_USERID%>" value="true" helpText="${bundle['page.course_tool.splash.step2.userid.instructions']}" />
          <%
              }
          %>
        </bbNG:dataElement>
        <bbNG:dataElement isRequired="false" label="${bundle['page.course_tool.splash.step2.username.label']}">
          <%
              String usernameSetting = tool.getSendUsername();
              if (usernameSetting.equals(Constants.DATA_MANDATORY)) {
          %>
          <img src="${imageFiles['true']}" alt="${bundle[imageAlt['true']]}" title="${bundle[imageAlt['true']]}" />
          <%
          } else if (usernameSetting.equals(Constants.DATA_NOTUSED)) {
          %>
          <img src="${imageFiles['false']}" alt="${bundle[imageAlt['false']]}" title="${bundle[imageAlt['false']]}" />
          <%
          } else {
          %>
          <bbNG:checkboxElement isSelected="${tool.userUsername}" name="<%=Constants.TOOL_USERNAME%>" value="true" helpText="${bundle['page.course_tool.splash.step2.username.instructions']}" />
          <%
              }
          %>
        </bbNG:dataElement>
        <bbNG:dataElement isRequired="false" label="${bundle['page.course_tool.splash.step2.email.label']}">
          <%
              String emailSetting = tool.getSendEmail();
              if (emailSetting.equals(Constants.DATA_MANDATORY)) {
          %>
          <img src="${imageFiles['true']}" alt="${bundle[imageAlt['true']]}" title="${bundle[imageAlt['true']]}" />
          <%
          } else if (emailSetting.equals(Constants.DATA_NOTUSED)) {
          %>
          <img src="${imageFiles['false']}" alt="${bundle[imageAlt['false']]}" title="${bundle[imageAlt['false']]}" />
          <%
          } else {
          %>
          <bbNG:checkboxElement isSelected="${tool.userEmail}" name="<%=Constants.TOOL_EMAIL%>" value="true" helpText="${bundle['page.course_tool.splash.step2.email.instructions']}" />
          <%
              }
          %>
        </bbNG:dataElement>
      </bbNG:step>
      <bbNG:stepSubmit hideNumber="false" showCancelButton="false" />
    </bbNG:dataCollection>
  </bbNG:form>
</bbNG:genericPage>
