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
        import="java.util.List,
        java.util.ArrayList,
        java.util.Map,
        com.spvsoftwareproducts.blackboard.utils.B2Context,
        org.oscelot.blackboard.lti.Constants,
        org.oscelot.blackboard.lti.Utils,
        org.oscelot.blackboard.lti.ServiceList,
        org.oscelot.blackboard.lti.services.Service,
        org.oscelot.blackboard.lti.resources.SettingDef"
        errorPage="../error.jsp"%>
<%@taglib uri="/bbNG" prefix="bbNG"%>
<bbNG:genericPage title="${bundle['page.system.services.title']}" entitlement="system.admin.VIEW">
  <%
      String formName = "page.system.services";
      Utils.checkForm(request, formName);

      B2Context b2Context = new B2Context(request);
      ServiceList serviceList = new ServiceList(b2Context, true);
      String sourcePage = b2Context.getRequestParameter(Constants.PAGE_PARAMETER_NAME, "");
      String handle = "admin_main";
      if (sourcePage.length() > 0) {
          handle = "admin_plugin_manage";
      }
      String cancelUrl = b2Context.getNavigationItem(handle).getHref();
      String query = Utils.getQuery(request);

      pageContext.setAttribute("query", query);
      pageContext.setAttribute("bundle", b2Context.getResourceStrings());
      pageContext.setAttribute("imageFiles", Constants.IMAGE_FILE);
      pageContext.setAttribute("imageAlt", Constants.IMAGE_ALT_RESOURCE);
      pageContext.setAttribute("cancelUrl", cancelUrl);
      pageContext.setAttribute("DoEnable", Constants.ACTION_ENABLE);
      pageContext.setAttribute("DoDisable", Constants.ACTION_DISABLE);
      pageContext.setAttribute("DoDelete", Constants.ACTION_DELETE);
      pageContext.setAttribute("xmlTitle", b2Context.getResourceString("page.system.tools.action.xml"));
  %>
  <bbNG:jsBlock>
    <script type="text/javascript">
      function doAction(value) {
        document.frmServices.action.value = value;
        document.frmServices.submit();
      }
      function doDelete() {
        if (confirm('${bundle['page.system.tools.action.confirm']}')) {
          doAction('delete');
        }
      }
    </script>
  </bbNG:jsBlock>
  <bbNG:pageHeader instructions="${bundle['page.system.services.instructions']}">
    <bbNG:breadcrumbBar environment="SYS_ADMIN" navItem="admin_plugin_manage">
      <bbNG:breadcrumb href="tools.jsp?${query}" title="${bundle['plugin.name']}" />
      <bbNG:breadcrumb title="${bundle['page.system.services.title']}" />
    </bbNG:breadcrumbBar>
    <bbNG:pageTitleBar iconUrl="../images/lti.gif" showTitleBar="true" title="${bundle['page.system.services.title']}"/>
    <bbNG:actionControlBar>
      <bbNG:actionButton title="${bundle['page.system.services.button.add']}" url="service.jsp?${query}" primary="true" />
      <bbNG:actionButton title="${bundle['page.system.domains.button.tools']}" url="tools.jsp?${query}" primary="false" />
      <bbNG:actionButton title="${bundle['page.system.tools.button.domains']}" url="domains.jsp?${query}" primary="false" />
    </bbNG:actionControlBar>
  </bbNG:pageHeader>
  <bbNG:form name="frmServices" method="post" action="toolsaction?${query}" isSecure="true" nonceId="<%=formName%>">
    <input type="hidden" name="<%=Constants.ACTION%>" value="" />
    <input type="hidden" name="<%=Constants.SERVICE_PARAMETER_PREFIX%>" value="true" />
    <bbNG:inventoryList collection="<%=serviceList.getList()%>" objectVar="service" className="org.oscelot.blackboard.lti.services.Service"
                        description="${bundle['page.system.services.description']}" reorderable="false"
                        itemIdAccessor="getId" itemNameAccessor="getName" showAll="false" emptyMsg="${bundle['page.system.services.empty']}">
      <bbNG:listActionBar>
        <bbNG:listActionMenu title="${bundle['page.system.tools.action.status']}">
          <bbNG:listActionItem title="${bundle['page.system.services.action.enable']}" url="JavaScript: doAction('${DoEnable}');" />
          <bbNG:listActionItem title="${bundle['page.system.services.action.disable']}" url="JavaScript: doAction('${DoDisable}');" />
        </bbNG:listActionMenu>
        <bbNG:listActionItem title="${bundle['page.system.tools.action.delete']}" url="JavaScript: doDelete('${DoDelete}');" />
      </bbNG:listActionBar>
      <bbNG:jspBlock>
        <%
            pageContext.setAttribute("id", Constants.TOOL_ID + "=" + service.getId());
            pageContext.setAttribute("alt", Constants.SERVICE_PARAMETER_PREFIX + "." + service.getIsEnabled());
        %>
      </bbNG:jspBlock>
      <bbNG:listCheckboxElement name="<%=Constants.TOOL_ID%>" value="${service.id}" />
      <bbNG:listElement isRowHeader="false" label="${bundle['page.system.tools.enabled.label']}" name="isenabled">
        <img src="${imageFiles[service.isEnabled]}" alt="${bundle[imageAlt[alt]]}" title="${bundle[imageAlt[alt]]}" />
      </bbNG:listElement>
      <%
          boolean enabled = service.getIsEnabled().equals(Constants.DATA_TRUE);
          if (enabled) {
              pageContext.setAttribute("statusTitle", b2Context.getResourceString("page.system.services.action.disable"));
              pageContext.setAttribute("statusAction", Constants.ACTION_DISABLE);
          } else {
              pageContext.setAttribute("statusTitle", b2Context.getResourceString("page.system.services.action.enable"));
              pageContext.setAttribute("statusAction", Constants.ACTION_ENABLE);
          }
      %>
      <bbNG:listElement isRowHeader="true" label="${bundle['page.system.tools.name.label']}" name="name">
        ${service.name}
        <%
            pageContext.setAttribute("id", Constants.TOOL_ID + "=" + service.getId());
            List<SettingDef> settings = service.getSettings();
            if (ServiceList.isStandardService(service.getClassName())) {
        %>
        <bbNG:listContextMenu order="status">
          <bbNG:contextMenuItem title="${statusTitle}" url="JavaScript: doAction('${statusAction}');" id="status" />
        </bbNG:listContextMenu>
        <%
        } else if (settings.isEmpty()) {
        %>
        <bbNG:listContextMenu order="status,*separator*,delete">
          <bbNG:contextMenuItem title="${statusTitle}" url="JavaScript: doAction('${statusAction}');" id="status" />
          <bbNG:contextMenuItem title="${bundle['page.system.tools.action.delete']}" url="JavaScript: doDelete();" id="delete" />
        </bbNG:listContextMenu>
        <%
        } else {
        %>
        <bbNG:listContextMenu order="status,*separator*,settings,*separator*,delete">
          <bbNG:contextMenuItem title="${statusTitle}" url="JavaScript: doAction('${statusAction}');" id="status" />
          <bbNG:contextMenuItem title="${bundle['page.system.services.action.settings']}" url="servicesettings.jsp?${id}&${query}" id="settings" />
          <bbNG:contextMenuItem title="${bundle['page.system.tools.action.delete']}" url="JavaScript: doDelete();" id="delete" />
        </bbNG:listContextMenu>
        <%
            }
        %>
      </bbNG:listElement>
      <bbNG:listElement isRowHeader="false" label="${bundle['page.system.services.class.label']}" name="class">
        ${service.className}
      </bbNG:listElement>
    </bbNG:inventoryList>
  </bbNG:form>
  <bbNG:okButton url="${cancelUrl}" />
</bbNG:genericPage>
