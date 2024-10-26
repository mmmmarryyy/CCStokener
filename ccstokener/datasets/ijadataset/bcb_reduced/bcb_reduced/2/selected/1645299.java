package org.xaware.ide.applet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xaware.shared.util.logging.XAwareLogger;

public class XAJMSServer extends Thread implements MessageListener {

    private static String JNDI_FACTORY = "";

    private static String JMS_FACTORY = "";

    private static String TOPIC_REQUEST = "";

    private static String url = "";

    private String user = "";

    private String password = "";

    private String initMsg = "XAware JMS Client Ready To Receive Messages.";

    public int MAX_ROWS = 1000;

    private int maxRows = 1000;

    private ArrayList savedRows = null;

    public boolean quit = false;

    private boolean wait = false;

    public boolean pause = false;

    public boolean haveRow = false;

    LogTableModel logTableModel;

    protected TextMessage msg;

    private TopicConnectionFactory subscriberFactory;

    TopicConnection subscriberConnection;

    TopicSession subscriberSession;

    Topic subscribeTopic;

    private TopicSubscriber tsubscriber;

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(XAJMSServer.class.getName());

    public static String BIZVIEW = "logViewer";

    public static String bizViewString = "xaware/XAServlet?_BIZVIEW=" + BIZVIEW;

    public static URL hostURL;

    public static String hostString = "";

    /**
     * XAJMSServer() constructor takes all the parameters required to set up a connection to JMS.
     * 
     * @params LogTableModel Table for displaying log messages
     * @params JNDI_FACTORY something like org.jnp.interfaces.NamingContextFactory
     * @params JMS_FACTORY something like TopicConnectionFactory
     * @params TOPIC_REQUEST something like topic/XAwareLogging
     * @params url jnp://localhost:1099
     * @params userid
     * @params password
     */
    public XAJMSServer(final LogTableModel logTM, final String host) {
        logTableModel = logTM;
        hostString = new String(host);
    }

    public synchronized void onMessage(final Message msg) {
        try {
            final String[] row = new String[5];
            if (msg instanceof MapMessage) {
                final MapMessage m = (MapMessage) msg;
                final Enumeration bdParams = m.getMapNames();
                while (bdParams.hasMoreElements()) {
                    final String sName = (String) bdParams.nextElement();
                    final String sValue = m.getString(sName);
                }
            }
            if (msg instanceof TextMessage) {
                parseMessageToRow(((TextMessage) msg).getText(), row);
                if (pause == true) {
                    if (savedRows == null) {
                        savedRows = new ArrayList(MAX_ROWS);
                    }
                    if (savedRows.size() >= MAX_ROWS) {
                        savedRows.remove(0);
                    }
                    savedRows.add(row);
                    wait = true;
                } else {
                    while (wait) {
                        try {
                            Thread.sleep(500);
                        } catch (final InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                    if (maxRows != MAX_ROWS) {
                        maxRows = MAX_ROWS;
                        savedRows = new ArrayList(MAX_ROWS);
                    }
                    adjustTableRows();
                    logTableModel.addRow(row);
                }
            } else {
                if (pause == false) {
                    row[0] = "00:00:00";
                    row[1] = "INIT";
                    row[2] = "XAware JMS Server received an invalid message.";
                    row[3] = new String(url);
                    row[4] = "0";
                    logTableModel.addRow(row);
                }
                adjustTableRows();
            }
        } catch (final JMSException e) {
            e.printStackTrace();
        }
    }

    public void purgeBuffer() {
        pause = false;
        if (savedRows != null) {
            final Iterator rowItr = savedRows.iterator();
            while (rowItr.hasNext()) {
                adjustTableRows();
                final String[] savedRow = (String[]) rowItr.next();
                logTableModel.addRow(savedRow);
            }
            savedRows.clear();
        }
        if (maxRows != MAX_ROWS) {
            maxRows = MAX_ROWS;
            savedRows = new ArrayList(MAX_ROWS);
        }
        wait = false;
    }

    private void adjustTableRows() {
        final int curRowCount = logTableModel.getRowCount();
        if (curRowCount + 1 > maxRows) {
            for (int j = 0; j < curRowCount - maxRows + 1; j++) {
                try {
                    logTableModel.removeRow(0);
                } catch (final Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Creates a String [] with all the necessary objects for adding a row in our LoggingTabelModel.
     * 
     * @param String
     *            of valid XML conaining the log information
     * @exception Exception
     *                if problem occurred with XML Parsing
     */
    private void parseMessageToRow(final String text, final String[] pRow) {
        try {
            final SAXBuilder sb = new SAXBuilder();
            final StringReader sr = new StringReader(text);
            final Document doc = sb.build(sr);
            final Element root = doc.getRootElement();
            pRow[0] = root.getChild("datetime").getText().trim();
            pRow[1] = root.getChild("level").getText().trim();
            pRow[2] = root.getChild("message").getText().trim();
            pRow[3] = root.getChild("host").getText().trim();
            pRow[4] = root.getChild("thread-id").getText().trim();
        } catch (final Exception es) {
            es.printStackTrace();
        }
    }

    /**
     * Creates a String [] with all the necessary objects for adding a row in our LoggingTabelModel.
     * 
     * @param String
     *            of valid XML conaining the log information
     * @exception Exception
     *                if problem occurred with XML Parsing
     */
    private void parseMessageToParams() {
        try {
            final Element root = postMessage();
            lf.debug("root Element = " + root);
            final Element JMSNode = root.getChild("Logger").getChild("APPENDERS").getChild("LOG_JMS");
            JNDI_FACTORY = JMSNode.getChild("CONNECTIONFACTORY").getText().trim();
            JMS_FACTORY = JMSNode.getChild("TOPICCONNECTIONFACTORY").getText().trim();
            TOPIC_REQUEST = JMSNode.getChild("TOPIC").getText().trim();
            url = JMSNode.getChild("URL").getText().trim();
            user = JMSNode.getChild("USERID").getText().trim();
            password = JMSNode.getChild("PASSWORD").getText().trim();
        } catch (final Exception es) {
            es.printStackTrace();
        }
    }

    public static Element postMessage() throws Exception {
        final URL theUrl = getHostURL();
        lf.debug("url = " + theUrl.toExternalForm());
        final HttpURLConnection urlConn = (HttpURLConnection) (theUrl).openConnection();
        urlConn.setRequestMethod("POST");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        final BufferedOutputStream bos = new BufferedOutputStream(urlConn.getOutputStream());
        final InputStream bis = urlConn.getInputStream();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int count = 0;
        while ((count = bis.read(buffer)) > -1) {
            baos.write(buffer, 0, count);
        }
        final SAXBuilder sb = new SAXBuilder();
        lf.debug("Received XML response from server: " + baos.toString());
        return sb.build(new StringReader(baos.toString())).getRootElement();
    }

    private static URL getHostURL() throws Exception {
        String urlString = hostString;
        lf.debug("hostString = " + hostString);
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        urlString += bizViewString;
        hostURL = new URL(urlString);
        return hostURL;
    }

    /**
     * Initializes and subscribes to JMS server using variables initialized in constructor.
     */
    public void init() {
        try {
            parseMessageToParams();
            final InitialContext ctx = getInitialContext();
            lf.debug(ctx.lookup(JMS_FACTORY).getClass().toString());
            subscriberFactory = (TopicConnectionFactory) ctx.lookup(JMS_FACTORY);
            if (user != null && user.length() > 0) {
                subscriberConnection = subscriberFactory.createTopicConnection(user, password);
            } else {
                subscriberConnection = subscriberFactory.createTopicConnection();
            }
            subscriberSession = subscriberConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
            subscribeTopic = (Topic) ctx.lookup(TOPIC_REQUEST);
            tsubscriber = subscriberSession.createSubscriber(subscribeTopic);
            tsubscriber.setMessageListener(this);
            subscriberConnection.start();
        } catch (final Exception e) {
            initMsg = "Exception occured making connection: " + e;
            e.printStackTrace();
        }
    }

    /**
     * Closes JMS objects.
     * 
     * @exception JMSException
     *                if JMS fails to close objects due to internal error
     */
    public void close() throws JMSException {
        if (tsubscriber != null) {
            tsubscriber.close();
        }
        if (subscriberSession != null) {
            subscriberSession.close();
        }
        if (subscriberConnection != null) {
            subscriberConnection.close();
        }
    }

    /**
     * Goes into a wait() for messages after printing out initial connection message.
     */
    @Override
    public void run() {
        final String[] initRow = new String[5];
        initRow[0] = "00:00:00";
        initRow[1] = "INIT";
        initRow[2] = initMsg;
        initRow[3] = new String(url);
        initRow[4] = "0";
        logTableModel.addRow(initRow);
        try {
            synchronized (this) {
                while (!quit) {
                    wait();
                }
            }
        } catch (final InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Builds a Hashtable for using in getting a Context and returns the InitialContext
     */
    private InitialContext getInitialContext() throws NamingException {
        final Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, url);
        if (user != null && password != null) {
            if (user.length() > 0 && password.length() > 0) {
                env.put(Context.SECURITY_PRINCIPAL, user);
                env.put(Context.SECURITY_CREDENTIALS, password);
            }
        }
        return new InitialContext(env);
    }
}
