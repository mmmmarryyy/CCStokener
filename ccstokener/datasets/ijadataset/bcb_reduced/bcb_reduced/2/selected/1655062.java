package wyklad;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class WysylanieArchiwum2 extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    int BUF_LEN = 1024;

    String FILE_NAME = "/test.jar";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/jar");
        byte[] bufor = new byte[BUF_LEN];
        ServletContext context = getServletContext();
        URL url = context.getResource(FILE_NAME);
        InputStream in = url.openStream();
        OutputStream out = response.getOutputStream();
        while (in.read(bufor) != -1) out.write(bufor);
        in.close();
        out.close();
    }
}
