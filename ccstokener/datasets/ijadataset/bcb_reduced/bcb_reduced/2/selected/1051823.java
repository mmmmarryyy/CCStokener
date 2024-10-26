package net.sourceforge.sitemaps.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

public class HttpRequest {

    private String userAgent = "SitemapBot";

    private Metadata headers = new Metadata();

    public static final int BUFFER_SIZE = 8 * 1024;

    @SuppressWarnings("unused")
    private static final byte[] EMPTY_CONTENT = new byte[0];

    /** The proxy hostname. */
    private String proxyHost = null;

    /** The proxy port. */
    private int proxyPort = 8080;

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean useProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public int getMaxContent() {
        return maxContent;
    }

    /**
	 * Maximum number of bytes that can be downloaded
	 * @param maxContent
	 */
    public void setMaxContent(int maxContent) {
        this.maxContent = maxContent;
    }

    public int getMaxDelays() {
        return maxDelays;
    }

    public void setMaxDelays(int maxDelays) {
        this.maxDelays = maxDelays;
    }

    /** Indicates if a proxy is used */
    private boolean useProxy = false;

    /** The network timeout in millisecond */
    private int timeout = 10000;

    /** The length limit for downloaded content, in bytes. */
    private int maxContent = 64 * 1024;

    /** The number of times a thread will delay when trying to fetch a page. */
    protected int maxDelays = 3;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
	 * @param userAgent the userAgent to set
	 */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
	 * @return the userAgent
	 */
    public String getUserAgent() {
        return userAgent;
    }

    public HttpRequest() {
    }

    public HttpResponse getResponse(URL url) throws ProtocolException, IOException {
        HttpResponse response = new HttpResponse();
        String path = "".equals(url.getFile()) ? "/" : url.getFile();
        String host = url.getHost();
        int port;
        String portString;
        if (url.getPort() == -1) {
            port = 80;
            portString = "";
        } else {
            port = url.getPort();
            portString = ":" + port;
        }
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setSoTimeout(timeout);
            String sockHost = useProxy() ? getProxyHost() : host;
            int sockPort = useProxy() ? getProxyPort() : port;
            InetSocketAddress sockAddr = new InetSocketAddress(sockHost, sockPort);
            socket.connect(sockAddr, getTimeout());
            OutputStream req = socket.getOutputStream();
            StringBuffer reqStr = new StringBuffer("GET ");
            if (useProxy()) {
                reqStr.append(url.getProtocol() + "://" + host + portString + path);
            } else {
                reqStr.append(path);
            }
            reqStr.append(" HTTP/1.0\r\n");
            reqStr.append("Host: ");
            reqStr.append(host);
            reqStr.append(portString);
            reqStr.append("\r\n");
            reqStr.append("Accept-Encoding: x-gzip, gzip, deflate\r\n");
            String userAgent = getUserAgent();
            if ((userAgent == null) || (userAgent.length() == 0)) {
                reqStr.append("User-Agent: SitemapBot");
            } else {
                reqStr.append("User-Agent: " + userAgent);
            }
            reqStr.append("\r\n");
            reqStr.append("\r\n");
            byte[] reqBytes = reqStr.toString().getBytes();
            req.write(reqBytes);
            req.flush();
            PushbackInputStream in = new PushbackInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE), BUFFER_SIZE);
            StringBuffer line = new StringBuffer();
            boolean haveSeenNonContinueStatus = false;
            while (!haveSeenNonContinueStatus) {
                int code = parseStatusLine(in, line);
                response.setCode(code);
                parseHeaders(in, line);
                haveSeenNonContinueStatus = code != 100;
            }
            byte[] content;
            content = readPlainContent(in);
            String contentEncoding = headers.get(HttpResponse.CONTENT_ENCODING);
            if ("gzip".equals(contentEncoding) || "x-gzip".equals(contentEncoding)) {
                content = processGzipEncoded(content, url);
            } else if ("deflate".equals(contentEncoding)) {
                content = processDeflateEncoded(content, url);
            } else {
            }
            response.setContent(content);
            response.setHeaders(headers);
        } catch (UnknownHostException e) {
            throw new ProtocolException(e.getMessage());
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) socket.close();
        }
        return response;
    }

    public HttpResponse getResponseOLD(URL url) {
        HttpResponse response = null;
        int bytesRead = 0;
        int bytesToRead = 1024;
        byte[] input = new byte[bytesToRead];
        InputStream in;
        try {
            in = url.openStream();
            while (bytesRead < bytesToRead) {
                int result = in.read(input, bytesRead, bytesToRead - bytesRead);
                if (result == -1) break;
                bytesRead += result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        response = new HttpResponse();
        response.setContent(input);
        return response;
    }

    private void processHeaderLine(StringBuffer line) throws IOException, HttpException {
        int colonIndex = line.indexOf(":");
        if (colonIndex == -1) {
            int i;
            for (i = 0; i < line.length(); i++) if (!Character.isWhitespace(line.charAt(i))) break;
            if (i == line.length()) return;
            throw new HttpException("No colon in header:" + line);
        }
        String key = line.substring(0, colonIndex);
        int valueStart = colonIndex + 1;
        while (valueStart < line.length()) {
            int c = line.charAt(valueStart);
            if (c != ' ' && c != '\t') break;
            valueStart++;
        }
        String value = line.substring(valueStart);
        headers.set(key, value);
    }

    private void parseHeaders(PushbackInputStream in, StringBuffer line) throws IOException, HttpException {
        while (readLine(in, line, true) != 0) {
            int pos;
            if (((pos = line.indexOf("<!DOCTYPE")) != -1) || ((pos = line.indexOf("<HTML")) != -1) || ((pos = line.indexOf("<html")) != -1)) {
                in.unread(line.substring(pos).getBytes("UTF-8"));
                line.setLength(pos);
                try {
                    processHeaderLine(line);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            processHeaderLine(line);
        }
    }

    private byte[] readPlainContent(InputStream in) throws HttpException, IOException {
        int contentLength = Integer.MAX_VALUE;
        String contentLengthString = headers.get(HttpResponse.CONTENT_LENGTH);
        if (contentLengthString != null) {
            contentLengthString = contentLengthString.trim();
            try {
                contentLength = Integer.parseInt(contentLengthString);
            } catch (NumberFormatException e) {
                throw new HttpException("bad content length: " + contentLengthString);
            }
        }
        if (getMaxContent() >= 0 && contentLength > getMaxContent()) contentLength = getMaxContent();
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
        byte[] bytes = new byte[BUFFER_SIZE];
        int length = 0;
        for (int i = in.read(bytes); i != -1; i = in.read(bytes)) {
            out.write(bytes, 0, i);
            length += i;
            if (length >= contentLength) break;
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unused")
    private byte[] readChunkedContent(PushbackInputStream in, StringBuffer line) throws HttpException, IOException {
        boolean doneChunks = false;
        int contentBytesRead = 0;
        byte[] bytes = new byte[BUFFER_SIZE];
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
        while (!doneChunks) {
            readLine(in, line, false);
            String chunkLenStr;
            int pos = line.indexOf(";");
            if (pos < 0) {
                chunkLenStr = line.toString();
            } else {
                chunkLenStr = line.substring(0, pos);
            }
            chunkLenStr = chunkLenStr.trim();
            int chunkLen;
            try {
                chunkLen = Integer.parseInt(chunkLenStr, 16);
            } catch (NumberFormatException e) {
                throw new HttpException("bad chunk length: " + line.toString());
            }
            if (chunkLen == 0) {
                doneChunks = true;
                break;
            }
            if ((contentBytesRead + chunkLen) > getMaxContent()) chunkLen = getMaxContent() - contentBytesRead;
            int chunkBytesRead = 0;
            while (chunkBytesRead < chunkLen) {
                int toRead = (chunkLen - chunkBytesRead) < BUFFER_SIZE ? (chunkLen - chunkBytesRead) : BUFFER_SIZE;
                int len = in.read(bytes, 0, toRead);
                if (len == -1) throw new HttpException("chunk eof after " + contentBytesRead + " bytes in successful chunks" + " and " + chunkBytesRead + " in current chunk");
                out.write(bytes, 0, len);
                chunkBytesRead += len;
            }
            readLine(in, line, false);
        }
        if (!doneChunks) {
            if (contentBytesRead != getMaxContent()) throw new HttpException("chunk eof: !doneChunk && didn't max out");
            return null;
        }
        parseHeaders(in, line);
        return out.toByteArray();
    }

    private int parseStatusLine(PushbackInputStream in, StringBuffer line) throws IOException, HttpException {
        readLine(in, line, false);
        int codeStart = line.indexOf(" ");
        int codeEnd = line.indexOf(" ", codeStart + 1);
        if (codeEnd == -1) codeEnd = line.length();
        int code;
        try {
            code = Integer.parseInt(line.substring(codeStart + 1, codeEnd));
        } catch (NumberFormatException e) {
            throw new HttpException("bad status line '" + line + "': " + e.getMessage(), e);
        }
        return code;
    }

    private static int readLine(PushbackInputStream in, StringBuffer line, boolean allowContinuedLine) throws IOException {
        line.setLength(0);
        for (int c = in.read(); c != -1; c = in.read()) {
            switch(c) {
                case '\r':
                    if (peek(in) == '\n') {
                        in.read();
                    }
                case '\n':
                    if (line.length() > 0) {
                        if (allowContinuedLine) switch(peek(in)) {
                            case ' ':
                            case '\t':
                                in.read();
                                continue;
                        }
                    }
                    return line.length();
                default:
                    line.append((char) c);
            }
        }
        throw new EOFException();
    }

    private static int peek(PushbackInputStream in) throws IOException {
        int value = in.read();
        in.unread(value);
        return value;
    }

    public byte[] processGzipEncoded(byte[] compressed, URL url) throws IOException {
        byte[] content;
        if (getMaxContent() >= 0) {
            content = GZIPUtils.unzipBestEffort(compressed, getMaxContent());
        } else {
            content = GZIPUtils.unzipBestEffort(compressed);
        }
        if (content == null) throw new IOException("unzipBestEffort returned null");
        return content;
    }

    public byte[] processDeflateEncoded(byte[] compressed, URL url) throws IOException {
        byte[] content = DeflateUtils.inflateBestEffort(compressed, getMaxContent());
        if (content == null) throw new IOException("inflateBestEffort returned null");
        return content;
    }
}
