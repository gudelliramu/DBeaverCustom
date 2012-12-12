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
package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandAggregator;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.*;

/**
 * JDBC struct editor
 */
public abstract class JDBCStructEditor<OBJECT_TYPE extends DBSEntity & DBPSaveableObject, CONTAINER_TYPE extends DBSObject>
    extends JDBCObjectEditor<OBJECT_TYPE, CONTAINER_TYPE>
    implements DBEStructEditor<OBJECT_TYPE>
{

    protected abstract IDatabasePersistAction[] makeStructObjectCreateActions(StructCreateCommand command);

    @Override
    public StructCreateCommand makeCreateCommand(OBJECT_TYPE object)
    {
        return new StructCreateCommand(object, CoreMessages.model_jdbc_create_new_object);
    }

    protected Collection<NestedObjectCommand> getNestedOrderedCommands(final StructCreateCommand structCommand)
    {
        List<NestedObjectCommand> nestedCommands = new ArrayList<NestedObjectCommand>();
        nestedCommands.addAll(structCommand.getObjectCommands().values());
        Collections.sort(nestedCommands, new Comparator<NestedObjectCommand>() {
            @Override
            public int compare(NestedObjectCommand o1, NestedObjectCommand o2)
            {
                final DBPObject object1 = o1.getObject();
                final DBPObject object2 = o2.getObject();
                if (object1 == structCommand.getObject()) {
                    return 1;
                } else if (object2 == structCommand.getObject()) {
                    return -1;
                }
                int order1 = -1, order2 = 1;
                Class<?>[] childTypes = getChildTypes();
                for (int i = 0, childTypesLength = childTypes.length; i < childTypesLength; i++) {
                    Class<?> childType = childTypes[i];
                    if (childType.isAssignableFrom(object1.getClass())) {
                        order1 = i;
                    }
                    if (childType.isAssignableFrom(object2.getClass())) {
                        order2 = i;
                    }
                }
                return order1 - order2;
            }
        });

        return nestedCommands;
    }

    protected class StructCreateCommand extends ObjectCreateCommand
        implements DBECommandAggregator<OBJECT_TYPE> {

        private final Map<DBPObject, NestedObjectCommand> objectCommands = new LinkedHashMap<DBPObject, NestedObjectCommand>();

        public StructCreateCommand(OBJECT_TYPE object, String table)
        {
            super(object, table);
            objectCommands.put(getObject(), this);
        }

        public Map<DBPObject, NestedObjectCommand> getObjectCommands()
        {
            return objectCommands;
        }

        @Override
        public boolean aggregateCommand(DBECommand<?> command)
        {
            if (command instanceof NestedObjectCommand) {
                objectCommands.put(command.getObject(), (NestedObjectCommand) command);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void resetAggregatedCommands()
        {
            objectCommands.clear();
            objectCommands.put(getObject(), this);
        }

        @Override
        public IDatabasePersistAction[] getPersistActions()
        {
            return makeStructObjectCreateActions(this);
        }
    }



}

