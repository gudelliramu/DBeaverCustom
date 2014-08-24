/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2014 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2ViewBase;
import org.jkiss.dbeaver.ext.db2.model.DB2ViewBaseDep;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for dependencies for DB2 Views alike
 * 
 * @author Denis Forveille
 */
public class DB2ViewBaseDepCache extends JDBCObjectCache<DB2ViewBase, DB2ViewBaseDep> {

    private static final String SQL = "SELECT * FROM SYSCAT.TABDEP WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY BSCHEMA,BNAME WITH UR";

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2ViewBase db2ViewBase) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, db2ViewBase.getParentObject().getName());
        dbStat.setString(2, db2ViewBase.getName());
        return dbStat;
    }

    @Override
    protected DB2ViewBaseDep fetchObject(JDBCSession session, DB2ViewBase db2ViewBase, ResultSet resultSet)
        throws SQLException, DBException
    {
        return new DB2ViewBaseDep(session.getProgressMonitor(), db2ViewBase, resultSet);
    }
}