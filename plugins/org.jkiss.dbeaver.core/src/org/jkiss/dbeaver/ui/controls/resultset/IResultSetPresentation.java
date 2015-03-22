/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

/**
 * Result set renderer.
 * Visualizes result set viewer/editor.
 *
 * May additionally implement ISelectionProvider, IStatefulControl
 */
public interface IResultSetPresentation {

    public enum RowPosition {
        FIRST,
        PREVIOUS,
        NEXT,
        LAST
    }


    void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent);

    Control getControl();

    void refreshData(boolean refreshMetadata);

    /**
     * Called after results refresh
     * @param refreshData data was refreshed
     */
    void formatData(boolean refreshData);

    void clearData();

    void updateValueView();

    void fillToolbar(@NotNull IToolBarManager toolBar);

    void fillMenu(@NotNull IMenuManager menu);

    void changeMode(boolean recordMode);

    void scrollToRow(@NotNull RowPosition position);

    @Nullable
    DBDAttributeBinding getCurrentAttribute();

    @Nullable
    Control openValueEditor(final boolean inline);

    @Nullable
    String copySelectionToString(
        boolean copyHeader,
        boolean copyRowNumbers,
        boolean cut,
        String delimiter,
        DBDDisplayFormat format);

    void pasteFromClipboard();

}