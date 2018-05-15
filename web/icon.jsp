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
<%@page import="java.nio.charset.StandardCharsets,
        org.apache.commons.codec.binary.Base64,
        org.oscelot.blackboard.lti.Constants,
        org.oscelot.blackboard.lti.Tool,
        org.oscelot.blackboard.lti.Utils,
        com.spvsoftwareproducts.blackboard.utils.B2Context"
        errorPage="error.jsp"%>
<%
    B2Context b2Context = new B2Context(request);
    Utils.checkInheritSettings(b2Context);

    String icon;
    String toolId = b2Context.getSetting(false, true,
            Constants.TOOL_PARAMETER_PREFIX + "." + Constants.TOOL_ID,
            b2Context.getRequestParameter(Constants.TOOL_ID, ""));
    if (toolId.length() <= 0) {
        icon = b2Context.getSetting(false, true,
                Constants.TOOL_PARAMETER_PREFIX + "." + Constants.TOOL_ICON, "");
        boolean disabled = false;
        if (icon.length() <= 0) {
            Tool domain = Utils.urlToDomain(b2Context, b2Context.getSetting(false, true,
                    Constants.TOOL_PARAMETER_PREFIX + "." + Constants.TOOL_URL));
            if (domain != null) {
                disabled = !domain.getIsEnabled().equals(Constants.DATA_TRUE);
                icon = domain.getDisplayIcon();
            }
        }
        if (icon.length() <= 0) {
            if (!disabled) {
                icon = "images/lti.gif";
            } else {
                icon = "images/lti_disabled.gif";
            }
        }
    } else {
        boolean persist = false;  // Check for icon bug in 3.3.0
        icon = b2Context.getSetting(false, true, Constants.TOOL_PARAMETER_PREFIX + "." + toolId + "." + Constants.TOOL_ICON, null);
        if ((icon != null) && (icon.length() <= 0)) {
            b2Context.setSetting(false, true, Constants.TOOL_PARAMETER_PREFIX + "." + toolId + "." + Constants.TOOL_ICON, null);
            persist = true;
        }
        icon = b2Context.getSetting(false, true, Constants.TOOL_PARAMETER_PREFIX + "." + toolId + "." + Constants.TOOL_ICON_DISABLED, null);
        if ((icon != null) && (icon.length() <= 0)) {
            b2Context.setSetting(false, true, Constants.TOOL_PARAMETER_PREFIX + "." + toolId + "." + Constants.TOOL_ICON_DISABLED, null);
            persist = true;
        }
        if (persist) {
            b2Context.persistSettings(false, true);
        }
        Tool tool = new Tool(b2Context, toolId);
        icon = tool.getDisplayIcon();
        if (icon.length() <= 0) {
            Tool domain = tool.getDomain();
            if (domain != null) {
                icon = domain.getDisplayIcon();
            }
        }
        if (icon.length() <= 0) {
            if (tool.getIsEnabled().equals(Constants.DATA_TRUE)) {
                icon = "images/lti.gif";
            } else {
                icon = "images/lti_disabled.gif";
            }
        }
    }

    if (icon.startsWith("data:")) {
        String[] parts = icon.substring(5).split(",");
        icon = parts[1];
        parts = parts[0].split(";");
        response.setContentType(parts[0]);
        if (parts[parts.length - 1].equals("base64")) {
            response.getOutputStream().write(Base64.decodeBase64(icon.getBytes(StandardCharsets.UTF_8)));
        } else {
            response.getOutputStream().write(icon.getBytes(StandardCharsets.UTF_8));
        }
    } else {
        response.sendRedirect(icon);
    }
%>
