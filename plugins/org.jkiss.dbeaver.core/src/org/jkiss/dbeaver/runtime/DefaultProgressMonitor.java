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
package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBRProgressMonitor {

    static final Log log = LogFactory.getLog(DefaultProgressMonitor.class);

    private IProgressMonitor nestedMonitor;
    private List<DBRBlockingObject> blocks = null;

    public DefaultProgressMonitor(IProgressMonitor nestedMonitor)
    {
        this.nestedMonitor = nestedMonitor;
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return nestedMonitor;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        nestedMonitor.beginTask(name, totalWork);
    }

    @Override
    public void done()
    {
        nestedMonitor.done();
    }

    @Override
    public void subTask(String name)
    {
        nestedMonitor.subTask(name);
    }

    @Override
    public void worked(int work)
    {
        nestedMonitor.worked(work);
    }

    @Override
    public boolean isCanceled()
    {
        return nestedMonitor.isCanceled();
    }

    @Override
    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        if (taskName != null) {
            subTask(taskName);
        }
        if (blocks == null) {
            blocks = new ArrayList<DBRBlockingObject>();
        }
        blocks.add(object);
    }

    @Override
    public synchronized void endBlock()
    {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("End block invoked while no blocking objects are in stack"); //$NON-NLS-1$
            return;
        }
        //if (blocks.size() == 1) {
        //    this.done();
        //}
        blocks.remove(blocks.size() - 1);
    }

    @Override
    public synchronized DBRBlockingObject getActiveBlock()
    {
        return blocks == null || blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
    }

}