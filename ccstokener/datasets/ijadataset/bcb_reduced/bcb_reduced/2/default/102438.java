import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A most simple proxy, or perhaps better a "redirector". It gets an
 * URL as parameter, reads the data from this URL and writes them
 * into the response. This servlet can be used to load a file
 * from an applet, that is not allowed to open a connection
 * to another location.
 */
public class Proxy extends HttpServlet {

    static final int BUFFER_SIZE = 4096;

    /** */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
    *  Write the content of the URL (given in the parameter "URL") to the
    *  response
    */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String load_url = "";
        try {
            load_url = request.getParameter("URL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        URL url = new URL(load_url);
        URLConnection connection = url.openConnection();
        connection.connect();
        response.setContentType(connection.getContentType());
        InputStream in = connection.getInputStream();
        OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        READ: while (true) {
            read = in.read(buffer);
            if (read > 0) {
                out.write(buffer, 0, read);
            } else {
                break READ;
            }
        }
    }

    /**
    * Does the same as doGet
    */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
        doGet(req, resp);
    }

    /**Ressourcen bereinigen*/
    public void destroy() {
    }
}
