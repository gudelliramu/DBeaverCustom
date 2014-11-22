/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Objects cache
 */
public interface DBSObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> {

    Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException;

    Collection<OBJECT> getCachedObjects();

    @Nullable
    OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String name)
        throws DBException;

    @Nullable
    OBJECT getCachedObject(String name);

    boolean isCached();

    /**
     * Adds specified object to cache
     * @param object object to cache
     */
    void cacheObject(OBJECT object);

    /**
     * Removes specified object from cache
     * @param object object to remove
     */
    void removeObject(OBJECT object);

    void clearCache();

}