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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jboss.picketlink.idm.internal.JPAIdentityStore;
import org.jboss.picketlink.idm.model.IdentityType;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.spi.IdentityStore;
import org.junit.Test;

/**
 * <p>
 * Tests the creation of roles using the {@link JPAIdentityStore}.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class JPARoleTestCase extends AbstractJPAIdentityTypeTestCase {

    private static final String ROLE_NAME = "admin";

    /**
     * <p>
     * Tests the creation of an {@link Role} with populating some basic attributes.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testRoleStore() throws Exception {
        IdentityStore identityStore = createIdentityStore();

        Role role = identityStore.createRole(ROLE_NAME);

        assertNotNull(role);
        assertNotNull(role.getKey());
        assertEquals(ROLE_NAME, role.getName());

        testAddAttributes();

        testGetGroup();

        testRemoveRole();
    }

    /**
     * <p>
     * Tests the retrieval of an {@link Role} and the removal of attributes.
     * </p>
     *
     * @throws Exception
     */
    public void testGetGroup() throws Exception {
        IdentityStore identityStore = createIdentityStore();

        Role group = identityStore.getRole(ROLE_NAME);

        assertNotNull(group);
        assertNotNull(group.getKey());
        assertEquals(ROLE_NAME, group.getName());

        testRemoveAttributes();
    }

    /**
     * <p>
     * Tests the remove of an {@link Role}.
     * </p>
     *
     * @throws Exception
     */
    public void testRemoveRole() throws Exception {
        IdentityStore identityStore = createIdentityStore();

        Role role = identityStore.getRole(ROLE_NAME);

        assertNotNull(role);

        identityStore.removeRole(role);

        role = identityStore.getRole(ROLE_NAME);

        assertNull(role);
    }

    @Override
    protected IdentityType getIdentityTypeFromDatabase(IdentityStore identityStore) {
        return identityStore.getRole(ROLE_NAME);
    }

}