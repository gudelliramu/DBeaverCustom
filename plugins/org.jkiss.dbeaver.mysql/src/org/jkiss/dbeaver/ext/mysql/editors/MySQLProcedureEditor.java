/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * MySQLProcedureEditor
 */
public class MySQLProcedureEditor extends AbstractDatabaseObjectEditor<MySQLProcedure>
{
    static final Log log = LogFactory.getLog(MySQLProcedureEditor.class);

    private Text ddlText;

    public void createPartControl(Composite parent)
    {
        ddlText = new Text(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.BORDER);
        ddlText.setForeground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        ddlText.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    }

    public void activatePart()
    {
        try {
            ddlText.setText(getDatabaseObject().getBody());
        }
        catch (Exception ex) {
            log.error("Can't obtain procedure body", ex);
        }
    }

}