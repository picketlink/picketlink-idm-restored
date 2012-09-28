package org.picketlink.test.idm.internal.jpa;

import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.internal.DefaultIdentityManager;
import org.picketlink.idm.internal.jpa.DatabaseUser;
import org.picketlink.idm.model.User;
import org.picketlink.idm.spi.IdentityStore;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class IdentityManagerTestCase extends AbstractJPAIdentityStoreTestCase {

    private static final String USER_EMAIL = "myemail@company.com";
    private static final String USER_LAST_NAME = "Saldhana";
    private static final String USER_FIRST_NAME = "Anil";
    private static final String USER_FULL_NAME = "Anil Saldhana";
    private static final String USER_USERNAME = "asaldhana";

    @Test
    public void testSimpleUserStore() throws Exception {
        IdentityManager identityManager = new DefaultIdentityManager(createIdentityStore());

        User user = new DatabaseUser(USER_USERNAME);

        user.setEmail(USER_EMAIL);
        user.setFirstName(USER_FIRST_NAME);
        user.setLastName(USER_LAST_NAME);

        user = identityManager.createUser(user);

        assertUserBasicInformation(user);

        testGetUser();

        testRemoveUser();
    }

    /**
     * <p>
     * Tests if the user was properly created by retrieving him from the database.
     * </p>
     *
     * @throws Exception
     */
    public void testGetUser() throws Exception {
        IdentityStore identityStore = createIdentityStore();

        User user = identityStore.getUser(USER_USERNAME);

        assertUserBasicInformation(user);

    }

    /**
     * <p>
     * Tests the removal of users.
     * </p>
     *
     * @throws Exception
     */
    public void testRemoveUser() throws Exception {
        IdentityStore identityStore = createIdentityStore();

        User user = identityStore.getUser(USER_USERNAME);

        assertNotNull(user);

        identityStore.removeUser(user);

        user = identityStore.getUser(USER_USERNAME);

        assertNull(user);
    }

    /**
     * <p>
     * Asserts if the {@link User} is populated with the expected values.
     * </p>
     *
     * @param user
     */
    private void assertUserBasicInformation(User user) {
        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals(USER_USERNAME, user.getKey());
        assertEquals(USER_FULL_NAME, user.getFullName());
        assertEquals(USER_FIRST_NAME, user.getFirstName());
        assertEquals(USER_LAST_NAME, user.getLastName());
        assertEquals(USER_EMAIL, user.getEmail());
    }
}
