/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.edit.struct.JDBCTableColumnManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DB2 Table Column Manager
 * 
 * @author Denis Forveille
 */
public class DB2TableColumnManager extends JDBCTableColumnManager<DB2TableColumn, DB2TableBase> {

    private static final String SQL_ALTER = "ALTER TABLE %s ALTER COLUMN %s ";
    private static final String SQL_COMMENT = "COMMENT ON COLUMN %s.%s IS '%s'";
    private static final String SQL_REORG = "CALL SYSPROC.ADMIN_CMD('REORG TABLE %s')";

    private static final String CLAUSE_SET_TYPE = " SET DATA TYPE ";
    private static final String CLAUSE_SET_NULL = " SET NOT NULL";
    private static final String CLAUSE_DROP_NULL = " DROP NOT NULL";

    private static final String CMD_ALTER = "Alter Column";
    private static final String CMD_COMMENT = "Comment on Column";
    private static final String CMD_REORG = "Reorg table";

    private static final String lineSeparator = ContentUtils.getDefaultLineSeparator();

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public DBSObjectCache<? extends DBSObject, DB2TableColumn> getObjectsCache(DB2TableColumn object)
    {
        return object.getParentObject().getContainer().getTableCache().getChildrenCache((DB2Table) object.getParentObject());
    }

    // ------
    // Create
    // ------

    @Override
    protected DB2TableColumn createDatabaseObject(IWorkbenchWindow workbenchWindow, DBECommandContext context, DB2TableBase parent,
        Object copyFrom)
    {
        DB2TableColumn column = new DB2TableColumn(parent);
        column.setName(DBObjectNameCaseTransformer.transformName(column, getNewColumnName(context, parent)));
        return column;
    }

    // -----
    // Alter
    // -----
    @Override
    protected IDatabasePersistAction[] makeObjectModifyActions(ObjectChangeCommand command)
    {
        DB2TableColumn db2Column = command.getObject();

        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>(3);

        String sqlAlterColumn = String.format(SQL_ALTER, db2Column.getTable().getFullQualifiedName(), computeDeltaSQL(command));
        actions.add(new AbstractDatabasePersistAction(CMD_ALTER, sqlAlterColumn));

        // Comment
        IDatabasePersistAction commentAction = buildCommentAction(db2Column);
        if (commentAction != null) {
            actions.add(commentAction);
        }

        // Be Safe, Add a reorg action
        actions.add(buildReorgAction(db2Column));

        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }

    // -------
    // Helpers
    // -------
    private String computeDeltaSQL(ObjectChangeCommand command)
    {

        if (command.getProperties().isEmpty()) {
            return "";
        }

        if (log.isDebugEnabled()) {
            for (Map.Entry<Object, Object> entry : command.getProperties().entrySet()) {
                log.debug(entry.getKey() + "=" + entry.getValue());
            }
        }

        DB2TableColumn column = command.getObject();

        StringBuilder sb = new StringBuilder(128);
        sb.append(column.getName());

        Boolean required = (Boolean) command.getProperty("required");
        if (required != null) {
            sb.append(lineSeparator);
            if (required) {
                sb.append(CLAUSE_SET_NULL);
            } else {
                sb.append(CLAUSE_DROP_NULL);
            }
        }

        String type = (String) command.getProperty("type");
        if (type != null) {
            sb.append(lineSeparator);
            sb.append(CLAUSE_SET_TYPE);
            sb.append(type);
        }

        return sb.toString();
    }

    private IDatabasePersistAction buildCommentAction(DB2TableColumn db2Column)
    {
        if ((db2Column.getDescription() != null) && (db2Column.getDescription().trim().length() > 0)) {
            String tableName = db2Column.getTable().getFullQualifiedName();
            String columnName = db2Column.getName();
            String comment = db2Column.getDescription();
            String commentSQL = String.format(SQL_COMMENT, tableName, columnName, comment);
            return new AbstractDatabasePersistAction(CMD_COMMENT, commentSQL);
        } else {
            return null;
        }
    }

    private IDatabasePersistAction buildReorgAction(DB2TableColumn db2Column)
    {
        String tableName = db2Column.getTable().getFullQualifiedName();
        String reorgSQL = String.format(SQL_REORG, tableName);
        return new AbstractDatabasePersistAction(CMD_REORG, reorgSQL);
    }
}
