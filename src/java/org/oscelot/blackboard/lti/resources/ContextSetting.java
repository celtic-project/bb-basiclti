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
package org.oscelot.blackboard.lti.resources;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import com.google.gson.Gson;

import blackboard.persist.Id;
import blackboard.data.course.Course;

import org.oscelot.blackboard.lti.services.Service;
import org.oscelot.blackboard.lti.resources.settings.ToolSettingsContainerV1;

import com.spvsoftwareproducts.blackboard.utils.B2Context;
import org.oscelot.blackboard.lti.Constants;
import org.oscelot.blackboard.lti.services.Setting;
import org.oscelot.blackboard.lti.Utils;

public class ContextSetting extends Resource {

    private static final String ID = "ToolProxyBindingSettings";
    private static final String TEMPLATE = "/lis/{context_type}/{context_id}/bindings/{vendor_code}/{product_code}(/custom)";
    private static List<String> FORMATS = new ArrayList<String>() {
        {
            add("application/vnd.ims.lti.v2.toolsettings+json");
            add("application/vnd.ims.lti.v2.toolsettings.simple+json");
        }

    };

    public ContextSetting(Service service) {

        super(service);
        this.methods.add("PUT");
        this.variables.add("ToolProxyBinding.custom.url");

    }

    public String getId() {

        return ID;

    }

    public String getTemplate() {

        return TEMPLATE;

    }

    public List<String> getFormats() {

        return Collections.unmodifiableList(FORMATS);

    }

    @Override
    public Map<String, String> getCustomParameters(B2Context b2Context, Properties props) {

        Map<String, String> customParams = new HashMap<String, String>();
        if (b2Context.hasCourseContext()) {
            customParams.put("context_setting_url", "$ToolProxyBinding.custom.url");
        }

        return customParams;

    }

    public void execute(B2Context b2Context, Response response) {

        Map<String, String> template = this.parseTemplate();
        String contextType = template.get("context_type");
        String contextId = template.get("context_id");
        String vendorCode = template.get("vendor_code");
        String productCode = template.get("product_code");
        String bubble = b2Context.getRequestParameter("bubble", null);
        boolean ok = (contextType.length() > 0) && (contextId.length() > 0)
                && (vendorCode.length() > 0) && (productCode.length() > 0)
                && this.getService().checkTool(productCode, response.getData());
        if (!ok) {
            response.setCode(401);
        }
        String contentType = response.getAccept();
        boolean simpleFormat = (contentType != null) && contentType.equals(FORMATS.get(1));
        if (ok) {
            ok = ((bubble == null) || ((bubble.equals("distinct") || bubble.equals("all"))))
                    && (!simpleFormat || (bubble == null) || !bubble.equals("all"))
                    && ((bubble == null) || b2Context.getRequest().getMethod().equals("GET"));
            if (!ok) {
                response.setCode(406);
            }
        }

        Course course = null;
        if (ok) {
            course = Utils.ltiContextId2Course(this.getService().getTool(), contextId, true);
            ok = course != null;
            if (!ok) {
                response.setCode(400);
            }
        }
        if (ok) {
            SystemSetting systemSetting = null;
            Properties contextSettings;
            Properties systemSettings = null;
            contextSettings = Setting.stringToProperties(getSettingsString(b2Context, course.getId()));
            if (bubble != null) {
                systemSetting = new SystemSetting(this.getService());
                systemSetting.params = new HashMap<String, String>();
                systemSetting.params.put("tool_proxy_guid", productCode);
                systemSettings = Setting.stringToProperties(systemSetting.getSettingsString(b2Context, productCode));
                if (bubble.equals("distinct")) {
                    Setting.distinctSettings(systemSettings, contextSettings, null);
                }
            }
            if (b2Context.getRequest().getMethod().equals("GET")) {
                StringBuilder json = new StringBuilder();
                if (simpleFormat) {
                    response.setContentType(FORMATS.get(1));
                    json.append("{\n");
                } else {
                    response.setContentType(FORMATS.get(0));
                    json.append("{\n").append("  \"@context\":\"http://purl.imsglobal.org/ctx/lti/v2/ToolSettings\",\n").append("  \"@graph\":[\n");
                }
                String settingValues = Setting.settingsToJson(systemSettings, simpleFormat, "ToolProxy", systemSetting);
                json.append(settingValues);
                boolean isFirst = settingValues.length() <= 0;
                settingValues = Setting.settingsToJson(contextSettings, simpleFormat, "ToolProxyBinding", this);
                if (settingValues.length() > 0) {
                    if (!isFirst) {
                        json.append(",\n");
                    }
                }
                json.append(settingValues);
                if (simpleFormat) {
                    json.append("\n}");
                } else {
                    json.append("\n  ]\n}");
                }
                response.setData(json.toString());
            } else {  // PUT
                Gson gson = new Gson();
                Map<String, String> settingValues = null;
                if (response.getContentType().equals(FORMATS.get(0))) {
                    ToolSettingsContainerV1 container = gson.fromJson(response.getData(), ToolSettingsContainerV1.class);
                    ok = (container.getGraph().length == 1) && (container.getGraph()[0].getType().equals("ToolProxyBinding"));
                    if (ok) {
                        settingValues = container.getGraph()[0].getCustom();
                    }
                } else {  // simple JSON
                    settingValues = gson.fromJson(response.getData(), Map.class);
                }
                if (ok) {
                    ok = settingValues != null;
                }
                if (ok) {
                    StringBuilder custom = new StringBuilder();
                    Map.Entry<String, String> entry;
                    String sep = "";
                    for (Iterator<Map.Entry<String, String>> iter = settingValues.entrySet().iterator(); iter.hasNext();) {
                        entry = iter.next();
                        if (!entry.getKey().startsWith("@")) {
                            custom.append(sep).append(entry.getKey()).append("=").append(entry.getValue());
                            sep = "\n";
                        }
                    }
                    setSettingsString(b2Context, course.getId(), custom.toString());
                }
                if (!ok) {
                    response.setCode(406);
                }
            }
        }

    }

    protected String getSettingsString(B2Context b2Context, Id contextId) {

        String settingsString;
        String settingPrefix = Constants.TOOL_PARAMETER_PREFIX + "." + this.getService().getTool().getId() + "." + Constants.SERVICE_PARAMETER_PREFIX + "." + this.getService().getId() + ".custom";
        b2Context.setCourseId(contextId);
        b2Context.setIgnoreContentContext(true);
        settingsString = b2Context.getSetting(false, true, settingPrefix, "");

        return settingsString;

    }

    private void setSettingsString(B2Context b2Context, Id contextId, String custom) {

        String settingPrefix = Constants.TOOL_PARAMETER_PREFIX + "." + this.getService().getTool().getId() + "." + Constants.SERVICE_PARAMETER_PREFIX + "." + this.getService().getId() + ".custom";
        b2Context.setCourseId(contextId);
        b2Context.setIgnoreContentContext(true);
        b2Context.setSetting(false, true, settingPrefix, custom);
        b2Context.persistSettings(false, true);

    }

    @Override
    public String parseValue(String value) {

        B2Context b2Context = this.getService().getB2Context();
        Course course = b2Context.getCourse();
        if (course != null) {
            String url = this.getEndpoint();
            url = url.replaceAll("\\{context_type\\}", "CourseSection");
            url = url.replaceAll("\\{context_id\\}", Utils.course2ltiContextId(b2Context, this.getService().getTool(), course));
            url = url.replaceAll("\\{vendor_code\\}", this.getService().getB2Context().getVendorId());
            url = url.replaceAll("\\{product_code\\}", this.getService().getTool().getId());
            value = value.replaceAll("\\$ToolProxyBinding.custom.url", url);
        }

        return value;

    }

}
