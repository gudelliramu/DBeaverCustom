/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo {

    private QMMSessionInfo session;
    private long startTime;
    private long endTime;
    private boolean finished;
    private boolean commited;
    private QMMSavePointInfo savepoint;

}
