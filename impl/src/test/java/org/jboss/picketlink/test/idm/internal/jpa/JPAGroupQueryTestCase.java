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
package org.jboss.picketlink.test.idm.internal.jpa;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.List;

import org.jboss.picketlink.idm.internal.jpa.DefaultGroupQuery;
import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.User;
import org.jboss.picketlink.idm.query.GroupQuery;
import org.jboss.picketlink.idm.spi.IdentityStore;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Tests the query support for {@link Group} instances.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class JPAGroupQueryTestCase extends AbstractJPAIdentityStoreTestCase {

    private static final String USER_NAME = "theuser";
    private static final String GROUP_NAME = "Administrators";

    /* (non-Javadoc)
     * @see org.jboss.picketlink.test.idm.internal.jpa.AbstractJPAIdentityStoreTestCase#onSetupTest()
     */
    @Override
    @Before
    public void onSetupTest() throws Exception {
        super.onSetupTest();
        loadGroups();
    }
    
    /**
     * <p>Create and persist a {@link Group} instance for testing.</p>
     */
    private void loadGroups() {
        IdentityStore identityStore = createIdentityStore();

        Group group = identityStore.getGroup(GROUP_NAME + 1);
        
        if (group != null) {
            return;
        }
        
        User user = identityStore.createUser(USER_NAME);
        Group parentGroup = identityStore.createGroup("parentGroup", null);
        
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            
            group = identityStore.createGroup(GROUP_NAME + index, parentGroup);

            Role role = identityStore.createRole("admin" + index);

            identityStore.createMembership(role, user, group);
            
            group.setAttribute("attribute1", "attributeValue1");
            group.setAttribute("attribute1", "attributeValue12");
            group.setAttribute("attribute1", "attributeValue123");
            
            group.setAttribute("attribute2", "attributeValue2");
        }
    }
    
    /**
     * <p>
     * Tests a simple query using the name property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByName() throws Exception {
        IdentityStore identityStore = createIdentityStore();
        
        List<Group> result = identityStore.executeQuery(createFindByNameQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(GROUP_NAME + 1, result.get(0).getKey());
    }

    /**
     * <p>
     * Tests a simple query using the id property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindById() throws Exception {
        IdentityStore identityStore = createIdentityStore();
        
        List<Group> result = identityStore.executeQuery(createFindByIdQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(GROUP_NAME + 1, result.get(0).getKey());
    }
    
    /**
     * <p>
     * Tests a simple query using the role property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByRole() throws Exception {
        IdentityStore identityStore = createIdentityStore();
        
        List<Group> result = identityStore.executeQuery(createFindByRoleQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(GROUP_NAME + 1, result.get(0).getKey());
    }
    
    /**
     * <p>
     * Tests a simple query using the user property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByUser() throws Exception {
        IdentityStore identityStore = createIdentityStore();
        
        List<Group> result = identityStore.executeQuery(createFindByUserQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(GROUP_NAME + 1, result.get(0).getKey());
    }
    
    /**
     * <p>
     * Tests a simple query using the parent group property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByParentGroup() throws Exception {
        IdentityStore identityStore = createIdentityStore();
        
        List<Group> result = identityStore.executeQuery(createFindByParentGroupQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(GROUP_NAME + 1, result.get(0).getKey());
    }

    private GroupQuery createFindByNameQuery() {
        GroupQuery query = new DefaultGroupQuery();
        
        query.setName(GROUP_NAME + 1);
        
        return query;
    }

    private GroupQuery createFindByIdQuery() {
        GroupQuery query = new DefaultGroupQuery();
        
        query.setId("2");
        
        return query;
    }
    
    private GroupQuery createFindByRoleQuery() {
        GroupQuery query = new DefaultGroupQuery();
        
        query.setRole("admin" + 1);
        
        return query;
    }
    
    private GroupQuery createFindByUserQuery() {
        GroupQuery query = new DefaultGroupQuery();
        
        query.setId("2");
        query.setRelatedUser("1");
        
        return query;
    }
    
    private GroupQuery createFindByParentGroupQuery() {
        GroupQuery query = new DefaultGroupQuery();
        
        query.setId("2");
        query.setParentGroup("1");
        
        return query;
    }
    

}