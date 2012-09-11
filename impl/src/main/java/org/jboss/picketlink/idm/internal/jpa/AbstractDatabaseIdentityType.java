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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.jboss.picketlink.idm.model.IdentityType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
@SuppressWarnings("rawtypes")
@MappedSuperclass
public abstract class AbstractDatabaseIdentityType<A extends AbstractDatabaseAttribute> implements IdentityType {

    @Id
    @GeneratedValue
    private String id;

    private String key;
    private boolean enabled = true;
    private Date expirationDate;
    private Date creationDate;

    @Transient
    private Map<String, String[]> userAttributesMap;

    public AbstractDatabaseIdentityType() {
    }

    public AbstractDatabaseIdentityType(String name) {
        setKey(name);
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the expirationDate
     */
    @Override
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * @param expirationDate the expirationDate to set
     */
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * @return the creationDate
     */
    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the userAttributes
     */
    public abstract List<A> getOwnerAttributes();

    @SuppressWarnings("unchecked")
    @Override
    @Transient
    public void setAttribute(String name, String value) {
        getUserAttributesMap().put(name, new String[] { value });

        A attribute = createAttribute(name, value);

        attribute.setIdentityType(this);

        getOwnerAttributes().add(attribute);
    }

    protected abstract A createAttribute(String name, String value);

    private Map<String, String[]> getUserAttributesMap() {
        if (this.userAttributesMap == null) {
            this.userAttributesMap = new HashMap<String, String[]>();

            for (AbstractDatabaseAttribute attribute : getOwnerAttributes()) {
                String[] values = this.userAttributesMap.get(attribute.getName());

                if (values == null) {
                    values = new String[] { attribute.getValue() };
                } else {
                    int len = values.length;

                    values = Arrays.copyOf(values, len + 1);

                    values[len] = attribute.getValue();
                }

                this.userAttributesMap.put(attribute.getName(), values);
            }
        }

        return this.userAttributesMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transient
    public void setAttribute(String name, String[] values) {
        getUserAttributesMap().put(name, values);
        for (String value : values) {
            A attribute = createAttribute(name, value);

            attribute.setIdentityType(this);

            getOwnerAttributes().add(attribute);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transient
    public void removeAttribute(String name) {
        getUserAttributesMap().remove(name);
        for (AbstractDatabaseAttribute attribute : new ArrayList<AbstractDatabaseAttribute>(getOwnerAttributes())) {
            if (attribute.getName().equals(name)) {
                attribute.setIdentityType(null);
                getOwnerAttributes().remove(attribute);
            }
        }
    }

    @Override
    @Transient
    public String getAttribute(String name) {
        String[] values = getUserAttributesMap().get(name);

        if (values != null) {
            return values[0];
        }

        return null;
    }

    @Override
    @Transient
    public String[] getAttributeValues(String name) {
        return getUserAttributesMap().get(name);
    }

    @Override
    @Transient
    public Map<String, String[]> getAttributes() {
        return null;
    }

    // TODO: implement hashcode and equals methods

}
