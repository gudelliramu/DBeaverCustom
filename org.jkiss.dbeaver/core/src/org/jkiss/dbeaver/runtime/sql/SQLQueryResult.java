package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;

import java.util.List;

/**
 * SQLQueryResult
 */
public class SQLQueryResult
{
    private String query;
    private DBCResultSetMetaData metaData;
    private List<Object[]> rows;
    private Integer updateCount;
    private Throwable error;
    private long queryTime;

    public SQLQueryResult(String query)
    {
        this.query = query;
    }

    public String getQuery()
    {
        return query;
    }

    public DBCResultSetMetaData getMetaData()
    {
        return metaData;
    }

    public List<Object[]> getRows()
    {
        return rows;
    }

    public void setResultSet(DBCResultSetMetaData metaData, List<Object[]> rows)
    {
        this.metaData = metaData;
        this.rows = rows;
    }

    public Integer getUpdateCount()
    {
        return updateCount;
    }

    public void setUpdateCount(Integer updateCount)
    {
        this.updateCount = updateCount;
    }

    public Throwable getError()
    {
        return error;
    }

    public void setError(Throwable error)
    {
        this.error = error;
    }

    public long getQueryTime()
    {
        return queryTime;
    }

    public void setQueryTime(long queryTime)
    {
        this.queryTime = queryTime;
    }
}
