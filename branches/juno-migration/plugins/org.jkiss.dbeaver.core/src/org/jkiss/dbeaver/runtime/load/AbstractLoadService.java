/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public abstract class AbstractLoadService<RESULT> implements ILoadService<RESULT> {
    private String serviceName;
    private DBRProgressMonitor progressMonitor;

    protected AbstractLoadService(String serviceName)
    {
        this.serviceName = serviceName;
    }

    protected AbstractLoadService()
    {
        this("Loading");
    }

    @Override
    public String getServiceName()
    {
        return serviceName;
    }

    @Override
    public DBRProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    @Override
    public void setProgressMonitor(DBRProgressMonitor monitor)
    {
        this.progressMonitor = monitor;
    }

    @Override
    public boolean cancel() throws InvocationTargetException
    {
        // Invoke nested service cancel
        DBRBlockingObject block = progressMonitor.getActiveBlock();
        if (block != null) {
            try {
                block.cancelBlock();
                return true;
            }
            catch (DBException e) {
                throw new InvocationTargetException(e, "Could not cancel blocking object");
            }
        }
        return false;
    }

}