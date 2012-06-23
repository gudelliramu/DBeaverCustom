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
package org.jkiss.dbeaver.registry;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverIcons;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;

/**
 * EntityEditorDescriptor
 */
public abstract class AbstractDescriptor {

    static final Log log = LogFactory.getLog(AbstractDescriptor.class);

    protected class ObjectType {
        final String implName;
        Class<?> implClass;
        Expression expression;

        ObjectType(String implName)
        {
            this.implName = implName;
        }

        ObjectType(IConfigurationElement cfg)
        {
            this.implName = cfg.getAttribute(RegistryConstants.ATTR_NAME);
            String condition = cfg.getAttribute(RegistryConstants.ATTR_IF);
            if (!CommonUtils.isEmpty(condition)) {
                try {
                    this.expression = RuntimeUtils.parseExpression(condition);
                } catch (DBException ex) {
                    log.warn("Can't parse object type expression: " + condition, ex); //$NON-NLS-1$
                }
            }
        }

        public boolean appliesTo(DBPObject object)
        {
            if (implName == null) {
                return false;
            }
            if (implClass == null) {
                implClass = getObjectClass(implName);
            }
            if (implClass == null) {
                return false;
            }
            if (!implClass.isAssignableFrom(object.getClass())) {
                return false;
            }
            if (expression != null) {
                Object result = expression.evaluate(makeContext(object));
                return Boolean.TRUE.equals(result);
            }
            return true;
        }

        private JexlContext makeContext(final DBPObject object)
        {
            return new JexlContext() {
                @Override
                public Object get(String name)
                {
                    return name.equals("object") ? object : null; //$NON-NLS-1$
                }

                @Override
                public void set(String name, Object value)
                {
                    log.warn("Set is not implemented"); //$NON-NLS-1$
                }

                @Override
                public boolean has(String name)
                {
                    return name.equals("object") && object != null; //$NON-NLS-1$
                }
            };
        }
    }


    private IContributor contributor;

    public AbstractDescriptor(IContributor contributor)
    {
        this.contributor = contributor;
    }

    public IContributor getContributor()
    {
        return contributor;
    }

    public String getContributorName()
    {
        return contributor.getName();
    }

    public Bundle getContributorBundle()
    {
        return Platform.getBundle(getContributorName());
    }

    protected Image iconToImage(String icon)
    {
        if (CommonUtils.isEmpty(icon)) {
            return null;
        } else if (icon.startsWith("#")) {
            // Predefined image
            return DBeaverIcons.getImage(icon.substring(1));
        } else {
            URL iconPath = getContributorBundle().getEntry(icon);
            if (iconPath != null) {
                try {
                    iconPath = FileLocator.toFileURL(iconPath);
                }
                catch (IOException ex) {
                    log.error(ex);
                    return null;
                }
                ImageDescriptor descriptor = ImageDescriptor.createFromURL(iconPath);
                return descriptor.createImage();
            }
        }
        return null;
    }

    public Class<?> getObjectClass(String className)
    {
        return getObjectClass(className, Object.class);
    }

    public <T> Class<T> getObjectClass(String className, Class<T> type)
    {
        Class<?> objectClass = null;
        try {
            objectClass = DBeaverCore.getInstance().getPlugin().getBundle().loadClass(className);
        } catch (Throwable ex) {
            // do nothing
            //log.warn("Can't load object class '" + className + "'", ex);
        }

        if (objectClass == null) {
            try {
                objectClass = getContributorBundle().loadClass(className);
            } catch (Throwable ex) {
                throw new IllegalStateException("Can't determine object class '" + className + "'", ex);
            }
        }
        if (!type.isAssignableFrom(objectClass)) {
            throw new IllegalStateException("Object class '" + className + "' doesn't match requested type '" + type.getName() + "'");
        }
        return (Class<T>) objectClass;
    }

}