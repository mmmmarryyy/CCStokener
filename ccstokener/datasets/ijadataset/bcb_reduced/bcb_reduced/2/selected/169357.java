package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import org.gudy.azureus2.core3.ipchecker.natchecker.NatChecker;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProgressListener;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class NetworkAdminHTTPTester implements NetworkAdminProtocolTester {

    private AzureusCore core;

    private NetworkAdminProgressListener listener;

    protected NetworkAdminHTTPTester(AzureusCore _core, NetworkAdminProgressListener _listener) {
        core = _core;
        listener = _listener;
    }

    public InetAddress testOutbound(InetAddress bind_ip, int bind_port) throws NetworkAdminException {
        if (bind_ip != null || bind_port != 0) {
            throw (new NetworkAdminException("HTTP tester doesn't support local bind options"));
        }
        try {
            return (VersionCheckClient.getSingleton().getExternalIpAddressHTTP(false));
        } catch (Throwable e) {
            try {
                URL url = new URL("http://www.google.com/");
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.connect();
                return (null);
            } catch (Throwable f) {
                throw (new NetworkAdminException("Outbound test failed", e));
            }
        }
    }

    public InetAddress testInbound(InetAddress bind_ip, int local_port) throws NetworkAdminException {
        NatChecker checker = new NatChecker(core, bind_ip, local_port, true);
        if (checker.getResult() == NatChecker.NAT_OK) {
            return (checker.getExternalAddress());
        } else {
            throw (new NetworkAdminException("NAT check failed: " + checker.getAdditionalInfo()));
        }
    }
}
