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
package org.jkiss.dbeaver.ui.controls;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

/**
 * DriverTreeControl
 *
 * @author Serge Rieder
 */
public class DriverTreeControl extends TreeViewer implements ISelectionChangedListener, IDoubleClickListener {

    private Object site;
    private List<DataSourceProviderDescriptor> providers;
    private Font boldFont;

    public static class DriverCategory {
        final DataSourceProviderDescriptor provider;
        final String name;
        final List<DriverDescriptor> drivers = new ArrayList<DriverDescriptor>();

        public DriverCategory(DataSourceProviderDescriptor provider, String name)
        {
            this.provider = provider;
            this.name = name;
        }

        public DataSourceProviderDescriptor getProvider()
        {
            return provider;
        }

        public String getName()
        {
            return name;
        }

        public List<DriverDescriptor> getDrivers()
        {
            return drivers;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof DriverCategory &&
                ((DriverCategory) obj).provider == provider &&
                ((DriverCategory) obj).name.equals(name);
        }

        @Override
        public int hashCode()
        {
            return provider.hashCode() + name.hashCode();
        }
    }

    public DriverTreeControl(Composite parent)
    {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(boldFont);
            }
        });
    }

    public void initDrivers(Object site, List<DataSourceProviderDescriptor> providers)
    {
        this.site = site;
        this.providers = providers;
        if (this.providers == null) {
            this.providers = DataSourceProviderRegistry.getDefault().getDataSourceProviders();
        }

        TreeColumn nameColumn = new TreeColumn(getTree(), SWT.LEFT);
        nameColumn.setText("Name");

        TreeColumn usersColumn = new TreeColumn(getTree(), SWT.RIGHT);
        usersColumn.setText("Connections");

        //TreeColumn descColumn = new TreeColumn(getTree(), SWT.RIGHT);
        //descColumn.setText("Description");

        this.setContentProvider(new ViewContentProvider());
        this.setLabelProvider(new ViewLabelProvider());
        this.setInput(DataSourceProviderRegistry.getDefault());

        this.addSelectionChangedListener(this);
        this.addDoubleClickListener(this);
        this.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        this.expandAll();
        UIUtils.packColumns(getTree());
        this.collapseAll();
        this.expandToLevel(2);
    }

    class ViewContentProvider implements ITreeContentProvider
    {
        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public Object[] getElements(Object parent)
        {
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child)
        {
            if (child instanceof DriverDescriptor) {
                return ((DriverDescriptor) child).getProviderDescriptor();
            } else if (child instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) child).getRegistry();
            } else {
                return null;
            }
        }

        @Override
        public Object[] getChildren(Object parent)
        {
            if (parent instanceof DataSourceProviderRegistry) {
                List<Object> children = new ArrayList<Object>();
                for (DataSourceProviderDescriptor provider : providers) {
                    List<DriverDescriptor> drivers = provider.getEnabledDrivers();
                    if (drivers.isEmpty()) {
                        // skip
                    } else if (drivers.size() == 1) {
                        children.add(drivers.get(0));
                    } else {
                        children.add(provider);
                    }
                }
                return children.toArray();
            } else if (parent instanceof DataSourceProviderDescriptor) {
                final List<DriverDescriptor> drivers = ((DataSourceProviderDescriptor) parent).getEnabledDrivers();
                final Map<String, DriverCategory> categoryMap = new HashMap<String, DriverCategory>();
                final List<Object> children = new ArrayList<Object>();
                for (DriverDescriptor driver : drivers) {
                    String categoryName = driver.getCategory();
                    if (CommonUtils.isEmpty(categoryName)) {
                        children.add(driver);
                    } else {
                        categoryName = categoryName.trim().toLowerCase();
                        DriverCategory category = categoryMap.get(categoryName);
                        if (category == null) {
                            category = new DriverCategory((DataSourceProviderDescriptor) parent, driver.getCategory());
                            categoryMap.put(categoryName, category);
                            children.add(category);
                        }
                        category.drivers.add(driver);
                    }
                }
                Collections.sort(children, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2)
                    {
                        String name1 = o1 instanceof DriverDescriptor ? ((DriverDescriptor) o1).getName() : ((DriverCategory) o1).getName();
                        String name2 = o2 instanceof DriverDescriptor ? ((DriverDescriptor) o2).getName() : ((DriverCategory) o2).getName();
                        return name1.compareTo(name2);
                    }
                });
                return children.toArray();
            } else if (parent instanceof DriverCategory) {
                return ((DriverCategory) parent).drivers.toArray();
            } else {
                return new Object[0];
            }
        }

        @Override
        public boolean hasChildren(Object parent)
        {
            if (parent instanceof DataSourceProviderRegistry) {
                return !providers.isEmpty();
            } else if (parent instanceof DataSourceProviderDescriptor) {
                return !((DataSourceProviderDescriptor) parent).getEnabledDrivers().isEmpty();
            } else if (parent instanceof DriverCategory) {
                return !((DriverCategory) parent).drivers.isEmpty();
            }
            return false;
        }
    }

    class ViewLabelProvider extends CellLabelProvider
    {

        @Override
        public void update(ViewerCell cell) {
            switch (cell.getColumnIndex()) {
                case 0:
                    cell.setText(getLabel(cell.getElement()));
                    break;
                case 1:
                    final int count = getConnectionCount(cell.getElement());
                    cell.setText(count <= 0 ? "" : String.valueOf(count));
                    break;
                default:
                    cell.setText("");
                    break;
            }
            cell.setImage(getImage(cell.getElement(), cell.getColumnIndex()));
            if (cell.getElement() instanceof DriverDescriptor && !((DriverDescriptor)cell.getElement()).getUsedBy().isEmpty()) {
                //cell.setFont(boldFont);
            } else {
                cell.setFont(null);
            }
        }

        public String getLabel(Object obj)
        {
            if (obj instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) obj).getName();
            } else if (obj instanceof DriverCategory) {
                return ((DriverCategory) obj).name;
            } else if (obj instanceof DriverDescriptor) {
                return ((DriverDescriptor) obj).getName();
            } else {
                return obj.toString();
            }
        }

        public int getConnectionCount(Object obj)
        {
            if (obj instanceof DataSourceProviderDescriptor) {
                int count = 0;
                for (DriverDescriptor driver : ((DataSourceProviderDescriptor) obj).getEnabledDrivers()) {
                    count += driver.getUsedBy().size();
                }
                return count;
            } else if (obj instanceof DriverCategory) {
                int count = 0;
                for (DriverDescriptor driver : ((DriverCategory) obj).drivers) {
                    count += driver.getUsedBy().size();
                }
                return count;
            } else if (obj instanceof DriverDescriptor) {
                return ((DriverDescriptor) obj).getUsedBy().size();
            } else {
                return 0;
            }
        }

        public String getDescription(Object obj)
        {
            if (obj instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) obj).getDescription();
            } else if (obj instanceof DriverDescriptor) {
                return ((DriverDescriptor) obj).getDescription();
            } else {
                return "";
            }
        }

        public Image getImage(Object obj, int index)
        {
            if (index != 0) {
                return null;
            }
            Image defImage = DBIcon.TREE_PAGE.getImage();
			if (obj instanceof DataSourceProviderDescriptor) {
                Image icon = ((DataSourceProviderDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
			    defImage = DBIcon.TREE_FOLDER.getImage();
            } else if (obj instanceof DriverCategory) {
                Image icon = ((DriverCategory) obj).drivers.get(0).getIcon();
                if (icon != null) {
                    return icon;
                }
            } else if (obj instanceof DriverDescriptor) {
                Image icon = ((DriverDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
            }

            return defImage;
        }
    }


    @Override
    public void selectionChanged(SelectionChangedEvent event)
    {
        if (site instanceof ISelectionChangedListener) {
            ((ISelectionChangedListener)site).selectionChanged(event);
        }
    }

    @Override
    public void doubleClick(DoubleClickEvent event)
    {
        if (site instanceof IDoubleClickListener) {
            ((IDoubleClickListener)site).doubleClick(event);
        }
        ISelection selection = event.getSelection();
        if (!selection.isEmpty()) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DriverCategory || element instanceof DataSourceProviderDescriptor) {
                if (Boolean.TRUE.equals(getExpandedState(element))) {
                    super.collapseToLevel(element, 1);
                } else {
                    super.expandToLevel(element, 1);
                }
            }
        }
    }

}
