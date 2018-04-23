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

import blackboard.servlet.tags.ngui.datacollection.fields.SimpleInputTag;

public class SettingDef {

// Default settings
    private String name;
    private String title;
    private String description;
    private SimpleInputTag.Type type;
    private int size;
    private String defaultValue;

    public SettingDef(String name, String title, String description, SimpleInputTag.Type type, int size) {

        this(name, title, description, type, size, "");

    }

    public SettingDef(String name, String title, String description, SimpleInputTag.Type type, int size, String defaultValue) {

        this.name = name;
        this.title = title;
        this.description = description;
        this.type = type;
        this.size = size;
        this.defaultValue = defaultValue;

    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SimpleInputTag.Type getType() {
        return this.type;
    }

    public void setType(SimpleInputTag.Type type) {
        this.type = type;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

}
