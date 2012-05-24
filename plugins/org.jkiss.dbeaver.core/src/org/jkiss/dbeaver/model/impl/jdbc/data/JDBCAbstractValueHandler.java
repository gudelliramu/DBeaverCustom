/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.swt.IFocusService;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    static final Log log = LogFactory.getLog(JDBCAbstractValueHandler.class);
    private static final String CELL_VALUE_INLINE_EDITOR = "org.jkiss.dbeaver.CellValueInlineEditor";

    @Override
    public final Object getValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject column, int columnIndex)
        throws DBCException
    {
        try {
            return getColumnValue(context, (JDBCResultSet) resultSet, column, columnIndex + 1);
        }
        catch (Throwable e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_get_result_set_value, e);
        }
    }

    @Override
    public final void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnMetaData,
                                      int paramIndex, Object value) throws DBCException {
        try {
            this.bindParameter((JDBCExecutionContext) context, (JDBCPreparedStatement) statement, columnMetaData, paramIndex + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @Override
    public Object createValueObject(DBCExecutionContext context, DBSTypedObject column) throws DBCException
    {
        // Default value for most object types is NULL
        return null;
    }

    @Override
    public Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard) throws DBException
    {
        // By default handler doesn't support any clipboard format
        return null;
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value) {
        return value == null ? DBConstants.NULL_VALUE_LABEL : value.toString();
    }

    @Override
    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException
    {
        return null;
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {

    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "column_size", //$NON-NLS-1$
            CoreMessages.model_jdbc_column_size,
            controller.getColumnMetaData().getMaxLength());
    }

    protected static interface ValueExtractor <T extends Control> {
         Object getValueFromControl(T control);
    }

    protected <T extends Control> void initInlineControl(
        final DBDValueController controller,
        final T control,
        final ValueExtractor<T> extractor)
    {
        control.setFont(controller.getInlinePlaceholder().getFont());
        control.addTraverseListener(new TraverseListener()
        {
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    controller.updateValue(extractor.getValueFromControl(control));
                    controller.closeInlineEditor();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    controller.closeInlineEditor();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                } else if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    controller.updateValue(extractor.getValueFromControl(control));
                    controller.nextInlineEditor(e.detail == SWT.TRAVERSE_TAB_NEXT);
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });
        final IFocusService focusService = (IFocusService) controller.getValueSite().getService(IFocusService.class);
        if (focusService != null) {
            focusService.addFocusTracker(control, CELL_VALUE_INLINE_EDITOR);
        }
        control.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                // Check new focus control in async mode
                // (because right now focus is still on edit control)
                control.getDisplay().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Control newFocus = control.getDisplay().getFocusControl();
                        if (newFocus != null) {
                            for (Control fc = newFocus.getParent(); fc != null; fc = fc.getParent()) {
                                if (fc == controller.getInlinePlaceholder()) {
                                    // New focus is still a child of inline placeholder - do not close it
                                    return;
                                }
                            }
                        }
                        controller.updateValue(extractor.getValueFromControl(control));
                        controller.closeInlineEditor();
                    }
                });
            }
        });
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (focusService != null) {
                    focusService.removeFocusTracker(control);
                }
            }
        });
    }

    protected abstract Object getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column, int columnIndex)
        throws DBCException, SQLException;

    protected abstract void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException;

}