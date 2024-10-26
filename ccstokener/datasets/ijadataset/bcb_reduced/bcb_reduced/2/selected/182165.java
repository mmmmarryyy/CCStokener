package net.sourceforge.esw.service;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.sourceforge.esw.properties.*;
import net.sourceforge.esw.util.FileUtil;

/**
 *  Sets up an RMI Codebase for the running VM serving out classes through HTTP
 * <p>
 * This class will add to the current RMI Codebase set for this VM to include
 * a reference to an HTTP service on a port that it starts.  This HTTP service
 * will serve out Class requests that come in, by delegating them to the local
 * ClassLoader.  It will also allow those classes to be loaded out of a jar file
 * as opposed to requiring them to exist in file form.
 *
 */
public class ClassServer implements Runnable {

    protected ServerSocket server;

    protected String codebase;

    protected int port;

    protected URLClassLoader loader;

    protected boolean bUseIPForCodebase = false;

    protected boolean bNeedToSetupCodebase = true;

    protected static final String CODEBASE_PROPERTY = "java.rmi.server.codebase";

    protected static final String HTTP = "http://";

    protected static final String COLON = ":";

    protected static final String SLASH = "/";

    protected static final String SPACE = " ";

    protected static final String HTTP_START = "HTTP/1.0 200 OK\r\n";

    protected static final String CONTENT_LENGTH = "Content-Length: ";

    protected static final String END_CONTENT_LENGTH = "    \r\n";

    protected static final String CONTENT_TYPE = "Content-Type: application/java\r\n\r\n";

    protected static final String CLASS_NOT_FOUND = "HTTP/1.0 400 ClassNotFound";

    protected static final String CR = "\r\n";

    protected static final String CONTENT_TYPE2 = "Content-Type: text/html\r\n\r\n";

    protected static final String GET = "GET /";

    protected static final String DOT_CLASS = ".class";

    /****************************************************************************
   * Creates a new ClassServer utilizing any availiable port.
   *
   * @exception IOException If there was a problem initializing the
   *                        server socket.
   */
    public ClassServer() throws IOException {
        this(0);
    }

    /****************************************************************************
   * Creates a new ClassServer utilizing the specified port.
   *
   * @param aPort The port on which to run the server. Passing in 0 states that
   *              any open port may be utilized.
   *
   * @exception IOException If there was a problem initializing the
   *                        server socket.
   * @exception UnknownHostException If the current hostname could not
   *                                 be determined (required for setting the
   *                                 codebase).
   */
    public ClassServer(int aPort) throws IOException, UnknownHostException {
        port = aPort;
        server = new ServerSocket(port);
        establishCodebase();
        ClassLoader tmp = Thread.currentThread().getContextClassLoader();
        if (tmp instanceof URLClassLoader) {
            loader = (URLClassLoader) tmp;
        } else {
            throw new ClassCastException("context ClassLoader is a " + tmp.getClass().getName() + " (expected URLClassLoader)");
        }
        new Thread(this, getClass().getName()).start();
    }

    /****************************************************************************
   * Shuts down the server socket.  This is done in a separate thread, as it
   * may take a while...
   */
    public void shutdown() {
        Thread t = new Thread(new Runnable() {

            ServerSocket s = server;

            public void run() {
                try {
                    s.close();
                } catch (IOException ex) {
                }
            }
        });
        t.start();
        server = null;
    }

    /****************************************************************************
   * Services a socket returned by the server socket
   */
    public void run() {
        if (null == server) {
            return;
        }
        Socket socket;
        try {
            socket = server.accept();
            if (bNeedToSetupCodebase) {
                establishCodebase();
            }
        } catch (InterruptedIOException iioe) {
            if (null != server) {
                try {
                    server.close();
                } catch (IOException ioe) {
                } finally {
                    server = null;
                }
            }
            return;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        new Thread(this, getClass().getName()).start();
        deliverClass(socket);
    }

    /****************************************************************************
   * Sets if this ClassServer should use my IP address in the Codebase
   */
    public void setUseIPForCodebase(boolean useIPForCodebase) {
        bUseIPForCodebase = useIPForCodebase;
        bNeedToSetupCodebase = true;
    }

    /****************************************************************************
   * If this ClassServer should use my IP address in the Codebase
   */
    public boolean getUseIPForCodebase() {
        return bUseIPForCodebase;
    }

    protected void deliverClass(Socket socket) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            InputStreamReader r = new InputStreamReader(socket.getInputStream());
            BufferedReader in = new BufferedReader(r);
            String className = getClassName(in);
            System.out.println(getClass().getName() + ": Class " + className + " requested from " + socket.getInetAddress() + COLON + socket.getPort());
            try {
                byte[] bytecodes = getByteCodes(className);
                try {
                    out.writeBytes(HTTP_START);
                    out.writeBytes(CONTENT_LENGTH + bytecodes.length + END_CONTENT_LENGTH);
                    out.writeBytes(CONTENT_TYPE);
                    out.write(bytecodes);
                    out.flush();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }
            } catch (ClassNotFoundException cnfe) {
                out.writeBytes(CLASS_NOT_FOUND + className + CR);
                out.writeBytes(CONTENT_TYPE2);
                out.flush();
                System.out.println(getClass().getName() + ": " + className + " not found.");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ioe) {
            }
        }
    }

    protected static String getClassName(BufferedReader in) throws IOException {
        String line = in.readLine();
        String path = "";
        if (line.startsWith(GET)) {
            line = line.substring(5, line.length() - 1).trim();
            int index = line.indexOf(DOT_CLASS);
            if (index != -1) {
                path = line.substring(0, index).replace('/', '.');
            }
        }
        do {
            line = in.readLine();
        } while ((line.length() != 0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));
        if (path.length() != 0) {
            return path;
        } else {
            throw new IOException("Malformed Header");
        }
    }

    /****************************************************************************
   * Return the bytecodes for the named class.  This implementation attempts
   * to find the class as a resource from the class loader specified at
   * construction time (which is usually the context class loader from the
   * thread that constructed the instance).
   *
   * @param aClassname The class to fetch the bytecodes for.
   * @exception ClassNotFoundException If the named class cannot be found.
   */
    protected byte[] getByteCodes(String aClassname) throws ClassNotFoundException {
        String path = aClassname.replace('.', File.separatorChar) + DOT_CLASS;
        URL url = loader.findResource(path);
        if (null == url) {
            byte abyte0[] = getClassFromJarURLs(path);
            if (abyte0 == null) {
                throw new ClassNotFoundException(aClassname);
            } else {
                return abyte0;
            }
        } else {
            try {
                URLConnection conn = url.openConnection();
                return FileUtil.getByteArrayFromInputStream(conn.getInputStream(), conn.getContentLength());
            } catch (IOException ex) {
                throw new ClassNotFoundException(ex.getMessage());
            }
        }
    }

    /****************************************************************************
   * Return the codebase URL that identifies this exporter.
   *
   * @return The codebase of the exporter.
   * @exception UnknownHostException If the local hostname could not
   * be determined.
   */
    protected String getCodebase() throws UnknownHostException {
        String host;
        if (bUseIPForCodebase) {
            host = InetAddress.getLocalHost().getHostAddress();
        } else {
            host = InetAddress.getLocalHost().getHostName();
        }
        if (null == codebase) {
            codebase = HTTP + host + COLON + server.getLocalPort() + SLASH;
        }
        return codebase;
    }

    /****************************************************************************
   * This adds the codebase for this exporter to the list of codebases
   * stored under the java.rmi.server.codebase property.  It will not
   * remove any preexisting codebases.
   *
   * @exception UnknownHostException If the local hostname could not be
   *                                 determined.
   */
    protected void establishCodebase() throws UnknownHostException {
        String codebase = PropertyUtil.getProperty(CODEBASE_PROPERTY);
        if (null != codebase) {
            codebase = codebase + SPACE + getCodebase();
        } else {
            codebase = getCodebase();
        }
        System.setProperty(CODEBASE_PROPERTY, codebase);
        bNeedToSetupCodebase = false;
        System.out.println(getClass().getName() + ": Codebase set to " + codebase);
    }

    /****************************************************************************
   * Attempts to retrieve the requested resource from a jar that's part of the
   * loader list
   *
   * @param String The path descriptor of the resource
   * @return the byte array that is this resource, or null if the resource isn't
   *         found
   */
    protected byte[] getClassFromJarURLs(String s) {
        byte abyte0[] = null;
        URL aurl[] = loader.getURLs();
        byte byte0 = -1;
        for (int j = 0; j < aurl.length && abyte0 == null; j++) {
            String s1 = aurl[j].toString();
            if (FileUtil.isJarFile(s1)) {
                int i = s1.indexOf("file:");
                if (i > -1) {
                    try {
                        String jarfileName = s1.substring(i + 5);
                        abyte0 = FileUtil.getResourceFromJar(s, jarfileName);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                } else {
                    System.out.println(getClass().getName() + ": its a remote url... failure");
                }
            }
        }
        return abyte0;
    }
}
