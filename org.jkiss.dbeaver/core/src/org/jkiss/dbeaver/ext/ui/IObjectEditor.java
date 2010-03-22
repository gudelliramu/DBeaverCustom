package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPObject;

/**
 * IObjectEditor
 */
public interface IObjectEditor extends IEmbeddedWorkbenchPart, IEditorPart {

    DBPObject getObject();

    void setObject(DBPObject object);

}
