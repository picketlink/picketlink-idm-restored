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

    private static final String GROUP_PARENT_NAME = "parentGroup";
    private static final String USER_NAME = "theuser";
    private static final String GROUP_NAME = "Administrators";
    private Group group;
    private User user;
    private Group parentGroup;

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.picketlink.test.idm.internal.jpa.AbstractJPAIdentityStoreTestCase#onSetupTest()
     */
    @Override
    @Before
    public void onSetupTest() throws Exception {
        super.onSetupTest();
        loadGroups();
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
        GroupQuery query = new DefaultGroupQuery();

        query.setName(this.group.getName());

        assertQueryResult(query);
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
        GroupQuery query = new DefaultGroupQuery();

        query.setId(this.group.getId());

        assertQueryResult(query);
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
        GroupQuery query = new DefaultGroupQuery();

        query.setRole("admin" + 1);

        assertQueryResult(query);
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
        GroupQuery query = new DefaultGroupQuery();

        query.setId(this.group.getId());
        query.setRelatedUser(this.user);

        assertQueryResult(query);
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
        GroupQuery query = new DefaultGroupQuery();

        query.setId(this.group.getId());
        query.setParentGroup(this.parentGroup);

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
        GroupQuery query = new DefaultGroupQuery();

        query.setId(this.group.getId());
        query.addAttributeFilter("attribute1", new String[] { "attributeValue1", "attributeValue12", "attributeValue123" });
        query.addAttributeFilter("attribute2", new String[] { "attributeValue2" });

        assertQueryResult(query);
    }

    /**
     * <p>
     * Create and persist a {@link Group} instance for testing.
     * </p>
     */
    private void loadGroups() {
        IdentityStore identityStore = createIdentityStore();

        this.group = identityStore.getGroup(GROUP_NAME + 1);
        this.user = identityStore.getUser(USER_NAME);
        this.parentGroup = identityStore.getGroup(GROUP_PARENT_NAME);

        // if groups are already loaded then do nothing
        if (this.group != null) {
            return;
        }

        this.user = identityStore.createUser(USER_NAME);
        this.parentGroup = identityStore.createGroup(GROUP_PARENT_NAME, null);

        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            Group currentGroup = identityStore.createGroup(GROUP_NAME + index, parentGroup);

            // store the instance used for testing
            if (this.group == null) {
                this.group = currentGroup;
            }

            Role role = identityStore.createRole("admin" + index);

            identityStore.createMembership(role, user, currentGroup);

            currentGroup.setAttribute("attribute1", "attributeValue1");
            currentGroup.setAttribute("attribute1", "attributeValue12");
            currentGroup.setAttribute("attribute1", "attributeValue123");

            currentGroup.setAttribute("attribute2", "attributeValue2");
        }
    }

    /**
     * <p>
     * Asserts if the result returned by the specified {@link GroupQuery} match the expected values.
     * </p>
     *
     * @param query
     */
    private void assertQueryResult(GroupQuery query) {
        IdentityStore identityStore = createIdentityStore();

        List<Group> result = identityStore.executeQuery(query, null);

        assertFalse(result.isEmpty());
        assertEquals(this.group.getId(), result.get(0).getId());
    }

}