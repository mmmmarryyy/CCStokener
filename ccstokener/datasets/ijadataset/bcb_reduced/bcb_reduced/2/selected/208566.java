package org.exist.xqj.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataFactory;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQMetaData;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQSequence;
import javax.xml.xquery.XQSequenceType;
import javax.xml.xquery.XQStaticContext;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.log4j.Logger;
import org.exist.xqj.Constants;
import org.exist.xqj.EXistXQDataFactoryImpl;
import org.exist.xqj.EXistXQMetadata;
import org.exist.xqj.EXistXQStaticContext;
import org.exist.xqj.XQConnectionEventHandler;
import org.w3c.dom.Node;

/**
 * The XQRemoteConnection models a connection to a remote eXist database. From 
 * the XQJ driver point of view, the connection object is a client session with associated
 * information like the username, the currently active expressions and results sequences.
 * Before closing, the connection object frees all his resources.
 * 
 * @author Cherif YAYA (allad)
 * @author Adam Retter <adam@exist-db.org>
 */
public class XQRemoteConnection implements XQConnection, XQConnectionEventHandler {

    private static final Logger LOG = Logger.getLogger(XQRemoteConnection.class);

    private HttpClient httpClient = null;

    private String serverName = null;

    private int serverPort = -1;

    private String collectionPath = null;

    private String username = null;

    private String password = null;

    private XQConnectionEventHandler conEventHandler = null;

    private String remoteURL = null;

    private boolean connectionClosed = false;

    private boolean autoCommit = true;

    private XQStaticContext staticContext = new EXistXQStaticContext();

    private XQDataFactory dataFactory = new EXistXQDataFactoryImpl(this);

    private List expressions = new ArrayList();

    public XQRemoteConnection(HttpClient httpClient, String serverName, int serverPort, String collectionPath, String username, String password, XQConnectionEventHandler conEventHandler) throws XQException {
        if (httpClient == null) throw new XQException("A HTTPClient is required for a remote XQJ connection");
        if (serverName == null) throw new XQException("A server name is required for a remote XQJ connection");
        if (collectionPath == null) throw new XQException("A collection path is required for a remote XQJ connection");
        if (!collectionPath.startsWith(Constants.PATH_SEPARATOR)) throw new XQException("Collection path must start with a '" + Constants.PATH_SEPARATOR + "'");
        if (username == null) throw new XQException("A username is required for a remote XQJ connection");
        if (password == null) throw new XQException("A password is required for a remote XQJ connection");
        if (conEventHandler == null) throw new XQException("A connection event handler is required for a remote XQJ connection");
        this.httpClient = httpClient;
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.collectionPath = collectionPath;
        this.username = username;
        this.password = password;
        this.conEventHandler = conEventHandler;
        this.remoteURL = Constants.XQJ_UNDERLYING_PROTOCOL + "://" + serverName + ":" + serverPort + collectionPath;
        checkResourceAvailable();
    }

    private void checkResourceAvailable() throws XQException {
        HttpUriRequest head = new HttpHead(remoteURL);
        try {
            HttpResponse response = httpClient.execute(head);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) throw new XQException("Could not connect to the remote resource, response code: " + response.getStatusLine().getStatusCode() + " reason: " + response.getStatusLine().getReasonPhrase());
        } catch (ClientProtocolException cpe) {
            throw new XQException(cpe.getMessage());
        } catch (IOException ioe) {
            throw new XQException(ioe.getMessage());
        }
    }

    public void close() throws XQException {
        conEventHandler.closeConnection(this);
    }

    public void setAutoCommit(boolean autoCommit) throws XQException {
        throwIfClosed();
        if (!autoCommit) throw new XQException("eXist's XQJ only supports auto-commit at present");
        this.autoCommit = autoCommit;
    }

    public boolean getAutoCommit() throws XQException {
        throwIfClosed();
        return autoCommit;
    }

    public void commit() throws XQException {
        throwIfClosed();
        if (autoCommit) throw new XQException("Cannot commit. Currently operating in auto-commit mode");
    }

    public XQExpression createExpression() throws XQException {
        return createExpression(this.staticContext);
    }

    public XQExpression createExpression(XQStaticContext properties) throws XQException {
        throwIfClosed();
        if (properties == null) throw new XQException("The static context may not be null");
        EXistXQStaticContext newContext = null;
        try {
            newContext = (EXistXQStaticContext) ((EXistXQStaticContext) properties).clone();
        } catch (CloneNotSupportedException cnse) {
            throw new XQException("Could not copy the provided static context: " + cnse.getMessage());
        }
        XQRemoteExpression expr = new XQRemoteExpression(this, newContext);
        expressions.add(expr);
        return expr;
    }

    private void throwIfClosed() throws XQException {
        if (connectionClosed) throw new XQException("Cannot process operation on a closed connection.");
    }

    public void closeConnection(XQConnectionEventHandler handler) throws XQException {
        if (connectionClosed) return;
        XQException xqException = null;
        try {
            for (Iterator itExpr = expressions.iterator(); itExpr.hasNext(); ) {
                XQExpression expr = (XQExpression) itExpr.next();
                if (!expr.isClosed()) {
                    try {
                        expr.close();
                    } catch (XQException xqe) {
                        if (xqException == null) xqException = xqe;
                    }
                }
            }
            if (xqException != null) throw xqException;
        } finally {
            connectionClosed = true;
        }
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getCollectionPath() {
        return collectionPath;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRemoteURL() {
        return remoteURL;
    }

    private void isConnectionClosed() throws XQException {
        if (connectionClosed) throw new XQException("The connection is no longer valid.");
    }

    public XQMetaData getMetaData() throws XQException {
        return new EXistXQMetadata(this, username);
    }

    public boolean isClosed() {
        return connectionClosed;
    }

    public void rollback() throws XQException {
    }

    public XQItemType createAtomicType(int baseType) throws XQException {
        isConnectionClosed();
        return dataFactory.createAtomicType(baseType);
    }

    public XQItem createItem(XQItem item) throws XQException {
        isConnectionClosed();
        return dataFactory.createItem(item);
    }

    public XQItem createItemFromAtomicValue(String value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromAtomicValue(value, type);
    }

    public XQItem createItemFromBoolean(boolean value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromBoolean(value, type);
    }

    public XQItem createItemFromByte(byte value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromByte(value, type);
    }

    public XQItem createItemFromDouble(double value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromDouble(value, type);
    }

    public XQItem createItemFromFloat(float value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromFloat(value, type);
    }

    public XQItem createItemFromInt(int value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromInt(value, type);
    }

    public XQItem createItemFromLong(long value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromLong(value, type);
    }

    public XQItem createItemFromNode(Node value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromNode(value, type);
    }

    public XQItem createItemFromObject(Object value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromObject(value, type);
    }

    public XQItem createItemFromShort(short value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromShort(value, type);
    }

    public XQSequence createSequence(Iterator i) throws XQException {
        isConnectionClosed();
        return dataFactory.createSequence(i);
    }

    public XQSequence createSequence(XQSequence s) throws XQException {
        isConnectionClosed();
        return dataFactory.createSequence(s);
    }

    public XQSequenceType createSequenceType(XQItemType item, int occurrence) throws XQException {
        isConnectionClosed();
        return dataFactory.createSequenceType(item, occurrence);
    }

    public XQStaticContext getStaticContext() throws XQException {
        return staticContext;
    }

    public XQPreparedExpression prepareExpression(InputStream xquery, XQStaticContext ctxt) throws XQException {
        isConnectionClosed();
        InputStreamReader reader = new InputStreamReader(xquery);
        return prepareExpression(reader, ctxt);
    }

    public XQPreparedExpression prepareExpression(Reader reader, XQStaticContext ctxt) throws XQException {
        isConnectionClosed();
        try {
            StringBuilder str = new StringBuilder();
            char[] buf = new char[512];
            while (reader.read(buf, 0, 512) != -1) str.append(buf);
            return prepareExpression(str.toString(), ctxt);
        } catch (IOException ie) {
            LOG.error(ie);
            return null;
        }
    }

    public XQPreparedExpression prepareExpression(String xquery, XQStaticContext ctxt) throws XQException {
        XQRemotePreparedExpression expr = new XQRemotePreparedExpression(xquery, this, ctxt);
        expressions.add(expr);
        return expr;
    }

    public void setStaticContext(XQStaticContext properties) throws XQException {
        isConnectionClosed();
        if (properties == null) throw new XQException("Static Context properties cannot be null");
        staticContext = properties;
    }

    public XQItemType createAtomicType(int basetype, QName typename, URI schemaURI) throws XQException {
        isConnectionClosed();
        return dataFactory.createAtomicType(basetype, typename, schemaURI);
    }

    public XQItemType createAttributeType(QName nodename, int basetype) throws XQException {
        isConnectionClosed();
        return dataFactory.createAttributeType(nodename, basetype);
    }

    public XQItemType createAttributeType(QName nodename, int basetype, QName typename, URI schemaURI) throws XQException {
        isConnectionClosed();
        return dataFactory.createAttributeType(nodename, basetype, typename, schemaURI);
    }

    public XQItemType createCommentType() throws XQException {
        isConnectionClosed();
        return dataFactory.createCommentType();
    }

    public XQItemType createDocumentElementType(XQItemType elementType) throws XQException {
        isConnectionClosed();
        return dataFactory.createDocumentElementType(elementType);
    }

    public XQItemType createDocumentSchemaElementType(XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createDocumentSchemaElementType(type);
    }

    public XQItemType createDocumentType() throws XQException {
        isConnectionClosed();
        return dataFactory.createDocumentType();
    }

    public XQItemType createElementType(QName nodename, int basetype) throws XQException {
        isConnectionClosed();
        return dataFactory.createElementType(nodename, basetype);
    }

    public XQItemType createElementType(QName nodename, int basetype, QName typename, URI schemaURI, boolean allowNill) throws XQException {
        isConnectionClosed();
        return dataFactory.createElementType(nodename, basetype, typename, schemaURI, allowNill);
    }

    public XQItem createItemFromDocument(InputStream value, String baseURI, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromDocument(value, baseURI, type);
    }

    public XQItem createItemFromDocument(Reader value, String baseURI, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromDocument(value, baseURI, type);
    }

    public XQItemType createItemType() throws XQException {
        isConnectionClosed();
        return dataFactory.createItemType();
    }

    public XQItemType createNodeType() throws XQException {
        isConnectionClosed();
        return dataFactory.createNodeType();
    }

    public XQItemType createProcessingInstructionType(String piTarget) throws XQException {
        isConnectionClosed();
        return dataFactory.createProcessingInstructionType(piTarget);
    }

    public XQItemType createSchemaAttributeType(QName nodename, int basetype, URI schemaURI) throws XQException {
        isConnectionClosed();
        return dataFactory.createSchemaAttributeType(nodename, basetype, schemaURI);
    }

    public XQItemType createSchemaElementType(QName nodename, int basetype, URI schemaURI) throws XQException {
        isConnectionClosed();
        return dataFactory.createSchemaElementType(nodename, basetype, schemaURI);
    }

    public XQItemType createTextType() throws XQException {
        isConnectionClosed();
        return dataFactory.createTextType();
    }

    public XQItem createItemFromDocument(Source value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromDocument(value, type);
    }

    public XQItem createItemFromDocument(String value, String baseURI, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromDocument(value, baseURI, type);
    }

    public XQItem createItemFromDocument(XMLStreamReader value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromDocument(value, type);
    }

    public XQItem createItemFromString(String value, XQItemType type) throws XQException {
        isConnectionClosed();
        return dataFactory.createItemFromString(value, type);
    }

    public XQPreparedExpression prepareExpression(InputStream xquery) throws XQException {
        isConnectionClosed();
        InputStreamReader reader = new InputStreamReader(xquery);
        return prepareExpression(reader);
    }

    public XQPreparedExpression prepareExpression(Reader reader) throws XQException {
        isConnectionClosed();
        try {
            StringBuilder str = new StringBuilder();
            char[] buf = new char[512];
            while (reader.read(buf, 0, 512) != -1) str.append(buf);
            return prepareExpression(str.toString());
        } catch (IOException ie) {
            LOG.error(ie);
            throw new XQException(ie.toString());
        }
    }

    public XQPreparedExpression prepareExpression(String xquery) throws XQException {
        XQRemotePreparedExpression expr = new XQRemotePreparedExpression(xquery, this, this.staticContext);
        expressions.add(expr);
        return expr;
    }
}
