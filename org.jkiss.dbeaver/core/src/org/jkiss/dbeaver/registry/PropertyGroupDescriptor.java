/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyGroupDescriptor
 */
public class PropertyGroupDescriptor implements DBPPropertyGroup
{
    public static final String PROPERTY_GROUP_TAG = "propertyGroup";

    private String name;
    private String description;
    private List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();

    public PropertyGroupDescriptor(IConfigurationElement config)
    {
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.PROPERTY_TAG);
        for (IConfigurationElement prop : propElements) {
            properties.add(new PropertyDescriptor(this, prop));
        }
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public List<PropertyDescriptor> getProperties()
    {
        return properties;
    }
}