package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Stored code interface
 */
public interface OracleSourceObject extends DBSObject {

    OracleSchema getSourceOwner();

    String getSourceType();

}
