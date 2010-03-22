package org.jkiss.dbeaver.ui.editors.splitted;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.ui.DBeaverConstants;

public class SplitterEditorSite implements IEditorSite
{
    private IWorkbenchPart part;
    private SplitterEditorPart splitterEditor;
    private ISelectionChangedListener postSelectionChangedListener = null;

    private ISelectionChangedListener selectionChangedListener = null;
    private ISelectionProvider selectionProvider = null;

    public SplitterEditorSite(
        SplitterEditorPart splitterEditorPart,
        IWorkbenchPart part)
    {
        assert(splitterEditorPart != null);
        assert(part != null);
        this.splitterEditor = splitterEditorPart;
        this.part = part;
    }

    private IEditorSite getParentSite()
    {
        return splitterEditor.getEditorSite();
    }

    public IEditorActionBarContributor getActionBarContributor()
    {
        return getParentSite().getActionBarContributor();
    }

    public IActionBars getActionBars()
    {
        return getParentSite().getActionBars();
    }

    /*
      * (non-Javadoc)
      *
      * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
      */
    public Object getAdapter(Class adapter)
    {
        return getParentSite().getAdapter(adapter);
    }

    public String getId()
    {
        return getParentSite().getId(); //$NON-NLS-1$
    }

    @Deprecated
    public IKeyBindingService getKeyBindingService()
    {
        return getParentSite().getKeyBindingService();
    }

    public SplitterEditorPart getSplitterEditor()
    {
        return splitterEditor;
    }

    public IWorkbenchPage getPage()
    {
        return getParentSite().getPage();
    }

    public IWorkbenchPart getPart()
    {
        return part;
    }

    public String getPluginId()
    {
        return DBeaverConstants.PLUGIN_ID;
    }

    private ISelectionChangedListener getPostSelectionChangedListener()
    {
        if (postSelectionChangedListener == null) {
            postSelectionChangedListener = new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    SplitterEditorSite.this.handlePostSelectionChanged(event);
                }
            };
        }
        return postSelectionChangedListener;
    }

    public String getRegisteredName()
    {
        return getParentSite().getRegisteredName(); //$NON-NLS-1$
    }

    private ISelectionChangedListener getSelectionChangedListener()
    {
        if (selectionChangedListener == null) {
            selectionChangedListener = new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    SplitterEditorSite.this.handleSelectionChanged(event);
                }
            };
        }
        return selectionChangedListener;
    }

    public ISelectionProvider getSelectionProvider()
    {
        return selectionProvider;
    }

    public final Object getService(final Class key)
    {
        return getParentSite().getService(key);
    }

    public final boolean hasService(final Class key)
    {
        return getParentSite().hasService(key);
    }

    public Shell getShell()
    {
        return getParentSite().getShell();
    }

    public IWorkbenchWindow getWorkbenchWindow()
    {
        return getParentSite().getWorkbenchWindow();
    }

    protected void handlePostSelectionChanged(SelectionChangedEvent event)
    {
        ISelectionProvider parentProvider = getParentSite().getSelectionProvider();
        if (parentProvider instanceof SplitterSelectionProvider) {
            SelectionChangedEvent newEvent = new SelectionChangedEvent(
                parentProvider, event.getSelection());
            SplitterSelectionProvider prov = (SplitterSelectionProvider) parentProvider;
            prov.firePostSelectionChanged(newEvent);
        }
    }

    protected void handleSelectionChanged(SelectionChangedEvent event)
    {
        ISelectionProvider parentProvider = getParentSite().getSelectionProvider();
        if (parentProvider instanceof SplitterSelectionProvider) {
            SelectionChangedEvent newEvent = new SelectionChangedEvent(
                parentProvider, event.getSelection());
            SplitterSelectionProvider prov = (SplitterSelectionProvider) parentProvider;
            prov.fireSelectionChanged(newEvent);
        }
    }

    public void registerContextMenu(MenuManager menuManager,
        ISelectionProvider selProvider)
    {
        getParentSite().registerContextMenu(menuManager, selProvider);
    }

    public final void registerContextMenu(final MenuManager menuManager,
        final ISelectionProvider selectionProvider,
        final boolean includeEditorInput)
    {
        getParentSite().registerContextMenu(menuManager, selectionProvider, includeEditorInput);
    }

    public void registerContextMenu(String menuID, MenuManager menuMgr, ISelectionProvider selProvider)
    {
        getParentSite().registerContextMenu(menuID, menuMgr, selProvider);
    }

    public final void registerContextMenu(final String menuId,
        final MenuManager menuManager,
        final ISelectionProvider selectionProvider,
        final boolean includeEditorInput)
    {
        getParentSite().registerContextMenu(menuId, menuManager, selectionProvider, includeEditorInput);
    }

    public void setSelectionProvider(ISelectionProvider provider)
    {
        ISelectionProvider oldSelectionProvider = selectionProvider;
        selectionProvider = provider;
        if (oldSelectionProvider != null) {
            oldSelectionProvider.removeSelectionChangedListener(getSelectionChangedListener());
            if (oldSelectionProvider instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) oldSelectionProvider)
                    .removePostSelectionChangedListener(getPostSelectionChangedListener());
            }
        }
        if (selectionProvider != null) {
            selectionProvider.addSelectionChangedListener(getSelectionChangedListener());
            if (selectionProvider instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) selectionProvider)
                    .addPostSelectionChangedListener(getPostSelectionChangedListener());
            }
        }
    }
}