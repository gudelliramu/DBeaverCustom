package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.meta.DBMModel;

/**
 * IDatabaseEditorInput
 */
public interface IDatabaseEditorInput extends IEditorInput {

    DBMModel getModel();

    DBSObject getDatabaseObject();
}
