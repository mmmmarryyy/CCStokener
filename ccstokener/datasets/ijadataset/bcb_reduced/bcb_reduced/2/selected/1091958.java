package org.logtime.monitor.servlet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * 
 * MonitorServlet is a service for check web is running
 * 
 * @version: 1.0
 * @author: sumin
 * @createdate: 2011-11-06
 */
@SuppressWarnings("serial")
public class MonitorServlet extends HttpServlet {

    /**
	 * Init new Thread
	 */
    @Override
    public void init() throws ServletException {
        Thread thread = new Thread() {

            public void run() {
                int send = 0;
                while (true) {
                    String result = check(getInitParameter("checkUrl"), getInitParameter("checkContent"));
                    if ("".equals(result)) {
                        if (send > 0) {
                            sendSMS("server~running~again");
                        }
                        send = 0;
                    } else {
                        if (send <= 2 && sendSMS(result)) {
                            send++;
                        }
                    }
                    try {
                        Thread.sleep(Long.parseLong(getInitParameter("checkWaitMinute")) * 60 * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("check running at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "(" + result + ")[" + send + "]");
                }
            }
        };
        thread.start();
    }

    /**
	 * Check url
	 */
    public String check(String checkUrl, String checkContent) {
        if (checkWebsite("http://www.baidu.com", "About Baidu")) {
            if (!checkWebsite(checkUrl, checkContent)) {
                return "(server)error";
            }
        }
        return "";
    }

    /**
	 * Send sms
	 */
    public boolean sendSMS(String content) {
        try {
            URL url = new URL("http://sms.ceeg.cn/zshsms.asmx/SendSms_empp?Uname=newoa&Upwd=ceegnewoalogon&Mobile=13291281612&Content=" + content);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            reader.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Check a web is ok
	 */
    public boolean checkWebsite(String URL, String content) {
        boolean run = false;
        try {
            URL url = new URL(URL + "?a=" + Math.random());
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.contains(content)) {
                    run = true;
                }
            }
        } catch (Exception e) {
            run = false;
        }
        return run;
    }
}
