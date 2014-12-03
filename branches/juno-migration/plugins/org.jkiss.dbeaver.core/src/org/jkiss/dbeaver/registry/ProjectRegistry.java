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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.resources.DefaultResourceHandlerImpl;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.GlobalPropertyTester;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRegistry implements IResourceChangeListener {
    static final Log log = LogFactory.getLog(ProjectRegistry.class);

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<ResourceHandlerDescriptor>();
    private final Map<String, ResourceHandlerDescriptor> rootMapping = new HashMap<String, ResourceHandlerDescriptor>();

    private final Map<IProject, DataSourceRegistry> projectDatabases = new HashMap<IProject, DataSourceRegistry>();
    private IProject activeProject;
    private IWorkspace workspace;

    private final List<DBPProjectListener> projectListeners = new ArrayList<DBPProjectListener>();

    public ProjectRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor handlerDescriptor = new ResourceHandlerDescriptor(ext);
                handlerDescriptors.add(handlerDescriptor);
            }
            for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
                for (String root : rhd.getRoots()) {
                    rootMapping.put(root, rhd);
                }
            }
        }
    }

    public void loadProjects(IWorkspace workspace, IProgressMonitor monitor) throws CoreException
    {
        final DBeaverCore core = DBeaverCore.getInstance();
        String activeProjectName = core.getGlobalPreferenceStore().getString("project.active");

        List<IProject> projects = core.getLiveProjects();
        if (DBeaverCore.isStandalone() && CommonUtils.isEmpty(projects)) {
            // Create initial project (onloy for standalone version)
            monitor.beginTask("Create general project", 1);
            try {
                createGeneralProject(workspace, monitor);
            } finally {
                monitor.done();
            }
            projects = core.getLiveProjects();
        }

        monitor.beginTask("Open active project", projects.size());
        try {
            List<IProject> availableProjects = new ArrayList<IProject>();
            for (IProject project : projects) {
                if (project.exists() && !project.isHidden()) {
                    if (project.getName().equals(activeProjectName)) {
                        activeProject = project;
                        break;
                    }
                    availableProjects.add(project);
                }
                monitor.worked(1);
            }
            if (activeProject == null && !availableProjects.isEmpty()) {
                setActiveProject(availableProjects.get(0));
            }
            if (activeProject != null) {
                activeProject.open(monitor);
            }
        } finally {
            monitor.done();
        }

        this.workspace = workspace;
        workspace.addResourceChangeListener(this);
    }

    public void dispose()
    {
        if (!this.projectDatabases.isEmpty()) {
            log.warn("Some projects are still open: " + this.projectDatabases.keySet());
        }
        // Dispose all DS registries
        for (DataSourceRegistry dataSourceRegistry : this.projectDatabases.values()) {
            dataSourceRegistry.dispose();
        }
        this.projectDatabases.clear();

        // Dispose resource handlers
        for (ResourceHandlerDescriptor handlerDescriptor : this.handlerDescriptors) {
            handlerDescriptor.dispose();
        }
        this.handlerDescriptors.clear();
        this.rootMapping.clear();

        // Remove listeners
        if (workspace != null) {
            workspace.removeResourceChangeListener(this);
            workspace = null;
        }

        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }
    }

    public void addProjectListener(DBPProjectListener listener)
    {
        synchronized (projectListeners) {
            projectListeners.add(listener);
        }
    }

    public void removeProjectListener(DBPProjectListener listener)
    {
        synchronized (projectListeners) {
            projectListeners.remove(listener);
        }
    }

    public DBPResourceHandler getResourceHandler(IResource resource)
    {
        if (resource == null || resource.isHidden() || resource.isPhantom()) {
            // Skip not accessible hidden and phantom resources
            return null;
        }
        if (resource.getParent() instanceof IProject && resource.getName().equals(DataSourceRegistry.CONFIG_FILE_NAME)) {
            // Skip connections settings file
            // TODO: remove in some older version
            return null;
        }
        DBPResourceHandler handler = null;
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            if (rhd.canHandle(resource)) {
                handler = rhd.getHandler();
                break;
            }
        }
        if (handler == null && resource instanceof IFolder) {
            IPath relativePath = resource.getFullPath().makeRelativeTo(resource.getProject().getFullPath());
            ResourceHandlerDescriptor handlerDescriptor = rootMapping.get(relativePath.toString());
            if (handlerDescriptor != null) {
                handler = handlerDescriptor.getHandler();
            }
        }
        if (handler == null) {
            handler = DefaultResourceHandlerImpl.INSTANCE;
        }
        return handler;
    }

    public IFolder getResourceDefaultRoot(IProject project, Class<? extends DBPResourceHandler> handlerType)
    {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            DBPResourceHandler handler = rhd.getHandler();
            if (handler != null && handler.getClass() == handlerType) {
                return project.getFolder(rhd.getDefaultRoot());
            }
        }
        return project.getFolder(DefaultResourceHandlerImpl.DEFAULT_ROOT);
    }

    public DataSourceRegistry getDataSourceRegistry(IProject project)
    {
        if (!project.isOpen()) {
            log.warn("Project '" + project.getName() + "' is not open - can't get datasource registry");
            return null;
        }
        DataSourceRegistry dataSourceRegistry = projectDatabases.get(project);
        if (dataSourceRegistry == null) {
            throw new IllegalStateException("Project '" + project.getName() + "' not found in registry");
        }
        return dataSourceRegistry;
    }

    public DataSourceRegistry getActiveDataSourceRegistry()
    {
        if (activeProject == null) {
            return null;
        }
        final DataSourceRegistry dataSourceRegistry = projectDatabases.get(activeProject);
        if (dataSourceRegistry == null) {
            throw new IllegalStateException("No registry for active project found");
        }
        return dataSourceRegistry;
    }

    public IProject getActiveProject()
    {
        return activeProject;
    }

    public void setActiveProject(IProject project)
    {
        final IProject oldValue = this.activeProject;
        this.activeProject = project;
        DBeaverCore.getInstance().getGlobalPreferenceStore().setValue("project.active", project == null ? "" : project.getName());

        GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_ACTIVE_PROJECT);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                synchronized (projectListeners) {
                    for (DBPProjectListener listener : projectListeners) {
                        listener.handleActiveProjectChange(oldValue, activeProject);
                    }
                }
            }
        });
    }

    private IProject createGeneralProject(IWorkspace workspace, IProgressMonitor monitor) throws CoreException
    {
        final IProject project = workspace.getRoot().getProject(
            DBeaverCore.isStandalone() ?
                "General" : "DBeaver");
        project.create(monitor);
        project.open(monitor);
        final IProjectDescription description = workspace.newProjectDescription(project.getName());
        description.setComment("General DBeaver project");
        project.setDescription(description, monitor);

        return project;
    }

    /**
     * We do not use resource listener in project registry because project should be added/removedhere
     * only after all other event handlers were finished and project was actually created/deleted.
     * Otherwise set of workspace synchronize problems occur
     * @param project project
     */
    public void addProject(IProject project)
    {
        projectDatabases.put(project, new DataSourceRegistry(project));
    }

    public void removeProject(IProject project)
    {
        // Remove project from registry
        if (project != null) {
            DataSourceRegistry dataSourceRegistry = projectDatabases.get(project);
            if (dataSourceRegistry == null) {
                log.warn("Project '" + project.getName() + "' not found in the registry");
            } else {
                dataSourceRegistry.dispose();
                projectDatabases.remove(project);
            }
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }
        for (IResourceDelta projectDelta : delta.getAffectedChildren()) {
            if (projectDelta.getResource() instanceof IProject) {
                IProject project = (IProject)projectDelta.getResource();
                if (projectDelta.getKind() == IResourceDelta.REMOVED) {
                    if (project == activeProject) {
                        setActiveProject(null);
                    }
                    GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                } else if (projectDelta.getKind() == IResourceDelta.ADDED) {
                    if (activeProject == null) {
                        setActiveProject(project);
                    }
                    GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                }
            }
        }
    }

}