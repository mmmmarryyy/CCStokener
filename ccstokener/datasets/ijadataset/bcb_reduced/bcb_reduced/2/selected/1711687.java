package com.beanstalktech.servlet.script;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.beanstalktech.common.connection.Connection;
import com.beanstalktech.common.context.AppEvent;
import com.beanstalktech.common.script.BaseScriptCommand;
import com.beanstalktech.common.script.ScriptException;
import com.beanstalktech.common.script.ScriptRequest;
import com.beanstalktech.common.script.ScriptResponse;
import com.beanstalktech.common.utility.StringUtility;

/**
 * Sends a request to a server
 *
 * This class must be registered
 * with the Command Manager by placing the following entry in the
 * application's properties:
 * <P>
 * <PRE>
 *      command.ServerRequest = com.beanstalktech.common.script.ServerRequest
 * </PRE>
 *
 */
public class ServletScriptCommands extends BaseScriptCommand {

    /**
     * Writes to an HTTPServletResponse writer using a specified
     * template string or template file name.
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * writeHTTP(
     *  text=&lt;text to be written&gt;
     * [decode=TRUE|*FALSE]
     * [flush=TRUE|*FALSE]
     * </PRE>
     */
    public void handle_WriteHTTP(AppEvent evt) throws ScriptException {
        try {
            String text = getStringParameter(evt, "text", "");
            if (text == null) return;
            boolean decode = getBooleanParameter(evt, "decode", false);
            if (decode) {
                text = StringUtility.beanstalkDecode(text);
            }
            boolean flush = getBooleanParameter(evt, "flush", false);
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (response instanceof HttpServletResponse) {
                PrintWriter writer = ((HttpServletResponse) response).getWriter();
                writer.write(text);
                if (flush) {
                    writer.flush();
                }
            }
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Writes to an HTTPServletResponse from a specified byte array
     * in the session context.
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * writeBinaryHTTP(
     *  byteArrayName=&lt;name of byte array to write&gt;
     * </PRE>
     */
    public void handle_WriteBinaryHTTP(AppEvent evt) throws ScriptException {
        try {
            evt.getApplication().getLogger().logMessage(8, "ServletScriptCommand: writeBinaryHTTP: Writing to HTTP response ...");
            String byteArrayName = getStringParameter(evt, "byteArrayName", "");
            byte[] array = (byte[]) evaluateExpression(evt, byteArrayName, true);
            if (array != null) {
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommand: writeBinaryHTTP: Writing byte array: " + byteArrayName + " of length: " + array.length);
            } else {
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommand: writeBinaryHTTP: Couldn't find byte array: " + byteArrayName);
                return;
            }
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (response instanceof HttpServletResponse) {
                OutputStream out = ((HttpServletResponse) response).getOutputStream();
                out.write(array);
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommand: writeBinaryHTTP: Wrote bytes to HTTP response: " + array.length);
            } else {
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommand: writeBinaryHTTP: No bytes written because response is not an HttpServletResponse.");
            }
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Reads the contents of a specified URL and writes the stream to
     * the HTTP response stream associated with the servlet.
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * writeURL(
     *  url=&lt;url to write) to write&gt;
     * </PRE>
     */
    @SuppressWarnings("unchecked")
    public void handle_WriteURL(AppEvent evt) throws ScriptException {
        int i = 0;
        try {
            String urlName = getStringParameter(evt, "url", "");
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (!(response instanceof HttpServletResponse)) {
                throw new Exception("Failed to extract HttpServletResponse from Application Event");
            }
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            OutputStream out = httpResponse.getOutputStream();
            URL url = new URL(urlName);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDefaultUseCaches(true);
            urlConnection.setUseCaches(true);
            urlConnection.setFollowRedirects(true);
            Connection connection = evt.getConnection();
            while (connection != null) {
                evt.getApplication().getLogger().logMessage(8, "writeURL: Found connection: " + connection.getClass().getCanonicalName());
                Object request = connection.getRequest();
                if (request != null && request instanceof HttpServletRequest) {
                    Enumeration<String> headers = ((HttpServletRequest) request).getHeaderNames();
                    while (headers.hasMoreElements()) {
                        String headerName = headers.nextElement();
                        evt.getApplication().getLogger().logMessage(8, "writeURL: Setting HTTP request header: " + headerName + ": " + ((HttpServletRequest) request).getHeader(headerName));
                        urlConnection.setRequestProperty(headerName, ((HttpServletRequest) request).getHeader(headerName));
                    }
                    break;
                }
                connection = connection.getParentConnection();
            }
            urlConnection.connect();
            int statusCode = urlConnection.getResponseCode();
            httpResponse.setStatus(statusCode);
            for (int j = 1; j < 64; j++) {
                String key = urlConnection.getHeaderFieldKey(j);
                String value = urlConnection.getHeaderField(j);
                if (value == null) break;
                httpResponse.addHeader(key, value);
                evt.getApplication().getLogger().logMessage(8, "writeURL: HTTP response header: " + key + ": " + value);
            }
            evt.getApplication().getLogger().logMessage(8, "writeURL: HTTP response status: " + statusCode);
            httpResponse.flushBuffer();
            InputStream is = urlConnection.getInputStream();
            int ch;
            boolean clientClosedStream = false;
            while (((ch = is.read()) != -1) && !clientClosedStream) {
                i++;
                try {
                    out.write(ch);
                    if (i % 1024 == 0) {
                        out.flush();
                        httpResponse.flushBuffer();
                    }
                } catch (SocketException e) {
                    evt.getApplication().getLogger().logMessage(4, "writeURL: Client closed HTTP connection after " + i + " bytes written.");
                    clientClosedStream = true;
                }
            }
            evt.getApplication().getLogger().logMessage(4, "writeURL: Wrote bytes to HTTP response: " + i);
            if (i < 1) {
                throw new IOException("The URL stream is empty");
            }
            out.flush();
            out.close();
            urlConnection.disconnect();
        } catch (IOException e) {
            evt.getApplication().getLogger().logMessage(10, "writeURL: Client closed HTTP connection after " + i + " bytes written.");
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Creates a cookie in the HTTPServletResponse containing the session ID.
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * writeSessionCookie();
     * </PRE>
     */
    public void handle_WriteSessionCookie(AppEvent evt) throws ScriptException {
        try {
            Cookie cookie = new Cookie("sessionID", evt.getSessionID());
            String cookieDomain = evt.getApplication().getApplicationContext().getProperty("cookieDomain");
            if (cookieDomain != null && cookieDomain.trim().length() > 0) {
                cookie.setDomain(cookieDomain);
            }
            String cookiePath = evt.getApplication().getApplicationContext().getProperty("cookiePath");
            if (cookiePath != null && cookiePath.trim().length() > 0) {
                cookie.setPath(cookiePath);
            }
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (response instanceof HttpServletResponse) {
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommands WriteSessionCookie writing session cookie with session id: " + evt.getSessionID());
                ((HttpServletResponse) response).addCookie(cookie);
            }
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Sets the content type header for the HTTPServerResponse
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * setContentType(
        contentType=&gt;MIME type for the Content-Type response header&lt;);
     * </PRE>
     */
    public void handle_SetContentType(AppEvent evt) throws ScriptException {
        try {
            String contentType = getStringParameter(evt, "contentType", "");
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (response instanceof HttpServletResponse) {
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommands: setContentType: Setting content type to: " + contentType);
                ((HttpServletResponse) response).setContentType(contentType);
            }
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Sets headers in the HTTPServerResponse to specify
     * that the response should not be cached in the
     * HTTP client
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * setNoCache();
     * </PRE>
     */
    public void handle_SetNoCache(AppEvent evt) throws ScriptException {
        try {
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).addHeader("Cache-control", "no-cache");
                ((HttpServletResponse) response).addHeader("Cache-control", "no-store");
                ((HttpServletResponse) response).addHeader("Pragma", "no-cache");
                ((HttpServletResponse) response).addHeader("Expires", "0");
            }
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Sets the status code and location header in the HttpServletResponse
     * to signal a redirection to another URL
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * redirectURL(
        url=&gt;URL to redirect to&lt;);
     * </PRE>
     */
    public void handle_RedirectURL(AppEvent evt) throws ScriptException {
        try {
            String url = getStringParameter(evt, "url", "");
            Object response = evt.getConnection().getResponse();
            while (response != null && response instanceof ScriptResponse) {
                response = ((ScriptResponse) response).getResponse();
            }
            if (response instanceof HttpServletResponse) {
                evt.getApplication().getLogger().logMessage(8, "ServletScriptCommands: redirectURL: Setting URL to: " + url);
                ((HttpServletResponse) response).setHeader("Location", url);
                ((HttpServletResponse) response).setStatus(302);
            }
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }

    /**
     * Sends a message via SMTP protocol
     *
     * <P>
     * Command format:
     *<P>
     * <PRE>
     * sendMessage(
     *   body=&lt;message body&gt;,
     *   toAddress=&lt;recipient's address&gt;,
     *   fromAddress=&lt;sender's address&gt;,
     *   subject=&lt;subject of message&gt;,
     *   [fromName=&lt;name of sender&gt;],
     *   [contentType=*text/plain|&lt;MIME type for the message&gt;]
     * );
     * </PRE>
     */
    public void handle_SendSMTPMessage(AppEvent evt) throws ScriptException {
        final String command = "SendSMTPMessage";
        try {
            putSessionElement(evt, "statusCode_" + command, new BigDecimal(100));
            putSessionElement(evt, "statusMessage_" + command, "Command did not complete.");
            editParameter(evt, "IsPresent", "body");
            editParameter(evt, "IsPresent", "toAddress");
            editParameter(evt, "IsPresent", "fromAddress");
            editParameter(evt, "IsPresent", "subject");
            String body = getStringParameter(evt, "body");
            String toAddress = getStringParameter(evt, "toAddress");
            String fromAddress = getStringParameter(evt, "fromAddress");
            String subject = getStringParameter(evt, "subject");
            String fromName = getStringParameter(evt, "fromName", "");
            String contentType = getStringParameter(evt, "contentType", "text/plain");
            String server = getAppProperty(evt, "SMTPServer");
            body = StringUtility.beanstalkDecode(body);
            Properties properties = new Properties();
            properties.put("mail.smtp.host", server);
            Session session = Session.getDefaultInstance(properties, null);
            Address replyToList[] = { new InternetAddress(fromAddress) };
            Address toList[] = { new InternetAddress(toAddress) };
            Message message = new MimeMessage(session);
            if (!fromName.trim().equals("")) message.setFrom(new InternetAddress(fromAddress, fromName)); else message.setFrom(new InternetAddress(fromAddress, getAppProperty(evt, "SMTPDefaultFromName")));
            message.setReplyTo(replyToList);
            message.setRecipients(Message.RecipientType.BCC, toList);
            message.setSubject(subject);
            message.setSentDate(new Date());
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(server, getAppProperty(evt, "SMTPUser"), getAppProperty(evt, "SMTPPassword"));
            transport.sendMessage(message, toList);
            putSessionElement(evt, "statusCode_" + command, new BigDecimal(0));
            putSessionElement(evt, "statusMessage_" + command, "OK");
        } catch (Exception e) {
            throw new ScriptException(evt, e);
        }
    }
}
