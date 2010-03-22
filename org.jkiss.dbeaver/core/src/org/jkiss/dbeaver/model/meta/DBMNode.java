package org.jkiss.dbeaver.model.meta;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

import net.sf.jkiss.utils.CommonUtils;

/**
 * DBMNode
 */
public abstract class DBMNode
{
    static Log log = LogFactory.getLog(DBMNode.class);

    private DBMModel model;
    private DBMNode parentNode;

    protected DBMNode(DBMModel model)
    {
        this.model = model;
        this.parentNode = null;
    }

    protected DBMNode(DBMNode parentNode)
    {
        this.model = parentNode.getModel();
        this.parentNode = parentNode;
    }

    public boolean isDisposed()
    {
        return model == null;
    }

    void dispose()
    {
        this.model = null;
        this.parentNode = null;
    }

    public DBMModel getModel()
    {
        return model;
    }

    public DBMNode getParentNode()
    {
        return parentNode;
    }

    public abstract DBSObject getObject();

    public abstract Object getValueObject();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract Image getNodeIcon();

    public Image getNodeIconDefault()
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

    public abstract boolean hasChildren();
    
    public abstract List<? extends DBMNode> getChildren(ILoadService loadService)  throws DBException;

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public abstract DBMNode refreshNode() throws DBException;

    public abstract IAction getDefaultAction();

    public abstract boolean isLazyNode();

    public static Object[] convertNodesToObjects(List<? extends DBMNode> children)
    {
        if (CommonUtils.isEmpty(children)) {
            return new Object[0];
        }
        Object[] result = new Object[children.size()];
        for (int i = 0; i < children.size(); i++) {
            DBMNode child = children.get(i);
            result[i] = child.getObject();
        }
        return result;
    }
}
