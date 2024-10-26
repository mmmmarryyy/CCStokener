package com.zagile.confluence.plugins.semforms.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Deprecated
public class ZSemanticServletModule extends javax.servlet.http.HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String zOntoJsonApiUrl = getInitParameter("zOntoJsonApiServletUrl");
        URL url = new URL(zOntoJsonApiUrl + "?" + req.getQueryString());
        resp.setContentType("text/html");
        InputStreamReader bf = new InputStreamReader(url.openStream());
        BufferedReader bbf = new BufferedReader(bf);
        String response = "";
        String line = bbf.readLine();
        PrintWriter out = resp.getWriter();
        while (line != null) {
            response += line;
            line = bbf.readLine();
        }
        out.print(response);
        out.close();
    }
}
