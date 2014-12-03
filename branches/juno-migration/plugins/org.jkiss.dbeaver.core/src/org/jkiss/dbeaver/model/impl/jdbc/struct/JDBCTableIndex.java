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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * JDBC abstract index
 */
public abstract class JDBCTableIndex<TABLE extends JDBCTable>
    extends AbstractTableIndex
    implements DBPSaveableObject
{
    private final TABLE table;
    protected String name;
    protected DBSIndexType indexType;
    private boolean persisted;

    protected JDBCTableIndex(TABLE table, String name, DBSIndexType indexType, boolean persisted) {
        this.table = table;
        this.name = name;
        this.indexType = indexType;
        this.persisted = persisted;
    }

    protected JDBCTableIndex(JDBCTableIndex<TABLE> source)
    {
        this.table = source.table;
        this.name = source.name;
        this.indexType = source.indexType;
        this.persisted = source.persisted;
    }

    @Override
    public TABLE getParentObject()
    {
        return table;
    }

    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String indexName)
    {
        this.name = indexName;
    }

    @Override
    @Property(viewable = true, order = 2)
    public TABLE getTable()
    {
        return table;
    }

    @Override
    @Property(viewable = true, order = 3)
    public DBSIndexType getIndexType()
    {
        return this.indexType;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}