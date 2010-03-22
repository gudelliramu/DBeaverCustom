package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.runtime.ConnectJob;
import org.jkiss.dbeaver.runtime.DisconnectJob;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionAuthDialog;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DataSourceDescriptor
 */
public class DataSourceDescriptor implements DBSDataSourceContainer, IAdaptable, IActionFilter
{
    static Log log = LogFactory.getLog(DataSourceDescriptor.class);

    private DriverDescriptor driver;
    private DBPConnectionInfo connectionInfo;

    private String name;
    private String description;
    private boolean savePassword;
    private boolean showSystemObjects;
    private Date createDate;
    private Date updateDate;
    private Date loginDate;
    private DataSourcePreferenceStore preferenceStore;

    private DBPDataSource dataSource;

    private List<DBPDataSourceUser> users = new ArrayList<DBPDataSourceUser>();

    public DataSourceDescriptor(DriverDescriptor driver,
        DBPConnectionInfo connectionInfo)
    {
        this.driver = driver;
        this.connectionInfo = connectionInfo;
        this.createDate = new Date();
        this.preferenceStore = new DataSourcePreferenceStore(this);
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public void setConnectionInfo(DBPConnectionInfo connectionInfo)
    {
        this.connectionInfo = connectionInfo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isSavePassword()
    {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword)
    {
        this.savePassword = savePassword;
    }

    public boolean isShowSystemObjects()
    {
        return showSystemObjects;
    }

    public void setShowSystemObjects(boolean showSystemObjects)
    {
        this.showSystemObjects = showSystemObjects;
    }

    public DBSObject getParentObject()
    {
        return null;
    }

    public boolean refreshObject()
        throws DBException
    {
        if (this.isConnected()) {
            this.reconnect(this);
        }
        return true;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    public Date getUpdateDate()
    {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate)
    {
        this.updateDate = updateDate;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public DBeaverCore getViewCallback()
    {
        return driver.getProviderDescriptor().getRegistry().getCore();
    }

    public boolean isConnected()
    {
        return dataSource != null;
    }

    public void connect(final Object source)
        throws DBException
    {
        if (this.isConnected()) {
            return;
        }
        if (!CommonUtils.isEmpty(Job.getJobManager().find(this))) {
            // Already connecting/disconnecting - jsut return
            return;
        }
        final String oldName = this.getConnectionInfo().getUserName();
        final String oldPassword = this.getConnectionInfo().getUserPassword();
        if (!this.isSavePassword()) {
            // Ask for password
            if (!askForPassword()) {
                throw new DBException("Authentification canceled");
            }
        }
/*
        try {
            this.getDriver().getProviderDescriptor().getRegistry().getCore().run(
                true,
                true,
                new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        monitor.beginTask("Open Datasource ...", 2);
                        try {
                            monitor.subTask("Connecting to Remote Database");
                            dataSource = driver.getDataSourceProvider().openDataSource(DataSourceDescriptor.this);
                            monitor.worked(1);
                            monitor.subTask("Initializing Datasource");
                            dataSource.initialize();
                            monitor.done();
                        }
                        catch (Exception ex) {
                            throw new InvocationTargetException(ex);
                        }
                    }
                }
            );
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                ex = ((InvocationTargetException)ex).getTargetException();
            }
            throw new DBException("Error opening database connection: " + ex.getMessage(), ex);
        }
*/
        ConnectJob connectJob = new ConnectJob(this);
        connectJob.addJobChangeListener(new JobChangeAdapter() {
            public void done(IJobChangeEvent event)
            {
                if (event.getResult().isOK()) {
                    DataSourceDescriptor.this.dataSource = ((ConnectJob)event.getJob()).getDataSource();
                    if (!isSavePassword()) {
                        // Rest password back to null
                        getConnectionInfo().setUserName(oldName);
                        getConnectionInfo().setUserPassword(oldPassword);
                    }
                    getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                        DataSourceEvent.Action.CONNECT,
                        DataSourceDescriptor.this,
                        source);
                } else {
                    getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                        DataSourceEvent.Action.CONNECT_FAIL,
                        DataSourceDescriptor.this,
                        source);
                }
            }
        });
        connectJob.schedule();
    }

    public void disconnect(final Object source)
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return;
        }
        if (!CommonUtils.isEmpty(Job.getJobManager().find(this))) {
            // Already connecting/disconnecting - just return
            return;
        }

/*
        if (!users.isEmpty()) {
            log.info("Can't close datasource connection: there are " + users.size() + " active user(s)");
            return;
        }
*/
        try {
            DBeaverCore.getInstance().run(true, true, new IRunnableWithProgress()
            {
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        Job.getJobManager().join(dataSource, monitor);
                    } catch (Exception e) {
                        log.error(e);
                        return;
                    }

                    DisconnectJob disconnectJob = new DisconnectJob(dataSource);
                    disconnectJob.addJobChangeListener(new JobChangeAdapter() {
                        public void done(IJobChangeEvent event)
                        {
                            dataSource = null;
                            getViewCallback().getDataSourceRegistry().fireDataSourceEvent(
                                DataSourceEvent.Action.DISCONNECT,
                                DataSourceDescriptor.this,
                                source);
                        }
                    });
                    disconnectJob.schedule();
                }
            });
        } catch (Exception e) {
            // do nothing
            log.error(e);
        }
    }

    public void invalidate()
        throws DBException
    {
        if (dataSource == null) {
            log.error("Datasource is not connected");
            return;
        }
        dataSource.checkConnection();
    }

    public void acquire(DBPDataSourceUser user)
    {
        users.add(user);
    }

    public void release(DBPDataSourceUser user)
    {
        users.remove(user);
    }

    public AbstractPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public void reconnect(Object source)
        throws DBException
    {
        this.disconnect(source);
        this.connect(source);
    }

    public void resetPassword()
    {
        if (connectionInfo != null) {
            connectionInfo.setUserPassword(null);
        }
    }

    public Object getAdapter(Class adapter)
    {
        if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return this;
        } else if (adapter == IPropertySource.class) {
            DBPDataSourceInfo info = null;
            if (this.isConnected()) {
                try {
                    info = this.getDataSource().getInfo();
                } catch (DBException e) {
                    log.warn(e);
                }
            }
            StringBuilder addr = new StringBuilder();
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                addr.append(connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                addr.append(':').append(connectionInfo.getHostPort());
            }

            PropertyCollector props = new PropertyCollector(this);
            props.addProperty("driverType", "Driver Type", driver.getName());
            if (info != null) {
                //props.addProperty("driverName", "Driver Name", info.getDriverName() + " " + info.getDriverVersion());
            }
            props.addProperty("address", "Address", addr.toString());
            props.addProperty("database", "Database Name", connectionInfo.getDatabaseName());
            if (info != null) {
                //props.addProperty("databaseType", "Database Type", info.getDatabaseProductName() + " " + info.getDatabaseProductVersion());
            }
            props.addProperty("url", "URL", connectionInfo.getJdbcURL());
            return props;
        }/* else if (adapter == IWorkbenchAdapter.class) {
            return new IWorkbenchAdapter() {

                public Object[] getChildren(Object o)
                {
                    return null;
                }

                public ImageDescriptor getImageDescriptor(Object object)
                {
                    return ImageDescriptor.createFromImage(driver.getIcon());
                }

                public String getLabel(Object o)
                {
                    return "DataSource " + getName();
                }

                public Object getParent(Object o)
                {
                    return null;
                }
            };
        }*/
        return null;
    }

    public boolean askForPassword()
    {
        ConnectionAuthDialog auth = new ConnectionAuthDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            this);
        int result = auth.open();
        if (result == IDialogConstants.OK_ID) {
            if (isSavePassword()) {
                // Update connection properties
                getDriver().getProviderDescriptor().getRegistry().updateDataSource(this);
            }
            return true;
        }
        return false;
    }

    public boolean testAttribute(Object target, String name, String value)
    {
        if (name.equals("connected")) {
            return String.valueOf(this.isConnected()).equals(value);
        } else if (name.equals("savePassword")) {
            return String.valueOf(this.isSavePassword()).equals(value);
        }
        return false;
    }

}
