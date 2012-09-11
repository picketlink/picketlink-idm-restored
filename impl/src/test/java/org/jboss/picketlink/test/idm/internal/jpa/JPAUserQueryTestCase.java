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

import org.jboss.picketlink.idm.internal.JPAIdentityStore;
import org.jboss.picketlink.idm.internal.jpa.DefaultUserQuery;
import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.User;
import org.jboss.picketlink.idm.query.UserQuery;
import org.jboss.picketlink.idm.spi.IdentityStore;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Tests the creation of users using the {@link JPAIdentityStore}.
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
        IdentityStore identityStore = createIdentityStore();
        
        List<User> result = identityStore.executeQuery(createFindByNameQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(USER_USERNAME, result.get(0).getKey());
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
        IdentityStore identityStore = createIdentityStore();
        
        List<User> result = identityStore.executeQuery(createFindByFirstNameQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(USER_FIRST_NAME, result.get(0).getFirstName());
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
        IdentityStore identityStore = createIdentityStore();
        
        List<User> result = identityStore.executeQuery(createFindByLastNameQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(USER_LAST_NAME, result.get(0).getLastName());
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
        IdentityStore identityStore = createIdentityStore();
        
        List<User> result = identityStore.executeQuery(createFindByEmailQuery(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(USER_EMAIL, result.get(0).getEmail());
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
        
        List<User> result = identityStore.executeQuery(createFindByRole(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(USER_USERNAME, result.get(0).getKey());
    }

    /**
     * <p>
     * Tests a simple query using the user attributes.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testfindByAttributes() throws Exception {
        IdentityStore identityStore = createIdentityStore();
        
        List<User> result = identityStore.executeQuery(createFindByAttributes(), null);
        
        assertFalse(result.isEmpty());
        assertEquals(USER_USERNAME, result.get(0).getKey());
    }

    private void loadUsers() {
        IdentityStore identityStore = createIdentityStore();

        this.user = identityStore.getUser(USER_USERNAME);
        
        if (this.user != null) {
            return;
        }
        
        this.user = identityStore.createUser(USER_USERNAME);

        user.setEmail(USER_EMAIL);
        user.setFirstName(USER_FIRST_NAME);
        user.setLastName(USER_LAST_NAME);
        
        Role role = identityStore.createRole("admin");
        Group group = identityStore.createGroup("Administrators", null);

        identityStore.createMembership(role, user, group);
        
        this.user.setAttribute("attribute1", "attributeValue1");
        this.user.setAttribute("attribute1", "attributeValue12");
        this.user.setAttribute("attribute1", "attributeValue123");
        
        this.user.setAttribute("attribute2", "attributeValue2");
    }

    private UserQuery createFindByNameQuery() {
        UserQuery query = new DefaultUserQuery();
        
        query.setName(USER_USERNAME);
        
        return query;
    }
    
    private UserQuery createFindByFirstNameQuery() {
        UserQuery query = new DefaultUserQuery();
        
        query.setFirstName(USER_FIRST_NAME);
        
        return query;
    }
    
    private UserQuery createFindByLastNameQuery() {
        UserQuery query = new DefaultUserQuery();
        
        query.setLastName(USER_LAST_NAME);
        
        return query;
    }
    
    private UserQuery createFindByRole() {
        UserQuery query = new DefaultUserQuery();
        
        query.setRole("admin");
        
        return query;
    }
    
    private UserQuery createFindByAttributes() {
        UserQuery query = new DefaultUserQuery();
        
        query.setAttributeFilter("attribute1", new String[] {"attributeValue1", "attributeValue12", "attributeValue123"});
        query.setAttributeFilter("attribute2", new String[] {"attributeValue2", "attributeValue21", "attributeValue23"});
        
        return query;
    }
    
    private UserQuery createFindByEmailQuery() {
        UserQuery query = new DefaultUserQuery();
        
        query.setEmail(USER_EMAIL);
        
        return query;
    }

}