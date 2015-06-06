/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Property descriptor
 */
public interface DBPPropertyDescriptor {

    /**
     * Returns the id for this property. This object is used internally to distinguish one property descriptor from another.
     */
    @NotNull
    public Object getId();

    /**
     * Returns the name of the category to which this property belongs. Properties
     * belonging to the same category are grouped together visually. This localized
     * string is shown to the user
     */
    @Nullable
    public String getCategory();

    /**
     * Returns the display name for this property. This localized string is shown to
     * the user as the name of this property.
     *
     * @return a displayable name
     */
    @NotNull
    public String getDisplayName();

    /**
     * Returns a brief description of this property. This localized string is shown
     * to the user when this property is selected.
     */
    @Nullable
    public String getDescription();

    /**
     * Returns the type of this property. Types is a java class.
     */
    @Nullable
    Class<?> getDataType();

    boolean isRequired();

    @Nullable
    Object getDefaultValue();

    // TODO: remove "object" parameter
    boolean isEditable(Object object);

}