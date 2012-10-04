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

package org.picketlink.idm.internal.jpa;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.picketlink.idm.model.Group;

/**
 * <p>
 * Implementation of {@link AbstractDatabaseAttribute} to manage {@link Group} attributes.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
@Entity
public class DatabaseGroupAttribute extends AbstractDatabaseAttribute<DatabaseGroup> {

    @ManyToOne
    private DatabaseGroup group;

    public DatabaseGroupAttribute() {
    }

    public DatabaseGroupAttribute(String name, String value) {
        super(name, value);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.picketlink.idm.internal.jpa.AbstractDatabaseAttribute#getIdentityType()
     */
    @Override
    protected DatabaseGroup getIdentityType() {
        return this.group;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.picketlink.idm.internal.jpa.AbstractDatabaseAttribute#setIdentityType(org.picketlink.idm.model.IdentityType )
     */
    @Override
    protected void setIdentityType(DatabaseGroup identityType) {
        this.group = identityType;
    }

}
