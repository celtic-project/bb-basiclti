/*
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
 */
package org.oscelot.blackboard.lti;

import com.spvsoftwareproducts.blackboard.utils.B2Context;

public class ConfigMessage extends LtiMessage {

    public ConfigMessage(B2Context b2Context, Tool tool) {

        super(b2Context, tool, null);
        this.props.setProperty("lti_message_type", Constants.CONFIG_MESSAGE_TYPE);
        String query = "&" + Utils.getQuery(b2Context.getRequest());
        query = query.replaceAll("&" + Constants.PAGE_PARAMETER_NAME + "=[^&]*", "");
        if (query.length() != 1) {
            query += "&";
        }
        String page = b2Context.getRequestParameter(Constants.PAGE_PARAMETER_NAME, "");
        if (page.length() > 0) {
            query += Constants.PAGE_PARAMETER_NAME + "=" + page;
        } else if (b2Context.hasCourseContext()) {
            query += Constants.PAGE_PARAMETER_NAME + "=" + Constants.COURSE_TOOLS_PAGE;
        } else {
            query += Constants.PAGE_PARAMETER_NAME + "=" + Constants.ADMIN_PAGE;
        }
        String returnUrl = b2Context.getServerUrl() + b2Context.getPath() + "return.jsp?" + query.substring(1);
        this.props.setProperty("launch_presentation_return_url", returnUrl);
        if (this.props.getProperty("launch_presentation_document_target").equals("iframe")) {
            this.props.setProperty("launch_presentation_document_target", "frame");
        }

        addServiceCustomParameters(b2Context);

        String customParameters = b2Context.getSetting(this.settingPrefix + Constants.TOOL_CUSTOM, "");
        customParameters = customParameters.replaceAll("\\r\\n", "\n");
        String[] items = customParameters.split("\\n");
        addParameters(b2Context, items, false);

// System-level settings
        customParameters = b2Context.getSetting(Constants.TOOL_PARAMETER_PREFIX + "." + this.tool.getId() + "." + Constants.SERVICE_PARAMETER_PREFIX + ".setting.custom", "");
        items = customParameters.split("\\n");
        addParameters(b2Context, items, true);

    }

}
