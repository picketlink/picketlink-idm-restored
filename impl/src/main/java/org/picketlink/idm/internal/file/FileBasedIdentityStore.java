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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.picketlink.idm.model.Group;
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
 * File based {@link IdentityStore} implementation.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class FileBasedIdentityStore implements IdentityStore {

    private static final String USER_PASSWORD_ATTRIBUTE = "userPassword";
    private File usersFile = new File("/tmp/pl-idm-work/pl-idm-users.db");
    private File rolesFile = new File("/tmp/pl-idm-work/pl-idm-roles.db");
    private File groupsFile = new File("/tmp/pl-idm-work/pl-idm-groups.db");
    private File membershipsFile = new File("/tmp/pl-idm-work/pl-idm-memberships.db");

    private Map<String, FileUser> users = new HashMap<String, FileUser>();
    private Map<String, Role> roles = new HashMap<String, Role>();
    private Map<String, FileGroup> groups = new HashMap<String, FileGroup>();
    private List<FileMembership> memberships = new ArrayList<FileMembership>();

    private FileChangeListener changeListener = new FileChangeListener(this);

    public FileBasedIdentityStore() {
        checkAndCreateFile(this.usersFile);
        checkAndCreateFile(this.rolesFile);
        checkAndCreateFile(this.groupsFile);
        checkAndCreateFile(this.membershipsFile);

        loadUsers();
        loadRoles();
        loadGroups();
        loadMemberships();
    }
    
    private void checkAndCreateFile(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadGroups() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(groupsFile);
            ois = new ObjectInputStream(fis);

            this.groups = (Map<String, FileGroup>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private void loadMemberships() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(membershipsFile);
            ois = new ObjectInputStream(fis);

            this.memberships = (List<FileMembership>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private void loadRoles() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(rolesFile);
            ois = new ObjectInputStream(fis);

            this.roles = (Map<String, Role>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private void loadUsers() {
        ObjectInputStream ois = null;

        try {
            FileInputStream fis = new FileInputStream(usersFile);
            ois = new ObjectInputStream(fis);

            this.users = (Map<String, FileUser>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
            }
        }
    }

    void flushUsers() {
        try {
            FileOutputStream fos = new FileOutputStream(this.usersFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.users);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void flushRoles() {
        try {
            FileOutputStream fos = new FileOutputStream(this.rolesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.roles);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void flushGroups() {
        try {
            FileOutputStream fos = new FileOutputStream(this.groupsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.groups);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void flushMemberships() {
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

    @Override
    public boolean validateCertificate(User user, X509Certificate certificate) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateCertificate(User user, X509Certificate certificate) {
        // TODO Auto-generated method stub
        return false;
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

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.UserQuery, org.picketlink.idm.query.Range)
     */
    @Override
    public List<User> executeQuery(UserQuery query, Range range) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.GroupQuery, org.picketlink.idm.query.Range)
     */
    @Override
    public List<Group> executeQuery(GroupQuery query, Range range) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.RoleQuery, org.picketlink.idm.query.Range)
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
        
        return roles;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#executeQuery(org.picketlink.idm.query.MembershipQuery, org.picketlink.idm.query.Range)
     */
    @Override
    public List<Membership> executeQuery(MembershipQuery query, Range range) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#setAttribute(org.picketlink.idm.model.User, java.lang.String, java.lang.String[])
     */
    @Override
    public void setAttribute(User user, String name, String[] values) {
        FileUser fileUser = (FileUser) getUser(user.getId());
        
        fileUser.setAttribute(name, values);
        
        flushUsers();
    }

    /* (non-Javadoc)
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

    /* (non-Javadoc)
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

    /* (non-Javadoc)
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

    /* (non-Javadoc)
     * @see org.picketlink.idm.spi.IdentityStore#setAttribute(org.picketlink.idm.model.Group, java.lang.String, java.lang.String[])
     */
    @Override
    public void setAttribute(Group group, String name, String[] values) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());
        
        if (fileGroup != null) {
            fileGroup.setAttribute(name, values);
        }
        
        flushGroups();
    }

    /* (non-Javadoc)
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

    @Override
    public String[] getAttributeValues(Group group, String name) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());
        
        if (fileGroup != null) {
            return fileGroup.getAttributeValues(name);
        }
        
        return null;
    }

    @Override
    public Map<String, String[]> getAttributes(Group group) {
        FileGroup fileGroup = (FileGroup) getGroup(group.getId());
        
        if (fileGroup != null) {
            return fileGroup.getAttributes();
        }
        
        return null;
    }

    @Override
    public void setAttribute(Role role, String name, String[] values) {
        FileRole fileRole = (FileRole) getRole(role.getName());
        
        if (fileRole != null) {
            fileRole.setAttribute(name, values);
        }
        
        flushRoles();
    }

    @Override
    public void removeAttribute(Role role, String name) {
        FileRole fileRole = (FileRole) getRole(role.getName());
        
        if (fileRole != null) {
            fileRole.removeAttribute(name);
        }
        
        flushRoles();
    }

    @Override
    public String[] getAttributeValues(Role role, String name) {
        FileRole fileRole = (FileRole) getRole(role.getName());
        
        if (fileRole != null) {
            return fileRole.getAttributeValues(name);
        }
        
        return null;
    }

    @Override
    public Map<String, String[]> getAttributes(Role role) {
        FileRole fileRole = (FileRole) getRole(role.getName());
        
        if (fileRole != null) {
            return fileRole.getAttributes();
        }

        return null;
    }

    @Override
    public MembershipQuery createMembershipQuery() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean validatePassword(User user, PasswordValidator passwordValidator) {
        User storedUser = getUser(user.getKey());

        return passwordValidator.validate(storedUser.getAttribute(USER_PASSWORD_ATTRIBUTE));
    }

}
