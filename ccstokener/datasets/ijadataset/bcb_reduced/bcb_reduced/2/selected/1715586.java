package jaapy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONString;
import org.mortbay.util.ajax.JSON;
import com.google.gson.Gson;

public class GadgetServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = (String) req.getParameter("id");
        Gson gson = new Gson();
        JaapResult result = gson.fromJson(getSearchResults(id), JaapResult.class);
        if (result.properties.length != 0) {
            req.setAttribute("property", result.properties[0]);
            RequestDispatcher rd = req.getRequestDispatcher("/jaap.jsp");
            rd.forward(req, resp);
        } else {
            RequestDispatcher rd = req.getRequestDispatcher("/gadget/jaap.xml");
            rd.forward(req, resp);
        }
    }

    private String getSearchResults(String id) {
        try {
            final URL url = new URL("http://www.jaap.nl/api/jaapAPI.do?clientId=iPhone&limit=5&request=details&id=" + id + "&format=JSON&field=street_nr&field=zip&field=city&field=price&field=thumb&field=since&field=houseType&field=area&field=rooms&field=id");
            final StringBuilder builder = new StringBuilder();
            final BufferedReader rd = new BufferedReader(new InputStreamReader(url.openStream()));
            String s = "";
            while ((s = rd.readLine()) != null) {
                builder.append(s);
            }
            rd.close();
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
