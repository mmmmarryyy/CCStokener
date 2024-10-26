package com.bluesky.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        ThreadAttributes.setThreadAttribute("request", req);
        System.out.println("abc=" + request.getParameter("abc"));
        String tokenId = null;
        String userId = "";
        boolean authPass = false;
        if (request.getRequestURI().indexOf("sso-login.jsp") > 0) {
            chain.doFilter(request, response);
            return;
        }
        Cookie[] cookies = request.getCookies();
        for (int i = 0; i < cookies.length; i++) {
            Cookie c = cookies[i];
            if (c.getName().equals("sso_token")) tokenId = c.getValue();
        }
        if (tokenId != null) {
            try {
                String ssoServerUrl = PropertyUtil.getSSOServerUrl();
                URL url = new URL(ssoServerUrl + "/QueryUserServlet?tokenId=" + tokenId + "&ip=" + request.getLocalAddr());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line = in.readLine();
                if (line != null) userId = line;
                if (userId.length() > 0) {
                    request.getSession().setAttribute("userId", userId);
                    authPass = true;
                } else {
                    Cookie c = new Cookie("sso_token", "");
                    c.setMaxAge(0);
                    response.addCookie(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            userId = request.getParameter("userId");
            String password = request.getParameter("password");
            if (userId != null) {
                if (userId.equals(password)) {
                    request.getSession().setAttribute("userId", userId);
                    authPass = true;
                }
            }
        }
        if (authPass) chain.doFilter(request, response); else response.getWriter().println("<html><body>Welcome guest! <br><a href='sso-login.jsp?redirectUrl=" + request.getRequestURL() + "'>login</a></body></html>");
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }
}
