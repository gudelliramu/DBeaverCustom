package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.meta.AbstractConstraint;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.DBException;

/**
 * GenericConstraintColumn
 */
public class MySQLConstraintColumn implements DBSConstraintColumn
{
    private AbstractConstraint constraint;
    private MySQLTableColumn tableColumn;
    private int ordinalPosition;

    public MySQLConstraintColumn(AbstractConstraint constraint, MySQLTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    public DBSConstraint getConstraint()
    {
        return constraint;
    }

    @Property(name = "Column", viewable = true, order = 2)
    public MySQLTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Property(name = "Position", viewable = true, order = 1)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getName()
    {
        return tableColumn.getName();
    }

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public DBSObject getParentObject()
    {
        return constraint;
    }

    public DBPDataSource getDataSource()
    {
        return constraint.getDataSource();
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }
}
