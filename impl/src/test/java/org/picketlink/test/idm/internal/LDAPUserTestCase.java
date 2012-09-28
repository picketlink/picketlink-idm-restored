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
package org.picketlink.test.idm.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.picketlink.idm.internal.LDAPIdentityStore;
import org.picketlink.idm.internal.config.LDAPConfiguration;
import org.picketlink.idm.internal.config.LDAPConfigurationBuilder;
import org.picketlink.idm.internal.ldap.LDAPUser;
import org.picketlink.idm.internal.util.Base64;
import org.picketlink.idm.model.User;
import org.junit.Before;
import org.junit.Test;
import org.picketbox.test.ldap.AbstractLDAPTest;

/**
 * Unit test the {@link LDAPUser} construct
 *
 * @author anil saldhana
 * @since Sep 4, 2012
 */
public class LDAPUserTestCase extends AbstractLDAPTest {

    private static final String USER_FULL_NAME = "Anil Saldhana";
    private static final String USER_FIRSTNAME = "Anil";
    private static final String USER_LASTNAME = "Saldhana";

    @Before
    public void setup() throws Exception {
        super.setup();
        importLDIF("ldap/users.ldif");
    }

    private LDAPConfiguration getConfiguration() {
        LDAPConfigurationBuilder builder = new LDAPConfigurationBuilder();
        LDAPConfiguration config = (LDAPConfiguration) builder.build();

        config.setBindDN(adminDN).setBindCredential(adminPW).setLdapURL("ldap://localhost:10389");
        config.setUserDNSuffix("ou=People,dc=jboss,dc=org").setRoleDNSuffix("ou=Roles,dc=jboss,dc=org");
        config.setGroupDNSuffix("ou=Groups,dc=jboss,dc=org");
        return config;
    }

    @Test
    public void testLDAPIdentityStore() throws Exception {
        LDAPIdentityStore store = new LDAPIdentityStore();

        store.setConfiguration(getConfiguration());

        // Let us create an user
        User user = store.createUser("Anil Saldhana");
        assertNotNull(user);

        User anil = store.getUser("Anil Saldhana");
        assertNotNull(anil);
        assertEquals(USER_FULL_NAME, anil.getFullName());
        assertEquals(USER_FIRSTNAME, anil.getFirstName());
        assertEquals(USER_LASTNAME, anil.getLastName());

        // Deal with Anil's attributes
        anil.setAttribute("telephoneNumber", "12345678");

        anil.setAttribute("QuestionTotal", "2");
        anil.setAttribute("Question1", "What is favorite toy?");
        anil.setAttribute("Question1Answer", "Gum");

        anil.setAttribute("Question2", "What is favorite word?");
        anil.setAttribute("Question2Answer", "Hi");

        // Certificate
        InputStream bis = getClass().getClassLoader().getResourceAsStream("cert/servercert.txt");

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
        bis.close();

        String encodedCert = Base64.encodeBytes(cert.getEncoded());
        anil.setAttribute("x509", encodedCert);

        // let us retrieve the attributes from ldap store and see if they are the same
        Map<String, String[]> attributes = store.getAttributes(anil);
        assertNotNull(attributes);

        assertEquals("12345678", attributes.get("telephoneNumber")[0]);
        assertEquals("2", attributes.get("QuestionTotal")[0]);
        assertEquals("What is favorite toy?", attributes.get("Question1")[0]);
        assertEquals("Gum", attributes.get("Question1Answer")[0]);
        assertEquals("What is favorite word?", attributes.get("Question2")[0]);
        assertEquals("Hi", attributes.get("Question2Answer")[0]);

        String loadedCert = attributes.get("x509")[0];
        byte[] certBytes = Base64.decode(loadedCert);

        cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
        assertNotNull(cert);

        store.removeUser(anil);
        anil = store.getUser("Anil Saldhana");
        assertNull(anil);
    }

    /**
     * <p>
     * Tests the creation of an {@link User} with populating some basic attributes.
     * </p>
     *
     * @throws Exception
     */
    //TODO this method will throw OutOfMemoryError, must to find out what's happening here
    @Test
    public void testSimpleUserLdapStore() throws Exception {
        LDAPIdentityStore ldapIdentityStore = new LDAPIdentityStore();

        ldapIdentityStore.setConfiguration(getConfiguration());

        User user = new LDAPUser();

        user.setFirstName(USER_FIRSTNAME);
        user.setLastName(USER_LASTNAME);

        user = ldapIdentityStore.createUser(user);

        User anil = ldapIdentityStore.getUser(USER_FULL_NAME);
        assertNotNull(anil);
        assertEquals(USER_FULL_NAME, anil.getFullName());
        assertEquals(USER_FIRSTNAME, anil.getFirstName());
        assertEquals(USER_LASTNAME, anil.getLastName());

        ldapIdentityStore.removeUser(anil);
        anil = ldapIdentityStore.getUser(USER_FULL_NAME);
        assertNull(anil);

    }
}