/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC statement
 */
public interface JDBCPreparedStatement extends PreparedStatement, JDBCStatement {

    JDBCResultSet executeQuery()
        throws SQLException;

    void close();

}