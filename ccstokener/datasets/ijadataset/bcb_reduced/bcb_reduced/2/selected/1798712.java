package com.liferay.chat.servlet;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.security.auth.*;
import com.liferay.portal.service.*;
import com.liferay.portal.model.User;
import java.net.*;
import java.io.*;

public class VoicePortletSessionListener implements HttpSessionListener {

    private static final String m_strUrl = "http://localhost:9090/CallSetup/";

    private static Log _log = LogFactoryUtil.getLog(VoicePortletSessionListener.class);

    public void sessionCreated(HttpSessionEvent event) {
        HttpSession ses = event.getSession();
        long userId = GetterUtil.getLong(PrincipalThreadLocal.getName());
        if (userId == 0) return;
        String session = ses.getId();
        if (_log != null) _log.error("Voice: sesion create userId=" + userId + " session " + ses.getId());
        String userName = null;
        try {
            User user = UserLocalServiceUtil.getUserById(userId);
            userName = user.getFullName();
        } catch (Exception e) {
            if (_log != null) _log.error("Voice: getting username: " + e);
            userName = "NA";
        }
        if (!loginUser(userId, session)) {
            createUser(userId, userName, "NA");
            loginUser(userId, session);
        }
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession ses = event.getSession();
        long userId = GetterUtil.getLong(PrincipalThreadLocal.getName());
        logoutUser(ses.getId());
        if (_log != null) _log.error("Voice: sesion destroy userId=" + userId + " session " + ses.getId());
    }

    private boolean loginUser(long userId, String ses) {
        try {
            String data = URLEncoder.encode("USERID", "UTF-8") + "=" + URLEncoder.encode("" + userId, "UTF-8");
            data += "&" + URLEncoder.encode("SESSION", "UTF-8") + "=" + URLEncoder.encode(ses, "UTF-8");
            if (_log != null) _log.error("Voice: loginUser = " + m_strUrl + "LoginUserServlet&" + data);
            URL url = new URL(m_strUrl + "LoginUserServlet");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            line = rd.readLine();
            int result = Integer.parseInt(line);
            wr.close();
            rd.close();
            if (result == 1) {
                return true;
            }
        } catch (Exception e) {
            if (_log != null) _log.error("Voice error : " + e);
        }
        return false;
    }

    private void createUser(long userId, String userName, String userIp) {
        try {
            String data = URLEncoder.encode("USERID", "UTF-8") + "=" + URLEncoder.encode("" + userId, "UTF-8");
            data += "&" + URLEncoder.encode("USERNAME", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8");
            data += "&" + URLEncoder.encode("USERIP", "UTF-8") + "=" + URLEncoder.encode(userIp, "UTF-8");
            if (_log != null) _log.error("Voice: createUser = " + m_strUrl + "CreateUserServlet&" + data);
            URL url = new URL(m_strUrl + "CreateUserServlet");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            wr.close();
            rd.close();
        } catch (Exception e) {
            if (_log != null) _log.error("Voice error : " + e);
        }
    }

    private void logoutUser(String session) {
        try {
            String data = URLEncoder.encode("SESSION", "UTF-8") + "=" + URLEncoder.encode("" + session, "UTF-8");
            if (_log != null) _log.error("Voice: logoutUser = " + m_strUrl + "LogoutUserServlet&" + data);
            URL url = new URL(m_strUrl + "LogoutUserServlet");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            wr.close();
            rd.close();
        } catch (Exception e) {
            if (_log != null) _log.error("Voice error : " + e);
        }
    }
}
