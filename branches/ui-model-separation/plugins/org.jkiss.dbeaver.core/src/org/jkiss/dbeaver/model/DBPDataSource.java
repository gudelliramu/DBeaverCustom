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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * Data Source.
 * Root object of all database structure and data objects.
 * Note: do not store direct references on datasource objects in any GUI components -
 * datasource instance could be refreshed at any time. Obtain references on datasource only
 * from DBSObject or IDataSourceProvider interfaces.
 */
public interface DBPDataSource extends DBPObject,DBPCloseableObject
{
    /**
     * Datasource container
     * @return container implementation
     */
    DBSDataSourceContainer getContainer();

    /**
     * Datasource information/options
     * Info SHOULD be read at datasource initialization stage and should be cached and available
     * at the moment of invocation of this function.
     * @return datasource info.
     */
    DBPDataSourceInfo getInfo();

    /**
     * Checks this datasource is really connected to remote database.
     * Usually DBSDataSourceContainer.getDataSource() returns datasource only if datasource is connected.
     * But in some cases (e.g. connection invalidation) datasource remains disconnected for some period of time.
     * @return true if underlying connection is alive.
     */
    boolean isConnected();

    /**
     * Opens new execution context
     * @param monitor progress monitor
     * @param purpose context purpose
     * @param task task description
     * @return execution context
     */
    DBCExecutionContext openContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task);

    /**
     * Opens new isolated execution context.
     * @param monitor progress monitor
     * @param purpose context purpose
     * @param task task description
     * @return execution context
     */
    DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String task);

    /**
     * checks connection is alive and reconnects if needed.
     *
     * @throws org.jkiss.dbeaver.DBException on any error
     * @param monitor progress monitor
     */
    void invalidateConnection(DBRProgressMonitor monitor) throws DBException;

    /**
     * Reads base metadata from remote database or do any necessarily initialization routines.
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    void initialize(DBRProgressMonitor monitor) throws DBException;


}