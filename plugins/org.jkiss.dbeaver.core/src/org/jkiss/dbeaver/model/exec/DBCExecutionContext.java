/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Execution context
 */
public interface DBCExecutionContext extends DBPCloseableObject, DBDPreferences {

    String getTaskTitle();

    /**
     * Data source of this context
     * @return data source
     */
    DBPDataSource getDataSource();

    /**
     * Performs check that this context is really connected to remote database
     * @return connected state
     */
    boolean isConnected();

    /**
     * Context's progress monitor.
     * Each context has it's progress monitor which is passed at context creation time and never changes.
     * @return progress monitor
     */
    DBRProgressMonitor getProgressMonitor();

    /**
     * Associated transaction manager
     * @return transaction manager
     */
    DBCTransactionManager getTransactionManager();

    /**
     * Context's purpose
     * @return purpose
     */
    DBCExecutionPurpose getPurpose();

    /**
     * Prepares statements
     */
    DBCStatement prepareStatement(
        DBCStatementType type,
        String query,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys) throws DBCException;

}
