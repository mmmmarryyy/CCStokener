package org.regenstrief.xhl7;

import java.lang.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Map;
import java.util.IdentityHashMap;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXResult;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParserFactory;

/**
  An HL7 MLLP server that transforms an HL7 received.

  <p>
  Configuration file is in XML. See ConfigurationHandler.
  
  <p>
  The MLLP server is a multi-threaded thing. It's started by
  reading the configuration file and the configuration drives 
  all other actions. At the end of each server element, that
  server is started in a thread of its own (how about using
  select?) Then each sever thread spawns off handler threads,
  where a handler thread. For now we use default saturation
  behavior. It's simple minded but used to be the standard on
  UNIX and C for so many years, so it's not going to be a big
  concern now.
  
  <p>
  I am exceedingly inclined to put more rather than less functions
  into a single compilation unit. Particularly I hate to put
  little helper classes such as Exceptions or ContentHandlers
  into separate files, let alone make them public classes.

  <p>
  We have here, the MLLPServer class that has the following 
  phases and threads:

  <ol>
  <li>static main, initialize - the startup thread, creates the</li>
  <li>ConfigurationHandler - the helper object that creates
        one MLLPServer per each server element in configuration.
        This is a ContentHandlder called back by the SAX parser.</li>
  <li>MLLPServer object - one per server element, this is the 
        acceptor thread.</li>
  <li>Handler thread, each handling one connection.</li>
  </ol>
  
  <p>
  Notice, this is a Java application, not really an API. You
  can of course launch it by calling the static main() method,
  anything beyond that is at your own risk.

  @author Gunther Schadow
  @version $Id: MLLPServer.java 1357 2004-12-30 00:55:56Z gunterze $
*/
public class MLLPServer extends Thread {

    /** Main entry point. 
      @param args one argument, the URL of the configuration file.
      @throws java.lang.Excpetion if anthing goes wrong.
   */
    public static void main(String args[]) throws Exception {
        init(args[0]);
    }

    /** Initialize servers from XML configuration file. 
      
      @param url address of the configuration XML
   */
    private static void init(String url) throws Exception {
        XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        reader.setContentHandler(new ConfigurationHandler());
        InputSource isource = new InputSource((new URL(url)).openStream());
        isource.setSystemId(url);
        reader.parse(isource);
    }

    private static Map _serverNameMap = new IdentityHashMap();

    private String _id = null;

    private MLLPServer(String id) {
        if (id == null) throw new NullPointerException("id");
        id = id.intern();
        if (MLLPServer._serverNameMap.containsKey(id)) throw new IllegalArgumentException("duplicate server ids " + id);
        this._id = id;
        MLLPServer._serverNameMap.put(id, this);
    }

    private Templates _templates = null;

    private void setTemplates(Templates templates) {
        if (templates == null) throw new NullPointerException("templates");
        this._templates = templates;
    }

    private Templates _errorTemplates = null;

    private void setErrorTemplates(Templates templates) {
        if (templates == null) throw new NullPointerException("templates");
        this._errorTemplates = templates;
    }

    private Class _readerClass = null;

    private void setReaderClass(Class readerClass) {
        if (readerClass == null) throw new NullPointerException("readerClass");
        this._readerClass = readerClass;
    }

    private Class _writerClass = null;

    private void setWriterClass(Class writerClass) {
        if (writerClass == null) throw new NullPointerException("writerClass");
        this._writerClass = writerClass;
    }

    private static final int NO_PORT = -1;

    private int _portNumber = NO_PORT;

    private void setPortNumber(int portNumber) {
        if (portNumber == NO_PORT) throw new IllegalArgumentException("invalid port number " + portNumber);
        this._portNumber = portNumber;
    }

    private static final int DEFAULT_BACKLOG = 10;

    private int _backlog = DEFAULT_BACKLOG;

    private void setBacklog(int backlog) {
        if (backlog <= 0) throw new IllegalArgumentException("invalid backlog " + backlog);
        this._backlog = backlog;
    }

    /** A helper implementing a SAX ContentHandler 

      Configuration file is in XML

      <mllp>
        <server id="foo">
          <port number="1234"/>
          <backlog value="10"/>
	  <reader class="org.regenstrief.xhl7.HL7XMLReader"/>
	  <writer class="org.regenstrief.xhl7.HL7XMLWriter"/>
          <transformerFactory class="net.sf.saxon.TransformerFactory"/>
          <transform href="this.xsl"/>
	  <fail href="fail.xsl"/>
        </server>
      </mllp>      
   */
    private static class ConfigurationHandler implements ContentHandler {

        MLLPServer _currentServer = null;

        TransformerFactory _currentTransformerFactory = null;

        static final String TAG_ROOT = "mllp";

        static final String TAG_SERVER = "server";

        static final String TAG_PORT = "port";

        static final String TAG_BACKLOG = "backlog";

        static final String TAG_TRANSFORMER_FACTORY = "transformerFactory";

        static final String TAG_TRANSFORM = "transform";

        static final String TAG_ERROR = "error";

        static final String TAG_READER = "reader";

        static final String TAG_WRITER = "writer";

        static final String ATT_SERVER_ID = "id";

        static final String ATT_PORT_NUMBER = "number";

        static final String ATT_BACKLOG_VALUE = "value";

        static final String ATT_TRANSFORMER_FACTORY_CLASS = "class";

        static final String ATT_TRANSFORM_HREF = "href";

        static final String ATT_ERROR_HREF = "href";

        static final String ATT_READER_CLASS = "class";

        static final String ATT_WRITER_CLASS = "class";

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            localName = localName.intern();
            if (localName == TAG_ROOT) {
            } else if (localName == TAG_SERVER) {
                if (this._currentServer != null) throw new SAXException("nested " + localName + " elements");
                String id = atts.getValue(ATT_SERVER_ID);
                if (id == null) throw new SAXException(localName + " must have an id attribute");
                this._currentServer = new MLLPServer(id);
            } else if (localName == TAG_PORT) {
                if (_currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                String portNumberString = atts.getValue(ATT_PORT_NUMBER);
                int portNumber;
                if (portNumberString != null) portNumber = Integer.parseInt(portNumberString); else throw new SAXException(localName + " must have a " + ATT_PORT_NUMBER + " attribute");
                this._currentServer.setPortNumber(portNumber);
            } else if (localName == TAG_BACKLOG) {
                if (_currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                String backlogString = atts.getValue(ATT_BACKLOG_VALUE);
                int backlog;
                if (backlogString != null) backlog = Integer.parseInt(backlogString); else throw new SAXException(localName + " must have a " + ATT_BACKLOG_VALUE + " attribute");
                this._currentServer.setBacklog(backlog);
            } else if (localName == TAG_READER) {
                if (_currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                String className = atts.getValue(ATT_READER_CLASS);
                if (className == null) throw new SAXException(localName + " must have a " + ATT_READER_CLASS + " attribute");
                try {
                    Class readerClass = Class.forName(className);
                    _currentServer.setReaderClass(readerClass);
                } catch (Exception ex) {
                    throw new SAXException(ex);
                }
            } else if (localName == TAG_WRITER) {
                if (_currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                String className = atts.getValue(ATT_WRITER_CLASS);
                if (className == null) throw new SAXException(localName + " must have a " + ATT_WRITER_CLASS + " attribute");
                try {
                    Class writerClass = Class.forName(className);
                    _currentServer.setWriterClass(writerClass);
                } catch (Exception ex) {
                    throw new SAXException(ex);
                }
            } else if (localName == TAG_TRANSFORMER_FACTORY) {
                if (_currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                String className = atts.getValue(ATT_TRANSFORMER_FACTORY_CLASS);
                if (className == null) throw new SAXException(localName + " must have a " + ATT_TRANSFORMER_FACTORY_CLASS + " attribute");
                try {
                    this._currentTransformerFactory = (TransformerFactory) Class.forName(className).newInstance();
                } catch (Exception ex) {
                    throw new SAXException(ex);
                }
            } else if (localName == TAG_TRANSFORM) {
                if (this._currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                if (this._currentTransformerFactory == null) throw new SAXException(localName + " must follow a " + TAG_TRANSFORMER_FACTORY + " element");
                String href = atts.getValue(ATT_TRANSFORM_HREF);
                if (href == null) throw new SAXException(localName + " must have a " + ATT_TRANSFORM_HREF + " attribute");
                try {
                    Source transformSource = new StreamSource((new URL(href)).openStream());
                    this._currentServer.setTemplates(this._currentTransformerFactory.newTemplates(transformSource));
                } catch (Exception ex) {
                    throw new SAXException(ex);
                }
            } else if (localName == TAG_ERROR) {
                if (this._currentServer == null) throw new SAXException(localName + " must be in a " + TAG_SERVER + " element");
                if (this._currentTransformerFactory == null) throw new SAXException(localName + " must follow a " + TAG_TRANSFORMER_FACTORY + " element");
                String href = atts.getValue(ATT_ERROR_HREF);
                if (href == null) throw new SAXException(localName + " must have a " + ATT_ERROR_HREF + " attribute");
                try {
                    Source transformSource = new StreamSource((new URL(href)).openStream());
                    this._currentServer.setErrorTemplates(this._currentTransformerFactory.newTemplates(transformSource));
                } catch (Exception ex) {
                    throw new SAXException(ex);
                }
            } else {
                throw new SAXException("schema error, illegal element " + localName);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            localName = localName.intern();
            if (localName == TAG_ROOT) {
            } else if (localName == TAG_SERVER) {
                this._currentServer.start();
                this._currentTransformerFactory = null;
                this._currentServer = null;
            } else if (localName == TAG_PORT) {
            } else if (localName == TAG_BACKLOG) {
            } else if (localName == TAG_READER) {
            } else if (localName == TAG_WRITER) {
            } else if (localName == TAG_TRANSFORMER_FACTORY) {
            } else if (localName == TAG_TRANSFORM) {
            } else if (localName == TAG_ERROR) {
            } else {
                throw new SAXException("schema error, illegal element " + localName);
            }
        }

        public void characters(char[] ch, int start, int length) {
        }

        public void startDocument() {
        }

        public void endDocument() {
        }

        public void startPrefixMapping(String prefix, String uri) {
        }

        public void endPrefixMapping(String prefix) {
        }

        public void ignorableWhitespace(char[] ch, int start, int length) {
        }

        public void processingInstruction(String target, String data) {
        }

        public void setDocumentLocator(org.xml.sax.Locator locator) {
        }

        public void skippedEntity(String name) {
        }
    }

    /** Called by Thread.start(). */
    public void run() {
        if (this._portNumber == NO_PORT) throw new Error("no port number");
        if (this._templates == null) throw new Error("no templates");
        ServerSocket acceptor = null;
        try {
            acceptor = new ServerSocket(this._portNumber, this._backlog, null);
        } catch (Exception ex) {
            throw new Error(ex);
        }
        System.err.println("[" + this._id + "] accepting connections on " + acceptor.getInetAddress() + " " + acceptor.getLocalPort());
        while (true) {
            Socket connection = null;
            try {
                connection = acceptor.accept();
            } catch (Exception ex) {
                throw new Error(ex);
            }
            try {
                (new ClientHandler(connection)).start();
            } catch (Exception x) {
            }
        }
    }

    /** A helper class that handles a client connection. */
    private class ClientHandler extends Thread {

        /** Parameter name by which ther exception is handed to the error
        transform. */
        public static final String PAR_EXCEPTION = "exception";

        /** Parameter name by which the server id is handed to the error
        transform. */
        public static final String PAR_SERVER_ID = "serverId";

        /** Parameter name by which the remote socket address is handed to 
	the transform. */
        public static final String PAR_REMOTE_ADDR = "remoteSocketAddress";

        /** The established connection to the client. */
        private Socket _connection;

        /** Constructor. 
	@param connection the established connection to the client.
    */
        ClientHandler(Socket connection) {
            this._connection = connection;
        }

        /** From Runnable interface. */
        public void run() {
            System.err.println("[" + _id + "] serving " + this._connection.getRemoteSocketAddress());
            try {
                Transformer transformer = _templates.newTransformer();
                transformer.setParameter(PAR_REMOTE_ADDR, this._connection.getRemoteSocketAddress());
                MLLPDriver mllpDriver = new MLLPDriver(this._connection.getInputStream(), this._connection.getOutputStream(), false);
                Source source;
                if (_readerClass != null) source = new SAXSource((XMLReader) _readerClass.newInstance(), new InputSource(mllpDriver.getInputStream())); else source = new StreamSource(mllpDriver.getInputStream());
                source.setSystemId("file:" + _id + "/");
                Result result;
                if (_writerClass != null) {
                    XMLWriter writer = (XMLWriter) _writerClass.newInstance();
                    writer.setOutputStream(mllpDriver.getOutputStream());
                    result = new SAXResult(writer.getContentHandler());
                } else result = new StreamResult(mllpDriver.getOutputStream());
                result.setSystemId("file:ababa");
                while (mllpDriver.hasMoreInput()) {
                    try {
                        transformer.transform(source, result);
                        mllpDriver.turn();
                    } catch (Exception ex) {
                        System.err.println("[" + _id + "] ERROR " + ex.getMessage());
                        ex.printStackTrace(System.err);
                        mllpDriver.discardPendingOutput();
                        if (_errorTemplates != null) {
                            Transformer errorTransformer = _errorTemplates.newTransformer();
                            errorTransformer.setParameter(PAR_SERVER_ID, _id);
                            errorTransformer.setParameter(PAR_REMOTE_ADDR, this._connection.getRemoteSocketAddress());
                            errorTransformer.setParameter(PAR_EXCEPTION, ex);
                            errorTransformer.transform(new StreamSource(new ByteArrayInputStream("<null/>".getBytes())), result);
                            mllpDriver.turn();
                        }
                    }
                    System.err.println("[" + _id + "] message done from " + this._connection.getRemoteSocketAddress());
                }
                System.err.println("[" + _id + "] server exit from " + this._connection.getRemoteSocketAddress());
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                throw new Error(ex);
            }
        }
    }
}
