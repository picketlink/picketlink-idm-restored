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
package org.picketlink.test.idm.internal.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.picketlink.idm.internal.DefaultUserQuery;
import org.picketlink.idm.model.Group;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.User;
import org.picketlink.idm.query.UserQuery;
import org.picketlink.idm.spi.IdentityStore;

/**
 * <p>
 * Tests the query support for {@link User} instances.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class JPAUserQueryTestCase extends AbstractJPAIdentityStoreTestCase {

    private static final String USER_EMAIL = "myemail@company.com";
    private static final String USER_LAST_NAME = "Saldhana";
    private static final String USER_FIRST_NAME = "Anil";
    private static final String USER_USERNAME = "asaldhana";
    private User user;

    /*
     * (non-Javadoc)
     *
     * @see org.picketlink.test.idm.internal.jpa.AbstractJPAIdentityStoreTestCase#onSetupTest()
     */
    @Override
    @Before
    public void onSetupTest() throws Exception {
        super.onSetupTest();
        loadUsers();
    }

    /**
     * <p>
     * Tests a simple query using the username property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByUserName() throws Exception {
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setName(this.user.getKey());

        assertQueryResult(query);
    }

    /**
     * <p>
     * Tests a simple query using the firstName property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByFirstName() throws Exception {
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setFirstName(this.user.getFirstName());

        assertQueryResult(query);
    }

    /**
     * <p>
     * Tests a simple query using the lastName property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByLastName() throws Exception {
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setLastName(this.user.getLastName());

        assertQueryResult(query);
    }

    /**
     * <p>
     * Tests a simple query using the email property.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByEmail() throws Exception {
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setEmail(this.user.getEmail());

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
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setRole("admin" + 1);

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
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setRelatedGroup("Administrators" + 1);

        assertQueryResult(query);
    }

    /**
     * <p>
     * Tests a simple query using the user's attributes.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByAttributes() throws Exception {
        UserQuery query = new DefaultUserQuery(createIdentityStore());

        query.setName(this.user.getKey());
        query.setAttributeFilter("attribute1", new String[] { "attributeValue1", "attributeValue12", "attributeValue123" });
        query.setAttributeFilter("attribute2", new String[] { "attributeValue2", "attributeValue21", "attributeValue23" });

        assertQueryResult(query);
    }

    /**
     * <p>
     * Asserts if the result returned by the specified {@link UserQuery} match the expected values.
     * </p>
     *
     * @param query
     */
    private void assertQueryResult(UserQuery query) {
        List<User> result = query.executeQuery();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(this.user.getId(), result.get(0).getId());
    }

    /**
     * <p>
     * Create and persist a {@link User} instance for testing.
     * </p>
     */
    private void loadUsers() {
        IdentityStore identityStore = createIdentityStore();

        this.user = identityStore.getUser(USER_USERNAME + 1);

        // if users are already loaded then do nothing
        if (this.user != null) {
            return;
        }

        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            User currentUser = identityStore.createUser(USER_USERNAME + index);

            // store the instance used for testing
            if (this.user == null) {
                this.user = currentUser;
            }

            currentUser.setEmail(USER_EMAIL + index);
            currentUser.setFirstName(USER_FIRST_NAME + index);
            currentUser.setLastName(USER_LAST_NAME + index);

            Role role = identityStore.createRole("admin" + index);
            Group group = identityStore.createGroup("Administrators" + index, null);

            identityStore.createMembership(role, user, group);

            currentUser.setAttribute("attribute1", "attributeValue1");
            currentUser.setAttribute("attribute1", "attributeValue12");
            currentUser.setAttribute("attribute1", "attributeValue123");

            currentUser.setAttribute("attribute2", "attributeValue2");
        }
    }

}