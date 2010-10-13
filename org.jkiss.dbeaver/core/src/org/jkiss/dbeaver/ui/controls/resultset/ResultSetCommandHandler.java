/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.controls.spreadsheet.SpreadsheetCommandHandler;

/**
 * ResultSetCommandHandler
 */
public class ResultSetCommandHandler extends SpreadsheetCommandHandler {
    public static final String CMD_TOGLE_MODE = "org.jkiss.dbeaver.core.resultset.toggleMode";
    public static final String CMD_ROW_FIRST = "org.jkiss.dbeaver.core.resultset.row.first";
    public static final String CMD_ROW_PREVIOUS = "org.jkiss.dbeaver.core.resultset.row.previous";
    public static final String CMD_ROW_NEXT = "org.jkiss.dbeaver.core.resultset.row.next";
    public static final String CMD_ROW_LAST = "org.jkiss.dbeaver.core.resultset.row.last";
    public static final String CMD_ROW_EDIT = "org.jkiss.dbeaver.core.resultset.row.edit";
    public static final String CMD_ROW_ADD = "org.jkiss.dbeaver.core.resultset.row.add";
    public static final String CMD_ROW_COPY = "org.jkiss.dbeaver.core.resultset.row.copy";
    public static final String CMD_ROW_DELETE = "org.jkiss.dbeaver.core.resultset.row.delete";
    public static final String CMD_APPLY_CHANGES = "org.jkiss.dbeaver.core.resultset.applyChanges";
    public static final String CMD_REJECT_CHANGES = "org.jkiss.dbeaver.core.resultset.rejectChanges";

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        Spreadsheet spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }
        if (!(spreadsheet.getController() instanceof ResultSetViewer)) {
            return null;
        }
        ResultSetViewer resultSet = (ResultSetViewer) spreadsheet.getController();
        //ResultSetViewer.ResultSetMode resultSetMode = resultSet.getMode();
        String actionId = event.getCommand().getId();
        if (actionId.equals(IWorkbenchCommandConstants.FILE_REFRESH)) {
            resultSet.refresh();
        } else if (actionId.equals(CMD_TOGLE_MODE)) {
            resultSet.toggleMode();
        } else if (actionId.equals(CMD_ROW_PREVIOUS) || actionId.equals(ITextEditorActionDefinitionIds.WORD_PREVIOUS)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.PREVIOUS);
        } else if (actionId.equals(CMD_ROW_NEXT) || actionId.equals(ITextEditorActionDefinitionIds.WORD_NEXT)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.NEXT);
        } else if (actionId.equals(CMD_ROW_FIRST) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.FIRST);
        } else if (actionId.equals(CMD_ROW_LAST) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.LAST);
        } else if (actionId.equals(CMD_ROW_EDIT)) {
            resultSet.editCurrentRow();
        } else if (actionId.equals(CMD_ROW_ADD)) {
            resultSet.addNewRow(false);
        } else if (actionId.equals(CMD_ROW_COPY)) {
            resultSet.addNewRow(true);
        } else if (actionId.equals(CMD_ROW_DELETE)) {
            resultSet.deleteCurrentRow();
        } else if (actionId.equals(CMD_APPLY_CHANGES)) {
            resultSet.applyChanges();
        } else if (actionId.equals(CMD_REJECT_CHANGES)) {
            resultSet.rejectChanges();
        }


        return null;
    }

}