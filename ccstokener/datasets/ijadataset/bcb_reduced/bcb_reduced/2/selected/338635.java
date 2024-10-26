package be.pendragon.net.pac;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public final class ProxyAutoConfiguration {

    private URI pacFileUri;

    public URI getPacFileUri() {
        return pacFileUri;
    }

    private Context jsContext;

    private ScriptableObject scope;

    private Function findProxyForURLFunction;

    private ProxyAutoConfiguration() {
    }

    public static ProxyAutoConfiguration build(URI pacFileUri) throws IOException {
        return new ProxyAutoConfiguration().init(pacFileUri);
    }

    public static ProxyAutoConfiguration build(File pacFile) throws IOException {
        return build(pacFile.toURI());
    }

    public static ProxyAutoConfiguration build(String pacFilePath) throws IOException {
        return build(new File(pacFilePath));
    }

    private ProxyAutoConfiguration init(URI pacFileUri) throws IOException {
        this.pacFileUri = pacFileUri;
        jsContext = ContextFactory.getGlobal().enterContext();
        scope = jsContext.initStandardObjects();
        scope.defineFunctionProperties(new String[] { "isPlainHostName", "dnsDomainIs", "localHostOrDomainIs", "isResolvable", "isInNet", "dnsResolve", "myIpAddress", "dnsDomainLevels", "shExpMatch", "weekdayRange", "dateRange", "timeRange" }, ProxyAutoConfiguration.class, ScriptableObject.DONTENUM);
        URL url = pacFileUri.toURL();
        InputStream bytesStream = url.openStream();
        Reader charStream = new InputStreamReader(bytesStream);
        jsContext.evaluateReader(scope, charStream, pacFileUri.toString(), 1, null);
        findProxyForURLFunction = (Function) scope.get("FindProxyForURL", scope);
        return this;
    }

    /**
   * True iff there is no domain name in the hostname (no dots).
   * Examples:<ul>
   * <li><code>isPlainHostName("www")</code> is true.</li>
   * <li><code>isPlainHostName("www.netscape.com")</code> is false.</li>
   * </ul>
   * @param host the hostname from the URL (excluding port number).
   */
    public static boolean isPlainHostName(String host) {
        if (null == host) return false;
        return (host.indexOf('.') < 0);
    }

    /**
   * Returns true iff the domain of hostname matches.<ul>
   * <li><code>dnsDomainIs("www.netscape.com", ".netscape.com")</code> is true.</li>
   * <li><code>dnsDomainIs("www", ".netscape.com")</code> is false.</li>
   * <li><code>dnsDomainIs("www.mcom.com", ".netscape.com")</code> is false.</li>
   * </ul>
   * @param host is the hostname from the URL.
   * @param domain is the domain name to test the hostname against.
   */
    public static boolean dnsDomainIs(String host, String domain) {
        if ((null == host) || (domain == null)) return false;
        return host.endsWith(domain);
    }

    /**
   * Is true if the hostname matches exactly the specified hostname, or if there is no domain name part in the hostname, but the unqualified hostname matches.
   * Examples:<ul>
   * <li><code>localHostOrDomainIs("www.netscape.com", "www.netscape.com")</code> is true (exact match).</li>
   * <li><code>localHostOrDomainIs("www", "www.netscape.com")</code> is true (hostname match, domain not specified).</li>
   * <li><code>localHostOrDomainIs("www.mcom.com", "www.netscape.com")</code> is false (domain name mismatch).</li>
   * <li><code>localHostOrDomainIs("home.netscape.com", "www.netscape.com")</code> is false (hostname mismatch).</li>
   * </ul>
   * @param host the hostname from the URL.
   * @param hostdom fully qualified hostname to match against.
   */
    public static boolean localHostOrDomainIs(String host, String hostdom) {
        if ((null == host) || (hostdom == null)) return false;
        if (host.equals(hostdom)) return true;
        if (!isPlainHostName(host)) return false;
        return host.equals(hostdom.split("\\.")[0]);
    }

    /**
   * Tries to resolve the hostname. Returns true if succeeds.
   * Examples:<ul>
   * <li><code>isResolvable("www.netscape.com")</code> is true (unless DNS fails to resolve it due to a firewall or some other reason).</li>
   * <li><code>isResolvable("bogus.domain.foobar")</code> is false.</li>
   * </ul>
   * @param host is the hostname from the URL.
   */
    public static boolean isResolvable(String host) {
        try {
            InetAddress.getByName(host);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
   * True iff the IP address of the host matches the specified IP address pattern.
   * Pattern and mask specification is done the same way as for SOCKS configuration.
   * Examples:<ul>
   * <li><code>isInNet(host, "198.95.249.79", "255.255.255.255")</code> is true iff the IP address of host matches exactly 198.95.249.79.</li>
   * <li><code>isInNet(host, "198.95.0.0", "255.255.0.0")</code> is true iff the IP address of the host matches 198.95.*.*.</li>
   * </ul>
   * @param host a DNS hostname, or IP address. If a hostname is passed, it will be resoved into an IP address by this function.
   * @param pattern an IP address pattern in the dot-separated format
   * @param mask mask for the IP address pattern informing which parts of the IP address should be matched against. 0 means ignore, 255 means match.
   * @throws UnknownHostException 
   */
    public static boolean isInNet(String host, String pattern, String mask) throws UnknownHostException {
        int hostAddress = ipAddressToInteger(InetAddress.getByName(host).getHostAddress());
        int patternAsInt = ipAddressToInteger(pattern);
        int maskAsInt = ipAddressToInteger(mask);
        return (hostAddress & maskAsInt) == patternAsInt;
    }

    private static int ipAddressToInteger(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Invalid IP adress '" + ipAddress + "'");
        return ((toUByte(parts[0]) << 8 | toUByte(parts[1])) << 8 | toUByte(parts[2])) << 8 | toUByte(parts[3]);
    }

    private static byte toUByte(String value) {
        Integer integer = Integer.valueOf(value);
        if ((~0xff & integer) != 0) throw new IllegalArgumentException("Not a byte: '" + value + "'");
        return integer.byteValue();
    }

    /**
   * Resolves the given DNS hostname into an IP address, and returns it in the dot separated format as a string.
   * Example:<ul>
   * <li><code>dnsResolve("home.netscape.com")</code> returns the string "198.95.249.79".</li>
   * </ul>
   * @param host hostname to resolve
   * @throws UnknownHostException 
   */
    public static String dnsResolve(String host) throws UnknownHostException {
        return InetAddress.getByName(host).getHostAddress();
    }

    /**
   * Returns the IP address of the host that the Navigator is running on, as a string in the dot-separated integer format.
   * Example:<ul>
   * <li><code>myIpAddress()</code> would return the string "198.95.249.79" if you were running the Navigator on that host.</li>
   * </ul>
   * @throws SocketException
   * @throws UnknownHostException
   */
    public static String myIpAddress() throws SocketException, UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    /**
   * Returns the number (integer) of DNS domain levels (number of dots) in the hostname.
   * Examples:<ul>
   * <li><code>dnsDomainLevels("www")</code> returns 0.</li>
   * <li><code>dnsDomainLevels("www.netscape.com")</code> returns 2.</li>
   * </ul>
   * @param host is the hostname from the URL.
   * @return
   */
    public static int dnsDomainLevels(String host) {
        return host.split("\\.").length - 1;
    }

    /**
   * Returns true if the string matches the specified shell expression.
   * <b>Actually, currently the patterns are shell expressions, not regular expressions.</b>
   * Examples:<ul>
   * <li><code>shExpMatch("http://home.netscape.com/people/ari/index.html", "*<span/>/ari/*")</code> is true.</li>
   * <li><code>shExpMatch("http://home.netscape.com/people/montulli/index.html", "*<span/>/ari/*")</code> is false.</li>
   * </ul>
   * @param str is any string to compare (e.g. the URL, or the hostname).
   * @param shexp is a shell expression to compare against.
   */
    public boolean shExpMatch(String str, String shexp) {
        String regex = "\\Q" + shexp.replaceAll("\\*", "\\\\E.*\\\\Q") + "\\E";
        return str.matches(regex);
    }

    /**
   * NOT YET IMPLEMENTED
   */
    public static boolean weekdayRange() {
        throw new UnsupportedOperationException();
    }

    /**
   * NOT YET IMPLEMENTED
   */
    public static boolean dateRange() {
        throw new UnsupportedOperationException();
    }

    /**
   * NOT YET IMPLEMENTED
   */
    public static boolean timeRange() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        Context.exit();
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    /**
   * Returns an array of {@link Connection} specifications.
   * <p>If the array is empty, no proxies should be used.</p>
   * <p>If there are multiple connections, the first one must be used,
   * until the system fails to establish the connection to the proxy.
   * In that case the next value will be used, etc.
   * The system should automatically retry a previously unresponsive proxy
   * after a delay (e.g. 30 minutes).</p>
   * @param url the full URL being accessed.
   * @param host the hostname extracted from the URL. This is only for convenience, it is the exact same string as between :// and the first : or / after that. The port number is not included in this parameter. It can be extracted from the URL when necessary.
   * @return
   */
    public Connection[] findProxyForURL(URL url, String host) {
        Object functionArgs[] = { url.toString(), host };
        Object result = findProxyForURLFunction.call(jsContext, scope, scope, functionArgs);
        String[] connectionsDescriptions = Context.toString(result).trim().split("\\s*;\\s*");
        Connection[] connections = new Connection[connectionsDescriptions.length];
        for (int idx = 0; idx < connections.length; idx++) {
            String[] parts = connectionsDescriptions[idx].split("\\s+");
            if (parts.length < 1) throw new IllegalArgumentException("'" + connectionsDescriptions[idx] + "' is not a connection description");
            if (parts[0].equals(DirectConnection.PREFIX)) {
                if (parts.length != 1) throw new IllegalArgumentException("'" + connectionsDescriptions[idx] + "' is not a valid DIRECT connection description");
                connections[idx] = DirectConnection.DIRECT;
            } else if (parts[0].equals(ProxyConnection.PREFIX)) {
                if (parts.length != 2) throw new IllegalArgumentException("'" + connectionsDescriptions[idx] + "' is missing host an port");
                connections[idx] = new ProxyConnection(parts[1]);
            } else if (parts[0].equals(ProxyConnection.PREFIX)) {
                if (parts.length != 2) throw new IllegalArgumentException("'" + connectionsDescriptions[idx] + "' is missing host an port");
                connections[idx] = new SocksConnection(parts[1]);
            } else throw new IllegalArgumentException("'" + connectionsDescriptions[idx] + "' is not a valid connection description");
        }
        return connections;
    }

    public Connection[] findProxyForURL(URL url) {
        return findProxyForURL(url, null);
    }

    /**
   * @param args
   */
    public static void main(String[] args) {
        try {
            if ((args.length < 2) || (args.length > 3)) {
                usage();
                throw new IllegalArgumentException("Incorrect arguments count");
            }
            ProxyAutoConfiguration pac = ProxyAutoConfiguration.build(args[0]);
            Connection[] proxies = (args.length == 2) ? pac.findProxyForURL(new URL(args[1])) : pac.findProxyForURL(new URL(args[1]), args[2]);
            System.out.println(Arrays.toString(proxies));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.out.println("Usage: java " + ProxyAutoConfiguration.class.getName() + "<proxy file> <url> [<host>]");
    }
}
