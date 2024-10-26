package ti.protocols.oscript;

/**
 * This is sorta a kludge!  Because the swing text component when rendering
 * HTML wants to construct a URL instance to pass in an event to whoever is
 * listening for a link, in order to support links that are script actions
 * (ie. href="oscript:writeln(21);") we need to provide a <code>URLStreamHandler</code>,
 * despite the fact that you can't actually open this sort of URL as a stream.
 * 
 * @author Rob Clark
 * @version 0.1
 */
public class Handler extends java.net.URLStreamHandler {

    /**
   * Opens a connection to the object referenced by the
   * <code>URL</code> argument.
   * This method should be overridden by a subclass.
   *
   * <p>If for the handler's protocol (such as HTTP or JAR), there
   * exists a public, specialized URLConnection subclass belonging
   * to one of the following packages or one of their subpackages:
   * java.lang, java.io, java.util, java.net, the connection
   * returned will be of that subclass. For example, for HTTP an
   * HttpURLConnection will be returned, and for JAR a
   * JarURLConnection will be returned.
   *
   * @param      u   the URL that this connects to.
   * @return     a <code>URLConnection</code> object for the <code>URL</code>.
   * @exception  IOException  if an I/O error occurs while opening the
   *               connection.
   */
    protected java.net.URLConnection openConnection(java.net.URL url) throws java.io.IOException {
        throw new java.io.IOException("can't connect to url: " + url);
    }

    /**
   * Parses the string representation of a <code>URL</code> into a
   * <code>URL</code> object.
   * <p>
   * If there is any inherited context, then it has already been
   * copied into the <code>URL</code> argument.
   * <p>
   * The <code>parseURL</code> method of <code>URLStreamHandler</code>
   * parses the string representation as if it were an
   * <code>http</code> specification. Most URL protocol families have a
   * similar parsing. A stream protocol handler for a protocol that has
   * a different syntax must override this routine.
   *
   * @param   u       the <code>URL</code> to receive the result of parsing
   *                  the spec.
   * @param   spec    the <code>String</code> representing the URL that
   *                  must be parsed.
   * @param   start   the character index at which to begin parsing. This is
   *                  just past the '<code>:</code>' (if there is one) that
   *                  specifies the determination of the protocol name.
   * @param   limit   the character position to stop parsing at. This is the
   *                  end of the string or the position of the
   *                  "<code>#</code>" character, if present. All information
   *                  after the sharp sign indicates an anchor.
   */
    protected void parseURL(java.net.URL u, String spec, int start, int limit) {
        setURL(u, "oscript", null, -1, null, null, spec.substring(start, limit).replace('\'', '\"'), null, null);
    }
}
