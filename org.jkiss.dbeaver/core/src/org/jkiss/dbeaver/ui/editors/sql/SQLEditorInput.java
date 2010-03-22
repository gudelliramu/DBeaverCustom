package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * SQLEditorInput
 */
public class SQLEditorInput extends FileEditorInput implements IAutoSaveEditorInput
{
    private DBSDataSourceContainer dataSourceContainer;
    private String scriptName;

    public SQLEditorInput(IFile file, DBSDataSourceContainer dataSourceContainer, String scriptName)
    {
        super(file);
        this.dataSourceContainer = dataSourceContainer;
        this.scriptName = scriptName;
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(DBSDataSourceContainer container)
    {
        dataSourceContainer = container;
    }

    public String getScriptName()
    {
        return scriptName;
    }

    public String getName()
    {
        if (dataSourceContainer == null) {
            return "<None> - " + scriptName;
        }
        return dataSourceContainer.getName() + " - " + scriptName;
    }

    public String getToolTipText()
    {
        if (dataSourceContainer == null) {
            return super.getName();
        }
        String toolTip = dataSourceContainer.getDescription();
        if (toolTip == null) {
            toolTip = dataSourceContainer.getName();
        }
        return toolTip;
    }

    public String getFactoryId()
    {
        return SQLEditorInputFactory.getFactoryId();
    }

    public void saveState(IMemento memento) 
    {
        SQLEditorInputFactory.saveState(memento, this);
    }

    public boolean isAutoSaveEnabled()
    {
        return true;
    }

}
