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

package org.jboss.picketlink.idm.internal.jpa;

import java.util.List;

import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.IdentityType;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.SimpleGroup;
import org.jboss.picketlink.idm.query.RoleQuery;
import org.jboss.picketlink.idm.spi.IdentityStore;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class DefaultRoleQuery extends AbstractQuery<DefaultRoleQuery> implements RoleQuery {

    private IdentityStore store;
    private Group group;
    
    public DefaultRoleQuery(IdentityStore jpaIdentityStore) {
        this.store = jpaIdentityStore;
    }

    @Override
    public List<Role> executeQuery(RoleQuery query) {
        return this.store.executeQuery(query, null);
    }

    @Override
    public RoleQuery setOwner(IdentityType owner) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RoleQuery setGroup(Group group) {
        this.group = group;
        return this;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public RoleQuery setGroup(String groupId) {
        this.group = new SimpleGroup(groupId, null, null);
        return this;
    }

}
