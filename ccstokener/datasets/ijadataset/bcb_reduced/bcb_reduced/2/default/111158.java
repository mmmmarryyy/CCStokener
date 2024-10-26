import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A pure command line interface used to PUT calendars on a repository.
 * 
 * @author Jeremy Gustie
 * @version %I%, %G%
 * @since 1.0
 */
public class HttpPutUtility extends Bootstrap {

    public HttpPutUtility(PrintWriter out, PrintWriter err, BufferedReader in) {
        super(out, err, in);
    }

    private void sendFile(URL url, File file) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.addRequestProperty("Content-Type", "text/xml");
            connection.connect();
            InputStream fileIn = new FileInputStream(file);
            OutputStream urlOut = connection.getOutputStream();
            byte[] buffer = new byte[1024 * 4];
            int count = fileIn.read(buffer);
            while (count >= 0) {
                urlOut.write(buffer, 0, count);
                count = fileIn.read(buffer);
            }
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            fileIn.close();
            urlOut.close();
            connection.disconnect();
            if (responseCode == 200) {
                out.println(file.getName());
            } else {
                err.println(file.getName() + " (" + responseMessage + ")");
            }
        } catch (IOException e) {
            err.println(file.getName() + " (" + e.getMessage() + ")");
        }
    }

    protected String name() {
        return "HTTP PUT Utility";
    }

    protected int run(String[] args) {
        if (args.length == 0) {
            return USAGE;
        }
        String host = "localhost";
        String port = "8080";
        String contextRoot = "/CalendarWeb";
        String servletPath = "/CalendarServlet";
        Collection files = new LinkedList();
        for (int argc = 0; argc < args.length; ++argc) {
            if (args[argc].startsWith("-")) {
                String arg = args[argc];
                if (arg.equals("-h")) {
                    host = args[++argc];
                } else if (arg.equals("-p")) {
                    port = args[++argc];
                } else if (arg.equals("-r")) {
                    contextRoot = args[++argc];
                } else if (arg.equals("-s")) {
                    servletPath = args[++argc];
                } else if (arg.equals("-?")) {
                    return USAGE;
                }
            } else {
                files.add(new File(args[argc]));
            }
        }
        try {
            int portNumber = Integer.parseInt(port);
            if (portNumber < 0 || portNumber > 0xFFFF) {
                message = "Specified port '" + port + "' is out of range";
                return ERROR;
            }
        } catch (NumberFormatException nfe) {
            message = "Non-numeric port specified";
            return ERROR;
        }
        URL url = null;
        try {
            url = new URL("http", host, Integer.parseInt(port), contextRoot + servletPath);
            out.println("Request URL: " + url);
            out.println();
        } catch (MalformedURLException mue) {
            message = "Could not construct URL 'http://" + host + ":" + port + contextRoot + servletPath + "'";
            return ERROR;
        }
        out.println("File (Status)");
        out.println("=============");
        Iterator fileIter = files.iterator();
        while (fileIter.hasNext()) {
            File file = (File) fileIter.next();
            if (!file.exists()) {
                out.print(file.getAbsolutePath());
                out.println(" (File skipped, does not exist)");
                fileIter.remove();
            }
        }
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            sendFile(url, (File) i.next());
        }
        out.println();
        return SUCCESS;
    }

    protected void usage(String leader) {
        out.print(leader);
        out.println(" [-h host] [-p port] [-r root] [-s path] files ...");
        out.println("Options:");
        out.println("    -h  host to connect to (defaults to 'localhost')");
        out.println("    -p  port to connect on (defaults to '8080')");
        out.println("    -r  context root of calendar repository (defaults to '/CalendarWeb')");
        out.println("    -s  path to repository (defaults to '/CalendarServlet')");
        out.println("    -?  display this information");
        out.println("An attempt will be made to add each of the specified files to the repository");
        out.println("using the supplied connection information.");
    }

    protected String version() {
        return "1.0, 05/01/2006";
    }
}
