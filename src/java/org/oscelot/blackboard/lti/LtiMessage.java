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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.Properties;
import java.util.Iterator;
import java.util.Collections;
import java.util.UUID;
import java.util.Locale;

import java.io.IOException;
import java.net.URISyntaxException;

import net.oauth.OAuthMessage;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;

import org.apache.commons.httpclient.NameValuePair;

import blackboard.data.user.User;
import blackboard.data.course.Course;
import blackboard.data.course.CourseMembership;
import blackboard.data.content.Content;
import blackboard.platform.security.CourseRole;
import blackboard.data.role.PortalRole;
import blackboard.portal.data.Module;
import blackboard.persist.Id;
import blackboard.persist.course.CourseDbLoader;
import blackboard.persist.content.ContentDbLoader;
import blackboard.platform.user.MyPlacesUtil;
import blackboard.platform.persistence.PersistenceServiceFactory;
import blackboard.persist.BbPersistenceManager;
import blackboard.persist.PersistenceException;
import blackboard.platform.config.BbConfig;
import blackboard.platform.config.ConfigurationServiceFactory;
import blackboard.platform.context.Context;
import blackboard.util.GeneralUtil;
import blackboard.util.LocaleUtil;
import blackboard.util.UrlUtil;
import blackboard.platform.branding.BrandingUtil;
import blackboard.platform.branding.PersonalStyleHelper;

import blackboard.platform.institutionalhierarchy.service.Node;
import blackboard.platform.institutionalhierarchy.service.NodeManagerFactory;

import org.oscelot.blackboard.lti.resources.Resource;
import org.oscelot.blackboard.lti.services.Service;

import com.spvsoftwareproducts.blackboard.utils.B2Context;

public class LtiMessage {

    public Tool tool = null;
    protected String toolPrefix = null;
    protected String settingPrefix = null;
    protected Properties props = null;
    private List<Map.Entry<String, String>> params = null;

    public LtiMessage(B2Context b2Context, Tool tool, Module module) {

        User user = b2Context.getUser();
        Course course = null;
        Context context = b2Context.getContext();

        this.tool = tool;
        this.toolPrefix = Constants.TOOL_PARAMETER_PREFIX + ".";
        this.settingPrefix = "";
        if (!tool.getByUrl()) {
            this.toolPrefix += this.tool.getId() + ".";
            this.settingPrefix = this.toolPrefix;
        } else {
            String domainId = "";
            Tool domain = tool.getDomain();
            if (domain != null) {
                domainId = domain.getId();
            }
            this.settingPrefix = Constants.DOMAIN_PARAMETER_PREFIX + "." + domainId + ".";
        }

        this.props = new Properties();
        props.setProperty("lti_version", Constants.LTI_VERSION);

// User parameters
        if (this.tool.getDoSendUserId()) {
            String userId = Utils.getLTIUserId(this.tool.getUserIdType(), user);
            if (userId != null) {
                this.props.setProperty("user_id", userId);
            }
        }
        try {
            if (MyPlacesUtil.avatarsEnabled() && Utils.displayAvatar(user.getId()) && this.tool.getDoSendAvatar()) {
                String image = MyPlacesUtil.getAvatarImage(user.getId());
                if (image != null) {
                    this.props.setProperty("user_image", b2Context.getServerUrl() + image);
                }
            }
        } catch (Exception e) {
        }

        if (this.tool.getDoSendUsername()) {
            this.props.setProperty("lis_person_name_given", user.getGivenName());
            this.props.setProperty("lis_person_name_family", user.getFamilyName());
            String fullname = user.getGivenName();
            if ((user.getMiddleName() != null) && (user.getMiddleName().length() > 0)) {
                fullname += " " + user.getMiddleName();
            }
            fullname += " " + user.getFamilyName();
            this.props.setProperty("lis_person_name_full", fullname);
        }
        if (this.tool.getDoSendEmail()) {
            this.props.setProperty("lis_person_contact_email_primary", user.getEmailAddress());
        }
        if (this.tool.getDoSendUserSourcedid()) {
            this.props.setProperty("lis_person_sourcedid", user.getBatchUid());
        }

// Course parameters
        if (!b2Context.getIgnoreCourseContext()) {
            course = b2Context.getCourse();
        }
        String roles = "";
        boolean systemRolesOnly = !b2Context.getSetting(Constants.TOOL_COURSE_ROLES, Constants.DATA_FALSE).equals(Constants.DATA_TRUE);
        boolean sendAdminRole = this.tool.getSendAdministrator().equals(Constants.DATA_TRUE);
        boolean emulateCore = this.tool.getDoEmulateCore();
        String contextId = "";
        String resourceId = "";
        if (course != null) {
            String contentId = b2Context.getRequestParameter("content_id", "");
            if (contentId.equals("@X@content.pk_string@X@")) {
                contentId = "";
            }
            this.props.setProperty("context_type", "CourseSection");
            resourceId = Utils.course2ltiContextId(b2Context, this.tool, course);
            if (b2Context.hasGroupContext()) {
                this.props.setProperty("context_type", "Group");
                if (resourceId != null) {
                    resourceId += Constants.PREFIX_GROUP + b2Context.getGroupId().toExternalString();
                }
                if (this.tool.getDoSendContextTitle()) {
                    this.props.setProperty("context_title", Utils.stripTags(b2Context.getGroup().getTitle()));
                    this.props.setProperty("context_label", b2Context.getGroup().getTitle());
                }
            } else if (this.tool.getDoSendContextTitle()) {
                this.props.setProperty("context_title", Utils.stripTags(course.getTitle()));
                this.props.setProperty("context_label", course.getCourseId());
            }
            if (this.tool.getDoSendContextId() && (resourceId != null)) {
                this.props.setProperty("context_id", resourceId);
            }
            String title = tool.getName();
            String description = "";
            if (contentId.length() > 0) {
                resourceId += contentId;
                Content content = b2Context.getContent();
                if (content != null) {
                    title = content.getTitle();
                    description = content.getBody().getText();
                }
            } else if (b2Context.hasGroupContext()) {
                title = b2Context.getGroup().getTitle();
                description = b2Context.getGroup().getDescription().getText();
            } else if (module != null) {
                resourceId = Constants.PREFIX_MODULE + resourceId;
                title = module.getTitle();
                description = module.getDescriptionFormatted().getText();
            } else {
                description = this.tool.getDescription();
            }
            this.props.setProperty("resource_link_title", Utils.stripTags(title));
            if (description.length() > 0) {
                this.props.setProperty("resource_link_description", Utils.stripTags(description));
            }
            if (this.tool.getDoSendContextSourcedid()) {
                this.props.setProperty("lis_course_offering_sourcedid", course.getBatchUid());
                this.props.setProperty("lis_course_section_sourcedid", course.getBatchUid());
            }

            if (this.tool.getDoSendRoles()) {
                CourseMembership cm = context.getCourseMembership();
                CourseMembership.Role role = null;
                String roleId;
                if (cm != null) {
                    role = Utils.getRole(cm.getRole(), systemRolesOnly);
                    roleId = role.getIdentifier();
                } else {
                    roleId = CourseRole.Ident.Guest.getIdentifier();
                }
                roles = Utils.getCRoles(this.tool.getRole(roleId));
                if (this.tool.getDoSendCRoles() && (role != null)) {
                    CourseRole cRole = role.getDbRole();
                    if (systemRolesOnly && cRole.isRemovable()) {
                        if (cRole.isActAsInstructor()) {
                            cRole = CourseMembership.Role.INSTRUCTOR.getDbRole();
                        } else {
                            cRole = CourseMembership.Role.TEACHING_ASSISTANT.getDbRole();
                        }
                    }
                    this.props.setProperty("ext_context_roles", cRole.getCourseName());
                }
            }
        } else {
            try {
                contextId = CourseDbLoader.Default.getInstance().loadSystemCourse().getId().toExternalString();
                resourceId = contextId;
            } catch (PersistenceException e) {
                B2Context.log(true, null, (Object) e);
            }
            if (module != null) {
                resourceId = Constants.PREFIX_MODULE + module.getId().toExternalString();
            }
        }
        this.props.setProperty("resource_link_id", resourceId);
        if (this.tool.getDoSendRoles()) {
            if (this.tool.getDoSendORoles()) {
                List<User> observed = Utils.getObservedUsers(user.getId(), course.getId());
                if (!observed.isEmpty()) {
                    if (roles.length() > 0) {
                        roles += ",";
                    }
                    roles += Constants.ROLE_MENTOR;
                    StringBuilder mentees = new StringBuilder();
                    User aUser;
                    String userId;
                    for (Iterator<User> iter = observed.iterator(); iter.hasNext();) {
                        aUser = iter.next();
                        userId = Utils.getLTIUserId(this.tool.getUserIdType(), aUser);
                        if (userId != null) {
                            mentees.append(",").append(Utils.urlEncode(userId));
                        }
                    }
                    if (mentees.length() > 0) {
                        this.props.setProperty("role_scope_mentor", mentees.substring(1));
                    }
                }
            }
            if ((roles.length() <= 0) && this.tool.getSendGuest().equals(Constants.DATA_TRUE)) {
                roles = Constants.IROLE_GUEST;
            }
            if (sendAdminRole) {
                roles = Utils.addAdminRole(roles, user);
            }
            roles = Utils.addPreviewRole(roles, user);
            this.props.setProperty("roles", roles);
            if (this.tool.getDoSendIRoles()) {
                List<PortalRole> iRoles = Utils.getInstitutionRoles(systemRolesOnly, user);
                StringBuilder iRolesString = new StringBuilder();
                PortalRole role;
                String sep = "";
                for (Iterator<PortalRole> iter = iRoles.iterator(); iter.hasNext();) {
                    role = iter.next();
                    iRolesString.append(sep).append(role.getRoleName());
                    sep = ",";
                }
                this.props.setProperty("ext_institution_roles", iRolesString.toString());
            }
        }
        if (course == null) {
            if (this.tool.getDoSendContextId() && B2Context.getIsVersion(9, 1, 8)) {
                Id id = b2Context.getUserId();
                if (id != Id.UNSET_ID) {
                    try {
                        List<Node> nodes = NodeManagerFactory.getAssociationManager().loadUserAssociatedNodes(id);
                        if (nodes.size() > 0) {
                            contextId += nodes.get(0).getIdentifier();
                        }
                    } catch (PersistenceException e) {
                    }
                }
            }
            if (contextId.length() > 0) {
                this.props.setProperty("context_id", contextId);
            }
            this.props.setProperty("context_type", "Group");
            if (tool.getDoSendRoles()) {
                boolean systemIRolesOnly = !b2Context.getSetting(Constants.TOOL_INSTITUTION_ROLES, Constants.DATA_FALSE).equals(Constants.DATA_TRUE);
                List<PortalRole> iRoles = Utils.getInstitutionRoles(systemIRolesOnly, user);
                sendAdminRole = b2Context.getSetting(false, true, Constants.TOOL_PARAMETER_PREFIX + "." + Constants.TOOL_ADMINISTRATOR, Constants.DATA_FALSE).equals(Constants.DATA_TRUE);
                roles = Utils.getIRoles(b2Context, iRoles, sendAdminRole && user.getSystemRole().equals(User.SystemRole.SYSTEM_ADMIN));
                if (sendAdminRole) {
                    roles = Utils.addAdminRole(roles, user);
                }
                roles = Utils.addPreviewRole(roles, user);
                this.props.remove("role_scope_mentor");
                List<User> observed = Utils.getObservedUsers(user.getId(), null);
                if (!observed.isEmpty()) {
                    if (roles.length() > 0) {
                        roles += ",";
                    }
                    roles += Constants.ROLE_MENTOR;
                    StringBuilder mentees = new StringBuilder();
                    User aUser;
                    String userId;
                    for (Iterator<User> iter = observed.iterator(); iter.hasNext();) {
                        aUser = iter.next();
                        userId = Utils.getLTIUserId(this.tool.getUserIdType(), aUser);
                        if (userId != null) {
                            mentees.append(",").append(Utils.urlEncode(userId));
                        }
                    }
                    if (mentees.length() > 0) {
                        this.props.setProperty("role_scope_mentor", mentees.substring(1));
                    }
                }
                this.props.setProperty("roles", roles);
            }
        }
        if (module != null) {
            this.props.setProperty("resource_link_title", Utils.stripTags(module.getTitle()));
            String description = module.getDescriptionFormatted().getText();
            if (description.length() > 0) {
                this.props.setProperty("resource_link_description", Utils.stripTags(description));
            }
        } else if (course == null) {
            if (this.tool.getDoSendContextTitle()) {
                this.props.setProperty("context_title", Utils.stripTags(ConfigurationServiceFactory.getInstance().getBbProperty(BbConfig.INST_NAME, "")));
                this.props.setProperty("context_label", ConfigurationServiceFactory.getInstance().getBbProperty(BbConfig.INST_TYPE, ""));
            }
            String sourcePage = b2Context.getRequestParameter(Constants.PAGE_PARAMETER_NAME, "");
            if (sourcePage.equals(Constants.TOOL_USERTOOL)) {
                resourceId = Constants.PREFIX_USER_TOOL + resourceId;
            }
            this.props.setProperty("resource_link_id", resourceId);
            this.props.setProperty("resource_link_title", Utils.stripTags(this.tool.getName()));
            String description = this.tool.getDescription();
            if (description.length() > 0) {
                this.props.setProperty("resource_link_description", Utils.stripTags(description));
            }

        }
// Consumer
        String css = this.tool.getLaunchCSS();
        if (css.length() > 0) {
            this.props.setProperty("launch_presentation_css_url", css);
        }

        if (emulateCore) {
            if (b2Context.getRequest() != null) {
                List<String> cssUrls = BrandingUtil.getCssUrls(b2Context.getRequest(), user, course, null,
                        PersonalStyleHelper.isHighContrast(b2Context.getRequest()), !LocaleUtil.isLeftToRight());
                if (cssUrls.size() > 0) {
                    StringBuilder cssUrl = new StringBuilder();
                    String url;
                    String sep = "";
                    for (Iterator<String> iter = cssUrls.iterator(); iter.hasNext();) {
                        url = iter.next();
                        cssUrl.append(sep).append(UrlUtil.calculateFullUrl(b2Context.getRequest(), url));
                        sep = ",";
                    }
                    this.props.setProperty("ext_launch_presentation_css_url", cssUrl.toString());
                }
            }
            if (B2Context.getIsVersion(9, 1, 201510)) {
                this.props.setProperty("ext_launch_id", UUID.randomUUID().toString());
            }
            this.props.setProperty("ext_lms", "bb-" + B2Context.getVersionNumber(""));
            this.props.setProperty("tool_consumer_info_product_family_code", "Blackboard Learn");
            this.props.setProperty("tool_consumer_info_version", B2Context.getVersionNumber(""));
        } else {
            String[] version = B2Context.getVersionNumber("?.?.?").split("\\.");
            this.props.setProperty("ext_lms", Constants.LTI_LMS + "-" + version[0] + "." + version[1] + "." + version[2]);
            this.props.setProperty("tool_consumer_info_product_family_code", Constants.LTI_LMS);
            this.props.setProperty("tool_consumer_info_version", version[0] + "." + version[1] + "." + version[2]);
        }

        String resource = this.tool.getResourceUrl();
        if (resource.length() > 0) {
            this.props.setProperty("ext_resource_link_content", resource);
            resource = this.tool.getResourceSignature();
            if (resource.length() > 0) {
                this.props.setProperty("ext_resource_link_content_signature", resource);
            }
        }

        String locale = user.getLocale();
        if ((locale == null) || (locale.length() <= 0)) {
            locale = (String) context.getAttribute(Constants.LOCALE_ATTRIBUTE);
        }
        locale = locale.replaceAll("_", "-");
        this.props.setProperty("launch_presentation_locale", locale);

        if (b2Context.getSetting(Constants.TOOL_PARAMETER_PREFIX + "." + this.tool.getId() + "." + Constants.TOOL_CONSUMER_GUID, Constants.DATA_FALSE).equals(Constants.DATA_TRUE)) {
            this.props.setProperty("tool_consumer_instance_guid", GeneralUtil.getSystemInstallationId());
        } else {
            this.props.setProperty("tool_consumer_instance_guid", this.tool.getLaunchGUID());
        }
        this.props.setProperty("tool_consumer_instance_name", b2Context.getSetting(Constants.CONSUMER_NAME_PARAMETER,
                GeneralUtil.getSystemInstanceName()));
        this.props.setProperty("tool_consumer_instance_description", b2Context.getSetting(Constants.CONSUMER_DESCRIPTION_PARAMETER,
                ConfigurationServiceFactory.getInstance().getBbProperty(BbConfig.INST_TYPE, "")));
        String email = b2Context.getSetting(Constants.CONSUMER_EMAIL_PARAMETER, "");
        if (email.length() <= 0) {
            email = GeneralUtil.getSystemAdminEmail();
        }
        if (email.length() > 0) {
            this.props.setProperty("tool_consumer_instance_contact_email", email);
        }

        this.props.setProperty("tool_consumer_instance_url", b2Context.getServerUrl());

        String target = "frame";
        boolean dimensions = false;
        if (this.tool.getOpenIn().equals(Constants.DATA_WINDOW)) {
            target = "window";
        } else if (this.tool.getOpenIn().equals(Constants.DATA_IFRAME)) {
            target = "iframe";
            dimensions = true;
        } else if (this.tool.getOpenIn().equals(Constants.DATA_POPUP)) {
            target = "popup";
            dimensions = true;
        } else if (this.tool.getOpenIn().equals(Constants.DATA_OVERLAY)) {
            target = "overlay";
            dimensions = true;
        }
        this.props.setProperty("launch_presentation_document_target", target);
        if (dimensions) {
            if (this.tool.getWindowWidth().length() > 0) {
                this.props.setProperty("launch_presentation_width", this.tool.getWindowWidth());
            }
            if (this.tool.getWindowHeight().length() > 0) {
                this.props.setProperty("launch_presentation_height", this.tool.getWindowHeight());
            }
        }

    }

    public String getProperty(String name, String defaultValue) {

        if (this.props.containsKey(name)) {
            defaultValue = this.props.getProperty(name);
        }

        return defaultValue;

    }

    public void setProperty(String name, String value) {

        if (value != null) {
            this.props.setProperty(name, value);
        } else {
            this.props.remove(name);
        }

    }

    public void signParameters(String url, String consumerKey, String secret, String signaturemethod) {

        this.props.setProperty("oauth_callback", Constants.OAUTH_CALLBACK);
        if (signaturemethod.equals(Constants.DATA_SIGNATURE_METHOD_SHA512)) {
            this.props.setProperty("oauth_signature_method", "HMAC-SHA512");
        } else if (signaturemethod.equals(Constants.DATA_SIGNATURE_METHOD_SHA384)) {
            this.props.setProperty("oauth_signature_method", "HMAC-SHA384");
        } else if (signaturemethod.equals(Constants.DATA_SIGNATURE_METHOD_SHA256)) {
            this.props.setProperty("oauth_signature_method", "HMAC-SHA256");
        } else {
            this.props.setProperty("oauth_signature_method", "HMAC-SHA1");
        }
        OAuthMessage oAuthMessage = new OAuthMessage("POST", url, this.props.entrySet());
        OAuthConsumer oAuthConsumer = new OAuthConsumer(Constants.OAUTH_CALLBACK, consumerKey, secret, null);
        OAuthAccessor oAuthAccessor = new OAuthAccessor(oAuthConsumer);
        try {
            oAuthMessage.addRequiredParameters(oAuthAccessor);
            this.params = oAuthMessage.getParameters();
        } catch (OAuthException e) {
            B2Context.log(true, null, (Object) e);
        } catch (IOException e) {
            B2Context.log(true, null, (Object) e);
        } catch (URISyntaxException e) {
            B2Context.log(true, null, (Object) e);
        }

    }

    public List<Map.Entry<String, String>> getParams() {

        List<Map.Entry<String, String>> p;
        if (this.params != null) {
            p = Collections.unmodifiableList(this.params);
        } else {
            p = new ArrayList<Map.Entry<String, String>>();
        }

        return p;

    }

    public NameValuePair[] getHTTPParams() {

        NameValuePair[] nvPairs = null;
        if (this.params != null) {
            nvPairs = new NameValuePair[this.params.size()];
            int i = 0;
            for (Iterator<Map.Entry<String, String>> iter = this.params.iterator(); iter.hasNext();) {
                Map.Entry<String, String> entry = iter.next();
                nvPairs[i] = new NameValuePair(entry.getKey(), entry.getValue());
                i++;
            }
        }

        return nvPairs;

    }

    protected final void addServiceCustomParameters(B2Context b2Context) {

        Map<String, String> customParams = new HashMap<String, String>();
        Service service;
        List<Resource> resources;
        Resource resource;
        ServiceList services = new ServiceList(b2Context, false);
        for (Iterator<Service> iter = services.getList().iterator(); iter.hasNext();) {
            service = iter.next();
            if (this.tool.getHasService(service.getId()).equals(Constants.DATA_TRUE)) {
                service.setTool(tool);
                resources = service.getResources();
                for (Iterator<Resource> iter2 = resources.iterator(); iter2.hasNext();) {
                    resource = iter2.next();
                    customParams.putAll(resource.getCustomParameters(b2Context, this.props));
                }
            }
        }
        List paramList = new ArrayList<String>();
        Map.Entry entry;
        for (Iterator<Map.Entry<String, String>> iter = customParams.entrySet().iterator(); iter.hasNext();) {
            entry = iter.next();
            paramList.add(entry.getKey() + "=" + entry.getValue());
        }
        addParameters(b2Context, (String[]) paramList.toArray(new String[0]), false);

    }

    protected final void addParameters(B2Context b2Context, String[] items, boolean bothCases) {

        String[] item;
        String paramName;
        String name;
        String value;
        for (int i = 0; i < items.length; i++) {
            item = items[i].split("=", 2);
            if (item.length > 0) {
                paramName = item[0];
                if (paramName.length() > 0) {
                    if (item.length > 1) {
                        value = Utils.parseParameter(b2Context, this.props, this.tool, item[1]);
                    } else {
                        value = "";
                    }
                    if (bothCases) {
                        this.props.setProperty(Constants.CUSTOM_NAME_PREFIX + paramName, value);
                    }
                    name = paramName.toLowerCase(Locale.ENGLISH);
                    name = name.replaceAll("[^a-z0-9]", "_");
                    if (!bothCases || !name.equals(paramName)) {
                        this.props.setProperty(Constants.CUSTOM_NAME_PREFIX + name, value);
                    }
                }
            }
        }

    }

}
