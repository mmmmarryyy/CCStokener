package com.bluesky.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String tokenId = "";
        String userId = "";
        boolean authPass = false;
        Cookie[] cookies = request.getCookies();
        for (int i = 0; i < cookies.length; i++) {
            Cookie c = cookies[i];
            if (c.getName().equals("sso_token")) tokenId = c.getValue();
        }
        if (tokenId != null) {
            try {
                String ssoServerUrl = PropertyUtil.getSSOServerUrl();
                URL url = new URL(ssoServerUrl + "/QueryUserServlet?tokenId=" + tokenId);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                userId = in.readLine().toString();
                if (userId.length() > 0) {
                    request.getSession().setAttribute("userId", userId);
                    authPass = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            userId = request.getParameter("userId");
            String password = request.getParameter("password");
            if (userId.equals(password)) {
                request.getSession().setAttribute("userId", userId);
                authPass = true;
            }
        }
        if (authPass) {
            response.getWriter().println("Welcome " + userId + "!");
        } else {
            response.getWriter().println("<html><body>Welcome guest! <br><a href='sso-login.jsp'>login</a>'</body></html>");
        }
    }
}
