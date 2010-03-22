package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

public class MySQLDataSourceProvider implements DBPDataSourceProvider {

    public MySQLDataSourceProvider()
    {
    }

    public void close()
    {

    }

    public void init(DBPApplication application)
    {

    }

    public DBPDataSource openDataSource(
        DBSDataSourceContainer container)
        throws DBException
    {
        return new MySQLDataSource(container);
    }

}
