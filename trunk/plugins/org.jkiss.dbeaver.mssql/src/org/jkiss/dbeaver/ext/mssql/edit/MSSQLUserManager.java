/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.MSSQLMessages;
import org.jkiss.dbeaver.ext.mssql.model.MSSQLDataSource;
import org.jkiss.dbeaver.ext.mssql.model.MSSQLUser;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLScriptCommand;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * MSSQLUserManager
 */
public class MSSQLUserManager extends AbstractObjectManager<MSSQLUser> implements DBEObjectMaker<MSSQLUser, MSSQLDataSource>, DBECommandFilter<MSSQLUser> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MSSQLUser> getObjectsCache(MSSQLUser object)
    {
        return null;
    }

    @Override
    public boolean canCreateObject(MSSQLDataSource parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(MSSQLUser object)
    {
        return true;
    }

    @Override
    public MSSQLUser createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commandContext, MSSQLDataSource parent, Object copyFrom)
    {
        MSSQLUser newUser = new MSSQLUser(parent, null);
        if (copyFrom instanceof MSSQLUser) {
            MSSQLUser tplUser = (MSSQLUser)copyFrom;
            newUser.setUserName(tplUser.getUserName());
            newUser.setHost(tplUser.getHost());
            newUser.setMaxQuestions(tplUser.getMaxQuestions());
            newUser.setMaxUpdates(tplUser.getMaxUpdates());
            newUser.setMaxConnections(tplUser.getMaxConnections());
            newUser.setMaxUserConnections(tplUser.getMaxUserConnections());
        }
        commandContext.addCommand(new CommandCreateUser(newUser), new CreateObjectReflector<MSSQLUser>(this), true);

        return newUser;
    }

    @Override
    public void deleteObject(DBECommandContext commandContext, MSSQLUser user, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropUser(user), new DeleteObjectReflector<MSSQLUser>(this), true);
    }

    @Override
    public void filterCommands(DBECommandQueue<MSSQLUser> queue)
    {
        if (!queue.isEmpty()) {
            // Add privileges flush to the tail
            queue.add(
                new SQLScriptCommand<MSSQLUser>(
                    queue.getObject(),
                    MSSQLMessages.edit_user_manager_command_flush_privileges,
                    "FLUSH PRIVILEGES")); //$NON-NLS-1$
        }
    }

    private static class CommandCreateUser extends DBECommandAbstract<MSSQLUser> {
        protected CommandCreateUser(MSSQLUser user)
        {
            super(user, MSSQLMessages.edit_user_manager_command_create_user);
        }
    }


    private static class CommandDropUser extends DBECommandComposite<MSSQLUser, UserPropertyHandler> {
        protected CommandDropUser(MSSQLUser user)
        {
            super(user, MSSQLMessages.edit_user_manager_command_drop_user);
        }
        @Override
        public DBEPersistAction[] getPersistActions()
        {
            return new DBEPersistAction[] {
                new SQLDatabasePersistAction(MSSQLMessages.edit_user_manager_command_drop_user, "DROP USER " + getObject().getFullName()) { //$NON-NLS-2$
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(false);
                        }
                    }
                }};
        }
    }

}

