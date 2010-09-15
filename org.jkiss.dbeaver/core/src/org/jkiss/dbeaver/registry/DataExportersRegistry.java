/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public class DataExportersRegistry {

    private static final String CFG_EXPORT = "export";

    private List<DataExporterDescriptor> dataExporters = new ArrayList<DataExporterDescriptor>();

    public DataExportersRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataExporterDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (CFG_EXPORT.equals(ext.getName())) {
                DataExporterDescriptor descriptor = new DataExporterDescriptor(ext);
                dataExporters.add(descriptor);
            }
        }

    }

    public List<DataExporterDescriptor> getDataExporters(Class objectType)
    {
        List<DataExporterDescriptor> editors = new ArrayList<DataExporterDescriptor>();
        for (DataExporterDescriptor descriptor : dataExporters) {
            if (descriptor.appliesToType(objectType)) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

}
