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

package org.picketlink.idm.internal.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.picketlink.idm.internal.jpa.DefaultMembershipQuery;
import org.picketlink.idm.internal.util.Base64;
import org.picketlink.idm.model.Group;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Membership;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.User;
import org.picketlink.idm.password.PasswordValidator;
import org.picketlink.idm.query.GroupQuery;
import org.picketlink.idm.query.MembershipQuery;
import org.picketlink.idm.query.Range;
import org.picketlink.idm.query.RoleQuery;
import org.picketlink.idm.query.UserQuery;
import org.picketlink.idm.spi.IdentityStore;

/**
 * <p>
 * File based {@link IdentityStore} implementation. By default, each new instance recreate the data files. This behaviour can be
 * changed by configuring the <code>alwaysCreateFiles</code> property to false.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class FileBasedIdentityStore implements IdentityStore {

    private static final String USER_PASSWORD_ATTRIBUTE = "userPassword";
    private static final String USER_CERTIFICATE_ATTRIBUTE_NAME = "userCertificate";
    
    private File usersFile;
    private File rolesFile = new File("/tmp/pl-idm-work/pl-idm-roles.db");
    private File groupsFile = new File("/tmp/pl-idm-work/pl-idm-groups.db");
    private File membershipsFile = new File("/tmp/pl-idm-work/pl-idm-memberships.db");

    private Map<String, FileUser> users = new HashMap<String, FileUser>();
    private Map<String, Role> roles = new HashMap<String, Role>();
    private Map<String, FileGroup> groups = new HashMap<String, FileGroup>();
    private List<FileMembership> memberships = new ArrayList<FileMembership>();

    private FileChangeListener changeListener = new FileChangeListener(this);
    private String workingDir;
    private boolean alwaysCreateFiles = true;

    public FileBasedIdentityStore() {
        initialize();
    }

    public FileBasedIdentityStore(String workingDir, boolean alwaysCreateFiles) {
        this.workingDir = workingDir;
        this.alwaysCreateFiles = alwaysCreateFiles;
        initialize();
    }

    /**
     * <p>Initializes the store.</p>
     */
    private void initialize() {
        initDataFiles();

        loadUsers();
        loadRoles();
        loadGroups();
        loadMemberships();
    }

    /**
     * <p>
     * Initializes the files used to store the informations.
     * </p>
     */
    private void initDataFiles() {
        File workingDirectoryFile = initWorkingDirectory();

        this.usersFile = checkAndCreateFile(new File(workingDirectoryFile.getPath() + "/pl-idm-users.db"));
        this.rolesFile = checkAndCreateFile(new File(workingDirectoryFile.getPath() + "/pl-idm-roles.db"));
        this.groupsFile = checkAndCreateFile(new File(workingDirectoryFile.getPath() + "/pl-idm-groups.db"));
        this.membershipsFile = checkAndCreateFile(new File(workingDirectoryFile.getPath() + "/pl-idm-memberships.db"));
    }

    /**
     * <p>
     * Initializes the working directory.
     * </p>
     * 
     * @return
     */
    private File initWorkingDirectory() {
        String workingDir = getWorkingDir();

        if (workingDir == null) {
            workingDir = System.getProperty("java.io.tmpdir");
        }

        File workingDirectoryFile = new File(workingDir);

        if (!workingDirectoryFile.exists()) {
            workingDirectoryFile.mkdirs();
        }

        return workingDirectoryFile;
    }

    /**
     * <p>Check if the specified {@link File} exists. If not create it.</p>
     * 
     * @param file
     * @return
     */
    private File checkAndCreateFile(File file) {
        if (this.alwaysCreateFiles && file.exists()) {
            file.delete();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }

        return file;
    }

    /**
     * <p>Load all persisted groups from the filesystem.</p>
     */
    private void loadGroups() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(groupsFile);
            ois = new ObjectInputStream(fis);

            this.groups = (Map<String, FileGroup>) ois.readObject();
        } catch (Exception e) {
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * <p>Load all persisted memberships from the filesystem.</p>
     */
    private void loadMemberships() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(membershipsFile);
            ois = new ObjectInputStream(fis);

            this.memberships = (List<FileMembership>) ois.readObject();
        } catch (Exception e) {
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * <p>Load all persisted roles from the filesystem.</p>
     */
    private void loadRoles() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(rolesFile);
            ois = new ObjectInputStream(fis);

            this.roles = (Map<String, Role>) ois.readObject();
        } catch (Exception e) {
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * <p>Load all persisted users from the filesystem.</p>
     */
    private void loadUsers() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(usersFile);
            ois = new ObjectInputStream(fis);

            this.users = (Map<String, FileUser>) ois.readObject();
        } catch (Exception e) {
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * <p>Flush all changes made to users to the filesystem.</p>
     */
    synchronized void flushUsers() {
        try {
            FileOutputStream fos = new FileOutputStream(this.usersFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.users);
            oos.close();
        } catch (Exception e) {
        }
    }

    /**
     * <p>Flush all changes made to roles to the filesystem.</p>
     */
    synchronized void flushRoles() {
        try {
            FileOutputStream fos = new FileOutputStream(this.rolesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.roles);
            oos.close();
        } catch (Exception e) {
        }
    }

    /**
     * <p>Flush all changes made to groups to the filesystem.</p>
     */
    synchronized void flushGroups() {
        try {
            FileOutputStream fos = new FileOutputStream(this.groupsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.groups);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Flush all changes made to memberships to the filesystem.</p>
     */
    synchronized void flushMemberships() {
        try {
            FileOutputStream fos = new FileOutputStream(this.membershipsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.memberships);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#validatePassword(org.picketlink.idm.model.User, java.lang.String)
     */
    @Override
    public boolean validatePassword(User user, String password) {
        User storedUser = getUser(user.getId());
        String storedPassword = storedUser.getAttribute(USER_PASSWORD_ATTRIBUTE);

        return storedPassword != null && storedPassword.equals(password);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#updatePassword(org.picketlink.idm.model.User, java.lang.String)
     */
    @Override
    public void updatePassword(User user, String password) {
        User storedUser = getUser(user.getId());

        storedUser.setAttribute(USER_PASSWORD_ATTRIBUTE, password);
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#validateCertificate(org.picketlink.idm.model.User, java.security.cert.X509Certificate)
     */
    @Override
    public boolean validateCertificate(User user, X509Certificate certificate) {
        User storedUser = getUser(user.getKey());
        
        if (storedUser == null) {
            return false;
        }

        String encodedCertificate = storedUser.getAttribute(USER_CERTIFICATE_ATTRIBUTE_NAME);
        
        try {
            return encodedCertificate != null && encodedCertificate.equals(new String(Base64.encodeBytes(certificate.getEncoded())));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Error encoding certificate.", e);
        }
    }

    @Override
    public boolean updateCertificate(User user, X509Certificate certificate) {
        User storedUser = getUser(user.getKey());
        
        if (storedUser == null) {
            return false;
        }
        
        try {
            storedUser.setAttribute(USER_CERTIFICATE_ATTRIBUTE_NAME, new String(Base64.encodeBytes(certificate.getEncoded())));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Error encoding certificate.", e);
        }
        
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#createUser(java.lang.String)
     */
    @Override
    public User createUser(String name) {
        FileUser user = new FileUser(name);

        user.setChangeListener(this.changeListener);

        this.users.put(user.getId(), user);

        flushUsers();

        return user;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#createUser(org.picketlink.idm.model.User)
     */
    @Override
    public User createUser(User user) {
        this.users.put(user.getId(), (FileUser) user);

        flushUsers();

        return user;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#removeUser(org.picketlink.idm.model.User)
     */
    @Override
    public void removeUser(User user) {
        this.users.remove(user.getId());

        flushUsers();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#getUser(java.lang.String)
     */
    @Override
    public User getUser(String name) {
        FileUser user = this.users.get(name);

        if (user != null) {
            user.setChangeListener(this.changeListener);
        }

        return user;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#createGroup(java.lang.String, org.picketlink.idm.model.Group)
     */
    @Override
    public Group createGroup(String name, Group parent) {
        FileGroup group = new FileGroup(name, parent);

        this.groups.put(group.getName(), group);

        group.setChangeListener(this.changeListener);

        flushGroups();

        return group;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#removeGroup(org.picketlink.idm.model.Group)
     */
    @Override
    public void removeGroup(Group group) {
        this.groups.remove(group.getName());
        flushGroups();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#getGroup(java.lang.String)
     */
    @Override
    public Group getGroup(String name) {
        FileGroup group = this.groups.get(name);

        if (group != null) {
            group.setChangeListener(this.changeListener);
        }

        return group;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#createRole(java.lang.String)
     */
    @Override
    public Role createRole(String name) {
        FileRole role = new FileRole(name);

        this.roles.put(role.getName(), role);

        role.setChangeListener(this.changeListener);

        flushRoles();

        return role;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#removeRole(org.picketlink.idm.model.Role)
     */
    @Override
    public void removeRole(Role role) {
        this.roles.remove(role.getName());
        flushRoles();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#getRole(java.lang.String)
     */
    @Override
    public Role getRole(String role) {
        FileRole fileRole = (FileRole) this.roles.get(role);

        if (fileRole != null) {
            fileRole.setChangeListener(this.changeListener);
        }

        return fileRole;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#createMembership(org.picketlink.idm.model.Role, org.picketlink.idm.model.User,
     * org.picketlink.idm.model.Group)
     */
    @Override
    public Membership createMembership(Role role, User user, Group group) {
        FileMembership membership = new FileMembership(role, user, group);

        this.memberships.add(membership);

        flushMemberships();

        return membership;
    }

    @Override
    public void removeMembership(Role role, User user, Group group) {
        for (Membership membership : new ArrayList<FileMembership>(this.memberships)) {
            boolean match = false;

            if (role != null) {
                match = membership.getRole() != null && role.equals(membership.getRole());
            } else {
                match = true;
            }

            if (user != null) {
                match = membership.getUser() != null && user.equals(membership.getUser());
            } else {
                match = true;
            }

            if (group != null) {
                match = membership.getGroup() != null && group.equals(membership.getGroup());
            } else {
                match = true;
            }

            this.memberships.remove(membership);
        }

        flushMemberships();
    }

    @Override
    public Membership getMembership(Role role, User user, Group group) {
        for (Membership membership : new ArrayList<FileMembership>(this.memberships)) {
            boolean match = false;

            if (role != null) {
                match = membership.getRole() != null && role.equals(membership.getRole());
            } else {
                match = true;
            }

            if (user != null) {
                match = membership.getUser() != null && user.equals(membership.getUser());
            } else {
                match = true;
            }

            if (group != null) {
                match = membership.getGroup() != null && group.equals(membership.getGroup());
            } else {
                match = true;
            }

            if (match) {
                return membership;
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.UserQuery,
     * org.picketlink.idm.query.Range)
     */
    @Override
    public List<User> executeQuery(UserQuery query, Range range) {
        List<User> users = new ArrayList<User>();

        for (Entry<String, FileUser> entry : this.users.entrySet()) {
            FileUser fileUser = entry.getValue();

            if (query.getName() != null) {
                if (!fileUser.getKey().equals(query.getName())) {
                    continue;
                }
            }

            if (query.getEnabled() != fileUser.isEnabled()) {
                continue;
            }

            if (query.getEmail() != null) {
                if (!query.getEmail().equals(fileUser.getEmail())) {
                    continue;
                }
            }

            if (query.getFirstName() != null) {
                if (!query.getFirstName().equals(fileUser.getFirstName())) {
                    continue;
                }
            }

            if (query.getLastName() != null) {
                if (!query.getLastName().equals(fileUser.getLastName())) {
                    continue;
                }
            }

            users.add(fileUser);
        }

        Collection<? extends User> selectedUsers = users;

        if (users.isEmpty()) {
            selectedUsers = this.users.values();
        }

        if (query.getRole() != null || query.getRelatedGroup() != null) {
            List<User> fileteredUsers = new ArrayList<User>();

            for (User fileUser : new ArrayList<User>(selectedUsers)) {
                for (Membership membership : this.memberships) {
                    if ((query.getRole() != null && membership.getRole() == null)
                            || (query.getRelatedGroup() != null && membership.getGroup() == null)
                            || membership.getUser() == null) {
                        continue;
                    }

                    if (!membership.getUser().equals(fileUser)) {
                        continue;
                    }

                    if (query.getRole() != null) {
                        if (!membership.getRole().equals(query.getRole())) {
                            continue;
                        }
                    }

                    if (query.getRelatedGroup() != null) {
                        if (!membership.getGroup().equals(query.getRelatedGroup())) {
                            continue;
                        }
                    }

                    fileteredUsers.add(fileUser);
                }
            }

            users.retainAll(fileteredUsers);
        }

        Map<String, String[]> queryAttributes = query.getAttributeFilters();
        
        searchForIdentityTypeAttributes(users, queryAttributes);

        return users;
    }

    private void searchForIdentityTypeAttributes(List<? extends IdentityType> users, Map<String, String[]> queryAttributes) {
        if (queryAttributes != null) {
            Set<Entry<String, String[]>> entrySet = queryAttributes.entrySet();

            for (IdentityType fileUser : new ArrayList<IdentityType>(users)) {
                for (Entry<String, String[]> entry : entrySet) {
                    String searchAttributeKey = entry.getKey();
                    String[] searchAttributeValue = entry.getValue();

                    String[] userAttributes = fileUser.getAttributeValues(searchAttributeKey);

                    if (userAttributes == null) {
                        users.remove(fileUser);
                        continue;
                    }

                    if (Collections.indexOfSubList(Arrays.asList(userAttributes), Arrays.asList(searchAttributeValue)) > 0) {
                        users.remove(fileUser);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.GroupQuery,
     * org.picketlink.idm.query.Range)
     */
    @Override
    public List<Group> executeQuery(GroupQuery query, Range range) {
        List<Group> groups = new ArrayList<Group>();

        for (Entry<String, FileGroup> entry : this.groups.entrySet()) {
            FileGroup fileGroup = entry.getValue();

            if (query.getName() != null) {
                if (!fileGroup.getKey().equals(query.getName())) {
                    continue;
                }
            }

            if (query.getId() != null) {
                if (!query.getId().equals(fileGroup.getId())) {
                    continue;
                }
            }

            if (query.getParentGroup() != null) {
                if (fileGroup.getParentGroup() == null || !query.getParentGroup().equals(fileGroup.getParentGroup())) {
                    continue;
                }
            }

            groups.add(fileGroup);
        }

        Collection<? extends Group> selectedGroups = groups;

        if (groups.isEmpty()) {
            selectedGroups = this.groups.values();
        }

        if (query.getRole() != null || query.getRelatedUser() != null) {
            List<Group> fileteredGroups = new ArrayList<Group>();

            for (Group fileGroup : new ArrayList<Group>(selectedGroups)) {
                for (Membership membership : this.memberships) {
                    if ((query.getRole() != null && membership.getRole() == null)
                            || (query.getRelatedUser() != null && membership.getUser() == null)
                            || membership.getGroup() == null) {
                        continue;
                    }

                    if (!membership.getGroup().equals(fileGroup)) {
                        continue;
                    }

                    if (query.getRole() != null) {
                        if (!membership.getRole().equals(query.getRole())) {
                            continue;
                        }
                    }

                    if (query.getRelatedUser() != null) {
                        if (!membership.getUser().equals(query.getRelatedUser())) {
                            continue;
                        }
                    }

                    fileteredGroups.add(fileGroup);
                }
            }

            groups.retainAll(fileteredGroups);
        }

        if (query.getAttributeFilters() != null && !query.getAttributeFilters().isEmpty()) {
            searchForIdentityTypeAttributes(groups, query.getAttributeFilters());
        }

        return groups;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.RoleQuery,
     * org.picketlink.idm.query.Range)
     */
    @Override
    public List<Role> executeQuery(RoleQuery query, Range range) {
        List<Role> roles = new ArrayList<Role>();

        if (query.getName() != null) {
            Role role = getRole(query.getName());

            if (role != null) {
                roles.add(role);
            }
        }

        if (query.getOwner() != null || query.getGroup() != null) {
            for (Membership membership : this.memberships) {
                if (membership.getRole() == null) {
                    continue;
                }

                if (query.getOwner() != null) {
                    if (!(membership.getUser() != null && membership.getUser().getKey().equals(query.getOwner().getKey()))) {
                        continue;
                    }
                }

                if (query.getGroup() != null) {
                    if (!(membership.getGroup() != null && membership.getGroup().getKey().equals(query.getGroup().getKey()))) {
                        continue;
                    }
                }

                roles.add(membership.getRole());
            }
        }
        
        if (query.getAttributeFilters() != null && !query.getAttributeFilters().isEmpty()) {
            searchForIdentityTypeAttributes(roles, query.getAttributeFilters());
        }

        return roles;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.MembershipQuery,
     * org.picketlink.idm.query.Range)
     */
    @Override
    public List<Membership> executeQuery(MembershipQuery query, Range range) {
        List<Membership> memberships = new ArrayList<Membership>();

        for (Membership membership : this.memberships) {
            if ((query.getRole() != null && membership.getRole() == null)
                    || (query.getGroup() != null && membership.getGroup() == null)
                    || (query.getUser() != null && membership.getUser() == null)) {
                continue;
            }

            if (query.getRole() != null) {
                if (!membership.getRole().equals(query.getRole())) {
                    continue;
                }
            }

            if (query.getGroup() != null) {
                if (!membership.getGroup().equals(query.getGroup())) {
                    continue;
                }
            }

            if (query.getUser() != null) {
                if (!membership.getUser().equals(query.getUser())) {
                    continue;
                }
            }

            memberships.add(membership);
        }

        return memberships;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#setAttribute(org.picketlink.idm.model.User, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public void setAttribute(User user, String name, String[] values) {
        FileUser fileUser = (FileUser) getUser(user.getId());

        fileUser.setAttribute(name, values);

        flushUsers();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#removeAttribute(org.picketlink.idm.model.User, java.lang.String)
     */
    @Override
    public void removeAttribute(User user, String name) {
        FileUser fileUser = (FileUser) getUser(user.getId());

        if (fileUser != null) {
            this.users.remove(fileUser.getId());
        }

        flushUsers();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#getAttributeValues(org.picketlink.idm.model.User, java.lang.String)
     */
    @Override
    public String[] getAttributeValues(User user, String name) {
        FileUser fileUser = (FileUser) getUser(user.getId());

        if (fileUser != null) {
            return fileUser.getAttributeValues(name);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#getAttributes(org.picketlink.idm.model.User)
     */
    @Override
    public Map<String, String[]> getAttributes(User user) {
        FileUser fileUser = (FileUser) getUser(user.getId());

        if (fileUser != null) {
            return fileUser.getAttributes();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#setAttribute(org.picketlink.idm.model.Group, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public void setAttribute(Group group, String name, String[] values) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());

        if (fileGroup != null) {
            fileGroup.setAttribute(name, values);
        }

        flushGroups();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.picketlink.idm.spi.IdentityStore#removeAttribute(org.picketlink.idm.model.Group, java.lang.String)
     */
    @Override
    public void removeAttribute(Group group, String name) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());

        if (fileGroup != null) {
            fileGroup.removeAttribute(name);
        }

        flushGroups();
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#getAttributeValues(org.picketlink.idm.model.Group, java.lang.String)
     */
    @Override
    public String[] getAttributeValues(Group group, String name) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());

        if (fileGroup != null) {
            return fileGroup.getAttributeValues(name);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#getAttributes(org.picketlink.idm.model.Group)
     */
    @Override
    public Map<String, String[]> getAttributes(Group group) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());

        if (fileGroup != null) {
            return fileGroup.getAttributes();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#setAttribute(org.picketlink.idm.model.Role, java.lang.String, java.lang.String[])
     */
    @Override
    public void setAttribute(Role role, String name, String[] values) {
        FileRole fileRole = (FileRole) getRole(role.getName());

        if (fileRole != null) {
            fileRole.setAttribute(name, values);
        }

        flushRoles();
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#removeAttribute(org.picketlink.idm.model.Role, java.lang.String)
     */
    @Override
    public void removeAttribute(Role role, String name) {
        FileRole fileRole = (FileRole) getRole(role.getName());

        if (fileRole != null) {
            fileRole.removeAttribute(name);
        }

        flushRoles();
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#getAttributeValues(org.picketlink.idm.model.Role, java.lang.String)
     */
    @Override
    public String[] getAttributeValues(Role role, String name) {
        FileRole fileRole = (FileRole) getRole(role.getName());

        if (fileRole != null) {
            return fileRole.getAttributeValues(name);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#getAttributes(org.picketlink.idm.model.Role)
     */
    @Override
    public Map<String, String[]> getAttributes(Role role) {
        FileRole fileRole = (FileRole) getRole(role.getName());

        if (fileRole != null) {
            return fileRole.getAttributes();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#createMembershipQuery()
     */
    @Override
    public MembershipQuery createMembershipQuery() {
        return new DefaultMembershipQuery(this);
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#validatePassword(org.picketlink.idm.model.User, org.picketlink.idm.password.PasswordValidator)
     */
    @Override
    public boolean validatePassword(User user, PasswordValidator passwordValidator) {
        User storedUser = getUser(user.getKey());

        return passwordValidator.validate(storedUser.getAttribute(USER_PASSWORD_ATTRIBUTE));
    }

    public String getWorkingDir() {
        return this.workingDir;
    }

    /**
     * <p>Sets the base directory which will be used to store informations.</p>
     * 
     * @param workingDir
     */
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * <p>Indicates that the files must be always recreated during the initialization.</p>
     * 
     * @param alwaysCreateFiles
     */
    public void setAlwaysCreateFiles(boolean alwaysCreateFiles) {
        this.alwaysCreateFiles = alwaysCreateFiles;
    }
}
