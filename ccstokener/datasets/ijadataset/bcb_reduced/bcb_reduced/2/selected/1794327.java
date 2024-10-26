package com.jdkcn.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
 * @since Sep 26, 2007 9:47:35 PM
 * @version $Id CheckUpdateController.java$
 */
public class CheckUpdateController extends BaseController {

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = "http://jdkcn.com/checkUpdateNew.jsp?ver=" + blogFacade.getDatabaseSiteConfig().getAppVersion();
        response.setCharacterEncoding("UTF-8");
        URLConnection connection = new URL(url).openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line).append("\r\n");
            line = reader.readLine();
        }
        response.getWriter().println(sb.toString());
        return null;
    }
}
