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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.jboss.picketlink.idm.model.User;
import org.jboss.picketlink.idm.password.PasswordEncoder;

/**
 * <p>{@link PasswordEncoder} that uses SHA to created a salted hash the password.</p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class SHASaltedPasswordEncoder implements PasswordEncoder {

    private static final String PASSWORD_SALT_USER_ATTRIBUTE = "passwordSalt";
    private int strength;

    public SHASaltedPasswordEncoder(int strength) {
        this.strength = strength;
    }
    
    /* (non-Javadoc)
     * @see org.jboss.picketlink.idm.PasswordEncoder#encodePassword(java.lang.String, java.lang.Object)
     */
    @Override
    public String encodePassword(User user, String rawPassword) {
        MessageDigest messageDigest = getMessageDigest();
        
        String salt = user.getAttribute(PASSWORD_SALT_USER_ATTRIBUTE);
        
        if (salt == null) {
            SecureRandom psuedoRng = null;
            String algorithm = "SHA1PRNG";
            
            try {
                psuedoRng = SecureRandom.getInstance(algorithm);
                psuedoRng.setSeed(1024);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Error getting SecureRandom instance: " + algorithm, e);
            }
            
            salt = String.valueOf(psuedoRng.nextLong());
            
            user.setAttribute(PASSWORD_SALT_USER_ATTRIBUTE, salt);
        }
        
        String saltedPassword = rawPassword + salt.toString();
        byte[] encodedPassword = null;
        
        try {
            encodedPassword = messageDigest.digest(saltedPassword.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        return new String(encodedPassword);
    }
    
    protected final MessageDigest getMessageDigest() throws IllegalArgumentException {
        String algorithm = "SHA-" + this.strength;
        
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm: " + algorithm);
        }
    }

}
