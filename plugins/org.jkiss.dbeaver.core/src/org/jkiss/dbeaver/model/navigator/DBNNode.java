/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements DBPNamedObject, JexlContext
{
    static final Log log = LogFactory.getLog(DBNNode.class);

    public static final String JEXL_VAR_NODE = "node";

    private DBNModel model;
    private DBNNode parentNode;

    protected DBNNode(DBNModel model)
    {
        this.model = model;
        this.parentNode = null;
    }

    protected DBNNode(DBNNode parentNode)
    {
        this.model = parentNode.getModel();
        this.parentNode = parentNode;
    }

    public final boolean isDisposed()
    {
        return model == null;
    }

    void dispose(boolean reflect)
    {
        this.model = null;
        this.parentNode = null;
    }

    public final DBNModel getModel()
    {
        return model;
    }

    public final DBNNode getParentNode()
    {
        return parentNode;
    }

    public boolean isLocked()
    {
        return getParentNode() != null && getParentNode().isLocked();
    }

    public boolean isManagable()
    {
        return false;
    }

    public String getName()
    {
        return getNodeName();
    }

    public abstract String getNodeType();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract Image getNodeIcon();

    public final Image getNodeIconDefault()
    {
        Image image = getNodeIcon();
        if (image == null) {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
            if (this.hasChildren()) {
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            }
            image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
        return image;
    }

    public String getNodePathName()
    {
        StringBuilder pathName = new StringBuilder();
        pathName.append(getNodeName());
        for (DBNNode parent = getParentNode(); parent != null && !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
            if (!CommonUtils.isEmpty(parentName)) {
                pathName.insert(0, '.').insert(0, parentName);
            }
        }
        return pathName.toString();
    }

    public abstract boolean hasChildren();

    public abstract boolean hasNavigableChildren();
    
    public abstract List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException;

    void clearNode(boolean reflect)
    {

    }

    public boolean supportsRename()
    {
        return false;
    }

    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        throw new DBException("Rename is not supported");
    }

    public boolean supportsDrop(DBNNode otherNode)
    {
        return false;
    }

    public void dropNode(DBNNode otherNode) throws DBException
    {
        throw new DBException("Drop is not supported");
    }

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually changes navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     * @param monitor progress monitor
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public DBNNode refreshNode(DBRProgressMonitor monitor) throws DBException
    {
        if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor);
        } else {
            return null;
        }
    }
    
    public abstract String getDefaultCommandId();

    public final boolean isChildOf(DBNNode node)
    {
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent == node) {
                return true;
            }
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////
    // Jexl context implementation

    public Object get(String s)
    {
        if (JEXL_VAR_NODE.equals(s)) {
            return this;
        }
        return null;
    }

    public void set(String s, Object o)
    {
        log.warn("Try to set jexl property '" + s + "' in read-only DBN node context");
    }

    public boolean has(String s)
    {
        return JEXL_VAR_NODE.equals(s);
    }

}
