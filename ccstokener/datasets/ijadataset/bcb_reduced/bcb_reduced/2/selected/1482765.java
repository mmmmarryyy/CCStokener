package web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="mailto:czy88840616@gmail.com">czy</a>
 * @since 10-12-13 ����10:19
 */
public class ProxyServlet extends HttpServlet {

    private String ip = "127.0.0.1";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fullUrl = req.getRequestURL().toString();
        if (fullUrl.indexOf(ip) != -1) {
            fullUrl = fullUrl.replaceAll(ip, "a.tbcdn.cn");
        }
        URL url = new URL(fullUrl);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        PrintWriter out = resp.getWriter();
        String line;
        while ((line = in.readLine()) != null) {
            out.println(line);
        }
        in.close();
        out.flush();
    }

    @Override
    public void init() throws ServletException {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/config.properties"));
            if (!properties.isEmpty()) {
                ip = properties.getProperty("ip");
            } else {
            }
        } catch (IOException e) {
        }
    }
}
