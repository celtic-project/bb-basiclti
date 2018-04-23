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
        import="java.util.Map,
        java.util.HashMap,
        java.util.List,
        java.util.Iterator,
        java.net.URL,
        java.net.MalformedURLException,
        blackboard.platform.security.CourseRole,
        blackboard.platform.filesystem.MultipartRequest,
        blackboard.platform.filesystem.FileSystemService,
        blackboard.platform.filesystem.FileSystemServiceFactory,
        blackboard.servlet.tags.ngui.datacollection.fields.SimpleInputTag,
        com.spvsoftwareproducts.blackboard.utils.B2Context,
        org.oscelot.blackboard.lti.Utils,
        org.oscelot.blackboard.lti.services.Service,
        org.oscelot.blackboard.lti.resources.Resource,
        org.oscelot.blackboard.lti.resources.SettingDef,
        org.oscelot.blackboard.lti.ServiceList,
        org.oscelot.blackboard.lti.Constants"
        errorPage="../error.jsp"%>
<%@taglib uri="/bbNG" prefix="bbNG"%>
<bbNG:genericPage title="${bundle['page.system.servicesettings.title']}" entitlement="system.admin.VIEW">
  <%
      String formName = "page.system.servicesettings";
      Utils.checkForm(request, formName);

      B2Context b2Context = new B2Context(request);
      String query = Utils.getQuery(request);
      String cancelUrl = "services.jsp?" + query;
      String serviceId = b2Context.getRequestParameter(Constants.TOOL_ID, "");

      Service service = Service.getServiceFromClassName(b2Context, Service.getSettingValue(b2Context, serviceId, Constants.SERVICE_CLASS, ""));

      List<SettingDef> settings = null;
      if (service != null) {
          settings = service.getSettings();
      }
      if ((settings == null) || settings.isEmpty()) {
          cancelUrl = b2Context.setReceiptOptions(cancelUrl,
                  b2Context.getResourceString("page.system.servicesettings.receipt.nosettings"), null);
          response.sendRedirect(cancelUrl);
          return;
      }

      if (request.getMethod().equalsIgnoreCase("POST")) {
          b2Context.setSaveEmptyValues(false);
          SettingDef setting;
          String value;
          for (Iterator<SettingDef> iter = settings.iterator(); iter.hasNext();) {
              setting = iter.next();
              value = b2Context.getRequestParameter("setting" + setting.getName(), "");
              service.setSetting(setting.getName(), value);
          }
          b2Context.persistSettings();
          cancelUrl = b2Context.setReceiptOptions(cancelUrl,
                  b2Context.getResourceString("page.receipt.success"), null);
          response.sendRedirect(cancelUrl);
          return;
      }

      Map<String, String> resourceStrings = b2Context.getResourceStrings();
      pageContext.setAttribute("bundle", resourceStrings);
      pageContext.setAttribute("titleSuffix", ": " + service.getName());

      pageContext.setAttribute("query", query);
      pageContext.setAttribute("cancelUrl", cancelUrl);
  %>
  <bbNG:pageHeader instructions="${bundle['page.system.service.instructions']}">
    <bbNG:breadcrumbBar environment="SYS_ADMIN_PANEL" navItem="admin_plugin_manage">
      <bbNG:breadcrumb href="tools.jsp?${query}" title="${bundle['plugin.name']}" />
      <bbNG:breadcrumb href="services.jsp?${query}" title="${bundle['page.system.services.title']}" />
      <bbNG:breadcrumb title="${bundle['page.system.service.title']}" />
    </bbNG:breadcrumbBar>
    <bbNG:pageTitleBar iconUrl="../images/lti.gif" showTitleBar="true" title="${bundle['page.system.service.title']}${titleSuffix}"/>
  </bbNG:pageHeader>
  <bbNG:form action="servicesettings.jsp?${query}" name="serviceSettingsForm" method="post" onsubmit="return validateForm();" isSecure="true" nonceId="<%=formName%>">
    <bbNG:dataCollection markUnsavedChanges="true" showSubmitButtons="true">
      <bbNG:step hideNumber="false" title="${bundle['page.system.service.step1.title']}" instructions="${bundle['page.system.service.step1.instructions']}">
        <input type="hidden" name="<%=Constants.TOOL_ID%>" value="<%=serviceId%>" />
        <%
            SettingDef setting;
            for (Iterator<SettingDef> iter = settings.iterator(); iter.hasNext();) {
                setting = iter.next();
                pageContext.setAttribute("settingName", "setting" + setting.getName());
                pageContext.setAttribute("settingDescription", setting.getDescription());
                pageContext.setAttribute("settingSize", setting.getSize());
                pageContext.setAttribute("value", service.getSetting(setting.getName(), setting.getDefaultValue()));
        %>
        <bbNG:dataElement isRequired="false" label="<%=setting.getTitle()%>">
          <%
              if (setting.getType().equals(SimpleInputTag.Type.FLOAT)) {
          %>
          <bbNG:textElement type="float" name="${settingName}" value="${value}" size="${settingSize}" helpText="${settingDescription}" />
          <%
          } else if (setting.getType().equals(SimpleInputTag.Type.INTEGER)) {
          %>
          <bbNG:textElement type="integer" name="${settingName}" value="${value}" size="${settingSize}" helpText="${settingDescription}" />
          <%
          } else if (setting.getType().equals(SimpleInputTag.Type.UNSIGNED_FLOAT)) {
          %>
          <bbNG:textElement type="unsigned_float" name="${settingName}" value="${value}" size="${settingSize}" helpText="${settingDescription}" />
          <%
          } else if (setting.getType().equals(SimpleInputTag.Type.UNSIGNED_INTEGER)) {
          %>
          <bbNG:textElement type="unsigned_integer" name="${settingName}" value="${value}" size="${settingSize}" helpText="${settingDescription}" />
          <%
          } else {
          %>
          <bbNG:textElement type="string" name="${settingName}" value="${value}" size="${settingSize}" helpText="${settingDescription}" />
          <%
              }
          %>
        </bbNG:dataElement>
        <%
            }
        %>
      </bbNG:step>
      <bbNG:stepSubmit hideNumber="false" showCancelButton="true" cancelUrl="${cancelUrl}" />
    </bbNG:dataCollection>
  </bbNG:form>
</bbNG:genericPage>
