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

import org.jboss.picketlink.idm.internal.jpa.DefaultRoleQuery;
import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.User;
import org.jboss.picketlink.idm.query.GroupQuery;
import org.jboss.picketlink.idm.query.RoleQuery;
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
public class JPARoleQueryTestCase extends AbstractJPAIdentityStoreTestCase {

    private static final String USER_NAME = "theuser";
    private static final String GROUP_NAME = "Administrators";
    private static final String ROLE_NAME = "admin";
    private Group group;
    private User user;
    private Role role;

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.picketlink.test.idm.internal.jpa.AbstractJPAIdentityStoreTestCase#onSetupTest()
     */
    @Override
    @Before
    public void onSetupTest() throws Exception {
        super.onSetupTest();
        loadRoles();
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
        RoleQuery query = new DefaultRoleQuery();

        query.setName(this.role.getName());

        assertQueryResult(query);
    }

    /**
     * <p>
     * Tests a simple query using the group property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByGroup() throws Exception {
        RoleQuery query = new DefaultRoleQuery();

        query.setName(this.role.getName());
        query.setGroup(this.group);

        assertQueryResult(query);
    }

    /**
     * <p>
     * Tests a simple query using the group's attributes.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByAttributes() throws Exception {
        RoleQuery query = new DefaultRoleQuery();

        query.setName(this.role.getName());
        query.setAttributeFilter("attribute1", new String[] { "attributeValue1", "attributeValue12", "attributeValue123" });
        query.setAttributeFilter("attribute2", new String[] { "attributeValue2" });

        assertQueryResult(query);
    }

    /**
     * <p>
     * Create and persist a {@link Group} instance for testing.
     * </p>
     */
    private void loadRoles() {
        IdentityStore identityStore = createIdentityStore();

        this.group = identityStore.getGroup(GROUP_NAME);
        this.role = identityStore.getRole(ROLE_NAME + 1);
        this.user = identityStore.getUser(USER_NAME);

        // if groups are already loaded then do nothing
        if (this.role != null) {
            return;
        }

        this.group = identityStore.createGroup(GROUP_NAME, null);
        this.user = identityStore.createUser(USER_NAME);

        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            Role currentRole = identityStore.createRole(ROLE_NAME + index);

            // store the instance used for testing
            if (this.role == null) {
                this.role = currentRole;
            }

            identityStore.createMembership(currentRole, this.user, this.group);

            currentRole.setAttribute("attribute1", "attributeValue1");
            currentRole.setAttribute("attribute1", "attributeValue12");
            currentRole.setAttribute("attribute1", "attributeValue123");

            currentRole.setAttribute("attribute2", "attributeValue2");
        }
    }

    /**
     * <p>
     * Asserts if the result returned by the specified {@link GroupQuery} match the expected values.
     * </p>
     *
     * @param query
     */
    private void assertQueryResult(RoleQuery query) {
        IdentityStore identityStore = createIdentityStore();

        List<Role> result = identityStore.executeQuery(query, null);

        assertFalse(result.isEmpty());
        assertEquals(this.role.getName(), result.get(0).getName());
    }

}