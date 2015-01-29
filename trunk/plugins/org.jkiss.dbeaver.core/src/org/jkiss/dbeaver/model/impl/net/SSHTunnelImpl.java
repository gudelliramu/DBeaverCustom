/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.net;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * SSH tunnel
 */
public class SSHTunnelImpl implements DBWTunnel {

    private static final int CONNECT_TIMEOUT = 10000;
    public static final String LOCALHOST_NAME = "127.0.0.1";
    private static transient JSch jsch;
    private transient Session session;

    @Override
    public DBPConnectionInfo initializeTunnel(DBRProgressMonitor monitor, DBWHandlerConfiguration configuration, DBPConnectionInfo connectionInfo)
        throws DBException, IOException
    {
        String dbPortString = connectionInfo.getHostPort();
        if (CommonUtils.isEmpty(dbPortString)) {
            dbPortString = configuration.getDriver().getDefaultPort();
            if (CommonUtils.isEmpty(dbPortString)) {
                throw new DBException("Database port not specified and no default port number for driver '" + configuration.getDriver().getName() + "'");
            }
        }
        String dbHost = connectionInfo.getHostName();

        Map<String,String> properties = configuration.getProperties();
        String sshAuthType = properties.get(SSHConstants.PROP_AUTH_TYPE);
        String sshHost = properties.get(SSHConstants.PROP_HOST);
        String sshPort = properties.get(SSHConstants.PROP_PORT);
        String sshUser = configuration.getUserName();
        String aliveInterval = properties.get(SSHConstants.PROP_ALIVE_INTERVAL);
        //String aliveCount = properties.get(SSHConstants.PROP_ALIVE_COUNT);
        if (CommonUtils.isEmpty(sshHost)) {
            throw new DBException("SSH host not specified");
        }
        if (CommonUtils.isEmpty(sshPort)) {
            throw new DBException("SSH port not specified");
        }
        if (CommonUtils.isEmpty(sshUser)) {
            throw new DBException("SSH user not specified");
        }
        int sshPortNum;
        try {
            sshPortNum = Integer.parseInt(sshPort);
        }
        catch (NumberFormatException e) {
            throw new DBException("Invalid SSH port: " + sshPort);
        }
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        if (sshAuthType != null) {
            authType = SSHConstants.AuthType.valueOf(sshAuthType); 
        }
        File privKeyFile = null;
        String privKeyPath = properties.get(SSHConstants.PROP_KEY_PATH);
        if (authType == SSHConstants.AuthType.PUBLIC_KEY) {
            if (CommonUtils.isEmpty(privKeyPath)) {
                throw new DBException("Private key path is empty");
            }
            privKeyFile = new File(privKeyPath);
            if (!privKeyFile.exists()) {
                throw new DBException("Private key file '" + privKeyFile.getAbsolutePath() + "' doesn't exist");
            }
        }

        monitor.subTask("Initiating tunnel at '" + sshHost + "'");
        UserInfo ui = new UIUserInfo(configuration);
        int dbPort;
        try {
            dbPort = Integer.parseInt(dbPortString);
        } catch (NumberFormatException e) {
            throw new DBException("Bad database port number: " + dbPortString);
        }
        int localPort = findFreePort();
        try {
            if (jsch == null) {
                jsch = new JSch();
            }
            if (privKeyFile != null) {
                jsch.addIdentity(privKeyFile.getAbsolutePath());
            }

            session = jsch.getSession(sshUser, sshHost, sshPortNum);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications",
                    privKeyFile != null ? "publickey" : "password");
            session.setConfig("ConnectTimeout", String.valueOf(CONNECT_TIMEOUT));
            session.setUserInfo(ui);
            session.connect(CONNECT_TIMEOUT);
            try {
                session.setPortForwardingL(localPort, dbHost, dbPort);
                if (!CommonUtils.isEmpty(aliveInterval)) {
                    session.setServerAliveInterval(Integer.parseInt(aliveInterval));
                }
            } catch (JSchException e) {
                closeTunnel(monitor, connectionInfo);
                throw e;
            }
        } catch (JSchException e) {
            throw new DBException("Cannot establish tunnel", e);
        }
        connectionInfo = new DBPConnectionInfo(connectionInfo);
        String newPortValue = String.valueOf(localPort);
        // Replace database host/port and URL - let's use localhost
        connectionInfo.setHostName(LOCALHOST_NAME);
        connectionInfo.setHostPort(newPortValue);
        String newURL = configuration.getDriver().getDataSourceProvider().getConnectionURL(
            configuration.getDriver(),
            connectionInfo);
        connectionInfo.setUrl(newURL);
        return connectionInfo;
    }

    private int findFreePort()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        int minPort = store.getInt(DBeaverPreferences.NET_TUNNEL_PORT_MIN);
        int maxPort = store.getInt(DBeaverPreferences.NET_TUNNEL_PORT_MAX);
        int portRange = Math.abs(maxPort - minPort);
        while (true) {
            int portNum = minPort + SecurityUtils.getRandom().nextInt(portRange);
            try {
                ServerSocket socket = new ServerSocket(portNum);
                try {
                    socket.close();
                } catch (IOException e) {
                    // just skip
                }
                return portNum;
            } catch (IOException e) {
                // Port is busy
            }
        }
    }

    @Override
    public void closeTunnel(DBRProgressMonitor monitor, DBPConnectionInfo connectionInfo) throws DBException, IOException
    {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    private class UIUserInfo implements UserInfo {
        DBWHandlerConfiguration configuration;
        private UIUserInfo(DBWHandlerConfiguration configuration)
        {
            this.configuration = configuration;
        }

        @Override
        public String getPassphrase()
        {
            return configuration.getPassword();
        }

        @Override
        public String getPassword()
        {
            return configuration.getPassword();
        }

        @Override
        public boolean promptPassword(String message)
        {
            return true;
        }

        @Override
        public boolean promptPassphrase(String message)
        {
            return true;
        }

        @Override
        public boolean promptYesNo(String message)
        {
            return false;
        }

        @Override
        public void showMessage(String message)
        {
            UIUtils.showMessageBox(null, "SSH Tunnel", message, SWT.ICON_INFORMATION);
        }
    }
}