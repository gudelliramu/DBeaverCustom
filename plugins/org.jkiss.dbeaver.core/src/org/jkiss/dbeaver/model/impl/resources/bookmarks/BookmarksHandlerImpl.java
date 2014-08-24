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
package org.jkiss.dbeaver.model.impl.resources.bookmarks;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.impl.resources.AbstractResourceHandler;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bookmarks handler
 */
public class BookmarksHandlerImpl extends AbstractResourceHandler {

    private static final String BOOKMARK_EXT = "bm"; //$NON-NLS-1$

    public static IFolder getBookmarksFolder(IProject project, boolean forceCreate) throws CoreException
    {
        final IFolder bookmarksFolder = DBeaverCore.getInstance().getProjectRegistry().getResourceDefaultRoot(project, BookmarksHandlerImpl.class);
        if (!bookmarksFolder.exists() && forceCreate) {
            bookmarksFolder.create(true, true, new NullProgressMonitor());
        }
        return bookmarksFolder;
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    @Override
    public String getTypeName(IResource resource)
    {
        if (resource instanceof IFolder) {
            return "bookmark folder"; //$NON-NLS-1$
        } else {
            return "bookmark"; //$NON-NLS-1$
        }
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            return new DBNBookmark(parentNode, resource, this);
        } else {
            return new DBNBookmarkFolder(parentNode, resource, this);
        }
    }

    @Override
    public void openResource(final IResource resource, final IWorkbenchWindow window) throws CoreException, DBException
    {
        if (!(resource instanceof IFile)) {
            return;
        }
        final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(resource.getProject());
        if (projectNode == null) {
            throw new DBException("Can't find project node for '" + resource.getProject().getName() + "'"); //$NON-NLS-2$
        }
        final BookmarkStorage storage = new BookmarkStorage((IFile) resource, false);
        try {
            final DataSourceDescriptor dataSourceContainer = projectNode.getDatabases().getDataSourceRegistry().getDataSource(storage.getDataSourceId());
            if (dataSourceContainer == null) {
                throw new DBException("Can't find datasource '" + storage.getDataSourceId() + "'"); //$NON-NLS-2$
            }
            final DBNDataSource dsNode = (DBNDataSource)DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(dataSourceContainer);
            dsNode.initializeNode(null, new DBRProcessListener() {
                @Override
                public void onProcessFinish(IStatus status)
                {
                    if (status.isOK()) {
                        UIUtils.runInUI(null, new Runnable() {
                            @Override
                            public void run()
                            {
                            openNodeByPath(dsNode, (IFile) resource, storage, window);
                            }
                        });
                    }
                }
            });
        }
        finally {
            storage.dispose();
        }
    }

    private void openNodeByPath(final DBNDataSource dsNode, final IFile file, final BookmarkStorage storage, final IWorkbenchWindow window)
    {
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBNNode currentNode = dsNode;
                        final Collection<String> dataSourcePath = storage.getDataSourcePath();
                        for (String path : dataSourcePath) {
                            DBNNode nextChild = null;
                            final List<? extends DBNNode> children = currentNode.getChildren(monitor);
                            if (!CommonUtils.isEmpty(children)) {
                                for (DBNNode node : children) {
                                    if (path.equals(node.getNodeName())) {
                                        nextChild = node;
                                        break;
                                    }
                                }
                            }
                            if (nextChild == null) {
                                throw new DBException("Can't find node '" + path + "' in '" + currentNode.getNodeFullName() + "'"); //$NON-NLS-2$ //$NON-NLS-3$
                            }
                            currentNode = nextChild;
                        }
                        if (currentNode instanceof DBNDatabaseNode) {
                            // Update bookmark image
                            storage.setImage(currentNode.getNodeIconDefault());
                            file.setContents(storage.serialize(), true, false, monitor.getNestedMonitor());

                            // Open entity editor
                            final DBNDatabaseNode databaseNode = (DBNDatabaseNode) currentNode;
                            UIUtils.runInUI(null, new Runnable() {
                                @Override
                                public void run()
                                {
                                    NavigatorHandlerObjectOpen.openEntityEditor(databaseNode, null, window);
                                }
                            });
                        } else if (currentNode != null) {
                            throw new DBException("Node '" + currentNode.getNodeFullName() + "' is not a database object");
                        } else {
                            throw new DBException("Can't find database node by path");
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(window.getShell(), CoreMessages.model_project_open_bookmark, CoreMessages.model_project_cant_open_bookmark, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void createBookmark(final DBNDatabaseNode node, String title, IFolder folder) throws DBException
    {
        if (folder == null) {
            for (DBNNode parent = node.getParentNode(); parent != null; parent = parent.getParentNode()) {
                if (parent instanceof DBNProject) {
                    try {
                        folder = getBookmarksFolder(((DBNProject)parent).getProject(), true);
                    } catch (CoreException e) {
                        throw new DBException("Can't obtain folder for bookmark", e);
                    }
                    break;
                }
            }
        }
        if (folder == null) {
            throw new DBException("Can't detect folder for bookmark");
        }

        IFile file = ContentUtils.getUniqueFile(
            folder,
            CommonUtils.escapeFileName(title),
            BOOKMARK_EXT);

        updateBookmark(node, title, file);
    }

    private static void updateBookmark(DBNDatabaseNode node, String title, IFile file)
        throws DBException
    {
        if (CommonUtils.isEmpty(title)) {
            title = node.getNodeName();
        }

        List<String> nodePath = new ArrayList<String>();
        for (DBNNode parent = node; !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            nodePath.add(0, parent.getNodeName());
        }

        BookmarkStorage storage = new BookmarkStorage(
            title,
            node.getNodeType() + " " + node.getNodeName(), //$NON-NLS-1$
            node.getNodeIconDefault(),
            node.getObject().getDataSource().getContainer().getId(),
            nodePath);

        try {
            InputStream data = storage.serialize();
            file.create(data, true, new NullProgressMonitor());
        } catch (Exception e) {
            throw new DBException("Error saving bookmark", e);
        }
    }

}