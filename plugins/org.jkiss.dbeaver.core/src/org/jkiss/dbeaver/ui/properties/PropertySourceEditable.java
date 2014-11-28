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
package org.jkiss.dbeaver.ui.properties;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBECommandContext commandContext;
    private PropertyChangeCommand lastCommand = null;
    //private final List<IPropertySourceListener> listeners = new ArrayList<IPropertySourceListener>();
    private final CommandReflector commandReflector = new CommandReflector();

    public PropertySourceEditable(DBECommandContext commandContext, Object sourceObject, Object object)
    {
        super(sourceObject, object, true);
        this.commandContext = commandContext;
        //this.objectManager = editorInput.getObjectManager(DBEObjectEditor.class);
    }

    @Override
    public boolean isEditable(Object object)
    {
        DBEObjectEditor objectEditor = getObjectEditor();
        return commandContext != null && objectEditor != null &&
            object instanceof DBPObject && objectEditor.canEditObject((DBPObject) object);
    }

    private DBEObjectEditor getObjectEditor()
    {
        final Object editableValue = getEditableValue();
        if (editableValue == null) {
            return null;
        }
        return DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(
            editableValue.getClass(),
            DBEObjectEditor.class);
    }

    @Override
    public DBECommandContext getCommandContext()
    {
        return commandContext;
    }

    @Override
    public void setPropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object newValue)
    {
        if (prop.getValueTransformer() != null) {
            newValue = prop.getValueTransformer().transform(editableValue, newValue);
        }
        final Object oldValue = getPropertyValue(editableValue, prop);
        if (!updatePropertyValue(editableValue, prop, newValue, false)) {
            return;
        }
        if (lastCommand == null || lastCommand.getObject() != editableValue || lastCommand.property != prop) {
            final DBEObjectEditor<DBPObject> objectEditor = getObjectEditor();
            if (objectEditor == null) {
                log.error("Can't obtain object editor for " + getEditableValue());
                return;
            }
            final DBEPropertyHandler<DBPObject> propertyHandler = objectEditor.makePropertyHandler(
                (DBPObject)editableValue,
                prop);
            PropertyChangeCommand curCommand = new PropertyChangeCommand((DBPObject) editableValue, prop, propertyHandler, oldValue, newValue);
            getCommandContext().addCommand(curCommand, commandReflector);
            lastCommand = curCommand;
        } else {
            lastCommand.setNewValue(newValue);
            getCommandContext().updateCommand(lastCommand, commandReflector);
        }
/*
        // Notify listeners
        for (IPropertySourceListener listener : listeners) {
            listener.handlePropertyChange(editableValue, prop, newValue);
        }
*/
    }

    private boolean updatePropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object value, boolean force)
    {
        // Write property value
        try {
            // Check for complex object
            // If value should be a named object then try to obtain it from list provider
            if (value != null && value.getClass() == String.class) {
                final Object[] items = prop.getPossibleValues(editableValue);
                if (!ArrayUtils.isEmpty(items)) {
                    for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                        if ((items[i] instanceof DBPNamedObject && value.equals(((DBPNamedObject)items[i]).getName())) ||
                            (items[i] instanceof Enum && value.equals(((Enum)items[i]).name()))
                            )
                        {
                            value = items[i];
                            break;
                        }
                    }
                }
            }
            final Object oldValue = getPropertyValue(editableValue, prop);
            if (CommonUtils.equalObjects(oldValue, value)) {
                return false;
            }

            prop.writeValue(editableValue, value);
            // Fire object update event
            if (editableValue instanceof DBSObject) {
                DBUtils.fireObjectUpdate((DBSObject) editableValue, prop);
            }
            return true;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            log.error("Can't write property '" + prop.getDisplayName() + "' value", e);
            return false;
        }
    }

    @Override
    public boolean isPropertyResettable(Object object, ObjectPropertyDescriptor prop)
    {
        return (lastCommand != null && lastCommand.property == prop && lastCommand.getObject() == object);
    }

    @Override
    public void resetPropertyValue(Object object, ObjectPropertyDescriptor prop)
    {
//        final DBECommandComposite compositeCommand = (DBECommandComposite)getCommandContext().getUserParams().get(obj);
//        if (compositeCommand != null) {
//            final Object value = compositeCommand.getProperty(prop.getId());
//        }

        if (lastCommand != null && lastCommand.property == prop) {
            setPropertyValue(object, prop, lastCommand.getOldValue());
        }
//        final ObjectProps objectProps = getObjectProps(object);
//        DBECommandProperty curCommand = objectProps.propValues.get(prop);
//        if (curCommand != null) {
//            curCommand.resetValue();
//        }
        log.warn("Property reset not implemented");
    }

    private class PropertyChangeCommand extends DBECommandProperty<DBPObject> {
        ObjectPropertyDescriptor property;
        public PropertyChangeCommand(DBPObject editableValue, ObjectPropertyDescriptor property, DBEPropertyHandler<DBPObject> propertyHandler, Object oldValue, Object newValue)
        {
            super(editableValue, propertyHandler, oldValue, newValue);
            this.property = property;
        }

        @Override
        public void updateModel()
        {
            super.updateModel();
            updatePropertyValue(getObject(), property, getNewValue(), true);
        }
    }

    private class CommandReflector implements DBECommandReflector<DBPObject, PropertyChangeCommand> {

        @Override
        public void redoCommand(PropertyChangeCommand command)
        {
            updatePropertyValue(command.getObject(), command.property, command.getNewValue(), false);
/*
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, command.getNewValue());
            }
*/
        }

        @Override
        public void undoCommand(PropertyChangeCommand command)
        {
            updatePropertyValue(command.getObject(), command.property, command.getOldValue(), false);
/*
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, command.getOldValue());
            }
*/
        }
    }

}