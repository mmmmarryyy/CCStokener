package iws.listeOnLine.servlet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Source extends HttpServlet {

    private static final String CONTENT_TYPE = "text/html";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(CONTENT_TYPE);
        URL url;
        URLConnection urlConn;
        DataOutputStream cgiInput;
        url = new URL("http://localhost:8080/ListeOnLine/Target");
        urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        cgiInput = new DataOutputStream(urlConn.getOutputStream());
        String content = "param1=" + URLEncoder.encode("first parameter") + "&param2=" + URLEncoder.encode("the second one...");
        cgiInput.writeBytes(content);
        cgiInput.flush();
        cgiInput.close();
        BufferedReader cgiOutput = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        PrintWriter servletOutput = response.getWriter();
        servletOutput.print("<html><body><h1>This is the Source Servlet</h1><p />");
        String line = null;
        while (null != (line = cgiOutput.readLine())) {
            servletOutput.println(line);
        }
        cgiOutput.close();
        servletOutput.print("</body></html>");
        servletOutput.close();
    }
}
