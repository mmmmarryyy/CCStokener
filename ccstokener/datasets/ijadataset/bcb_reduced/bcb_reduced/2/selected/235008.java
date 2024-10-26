package de.forsthaus.webui.login;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;
import de.forsthaus.webui.util.WindowBaseCtrl;
import de.forsthaus.webui.util.ZksampleDateFormat;

/**
 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++<br>
 * This is the controller class for the /WEB-INF/zkloginDialog.zul file.<br>
 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++<br>
 * 
 * The Login action is defined in the zul-file where the login Button is from
 * html type 'submit'.
 * 
 * @author bbruhns
 * @author sgerth
 */
public class ZkLoginDialogCtrl extends WindowBaseCtrl implements Serializable {

    private static final Logger logger = Logger.getLogger(ZkLoginDialogCtrl.class);

    private static final long serialVersionUID = -71422545405325060L;

    protected Window loginwin;

    protected Label lbl_ServerTime;

    protected Textbox txtbox_Username;

    protected Textbox txtbox_Password;

    protected Button btnReset;

    /**
	 * default constructor. <br>
	 */
    public ZkLoginDialogCtrl() {
        super();
    }

    public void onCreate$loginwin(Event event) throws Exception {
        doOnCreateCommon(this.loginwin);
        this.txtbox_Username.setValue("admin");
        this.txtbox_Password.setValue("admin");
        this.txtbox_Username.focus();
        this.loginwin.setShadow(false);
        this.loginwin.doModal();
    }

    public void onClick$btnReset(Event event) {
        this.txtbox_Username.setValue("");
        this.txtbox_Password.setValue("");
        this.txtbox_Username.focus();
    }

    /**
	 * when the "close" button is clicked. <br>
	 * 
	 * @throws IOException
	 */
    public void onClick$button_ZKLoginDialog_Close() throws IOException {
        Executions.sendRedirect("/j_spring_logout");
    }

    /**
	 * when the "getServerTime" button is clicked. <br>
	 * 
	 * @throws IOException
	 */
    public void onClick$button_ZKLoginDialog_ServerTime() throws IOException {
        final long l = getCurrentHttpTokenTime();
        final String dateStr = ZksampleDateFormat.getDateTimeLongFormater().format(l);
        this.lbl_ServerTime.setMultiline(true);
        this.lbl_ServerTime.setValue("time on synchronization-server:\n" + dateStr);
    }

    /**
	 * Get the actual date/time on server. <br>
	 * Not used at time.<br>
	 * 
	 * @return String of date/time
	 */
    private String getDateTime() {
        return ZksampleDateFormat.getDateTimeLongFormater().format(new Date());
    }

    /**
	 * Get a date/time from a web server for our one-time-password
	 * synchronizing.<br>
	 * <br>
	 * We became our time with calling a PHP Function on a webserver.<br>
	 * This time-Url and time is used only for synchronizing the tokenizer <br>
	 * application on the users PC and the server method for calculate the <br>
	 * user token. So the running user-application must have an internet access. <br>
	 * In the case of non internet connection of the users pc, the tokenizer<br>
	 * takes the time from the users pc clock. So the user can set the pc clock
	 * to the servers time manually.<br>
	 * 
	 * <pre>
	 * File: time.php
	 * --------------
	 * 1. &lt;?php
	 * 2. echo mktime();
	 * 3. ?&gt;
	 * --------------
	 * End-File. = 3 lines
	 * </pre>
	 * 
	 * @return
	 */
    private long getCurrentHttpTokenTime() {
        final String urlString = "http://unixtime.forsthaus.de/time.php";
        try {
            final URL url = new URL(urlString);
            final URLConnection conn = url.openConnection();
            final InputStream istream = conn.getInputStream();
            try {
                final StringBuilder sb = new StringBuilder();
                int ch = -1;
                while ((ch = istream.read()) != -1) {
                    sb.append((char) ch);
                }
                final long l1 = Long.parseLong(sb.toString());
                return l1 * 1000;
            } catch (final NumberFormatException e) {
                throw new RuntimeException(e);
            } finally {
                istream.close();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
