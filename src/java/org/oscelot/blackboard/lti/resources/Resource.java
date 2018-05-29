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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.oscelot.blackboard.lti.Tool;
import org.oscelot.blackboard.lti.Constants;
import org.oscelot.blackboard.lti.services.Service;

import com.spvsoftwareproducts.blackboard.utils.B2Context;

public abstract class Resource {

// Default settings
    private static final String TYPE = "RestService";

    private Service service = null;
    protected List<String> variables = null;
    protected List<String> methods = null;
    protected Map<String, String> params = null;
    protected List<SettingDef> settings = null;

    public Resource(Service service) {

        this.service = service;
        this.methods = new ArrayList<String>();
        this.variables = new ArrayList<String>();
        this.methods.add("GET");
        this.settings = new ArrayList<SettingDef>();

    }

    public String getPath() {

        return this.getTemplate().replaceAll("[\\(\\)]", "");

    }

    public String getType() {

        return TYPE;

    }

    public Service getService() {

        return this.service;

    }

    public abstract String getId();

    public String getName() {

        return "Unnamed resource";

    }

    public abstract String getTemplate();

    public List<String> getMethods() {

        return Collections.unmodifiableList(this.methods);

    }

    public abstract List<String> getFormats();

    public List<String> getVariables() {

        return this.variables;

    }

    public Map<String, String> getCustomParameters(B2Context b2Context, Properties props) {

        return new HashMap<String, String>();

    }

    public List<SettingDef> getSettings() {

        return Collections.unmodifiableList(this.settings);

    }

    public String getEndpoint() {

        this.parseTemplate();
        B2Context b2Context = this.getService().getB2Context();
        String template = this.getTemplate();
        template = template.replaceAll("[\\(\\)]", "");
        String url = b2Context.getServerUrl() + b2Context.getPath() + Constants.RESOURCE_PATH + template;
        Map.Entry<String, String> entry;
        for (Iterator<Map.Entry<String, String>> iter = this.params.entrySet().iterator(); iter.hasNext();) {
            entry = iter.next();
            url = url.replaceAll("\\{" + entry.getKey() + "\\}", entry.getValue());
        }
        Tool tool = this.getService().getTool();
        if (tool != null) {
            url = url.replaceAll("\\{tool_id\\}", tool.getId());
        }

        return url;

    }

    public abstract void execute(B2Context b2Context, Response response);

    public String parseValue(String value) {

        return value;

    }

    protected Map<String, String> parseTemplate() {

        if (this.params == null) {
            this.params = new HashMap<String, String>();
            String pathInfo = this.getService().getB2Context().getRequest().getPathInfo();
            if (pathInfo != null) {
                String[] path = pathInfo.split("/");
                String template = this.getTemplate();
                template = template.replaceAll("[\\(\\)]", "");
                String[] parts = template.split("/");
                String value;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].startsWith("{") && parts[i].endsWith("}")) {
                        value = "";
                        if (i < path.length) {
                            value = path[i];
                        }
                        params.put(parts[i].substring(1, parts[i].length() - 1), value);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(this.params);

    }

}
