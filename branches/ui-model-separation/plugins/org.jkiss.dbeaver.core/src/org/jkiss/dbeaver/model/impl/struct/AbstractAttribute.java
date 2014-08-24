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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * AbstractAttribute
 */
public abstract class AbstractAttribute implements DBSAttributeBase
{
    protected String name;
    protected int valueType;
    protected long maxLength;
    protected boolean required;
    protected int scale;
    protected int precision;
    protected String typeName;
    protected int ordinalPosition;

    protected AbstractAttribute()
    {
    }

    protected AbstractAttribute(
            String name,
            String typeName,
            int valueType,
            int ordinalPosition,
            long maxLength,
            int scale,
            int precision,
            boolean required)
    {
        this.name = name;
        this.valueType = valueType;
        this.maxLength = maxLength;
        this.scale = scale;
        this.precision = precision;
        this.required = required;
        this.typeName = typeName;
        this.ordinalPosition = ordinalPosition;
    }

    @Override
    @Property(viewable = true, order = 10)
    public String getName()
    {
        return name;
    }

    public void setName(String columnName)
    {
        this.name = columnName;
    }

    @Override
    @Property(viewable = true, order = 20)
    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    @Property(viewable = true, order = 30)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    public void setValueType(int valueType)
    {
        this.valueType = valueType;
    }

    @Override
    @Property(viewable = true, order = 40)
    public long getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(long maxLength)
    {
        this.maxLength = maxLength;
    }

    @Property(viewable = true, order = 50)
    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    @Override
    @Property(viewable = false, order = 60)
    public int getScale()
    {
        return scale;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Override
    @Property(viewable = false, order = 61)
    public int getPrecision()
    {
        return precision;
    }

    public void setPrecision(int precision)
    {
        this.precision = precision;
    }

    public String getDescription()
    {
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

}