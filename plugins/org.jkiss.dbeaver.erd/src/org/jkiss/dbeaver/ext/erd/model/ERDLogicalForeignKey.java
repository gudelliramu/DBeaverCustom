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
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Logical foreign key
 */
public class ERDLogicalForeignKey implements DBSEntityAssociation, DBSEntityReferrer {

    private DBSEntity entity;
    private String name;
    private String description;
    private ERDLogicalPrimaryKey pk;
    private List<? extends DBSEntityAttributeRef> columns = new ArrayList<DBSEntityAttributeRef>();

    public ERDLogicalForeignKey(ERDEntity entity, String name, String description, ERDLogicalPrimaryKey pk)
    {
        this.entity = entity.getObject();
        this.name = name;
        this.description = description;
        this.pk = pk;
    }

    @NotNull
    @Override
    public DBSEntityConstraint getReferencedConstraint()
    {
        return pk;
    }

    @Override
    public DBSEntity getAssociatedEntity()
    {
        return pk.getParentObject();
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return entity.getDataSource();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return description;
    }

    @NotNull
    @Override
    public DBSEntity getParentObject()
    {
        return entity;
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return ERDConstants.CONSTRAINT_LOGICAL_FK;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isPersisted()
    {
        return false;
    }

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }
}