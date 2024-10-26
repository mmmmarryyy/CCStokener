package org.retro.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The TCP Client.
 *   
 * @author berlin.brown
 * @see org.retro.gis.BotServer
 * @see org.retro.gis.XMLSocketMessage
 */
public class BotTCPConnection {

    private String _url_connect = null;

    private int _port_connect = -1;

    private static final String _Encoding = "UTF-8";

    private String mainXMLRequest = null;

    private String finalXMLResponse = null;

    private Socket _mainSocket = null;

    private BufferedOutputStream _mainOutputStream = null;

    private InputStreamReader _mainInputStreamReader = null;

    public BotTCPConnection(String _url, int port) {
        _url_connect = _url;
        _port_connect = port;
    }

    private byte[] getRawBytes(URL url) throws IOException {
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        int bytes;
        while ((bytes = in.read(tmp)) != -1) {
            out.write(tmp, 0, bytes);
        }
        return out.toByteArray();
    }

    private String getReadline(BufferedReader d) throws IOException {
        StringBuffer _buf = new StringBuffer();
        try {
            String line = null;
            line = d.readLine();
            _buf.append(line.trim());
            while ((line = d.readLine()) != null) {
                _buf.append(line.trim());
                if (line.length() == 0) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
        return _buf.toString();
    }

    private String getBytes(InputStream _instream) throws IOException {
        String _fin = null;
        StringBuffer _str_buffer = null;
        byte[] tmp = new byte[1024];
        int bytes;
        _str_buffer = new StringBuffer();
        while ((bytes = _instream.read(tmp)) != -1) {
            String _str = new String(tmp, 0, bytes, _Encoding);
            _str_buffer.append(_str);
        }
        _fin = (String) _str_buffer.toString();
        return _fin;
    }

    /**
	 * This method will build the XML based chat message to send to the server. 
	 * 
	 * @param _instr	String to send to server.
	 */
    public void buildXMLRequest(String _instr, String _req) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document _doc = null;
        mainXMLRequest = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            _doc = builder.newDocument();
            Element root = _doc.createElement("BotXML");
            Element msg = _doc.createElement("Message");
            Element prop = _doc.createElement("Properties");
            Element msgdata = _doc.createElement("MsgData");
            Node data = _doc.createTextNode(_instr);
            prop.setAttribute("url", "none");
            prop.setAttribute("time", "" + (new java.util.Date()).getTime());
            prop.setAttribute("username", "none");
            prop.setAttribute("request", _req);
            root.appendChild(msg);
            msg.appendChild(prop);
            prop.appendChild(msgdata);
            msgdata.appendChild(data);
            _doc.appendChild(root);
            mainXMLRequest = XMLMessageString(_doc);
        } catch (Exception _z) {
            _z.printStackTrace();
        }
    }

    private String XMLMessageString(Document d) throws Exception {
        OutputFormat _format = new OutputFormat(d);
        _format.setLineWidth(80);
        _format.setIndenting(true);
        _format.setIndent(2);
        StringWriter _sWriter = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(_sWriter, _format);
        serializer.serialize(d);
        return _sWriter.toString();
    }

    public String getRawResponse() {
        return finalXMLResponse;
    }

    /**
	 * Establish a connection for the full duration of running the application,
	 * make sure to close on exit.
	 * 
	 * @throws IOException
	 */
    public void establishConnection() throws IOException {
        _mainSocket = null;
        _mainOutputStream = null;
        _mainInputStreamReader = null;
        try {
            _mainSocket = new Socket(_url_connect, _port_connect);
            _mainSocket.setSoTimeout(4 * 60 * 1000);
            _mainOutputStream = new BufferedOutputStream(_mainSocket.getOutputStream());
            _mainInputStreamReader = new InputStreamReader((InputStream) _mainSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Send a XML based message to the server.
	 * 
	 * @throws IOException
	 */
    public void sendMessageServer() throws IOException {
        DataOutputStream _data_stream = new DataOutputStream(_mainOutputStream);
        BufferedReader d = new BufferedReader(_mainInputStreamReader);
        mainXMLRequest += "\r\n";
        byte _getData[] = mainXMLRequest.getBytes(_Encoding);
        _data_stream.write(_getData);
        _data_stream.flush();
        String _final_response = getReadline(d);
        finalXMLResponse = _final_response.trim();
    }

    /**
	 * Close the object connection to the server.
	 *
	 */
    public void closeConnection() {
        if (_mainSocket != null) {
            try {
                _mainSocket.close();
            } catch (Exception e) {
            }
        }
        if (_mainInputStreamReader != null) {
            try {
                _mainInputStreamReader.close();
            } catch (Exception nothing) {
            }
        }
    }

    /**
	 * Performed during the garbage-collection on this object,
	 * this will ensure that all connections are closed properly. 
	 */
    protected void finalize() throws Throwable {
        closeConnection();
        super.finalize();
    }

    /**
	 * Make a connection, communicate with server and close.
	 * 
	 * @throws IOException
	 */
    public void makeDirectConnect() throws IOException {
        Socket _sock = null;
        BufferedOutputStream _output_stream = null;
        InputStreamReader _input_stream = null;
        try {
            _sock = new Socket(_url_connect, _port_connect);
            _sock.setSoTimeout(4 * 60 * 1000);
            _output_stream = new BufferedOutputStream(_sock.getOutputStream());
            _input_stream = new InputStreamReader((InputStream) _sock.getInputStream());
            DataOutputStream _data_stream = new DataOutputStream(_output_stream);
            BufferedReader d = new BufferedReader(_input_stream);
            mainXMLRequest += "\r\n";
            byte _getData[] = mainXMLRequest.getBytes(_Encoding);
            _data_stream.write(_getData);
            _data_stream.flush();
            String _final_response = getReadline(d);
            finalXMLResponse = _final_response.trim();
            _input_stream.close();
            _sock.close();
            _input_stream = null;
            _sock = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            if (_sock != null) {
                try {
                    _sock.close();
                } catch (Exception e) {
                }
            }
            if (_input_stream != null) {
                try {
                    _input_stream.close();
                } catch (Exception nothing) {
                }
            }
        }
    }
}
