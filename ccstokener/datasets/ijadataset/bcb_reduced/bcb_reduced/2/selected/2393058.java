package org.openacs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import javax.ejb.*;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.openacs.message.*;
import org.openacs.utils.Ejb;
import org.openacs.utils.Jms;

/**
 * This is the bean class for the CPEBean enterprise bean.
 * Created 2008.1.26 17.41.59
 * @author Administrator
 */
public class CPEBean implements SessionBean, CPELocalBusiness {

    private SessionContext context;

    /**
     * @see javax.ejb.SessionBean#setSessionContext(javax.ejb.SessionContext)
     */
    public void setSessionContext(SessionContext aContext) {
        context = aContext;
    }

    /**
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {
    }

    /**
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {
    }

    /**
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
    }

    private long timeoutReceive = 30;

    private Jms jms;

    public void ejbCreate() throws NamingException, JMSException {
        jms = new Jms();
    }

    public void RequestCPEConnection(HostsLocal host) {
        requestCpeConnection(host.getUrl());
    }

    private void requestCpeConnection(String cpeurl) {
        try {
            URL url = new URL(cpeurl);
            URLConnection httpconn = url.openConnection();
            httpconn.setReadTimeout(5000);
            httpconn.getContent();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(cpeurl + " is malformed.");
        } catch (UnknownServiceException e) {
        } catch (IOException ex) {
            throw new RuntimeException(cpeurl + " problem." + ex.getMessage() + " " + ex.getClass().getName());
        }
    }

    private HostsLocal findDevice(String oui, String hclass, String sn) {
        try {
            HardwareModelLocal hw = Ejb.lookupHardwareModelBean().findByOuiAndClass(oui, hclass);
            HostsLocalHome lhHosts = Ejb.lookupHostsBean();
            return lhHosts.findByHwidAndSn((Integer) hw.getId(), sn);
        } catch (FinderException ex) {
            throw new RuntimeException("CPE not found in DB.");
        }
    }

    public Message WaitJmsReply(String filter, long timeoutReceive) throws JMSException {
        Message msg = (Message) jms.Receive(filter, timeoutReceive);
        System.out.println("RCV1:  req=" + ((msg != null) ? msg.name : null));
        return msg;
    }

    private Message Call_(HostsLocal host, Message call) {
        return Call(host, call, true, 20);
    }

    public Message Call(HostsLocal host, Message call, long timeout) {
        return Call(host, call, false, timeout);
    }

    private Message Call(HostsLocal host, Message call, boolean requestConnection, long timeout) {
        try {
            jms.sendCallMessage(call, call.getId(), host);
            if (requestConnection) {
                RequestCPEConnection(host);
            }
            if (timeout <= 0 || timeout > 300) timeout = timeoutReceive;
            return WaitJmsReply("JMSCorrelationID='" + call.getId() + "'", timeout * 1000);
        } catch (JMSException e) {
            throw new RuntimeException("JMSException");
        }
    }

    public Message FactoryReset(HostsLocal host) {
        return Call_(host, new FactoryReset());
    }

    public GetRPCMethodsResponse GetRPCMethods(HostsLocal host) {
        return (GetRPCMethodsResponse) Call_(host, new GetRPCMethods());
    }

    public GetParameterNamesResponse GetParameterNames(HostsLocal host, String path, boolean next) {
        return (GetParameterNamesResponse) Call_(host, new GetParameterNames(path, next));
    }

    public GetParameterValuesResponse GetParameterValues(HostsLocal host, String[] names) {
        return (GetParameterValuesResponse) Call_(host, new GetParameterValues(names));
    }

    public SetParameterValuesResponse SetParameterValues(HostsLocal host, SetParameterValues values) {
        return (SetParameterValuesResponse) Call_(host, values);
    }

    private HardwareModelLocalHome lookupHardwareModelBean() {
        try {
            Context c = new InitialContext();
            HardwareModelLocalHome rv = (HardwareModelLocalHome) c.lookup("java:comp/env/HardwareModelBean");
            return rv;
        } catch (NamingException ne) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    private HostsLocalHome lookupHostsBean() {
        try {
            Context c = new InitialContext();
            HostsLocalHome rv = (HostsLocalHome) c.lookup("java:comp/env/HostsBean");
            return rv;
        } catch (NamingException ne) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }
}
