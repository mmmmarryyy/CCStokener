package org.openremote.controller.protocol.isy99;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openremote.controller.command.StatusCommand;
import org.openremote.controller.command.ExecutableCommand;
import org.openremote.controller.component.EnumSensorType;
import org.openremote.controller.exception.NoSuchCommandException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * OpenRemote isy99 command implementation.  <p>
 *
 * Maintains a isy99-machine-wide state for each address.<p>  
 * TODO <p>
 * Parse the dot.properties file. 
 * 
 * @author <a href="mailto:"></a>
 */
public class Isy99Command implements ExecutableCommand, StatusCommand {

    /**
   * Logging. Use common log category for all related classes.
   */
    private static final Logger log = Logger.getLogger(Isy99CommandBuilder.ISY99_LOG_CATEGORY);

    private String host;

    private String userName;

    private String password;

    /**
   * The address for this particular 'command' instance.
   */
    private String address = null;

    /**
   * The command string for this particular 'command' instance.
   */
    private String command = null;

    private String commandParam = null;

    public Isy99Command(String host, String userName, String password, String address, String command) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.address = address;
        this.command = command;
        log.info("Got isy99 config");
        log.info("Host >" + this.host + "<");
        log.info("UserName >" + this.userName + "<");
        log.info("Password >" + this.password + "<");
        log.info("Switch Address >" + this.address + "<");
        log.info("Switch Command >" + this.command + "<");
    }

    public Isy99Command(String host, String userName, String password, String address, String command, String commandParam) {
        this(host, userName, password, address, command);
        this.commandParam = commandParam;
        log.info("Got isy99 config-commandParmam");
        log.info("Host >" + this.host + "<");
        log.info("UserName >" + this.userName + "<");
        log.info("Password >" + this.password + "<");
        log.info("Switch Address >" + this.address + "<");
        log.info("Switch Command >" + this.command + "<");
        log.info("Switch CommandParam >" + this.commandParam + "<");
    }

    public void send() {
        final String urlPath = "/rest/nodes/";
        final String preIsy99Cmd = "/cmd/";
        String urlStr = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(host, 80), new UsernamePasswordCredentials(userName, password));
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://");
            urlBuilder.append(host);
            urlBuilder.append(urlPath);
            urlBuilder.append(address);
            urlBuilder.append(preIsy99Cmd);
            urlBuilder.append(command);
            if (commandParam != null) {
                urlBuilder.append("/");
                urlBuilder.append(commandParam);
                log.warn("commandParam  " + urlBuilder.toString());
            }
            urlStr = urlBuilder.toString();
            log.debug("send(): URL is " + urlStr);
            log.warn("send(): URL is rest call  " + urlStr);
            HttpGet httpget = new HttpGet(urlStr);
            log.debug("executing request" + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (responseStatusCode != 200) {
                log.error("send(): response status code was " + responseStatusCode);
            }
        } catch (IOException e) {
            log.error("send(): IOException: address: " + address + "command: " + command, e);
        } finally {
        }
    }

    public String read(EnumSensorType sensorType, Map<String, String> stateMap) {
        String urlPath = "/rest/nodes/";
        String preIsy99Cmd = "/";
        String urlStr = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        InputStream content = null;
        String value = null;
        try {
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(host, 80), new UsernamePasswordCredentials(userName, password));
            urlStr = "http://" + host + urlPath + address + preIsy99Cmd;
            HttpGet httpget = new HttpGet(urlStr);
            log.debug("executing request " + httpget.getURI());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != 200) {
                log.debug("status line " + response.getStatusLine());
                if (entity != null) {
                    log.debug("Response content length: " + entity.getContentLength());
                }
            } else {
                log.debug("Command was sent successfull ");
            }
            log.debug("----------------------------------------");
            log.debug("----------------------------------------");
            SAXBuilder builder = new SAXBuilder();
            content = response.getEntity().getContent();
            Document document = (Document) builder.build(content);
            Element rootNode = document.getRootElement();
            @SuppressWarnings("unchecked") List<Element> list = rootNode.getChildren("node");
            for (Element node : list) {
                log.debug("XML Parsing ");
                log.debug("address : " + node.getChildText("address"));
                log.debug("name : " + node.getChildText("name"));
                log.debug("type: " + node.getChildText("type"));
                log.debug("enabled: " + node.getChildText("enabled"));
                log.debug("elk_id: " + node.getChildText("ELK_ID"));
                log.debug("property: " + node.getChildText("property"));
                value = node.getChild("property").getAttributeValue("value");
                log.debug("prop->value-> " + value);
            }
        } catch (IOException ioe) {
            log.error("IOException while reading data from ISY-99", ioe);
            return "";
        } catch (JDOMException jdomex) {
            log.error("error while parsing response from ISY-99", jdomex);
            return "";
        } finally {
            try {
                content.close();
            } catch (IOException e) {
                ;
            }
        }
        int integerValue = -1;
        try {
            integerValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("invalid sensor reading from ISY-99: expected an integer, got \"" + value + "\"");
            return "";
        }
        switch(sensorType) {
            case SWITCH:
                if (value == null) {
                    return "off";
                } else if (integerValue >= 1) {
                    return "on";
                } else if (integerValue == 0) {
                    return "off";
                }
            case LEVEL:
                return "" + (integerValue * (100 / 250));
            case RANGE:
                return "" + integerValue;
            default:
                return "";
        }
    }
}
