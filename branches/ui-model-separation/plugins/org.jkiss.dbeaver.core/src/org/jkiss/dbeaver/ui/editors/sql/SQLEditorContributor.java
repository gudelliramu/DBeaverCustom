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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.editors.sql.handlers.CopyUnformattedTextAction;
import org.jkiss.dbeaver.ui.editors.sql.handlers.NavigateObjectAction;

import java.util.ResourceBundle;

/**
 * SQL Editor contributor
 */
public class SQLEditorContributor extends BasicTextEditorActionContributor
{
    static final String ACTION_CONTENT_ASSIST_PROPOSAL = "ContentAssistProposal"; //$NON-NLS-1$
    static final String ACTION_CONTENT_ASSIST_TIP = "ContentAssistTip"; //$NON-NLS-1$
    static final String ACTION_CONTENT_FORMAT_PROPOSAL = "ContentFormatProposal"; //$NON-NLS-1$

    private SQLEditorBase activeEditorPart;

    private RetargetTextEditorAction contentAssistProposal;
    private RetargetTextEditorAction contentAssistTip;
    private RetargetTextEditorAction contentFormatProposal;
    private CopyUnformattedTextAction copyUnformattedTextAction;
    private NavigateObjectAction navigateObjectAction;

    public SQLEditorContributor()
    {
        super();

        createActions();
    }

    static String getActionResourcePrefix(String actionId)
    {
        return "actions_" + actionId + "_";//$NON-NLS-1$
    }

    protected boolean isNestedEditor()
    {
        return false;
    }

    private void createActions()
    {
        // Init custom actions
        ResourceBundle bundle = DBeaverActivator.getResourceBundle();
        contentAssistProposal = new RetargetTextEditorAction(bundle, getActionResourcePrefix(ACTION_CONTENT_ASSIST_PROPOSAL));
        contentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        contentFormatProposal = new RetargetTextEditorAction(bundle, getActionResourcePrefix(ACTION_CONTENT_FORMAT_PROPOSAL));
        contentFormatProposal.setActionDefinitionId(ICommandIds.CMD_CONTENT_FORMAT);
        contentAssistTip = new RetargetTextEditorAction(bundle, getActionResourcePrefix(ACTION_CONTENT_ASSIST_TIP));
        contentAssistTip.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
        copyUnformattedTextAction = new CopyUnformattedTextAction();
        navigateObjectAction = new NavigateObjectAction();
        navigateObjectAction.setActionDefinitionId(ICommandIds.CMD_NAVIGATE_OBJECT);
    }

    @Override
    public void dispose()
    {
        setActiveEditor(null);

        super.dispose();
    }

    @Override
    public void setActiveEditor(IEditorPart targetEditor)
    {
        super.setActiveEditor(targetEditor);

        if (activeEditorPart == targetEditor) {
            return;
        }
        if (targetEditor instanceof SQLEditorBase) {
        	activeEditorPart = (SQLEditorBase)targetEditor;
        } else {
        	activeEditorPart = null;
        }

        if (activeEditorPart != null) {
            // Update editor actions
            contentAssistProposal.setAction(getAction(activeEditorPart, ACTION_CONTENT_ASSIST_PROPOSAL)); //$NON-NLS-1$
            contentAssistTip.setAction(getAction(activeEditorPart, ACTION_CONTENT_ASSIST_TIP)); //$NON-NLS-1$
            contentFormatProposal.setAction(getAction(activeEditorPart, ACTION_CONTENT_FORMAT_PROPOSAL)); //$NON-NLS-1$
            copyUnformattedTextAction.setEditor(activeEditorPart);

            activeEditorPart.setAction(ICommandIds.CMD_NAVIGATE_OBJECT, navigateObjectAction);
            navigateObjectAction.setEditor(activeEditorPart);
        }
    }

    @Override
    public void init(IActionBars bars)
    {
        super.init(bars);
    }

    @Override
    public void contributeToMenu(IMenuManager manager)
    {
        if (!isNestedEditor()) {
            super.contributeToMenu(manager);
        }

        IMenuManager editMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null && !isNestedEditor()) {
            //editMenu.add(new Separator());
            editMenu.add(contentAssistProposal);
            editMenu.add(contentAssistTip);
            MenuManager formatMenu = new MenuManager(CoreMessages.actions_menu_edit_ContentFormat);
            editMenu.add(formatMenu);
            formatMenu.add(contentFormatProposal);
            formatMenu.add(copyUnformattedTextAction);
            //editMenu.add(executeStatementAction);
            //editMenu.add(executeScriptAction);
        }
        IMenuManager navigateMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
        if (navigateMenu != null && !isNestedEditor()) {
            navigateMenu.add(navigateObjectAction);
        }
    }

    @Override
    public void contributeToCoolBar(ICoolBarManager manager)
    {
        if (!isNestedEditor()) {
            super.contributeToCoolBar(manager);
        }
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager)
    {
        if (!isNestedEditor()) {
            super.contributeToToolBar(manager);

            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_EXECUTE_STATEMENT));
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_EXECUTE_SCRIPT));
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_EXPLAIN_PLAN));
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_ANALYSE_STATEMENT));
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_VALIDATE_STATEMENT));
        }
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        if (!isNestedEditor()) {
            super.contributeToStatusLine(statusLineManager);
        }
    }

}