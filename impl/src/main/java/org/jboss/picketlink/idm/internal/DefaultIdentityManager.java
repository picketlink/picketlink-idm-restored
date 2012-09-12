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

import java.util.Collection;
import java.util.Date;

import org.jboss.picketlink.idm.IdentityManager;
import org.jboss.picketlink.idm.spi.IdentityStore;
import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.IdentityType;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.User;
import org.jboss.picketlink.idm.query.GroupQuery;
import org.jboss.picketlink.idm.query.MembershipQuery;
import org.jboss.picketlink.idm.query.RoleQuery;
import org.jboss.picketlink.idm.query.UserQuery;

/**
 * Default implementation of the IdentityManager interface
 *
 * @author Shane Bryzak
 * @author anil saldhana
 */
public class DefaultIdentityManager implements IdentityManager {
    private IdentityStore store = null;

    public DefaultIdentityManager() {
    }

    public void setIdentityStore(IdentityStore theStore) {
        this.store = theStore;
    }

    @Override
    public User createUser(String name) {
        ensureStoreExists();
        return store.createUser(name);
    }

    @Override
    public void removeUser(User user) {
        ensureStoreExists();
        store.removeUser(user);
    }

    @Override
    public void removeUser(String name) {
        ensureStoreExists();
        store.removeUser(getUser(name));
    }

    @Override
    public User getUser(String name) {
        ensureStoreExists();
        return store.getUser(name);
    }

    @Override
    public Collection<User> getAllUsers() {
        throw new RuntimeException();
    }

    @Override
    public Group createGroup(String id) {
        return store.createGroup(id, null);
    }

    @Override
    public Group createGroup(String id, Group parent) {
        ensureStoreExists();
        return store.createGroup(id, parent);
    }

    @Override
    public Group createGroup(String id, String parent) {
        ensureStoreExists();
        Group parentGroup = store.getGroup(parent);
        return store.createGroup(id, parentGroup);
    }

    @Override
    public void removeGroup(Group group) {
        ensureStoreExists();
        store.removeGroup(group);
    }

    @Override
    public void removeGroup(String groupId) {
        ensureStoreExists();
        store.removeGroup(getGroup(groupId));
    }

    @Override
    public Group getGroup(String groupId) {
        ensureStoreExists();
        return store.getGroup(groupId);
    }

    @Override
    public Group getGroup(String groupId, Group parent) {
        ensureStoreExists();
        return getGroup(groupId); // What about parent?
    }

    @Override
    public Collection<Group> getAllGroups() {
        throw new RuntimeException();
    }

    @Override
    public void addToGroup(IdentityType identityType, Group group) {
        throw new RuntimeException();
    }

    @Override
    public void removeFromGroup(IdentityType identityType, Group group) {
        throw new RuntimeException();
    }

    @Override
    public Collection<IdentityType> getGroupMembers(Group group) {
        throw new RuntimeException();
    }

    @Override
    public Role createRole(String name) {
        ensureStoreExists();
        return store.createRole(name);
    }

    @Override
    public void removeRole(Role role) {
        ensureStoreExists();
        store.removeRole(role);
    }

    @Override
    public void removeRole(String name) {
        ensureStoreExists();
        store.removeRole(getRole(name));
    }

    @Override
    public Role getRole(String name) {
        ensureStoreExists();
        return store.getRole(name);
    }

    @Override
    public Collection<Role> getAllRoles() {
        throw new RuntimeException();
    }

    @Override
    public Collection<Role> getRoles(IdentityType identityType, Group group) {
        throw new RuntimeException();
    }

    @Override
    public boolean hasRole(Role role, IdentityType identityType, Group group) {
        throw new RuntimeException();
    }

    @Override
    public void grantRole(Role role, IdentityType identityType, Group group) {
        throw new RuntimeException();
    }

    @Override
    public void revokeRole(Role role, IdentityType identityType, Group group) {
        throw new RuntimeException();
    }

    @Override
    public UserQuery createUserQuery() {
        throw new RuntimeException();
    }

    @Override
    public GroupQuery createGroupQuery() {
        throw new RuntimeException();
    }

    @Override
    public RoleQuery createRoleQuery() {
        throw new RuntimeException();
    }

    @Override
    public MembershipQuery createMembershipQuery() {
        throw new RuntimeException();
    }

    @Override
    public boolean validatePassword(String password) {
        throw new RuntimeException();
    }

    @Override
    public void updatePassword(String password) {
        throw new RuntimeException();
    }

    public void setEnabled(IdentityType identityType, boolean enabled) {
        throw new RuntimeException();
    }

    public void setExpirationDate(IdentityType identityType, Date expirationDate) {
        throw new RuntimeException();
    }

    private void ensureStoreExists() {
        if (store == null) {
            throw new RuntimeException("Identity Store has not been set");
        }
    }
}