/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ImageUtils;

import java.text.Collator;
import java.util.*;
import java.util.List;

/**
 * Driver properties control
 */
public class PropertyTreeViewer extends TreeViewer {

    private static final String CATEGORY_GENERAL = "General";

    private boolean expandSingleRoot = true;
    private TreeEditor treeEditor;

    private Font boldFont;
    //private Color colorBlue;
    private Clipboard clipboard;
    private int selectedColumn = -1;
    private CellEditor curCellEditor;
    private IPropertyDescriptor selectedProperty;

    private String[] customCategories;
    private IBaseLabelProvider extraLabelProvider;

    public PropertyTreeViewer(Composite parent, int style)
    {
        super(parent, style | SWT.SINGLE | SWT.FULL_SELECTION);

        //colorBlue = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
        clipboard = new Clipboard(parent.getDisplay());

        //this.setLayout(new GridLayout(1, false));
        //GridData gd = new GridData(GridData.FILL_BOTH);
        //this.setLayoutData(gd);

        PropsLabelProvider labelProvider = new PropsLabelProvider();

        super.setContentProvider(new PropsContentProvider());
        //super.setLabelProvider(labelProvider);
        final Tree treeControl = super.getTree();
        if (parent.getLayout() instanceof GridLayout) {
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            gd.minimumHeight = 120;
            gd.heightHint = 120;
            gd.widthHint = 300;
            treeControl.setLayoutData(gd);
        }
        treeControl.setHeaderVisible(true);
        treeControl.setLinesVisible(true);

        treeControl.addControlListener(new ControlAdapter() {
            private boolean packing = false;
            @Override
            public void controlResized(ControlEvent e) {
                if (!packing) {
                    try {
                        packing = true;
                        UIUtils.packColumns(treeControl, true, new float[] { 0.2f , 0.8f });
                    }
                    finally {
                        packing = false;
                    }
                }
            }
        });
        treeControl.addListener(SWT.PaintItem, new PaintListener());
        this.boldFont = UIUtils.makeBoldFont(treeControl.getFont());

        ColumnViewerToolTipSupport.enableFor(this, ToolTip.NO_RECREATE);

        TreeViewerColumn column = new TreeViewerColumn(this, SWT.NONE);
        column.getColumn().setWidth(200);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Name");
        column.setLabelProvider(labelProvider);
        column.getColumn().addListener(SWT.Selection, new SortListener());


        column = new TreeViewerColumn(this, SWT.NONE);
        column.getColumn().setWidth(120);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Value");
        column.setLabelProvider(labelProvider);

        /*
                List<? extends DBPProperty> props = ((DBPPropertyGroup) parent).getProperties();
                Collections.sort(props, new Comparator<DBPProperty>() {
                    public int compare(DBPProperty o1, DBPProperty o2)
                    {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                return props.toArray();

        */
        registerEditor();
        registerContextMenu();
    }

    public void loadProperties(IPropertySource propertySource)
    {
        loadProperties(null, propertySource);
    }

    protected void loadProperties(TreeNode parent, IPropertySource propertySource)
    {
        // Make tree model
        customCategories = getCustomCategories();

        Map<String, TreeNode> categories = new LinkedHashMap<String, TreeNode>();
        final IPropertyDescriptor[] props = propertySource.getPropertyDescriptors();
        for (IPropertyDescriptor prop : props) {
            String categoryName = prop.getCategory();
            if (CommonUtils.isEmpty(categoryName)) {
                categoryName = CATEGORY_GENERAL;
            }
            TreeNode category = categories.get(categoryName);
            if (category == null) {
                category = new TreeNode(parent, propertySource, categoryName);
                categories.put(categoryName, category);
            }
            new TreeNode(category, propertySource, prop);
        }
        if (customCategories != null) {
            for (String customCategory : customCategories) {
                TreeNode node = categories.get(customCategory);
                if (node == null) {
                    node = new TreeNode(parent, propertySource, customCategory);
                    categories.put(customCategory, node);
                }
            }
        }
        Object root;
        if (categories.size() == 1 && expandSingleRoot) {
            final Collection<TreeNode> values = categories.values();
            root = values.iterator().next();
        } else {
            root = categories.values();
        }

        super.setInput(root);
        super.expandAll();

        disposeOldEditor();
    }

    protected void addProperty(Object node, IPropertyDescriptor property)
    {
        if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)node;
            while (treeNode.property != null) {
                treeNode = treeNode.parent;
            }
            final TreeNode newNode = new TreeNode(treeNode, treeNode.propertySource, property);
            handlePropertyCreate(newNode);
        }
    }

    protected void removeProperty(Object node)
    {
        if (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode)node;
            if (treeNode.propertySource instanceof IPropertySourceEx) {
                ((IPropertySourceEx) treeNode.propertySource).resetPropertyValueToDefault(treeNode.property.getId());
            } else {
                treeNode.propertySource.resetPropertyValue(treeNode.property.getId());
            }
            treeNode.parent.children.remove(treeNode);
            handlePropertyRemove(treeNode);
        }
    }

    public void refresh()
    {
        disposeOldEditor();
        super.refresh();
    }

    public IPropertyDescriptor getSelectedProperty()
    {
        return selectedProperty;
    }

    private void disposeOldEditor()
    {
        if (curCellEditor != null) {
            curCellEditor.deactivate();
            curCellEditor.dispose();
            curCellEditor = null;
            selectedProperty = null;
        }
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    private void registerEditor() {
        // Make an editor
        final Tree treeControl = super.getTree();
        treeEditor = new TreeEditor(treeControl);
        treeEditor.horizontalAlignment = SWT.RIGHT;
        treeEditor.verticalAlignment = SWT.CENTER;
        treeEditor.grabHorizontal = true;
        treeEditor.minimumWidth = 50;

        treeControl.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                showEditor((TreeItem) e.item, true);
            }

            public void widgetSelected(SelectionEvent e) {
                showEditor((TreeItem) e.item, selectedColumn == 1 && (e.stateMask & SWT.BUTTON_MASK) != 0);
            }
        });
        treeControl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e)
            {
                TreeItem item = treeControl.getItem(new Point(e.x, e.y));
                if (item != null) {
                    selectedColumn = UIUtils.getColumnAtPos(item, e.x, e.y);
                } else {
                    selectedColumn = -1;
                }
            }
        });
        treeControl.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    // Set focus on editor
                    if (curCellEditor != null) {
                        curCellEditor.setFocus();
                    } else {
                        final TreeItem[] selection = treeControl.getSelection();
                        if (selection.length == 0) {
                            return;
                        }
                        showEditor(selection[0], true);
                    }
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });
    }

    private void showEditor(final TreeItem item, boolean isDef) {
        // Clean up any previous editor control
        disposeOldEditor();
        if (item == null) {
            return;
        }

        // Identify the selected row
        if (item.getData() instanceof TreeNode) {
            final Tree treeControl = super.getTree();
            final TreeNode prop = (TreeNode)item.getData();
            if (prop.property == null || !prop.isEditable()) {
                return;
            }
            final CellEditor cellEditor = prop.property.createPropertyEditor(treeControl);
            if (cellEditor == null) {
                return;
            }
            final Object propertyValue = prop.propertySource.getPropertyValue(prop.property.getId());
            final ICellEditorListener cellEditorListener = new ICellEditorListener() {
                public void applyEditorValue()
                {
                    //editorValueChanged(true, true);
                    final Object value = cellEditor.getValue();
                    final Object oldValue = prop.propertySource.getPropertyValue(prop.property.getId());
                    if (!CommonUtils.equalObjects(oldValue, value)) {
                        prop.propertySource.setPropertyValue(
                            prop.property.getId(),
                            value);
                        handlePropertyChange(prop);
                    }
                }

                public void cancelEditor()
                {
                    disposeOldEditor();
                }

                public void editorValueChanged(boolean oldValidState, boolean newValidState)
                {
                }
            };
            cellEditor.addListener(cellEditorListener);
            if (propertyValue != null) {
                cellEditor.setValue(propertyValue);
            }
            curCellEditor = cellEditor;
            selectedProperty = prop.property;

            cellEditor.activate();
            final Control editorControl = cellEditor.getControl();
            if (editorControl != null) {
                editorControl.addTraverseListener(new TraverseListener() {
                    public void keyTraversed(TraverseEvent e)
                    {
                        if (e.detail == SWT.TRAVERSE_RETURN) {
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                            cellEditorListener.applyEditorValue();
                            disposeOldEditor();
                        } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                            disposeOldEditor();
                            if (prop.isEditable()) {
                                new ActionResetProperty(prop, false).run();
                            }
                        }
                    }
                });
                treeEditor.setEditor(editorControl, item, 1);
            }
            if (isDef) {
                // Selected by mouse
                cellEditor.setFocus();
            }
        }
    }

    private void registerContextMenu() {
        // Register context menu
        {
            MenuManager menuMgr = new MenuManager();
            menuMgr.addMenuListener(new IMenuListener()
            {
                public void menuAboutToShow(final IMenuManager manager)
                {
                    final IStructuredSelection selection = (IStructuredSelection)PropertyTreeViewer.this.getSelection();

                    if (selection.isEmpty()) {
                        return;
                    }
                    final Object object = selection.getFirstElement();
                    if (object instanceof TreeNode) {
                        final TreeNode prop = (TreeNode)object;
                        if (prop.property != null) {
                            manager.add(new Action("Copy value") {
                                @Override
                                public void run() {
                                    TextTransfer textTransfer = TextTransfer.getInstance();
                                    clipboard.setContents(
                                        new Object[]{CommonUtils.toString(getPropertyValue(prop))},
                                        new Transfer[]{textTransfer});
                                }
                            });
                            if (isPropertyChanged(prop) && prop.isEditable()) {
                                if (prop.propertySource instanceof IPropertySource2 && !((IPropertySource2) prop.propertySource).isPropertyResettable(prop.property.getId())) {
                                    // it is not resettable
                                } else {
                                    manager.add(new ActionResetProperty(prop, false));
                                    if (!isCustomProperty(prop.property) &&
                                        prop.propertySource instanceof IPropertySourceEx)
                                    {
                                        manager.add(new ActionResetProperty(prop, true));
                                    }
                                }
                            }
                            manager.add(new Separator());
                        }
                        contributeContextMenu(manager, object, prop.category != null ? prop.category : prop.property.getCategory(), prop.property);
                    }
                }
            });

            menuMgr.setRemoveAllWhenShown(true);
            Menu menu = menuMgr.createContextMenu(getTree());

            getTree().setMenu(menu);
        }
    }

    private boolean isCustomProperty(IPropertyDescriptor property)
    {
        if (customCategories != null) {
            for (String category : customCategories) {
                if (category.equals(property.getCategory())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String[] getCustomCategories()
    {
        return null;
    }

    protected void contributeContextMenu(IMenuManager manager, Object node, String category, IPropertyDescriptor property)
    {

    }

    private Object getPropertyValue(TreeNode prop)
    {
        if (prop.category != null) {
            return prop.category;
        } else {
            final Object propertyValue = prop.propertySource.getPropertyValue(prop.property.getId());
            if (propertyValue instanceof DBPNamedObject) {
                return ((DBPNamedObject) propertyValue).getName();
            }
            return UIUtils.makeStringForUI(propertyValue);
        }
    }

    private boolean isPropertyChanged(TreeNode prop)
    {
        return prop.propertySource.isPropertySet(prop.property.getId());
    }

    private void handlePropertyChange(TreeNode prop)
    {
        super.update(prop, null);

        // Send modify event
        Event event = new Event();
        event.data = prop.property;
        getTree().notifyListeners(SWT.Modify, event);
    }

    protected void handlePropertyCreate(TreeNode prop) {
        handlePropertyChange(prop);
        super.refresh(prop.parent);
        super.expandToLevel(prop.parent, 1);
        super.reveal(prop);
        super.setSelection(new StructuredSelection(prop));
    }

    protected void handlePropertyRemove(TreeNode prop) {
        handlePropertyChange(prop);
        super.refresh(prop.parent);
    }

    public void setExpandSingleRoot(boolean expandSingleRoot)
    {
        this.expandSingleRoot = expandSingleRoot;
    }

    public void setExtraLabelProvider(IBaseLabelProvider extraLabelProvider)
    {
        this.extraLabelProvider = extraLabelProvider;
    }

    private static class TreeNode {
        final TreeNode parent;
        final IPropertySource propertySource;
        final IPropertyDescriptor property;
        final String category;
        final List<TreeNode> children = new ArrayList<TreeNode>();

        private TreeNode(TreeNode parent, IPropertySource propertySource, IPropertyDescriptor property, String category)
        {
            this.parent = parent;
            this.propertySource = propertySource;
            this.property = property;
            this.category = category;
            if (parent != null) {
                parent.children.add(this);
            }
        }

        private TreeNode(TreeNode parent, IPropertySource propertySource, IPropertyDescriptor property)
        {
            this(parent, propertySource, property, null);
        }

        private TreeNode(TreeNode parent, IPropertySource propertySource, String category)
        {
            this(parent, propertySource, null, category);
        }

        boolean isEditable()
        {
            if (property instanceof IPropertyDescriptorEx) {
                return ((IPropertyDescriptorEx)property).isEditable(propertySource.getEditableValue());
            } else {
                return property != null;
            }
        }
    }

    class PropsContentProvider implements IStructuredContentProvider, ITreeContentProvider
    {
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
        }

        public void dispose()
        {
        }

        public Object[] getElements(Object parent)
        {
            return getChildren(parent);
        }

        public Object getParent(Object child)
        {
            if (child instanceof TreeNode) {
                return ((TreeNode) child).parent;
            } else {
                return null;
            }
        }

        public Object[] getChildren(Object parent)
        {
            if (parent instanceof Collection) {
                return ((Collection) parent).toArray();
            } else if (parent instanceof TreeNode) {
                // Add all available property groups
                return ((TreeNode) parent).children.toArray();
            } else {
                return new Object[0];
            }
        }

        public boolean hasChildren(Object parent)
        {
            return getChildren(parent).length > 0;
        }
    }

    private class PropsLabelProvider extends CellLabelProvider
    {

        public String getText(Object obj, int columnIndex)
        {
            if (!(obj instanceof TreeNode)) {
                return "";
            }
            TreeNode node = (TreeNode)obj;
            if (columnIndex == 0) {
                if (node.category != null) {
                    return node.category;
                } else {
                    return node.property.getDisplayName();
                }
            } else {
                if (node.property != null) {
                    final Object propertyValue = getPropertyValue(node);
                    if (propertyValue instanceof Boolean) {
                        return "";
                    }
                    return CommonUtils.toString(propertyValue);
                } else {
                    return "";
                }
            }
        }

        public String getToolTipText(Object obj)
        {
            if (!(obj instanceof TreeNode)) {
                return "";
            }
            TreeNode node = (TreeNode)obj;
            if (node.category != null) {
                return node.category;
            } else {
                return node.property.getDescription();
            }
        }

        public Point getToolTipShift(Object object)
        {
            return new Point(5, 5);
        }

        public void update(ViewerCell cell)
        {
            Object element = cell.getElement();
            cell.setText(getText(element, cell.getColumnIndex()));
            if (!(element instanceof TreeNode)) {
                return;
            }
            TreeNode node = (TreeNode) element;
            boolean changed = false;
            if (node.property != null) {
                changed = node.isEditable() && isPropertyChanged(node);
/*
                if (((DBPProperty)element).isRequired() && cell.getColumnIndex() == 0) {
                    cell.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
                }
*/
            }
            if (extraLabelProvider instanceof IFontProvider) {
                cell.setFont(((IFontProvider) extraLabelProvider).getFont(node.property));

            } else if (changed) {
                cell.setFont(boldFont);
            } else {
                cell.setFont(null);
            }
        }

    }

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        TreeColumn prevColumn = null;

        public void handleEvent(Event e) {
            disposeOldEditor();

            Collator collator = Collator.getInstance(Locale.getDefault());
            TreeColumn column = (TreeColumn)e.widget;
            Tree tree = getTree();
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = (sortDirection == SWT.UP ? SWT.DOWN : SWT.UP);
            }
            prevColumn = column;
            tree.setSortColumn(column);
            tree.setSortDirection(sortDirection);

            PropertyTreeViewer.this.setSorter(new ViewerSorter(collator) {
                public int compare(Viewer viewer, Object e1, Object e2)
                {
                    int mul = (sortDirection == SWT.UP ? 1 : -1);
                    int result;
                    TreeNode n1 = (TreeNode) e1, n2 = (TreeNode) e2;
                    if (n1.property != null && n2.property != null) {
                        result = n1.property.getDisplayName().compareTo(n2.property.getDisplayName());
                    } else if (n1.category != null && n2.category != null) {
                        result = n1.category.compareTo(n2.category);
                    } else {
                        result = 0;
                    }
                    return result * mul;
                }
            });
        }
    }

    private class ActionResetProperty extends Action {
        private final TreeNode prop;
        private final boolean toDefault;

        public ActionResetProperty(TreeNode prop, boolean toDefault)
        {
            super("Reset value" + (!toDefault ? "" : " to default"));
            this.prop = prop;
            this.toDefault = toDefault;
        }

        @Override
        public void run() {
            if (toDefault && prop.propertySource instanceof IPropertySourceEx) {
                ((IPropertySourceEx)prop.propertySource).resetPropertyValueToDefault(prop.property.getId());
            } else {
                prop.propertySource.resetPropertyValue(prop.property.getId());
            }
            handlePropertyChange(prop);
            PropertyTreeViewer.this.update(prop, null);
            disposeOldEditor();
        }
    }

    class PaintListener implements Listener {

        public void handleEvent(Event event) {
            if (getTree().isDisposed()) {
                return;
            }
            switch(event.type) {
                case SWT.PaintItem: {
                    if (event.index == 1) {
                        final TreeNode node = (TreeNode)event.item.getData();
                        if (node != null && node.property != null) {
                            final Object propertyValue = getPropertyValue(node);
                            if (propertyValue instanceof Boolean) {
                                GC gc = event.gc;
                                final Tree tree = getTree();
                                int columnWidth = tree.getColumn(1).getWidth();
                                int columnHeight = getTree().getItemHeight();
                                Image image = node.isEditable() ?
                                    ((Boolean)propertyValue ? ImageUtils.getImageCheckboxEnabledOn() : ImageUtils.getImageCheckboxEnabledOff()) :
                                    ((Boolean)propertyValue ? ImageUtils.getImageCheckboxDisabledOn() : ImageUtils.getImageCheckboxDisabledOff());
                                final Rectangle imageBounds = image.getBounds();
                                gc.drawImage(image, event.x + 4, event.y + (columnHeight - imageBounds.height) / 2);
                                event.doit = false;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}
