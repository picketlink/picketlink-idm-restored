/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.picketlink.idm.internal;

import static org.jboss.picketlink.idm.internal.ldap.LDAPConstants.CN;
import static org.jboss.picketlink.idm.internal.ldap.LDAPConstants.MEMBER;
import static org.jboss.picketlink.idm.internal.ldap.LDAPConstants.OBJECT_CLASS;
import static org.jboss.picketlink.idm.internal.ldap.LDAPConstants.UID;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.jboss.picketlink.idm.internal.config.LDAPConfiguration;
import org.jboss.picketlink.idm.internal.ldap.LDAPChangeNotificationHandler;
import org.jboss.picketlink.idm.internal.ldap.LDAPGroup;
import org.jboss.picketlink.idm.internal.ldap.LDAPObjectChangedNotification;
import org.jboss.picketlink.idm.internal.ldap.LDAPObjectChangedNotification.NType;
import org.jboss.picketlink.idm.internal.ldap.LDAPRole;
import org.jboss.picketlink.idm.internal.ldap.LDAPUser;
import org.jboss.picketlink.idm.internal.ldap.LDAPUserCustomAttributes;
import org.jboss.picketlink.idm.internal.ldap.ManagedAttributeLookup;
import org.jboss.picketlink.idm.internal.util.IDMUtil;
import org.jboss.picketlink.idm.model.DefaultMembership;
import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.Membership;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.User;
import org.jboss.picketlink.idm.query.GroupQuery;
import org.jboss.picketlink.idm.query.MembershipQuery;
import org.jboss.picketlink.idm.query.Range;
import org.jboss.picketlink.idm.query.RoleQuery;
import org.jboss.picketlink.idm.query.UserQuery;
import org.jboss.picketlink.idm.spi.IdentityStore;

/**
 * An IdentityStore implementation backed by an LDAP directory
 *
 * @author Shane Bryzak
 * @author Anil Saldhana
 */
public class LDAPIdentityStore implements IdentityStore, LDAPChangeNotificationHandler, ManagedAttributeLookup {
    public final String COMMA = ",";
    public final String EQUAL = "=";

    protected DirContext ctx = null;
    protected String userDNSuffix, roleDNSuffix, groupDNSuffix;
    protected boolean isActiveDirectory = false;

    protected List<String> managedAttributes = new ArrayList<String>();

    protected LDAPConfiguration ldapConfiguration = null;

    public LDAPIdentityStore() {
    }

    public void setConfiguration(LDAPConfiguration configuration) {
        this.ldapConfiguration = configuration;
        userDNSuffix = configuration.getUserDNSuffix();
        roleDNSuffix = configuration.getRoleDNSuffix();
        groupDNSuffix = configuration.getGroupDNSuffix();
        isActiveDirectory = configuration.isActiveDirectory();

        constructContext();

        /*
         * // Construct the dir ctx Properties env = new Properties(); env.setProperty(Context.INITIAL_CONTEXT_FACTORY,
         * configuration.getFactoryName()); env.setProperty(Context.SECURITY_AUTHENTICATION, configuration.getAuthType());
         *
         * String protocol = configuration.getProtocol(); if (protocol != null) { env.setProperty(Context.SECURITY_PROTOCOL,
         * protocol); } String bindDN = configuration.getBindDN(); char[] bindCredential = null;
         *
         * if (configuration.getBindCredential() != null) { bindCredential = configuration.getBindCredential().toCharArray(); }
         *
         * if (bindDN != null) { env.setProperty(Context.SECURITY_PRINCIPAL, bindDN); env.put(Context.SECURITY_CREDENTIALS,
         * bindCredential); }
         *
         * String url = configuration.getLdapURL(); if (url == null) { throw new RuntimeException("url"); }
         *
         * env.setProperty(Context.PROVIDER_URL, url);
         *
         * try { ctx = new InitialLdapContext(env, null); } catch (NamingException e1) { throw new RuntimeException(e1); }
         */
    }

    @Override
    public User createUser(String name) {
        LDAPUser user = new LDAPUser();
        user.setLookup(this);
        user.setLDAPChangeNotificationHandler(this);

        user.setFullName(name);
        String firstName = getFirstName(name);
        String lastName = getLastName(name);

        user.setFirstName(firstName);
        user.setLastName(lastName);

        // TODO: How do we get the userid?
        String userid = generateUserID(firstName, lastName);
        user.setId(userid);
        user.setUserDNSuffix(userDNSuffix);

        try {
            ctx.bind(UID + "=" + userid + COMMA + userDNSuffix, user);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    @SuppressWarnings("unused")
    @Override
    public void removeUser(User user) {
        try {
            // Look for custom attributes
            LDAPUser ldapUser = (LDAPUser) getUser(user.getFullName());
            String customDN = ldapUser.getCustomAttributes().getDN() + COMMA + ldapUser.getDN();
            try {
                LDAPUserCustomAttributes lca = (LDAPUserCustomAttributes) ctx.lookup(customDN);
                ctx.destroySubcontext(customDN);
            } catch (Exception ignore) {
            }
            ctx.destroySubcontext(UID + "=" + user.getId() + COMMA + userDNSuffix);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User getUser(String name) {
        LDAPUser user = null;
        try {
            Attributes matchAttrs = new BasicAttributes(true); // ignore attribute name case
            matchAttrs.put(new BasicAttribute(CN, name));

            NamingEnumeration<SearchResult> answer = ctx.search(userDNSuffix, matchAttrs);
            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                Attributes attributes = sr.getAttributes();
                user = new LDAPUser();
                user.setLookup(this);
                user.setUserDNSuffix(userDNSuffix);
                user.addAllLDAPAttributes(attributes);

                user.setLDAPChangeNotificationHandler(this);

                // Get the custom attributes
                String customDN = user.getCustomAttributes().getDN() + COMMA + user.getDN();
                try {
                    LDAPUserCustomAttributes lca = (LDAPUserCustomAttributes) ctx.lookup(customDN);
                    if (lca != null) {
                        user.setCustomAttributes(lca);
                    }
                } catch (Exception ignore) {
                }
                break;
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    @Override
    public Group createGroup(String name, Group parent) {
        ensureGroupDNExists();
        LDAPGroup ldapGroup = new LDAPGroup();
        ldapGroup.setLDAPChangeNotificationHandler(this);

        ldapGroup.setName(name);
        ldapGroup.setGroupDNSuffix(groupDNSuffix);

        try {
            ctx.bind(CN + "=" + name + COMMA + groupDNSuffix, ldapGroup);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        if (parent != null) {
            ldapGroup.setParentGroup(parent);

            LDAPGroup parentGroup = (LDAPGroup) getGroup(parent.getName());
            ldapGroup.setParentGroup(parentGroup);
            parentGroup.addChildGroup(ldapGroup);
            try {
                ctx.rebind(CN + "=" + parentGroup.getName() + COMMA + groupDNSuffix, parentGroup);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
        return ldapGroup;
    }

    @Override
    public void removeGroup(Group group) {
        try {
            ctx.destroySubcontext(CN + "=" + group.getName() + COMMA + groupDNSuffix);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Group getGroup(String name) {
        LDAPGroup ldapGroup = null;
        try {
            Attributes matchAttrs = new BasicAttributes(true); // ignore attribute name case
            matchAttrs.put(new BasicAttribute(CN, name));

            NamingEnumeration<SearchResult> answer = ctx.search(groupDNSuffix, matchAttrs);
            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                Attributes attributes = sr.getAttributes();
                ldapGroup = new LDAPGroup();
                ldapGroup.setGroupDNSuffix(groupDNSuffix);
                ldapGroup.addAllLDAPAttributes(attributes);
                // Let us work out any parent groups for this group exist
                Group parentGroup = parentGroup(ldapGroup);
                if (parentGroup != null) {
                    ldapGroup.setParentGroup(parentGroup);
                }
                ldapGroup.setLDAPChangeNotificationHandler(this);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return ldapGroup;
    }

    @Override
    public Role createRole(String name) {
        LDAPRole role = new LDAPRole();
        role.setLDAPChangeNotificationHandler(this);

        role.setName(name);
        role.setRoleDNSuffix(roleDNSuffix);

        try {
            ctx.bind(CN + "=" + name + COMMA + roleDNSuffix, role);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    @Override
    public void removeRole(Role role) {
        try {
            ctx.destroySubcontext(CN + "=" + role.getName() + COMMA + roleDNSuffix);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Role getRole(String role) {
        LDAPRole ldapRole = null;
        try {
            Attributes matchAttrs = new BasicAttributes(true); // ignore attribute name case
            matchAttrs.put(new BasicAttribute(CN, role));

            NamingEnumeration<SearchResult> answer = ctx.search(roleDNSuffix, matchAttrs);
            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                Attributes attributes = sr.getAttributes();
                ldapRole = new LDAPRole();
                ldapRole.setRoleDNSuffix(roleDNSuffix);
                ldapRole.addAllLDAPAttributes(attributes);
                ldapRole.setLDAPChangeNotificationHandler(this);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return ldapRole;
    }

    @Override
    public Membership createMembership(Role role, User user, Group group) {
        final LDAPRole ldapRole = (LDAPRole) getRole(role.getName());
        final LDAPUser ldapUser = (LDAPUser) getUser(user.getFullName());
        final LDAPGroup ldapGroup = (LDAPGroup) getGroup(group.getName());

        ldapRole.addUser(ldapUser);
        ldapGroup.addRole(ldapRole);
        return new DefaultMembership(ldapUser, ldapRole, ldapGroup);
    }

    @Override
    public void removeMembership(Role role, User user, Group group) {
        final LDAPRole ldapRole = (LDAPRole) getRole(role.getName());
        final LDAPUser ldapUser = (LDAPUser) getUser(user.getFullName());
        final LDAPGroup ldapGroup = (LDAPGroup) getGroup(group.getName());

        ldapRole.removeUser(ldapUser);
        ldapGroup.removeRole(ldapRole);
    }

    @Override
    public Membership getMembership(Role role, User user, Group group) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<User> executeQuery(UserQuery query, Range range) {
        // TODO: Deal with range

        List<User> users = new ArrayList<User>();
        Map<String, String[]> filters = query.getAttributeFilters();
        if (filters != null) {
            // we are dealing with attributes
            // Get all the managed attributes first to do the search
            Attributes matchAttrs = getManagedAttributes(filters);
            if (matchAttrs.size() == 0) {
                // go for custom attributes
                List<User> allUsers = getAllUsers();
                for (User theUser : allUsers) {
                    if (userHasRequiredAttributes((LDAPUser) theUser, filters)) {
                        users.add(theUser);
                    }
                }
                return users;
            }

            // Perform the search
            try {
                NamingEnumeration<SearchResult> answer = ctx.search(userDNSuffix, matchAttrs);
                while (answer.hasMore()) {
                    SearchResult sr = answer.next();
                    Attributes attributes = sr.getAttributes();

                    LDAPUser user = new LDAPUser();
                    user.setLookup(this);
                    user.setUserDNSuffix(userDNSuffix);
                    user.addAllLDAPAttributes(attributes);

                    user.setLDAPChangeNotificationHandler(this);

                    // Get the custom attributes
                    String customDN = user.getCustomAttributes().getDN() + COMMA + user.getDN();
                    try {
                        LDAPUserCustomAttributes lca = (LDAPUserCustomAttributes) ctx.lookup(customDN);
                        if (lca != null) {
                            user.setCustomAttributes(lca);
                        }
                    } catch (Exception ignore) {
                    }
                    if (userHasRequiredAttributes(user, filters)) {
                        users.add(user);
                    }
                }
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
        return users;
    }

    @Override
    public List<Group> executeQuery(GroupQuery query, Range range) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Role> executeQuery(RoleQuery query, Range range) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Membership> executeQuery(MembershipQuery query, Range range) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttribute(User user, String name, String[] values) {
        LDAPUser ldapUser = null;

        if (user instanceof LDAPUser) {
            ldapUser = (LDAPUser) user;
        } else {
            ldapUser = (LDAPUser) getUser(user.getFullName());
        }
        if (isManaged(name)) {
            ldapUser.setAttribute(name, values);
        } else {
            ldapUser.setCustomAttribute(name, values);
        }
    }

    @Override
    public void removeAttribute(User user, String name) {
        if (user instanceof LDAPUser == false) {
            throw new RuntimeException("Wrong type:" + user);
        }
        LDAPUser ldapUser = (LDAPUser) user;
        ldapUser.removeAttribute(name);
    }

    @Override
    public String[] getAttributeValues(User user, String name) {
        if (user instanceof LDAPUser == false) {
            throw new RuntimeException("Wrong type:" + user);
        }
        LDAPUser ldapUser = (LDAPUser) user;
        return ldapUser.getAttributeValues(name);
    }

    @Override
    public Map<String, String[]> getAttributes(User user) {
        if (user instanceof LDAPUser == false) {
            throw new RuntimeException("Wrong type:" + user);
        }
        LDAPUser ldapUser = (LDAPUser) user;
        return ldapUser.getAttributes();
    }

    @Override
    public void setAttribute(Group group, String name, String[] values) {
        LDAPGroup ldapGroup = null;
        if (group instanceof LDAPGroup) {
            ldapGroup = (LDAPGroup) group;
        } else {
            ldapGroup = (LDAPGroup) getGroup(group.getName());
        }
        ldapGroup.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(Group group, String name) {
        LDAPGroup ldapGroup = null;
        if (group instanceof LDAPGroup) {
            ldapGroup = (LDAPGroup) group;
        } else {
            ldapGroup = (LDAPGroup) getGroup(group.getName());
        }
        ldapGroup.removeAttribute(name);
    }

    @Override
    public String[] getAttributeValues(Group group, String name) {
        LDAPGroup ldapGroup = null;
        if (group instanceof LDAPGroup) {
            ldapGroup = (LDAPGroup) group;
        } else {
            ldapGroup = (LDAPGroup) getGroup(group.getName());
        }
        return ldapGroup.getAttributeValues(name);
    }

    @Override
    public Map<String, String[]> getAttributes(Group group) {
        LDAPGroup ldapGroup = null;
        if (group instanceof LDAPGroup) {
            ldapGroup = (LDAPGroup) group;
        } else {
            ldapGroup = (LDAPGroup) getGroup(group.getName());
        }
        return ldapGroup.getAttributes();
    }

    @Override
    public void setAttribute(Role role, String name, String[] values) {
        LDAPRole ldapRole = null;
        if (role instanceof LDAPGroup) {
            ldapRole = (LDAPRole) role;
        } else {
            ldapRole = (LDAPRole) getRole(role.getName());
        }
        ldapRole.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(Role role, String name) {
        LDAPRole ldapRole = null;
        if (role instanceof LDAPGroup) {
            ldapRole = (LDAPRole) role;
        } else {
            ldapRole = (LDAPRole) getRole(role.getName());
        }
        ldapRole.removeAttribute(name);
    }

    @Override
    public String[] getAttributeValues(Role role, String name) {
        LDAPRole ldapRole = null;
        if (role instanceof LDAPGroup) {
            ldapRole = (LDAPRole) role;
        } else {
            ldapRole = (LDAPRole) getRole(role.getName());
        }
        return ldapRole.getAttributeValues(name);
    }

    @Override
    public Map<String, String[]> getAttributes(Role role) {
        LDAPRole ldapRole = null;
        if (ldapRole instanceof LDAPRole) {
            ldapRole = (LDAPRole) role;
        } else {
            ldapRole = (LDAPRole) getRole(role.getName());
        }
        return ldapRole.getAttributes();
    }

    protected String getFirstName(String name) {
        String[] tokens = name.split("\\ ");
        int length = tokens.length;
        String firstName = null;

        if (length > 0) {
            firstName = tokens[0];
        }
        return firstName;
    }

    protected String getLastName(String name) {
        String[] tokens = name.split("\\ ");
        int length = tokens.length;
        String lastName = null;

        if (length > 2) {
            lastName = tokens[2];
        } else {
            lastName = tokens[1];
        }
        return lastName;
    }

    protected String generateUserID(String firstName, String lastName) {
        char f = firstName.charAt(0);
        StringBuilder builder = new StringBuilder();
        builder.append(f).append(lastName);

        String userID = builder.toString();
        int length = userID.length();
        if (length > 7) {
            return userID.substring(0, 7);
        } else {
            return userID;
        }
    }

    protected void ensureGroupDNExists() {
        try {
            Object obj = ctx.lookup(groupDNSuffix);
            if (obj == null) {
                createGroupDN();
            }
            return; // exists
        } catch (NamingException e) {
            if (e instanceof NameNotFoundException) {
                createGroupDN();
                return;
            }
            throw new RuntimeException(e);
        }
    }

    protected void createGroupDN() {
        try {
            Attributes attributes = new BasicAttributes(true);

            Attribute oc = new BasicAttribute(OBJECT_CLASS);
            oc.add("top");
            oc.add("organizationalUnit");
            attributes.put(oc);
            ctx.createSubcontext(groupDNSuffix, attributes);
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    // Get the parent group by searching
    protected Group parentGroup(LDAPGroup group) {
        Attributes matchAttrs = new BasicAttributes(true);
        matchAttrs.put(new BasicAttribute(MEMBER, CN + EQUAL + group.getName() + COMMA + groupDNSuffix));
        // Search for objects with these matching attributes
        try {
            NamingEnumeration<SearchResult> answer = ctx.search(groupDNSuffix, matchAttrs, new String[] { CN });
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.nextElement();
                Attributes attributes = sr.getAttributes();
                String cn = (String) attributes.get(CN).get();
                return getGroup(cn);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void handle(LDAPObjectChangedNotification notification) {
        DirContext object = notification.getLDAPObject();
        if (object instanceof LDAPUser) {
            LDAPUser user = (LDAPUser) object;
            LDAPUserCustomAttributes ldapUserCustomAttributes = user.getCustomAttributes();
            try {
                String userDN = user.getDN();
                if (notification.getNtype() == NType.ADD_ATTRIBUTE) {
                    Attribute attrib = notification.getAttribute();
                    if (attrib == null)
                        throw new RuntimeException("attrib is null");
                    ModificationItem[] mods = new ModificationItem[] { new ModificationItem(DirContext.ADD_ATTRIBUTE, attrib) };
                    ctx.modifyAttributes(user.getDN(), mods);
                }
                if (notification.getNtype() == NType.REMOVE_ATTRIBUTE) {
                    Attribute attrib = notification.getAttribute();
                    if (attrib == null)
                        throw new RuntimeException("attrib is null");
                    ModificationItem[] mods = new ModificationItem[] { new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attrib) };
                    ctx.modifyAttributes(user.getDN(), mods);
                }
                // ctx.rebind(userDN, object);
                ctx.rebind(ldapUserCustomAttributes.getDN() + COMMA + userDN, ldapUserCustomAttributes);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isManaged(String attributeName) {
        if (managedAttributes.contains(attributeName)) {
            return true;
        } else {
            if (checkDirectoryServerForAttributePresence(attributeName)) {
                managedAttributes.add(attributeName);
                return true;
            }
        }
        return false;
    }

    /**
     * Ask the ldap server for the schema for the attribute
     *
     * @param attributeName
     * @return
     */
    private boolean checkDirectoryServerForAttributePresence(String attributeName) {

        try {
            DirContext schema = ctx.getSchema("");

            DirContext cnSchema = (DirContext) schema.lookup("AttributeDefinition/" + attributeName);
            if (cnSchema != null) {
                return true;
            }
        } catch (Exception e) {
            return false; // Probably an unmanaged attribute
        }

        return false;
    }

    private Attributes getManagedAttributes(Map<String, String[]> filters) {
        Attributes attr = new BasicAttributes(true);
        Set<String> keys = filters.keySet();
        for (String key : keys) {
            if (isManaged(key)) {
                attr.put(key, filters.get(key));
            }
        }
        return attr;
    }

    private boolean userHasRequiredAttributes(LDAPUser user, Map<String, String[]> filters) {
        Set<String> keys = filters.keySet();

        for (String key : keys) {
            String[] values = filters.get(key);
            String[] attValues = user.getAttributeValues(key);
            if (IDMUtil.arraysEqual(values, attValues) == false) {
                return false;
            }
        }
        return true;
    }

    private List<User> getAllUsers() {
        List<User> users = new ArrayList<User>();
        // Perform the search
        try {
            Attributes attr = new BasicAttributes(true);
            NamingEnumeration<SearchResult> answer = ctx.search(userDNSuffix, attr);
            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                Attributes attributes = sr.getAttributes();
                LDAPUser user = new LDAPUser();
                user.setLookup(this);
                user.setUserDNSuffix(userDNSuffix);
                user.addAllLDAPAttributes(attributes);

                user.setLDAPChangeNotificationHandler(this);

                // Get the custom attributes
                String customDN = user.getCustomAttributes().getDN() + COMMA + user.getDN();
                try {
                    LDAPUserCustomAttributes lca = (LDAPUserCustomAttributes) ctx.lookup(customDN);
                    if (lca != null) {
                        user.setCustomAttributes(lca);
                    }
                } catch (Exception ignore) {
                }
                users.add(user);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    @Override
    public MembershipQuery createMembershipQuery() {
        throw new RuntimeException();
    }

    public boolean validatePassword(User user, String password) {
        boolean valid = false;
        // We have to bind
        try {
            LDAPUser ldapUser = (LDAPUser) user;
            String filter = "(&(objectClass=inetOrgPerson)(uid={0}))";
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            ctls.setReturningAttributes(new String[0]);
            ctls.setReturningObjFlag(true);
            NamingEnumeration<SearchResult> enm = ctx.search(userDNSuffix, filter, new String[] { ldapUser.getId() }, ctls);

            String dn = null;
            if (enm.hasMore()) {
                SearchResult result = enm.next();
                dn = result.getNameInNamespace();

                System.out.println("dn: " + dn);
            }

            if (dn == null || enm.hasMore()) {
                // uid not found or not unique
                throw new NamingException("Authentication failed");
            }

            // Step 3: Bind with found DN and given password
            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
            // Perform a lookup in order to force a bind operation with JNDI
            ctx.lookup(dn);
            valid = true;
        } catch (NamingException e) {
            // Ignore
        }

        constructContext();
        return valid;
    }

    @Override
    public void updatePassword(User user, String password) {
        if (isActiveDirectory) {
            updateADPassword((LDAPUser) user, password);
        } else {
            LDAPUser ldapuser = (LDAPUser) user;

            ModificationItem[] mods = new ModificationItem[1];

            Attribute mod0 = new BasicAttribute("userpassword", password);

            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);

            try {
                ctx.modifyAttributes(ldapuser.getDN(), mods);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean validateCertificate(User user, X509Certificate certificate) {
        return false;
    }

    @Override
    public boolean updateCertificate(User user, X509Certificate certificate) {
        return false;
    }

    private void constructContext() {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ignore) {

            }
        }
        // Construct the dir ctx
        Properties env = new Properties();
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, ldapConfiguration.getFactoryName());
        env.setProperty(Context.SECURITY_AUTHENTICATION, ldapConfiguration.getAuthType());

        String protocol = ldapConfiguration.getProtocol();
        if (protocol != null) {
            env.setProperty(Context.SECURITY_PROTOCOL, protocol);
        }
        String bindDN = ldapConfiguration.getBindDN();
        char[] bindCredential = null;

        if (ldapConfiguration.getBindCredential() != null) {
            bindCredential = ldapConfiguration.getBindCredential().toCharArray();
        }

        if (bindDN != null) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.put(Context.SECURITY_CREDENTIALS, bindCredential);
        }

        String url = ldapConfiguration.getLdapURL();
        if (url == null) {
            throw new RuntimeException("url");
        }

        env.setProperty(Context.PROVIDER_URL, url);

        // Just dump the additional properties
        Properties additionalProperties = ldapConfiguration.getAdditionalProperties();
        Set<Object> keys = additionalProperties.keySet();
        for (Object key : keys) {
            env.setProperty((String) key, additionalProperties.getProperty((String) key));
        }

        try {
            ctx = new InitialLdapContext(env, null);
        } catch (NamingException e1) {
            throw new RuntimeException(e1);
        }
    }

    // Remember the updation has to happen over SSL. That is handled by the JNDI Ctx Parameters
    private void updateADPassword(LDAPUser user, String password) {
        try {
            // set password is a ldap modfy operation
            ModificationItem[] mods = new ModificationItem[1];

            // Replace the "unicdodePwd" attribute with a new value
            // Password must be both Unicode and a quoted string
            String newQuotedPassword = "\"" + password + "\"";
            byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");

            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", newUnicodePassword));

            // Perform the update
            ctx.modifyAttributes(user.getDN(), mods);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}